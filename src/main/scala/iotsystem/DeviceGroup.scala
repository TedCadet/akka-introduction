package iotsystem

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, LoggerOps}
import iotsystem.DeviceManager.RequestAllTemperatures

import scala.concurrent.duration.DurationInt

object DeviceGroup {

  trait Command

  // messages
  private final case class DeviceTerminated(device: ActorRef[Device.Command], groupId: String, deviceId: String) extends Command

  case object Passivate extends Command

  def apply(groupId: String): Behavior[Command] = Behaviors.setup(context => new DeviceGroup(context, groupId))
}

class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
  extends AbstractBehavior[DeviceGroup.Command](context) {

  import DeviceGroup._
  import DeviceManager.{DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice}

  context.log.info("DeviceGroup {} started", groupId)

  private val timeout = 3.seconds

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  def onRequestAllTemperatures(requestId: Long,
                               gId: String,
                               replyTo: ActorRef[DeviceManager.RespondAllTemperatures]): Behavior[Command] = {
    if (gId.equals(groupId)) {
      context.spawnAnonymous(DeviceGroupQuery(deviceIdToActor,
        requestId = requestId,
        requester = replyTo,
        timeout = timeout))
      this
    } else {
      Behaviors.unhandled
    }
  }

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      // cas ou on recoit pas de groupId?
      // @ est egal a as(alias)
      case trackMsg@RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {

          // si le device qui request est dans le map deviceIdActor
          // renvoie un msg DeviceRegistered avec le deviceActor matchant le id au deviceManager
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)

          // s'il n'est pas present, cree et renvoie un deviceActor au deviceManager
          case None =>
            context.log.info("Creating device actor for {}", trackMsg.deviceId)

            // spawn un nouvel actor
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")

            // surveil au cas qu'il est Terminated
            context.watchWith(deviceActor, DeviceTerminated(device = deviceActor.ref, groupId = groupId, deviceId = deviceId))

            // rajoute dans la map un Key->Value ( deviceId: String, deviceActor: ActorRef[device.Command])
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      // surement cas ou on recois  un groupId different que le groupId de cet acteur
      case RequestTrackDevice(gId, _, _) =>
        context.log.info2("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
        this

      // cas ou on recoit un request pour la liste des devices et que le groupeId match celuide cet acteur
      case RequestDeviceList(rId, `groupId`, replyTo) =>
        context.log.info("sending the ids to {}", replyTo)
        replyTo ! ReplyDeviceList(requestId = rId, ids = deviceIdToActor.keySet)
        this

      // cas ou le groupeId ne match pas celui de "cet" acteur
      case RequestDeviceList(_, gId, _) =>
        Behaviors.unhandled

      case RequestAllTemperatures(requestId, groupId, replyTo) => onRequestAllTemperatures(requestId, groupId, replyTo)

      // cas ou un device est arrete
      case DeviceTerminated(_, _, dId) =>
        context.log.info(" device {} has been ternminated", dId)
        deviceIdToActor -= dId
        this

      case Passivate =>
        context.log.info("group-{} terminated", groupId)
        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("deviceGroup {} stopped", groupId)
      this
  }
}
