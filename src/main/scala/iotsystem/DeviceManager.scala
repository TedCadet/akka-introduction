package iotsystem

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import iotsystem.Device.Command

object DeviceManager {
  def apply(): Behavior[Command] = Behaviors.setup(context => new DeviceManager(context))

  // Commands msgs
  sealed trait Command

  final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered])
    extends DeviceManager.Command with DeviceGroup.Command

  final case class DeviceRegistered(device: ActorRef[Device.Command])

  final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList])
    extends DeviceManager.Command with DeviceGroup.Command

  final case class ReplyDeviceList(requestId: Long, ids: Set[String])

  final case class RequestGroupList(requestId: Long, replyTo: ActorRef[ReplyGroupList])
    extends DeviceManager.Command

  final case class ReplyGroupList(requestId: Long, ids: Set[String])

  private final case class DeviceGroupTerminated(groupId: String) extends DeviceManager.Command

  //  private final case class GroupTerminated(device: )

  final case class RequestAllTemperatures(requestId: Long, groupId: String, replyTo: ActorRef[RespondAllTemperatures])
    extends DeviceGroup.Command
      with DeviceManager.Command
  //    with DeviceGroupQuery.Command

  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  // TemperatureReading msgs
  sealed trait TemperatureReading

  final case class Temperature(value: Double) extends TemperatureReading

  case object TemperatureNotAvailable extends TemperatureReading

  case object DeviceNotAvailable extends TemperatureReading

  case object DeviceTimedOut extends TemperatureReading
}

class DeviceManager(context: ActorContext[DeviceManager.Command]) extends AbstractBehavior[DeviceManager.Command](context) {

  import DeviceManager._

  context.log.info("Device Manager started")

  var groupIdsToActor = Map.empty[String, ActorRef[DeviceGroup.Command]]

  override def onMessage(msg: DeviceManager.Command): Behavior[DeviceManager.Command] =
    msg match {
      case trackMsg@RequestTrackDevice(groupId, _, _) =>
        groupIdsToActor.get(groupId) match {
          case Some(group) =>
            group ! trackMsg

          case None =>
            context.log.info("Creating device group actor for {}", groupId)
            val newGroup = context.spawn(DeviceGroup(groupId), "group-" + groupId)
            groupIdsToActor += groupId -> newGroup
            newGroup ! trackMsg
        }
        this

      case listDeviceMsg@RequestDeviceList(requestId, groupId, replyTo) =>
        groupIdsToActor.get(groupId) match {
          case Some(group) =>
            group ! listDeviceMsg
          case None =>
            replyTo ! ReplyDeviceList(requestId, ids = Set.empty)
        }
        this

      case RequestGroupList(requestId, ref) =>
        context.log.info("sending the group list")
        ref ! ReplyGroupList(requestId = requestId, ids = groupIdsToActor.keySet)
        this

      case DeviceGroupTerminated(groupId) =>
        context.log.info("device group actor for {} has been terminated", groupId)
        groupIdsToActor -= groupId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[DeviceManager.Command]] = {
    case PostStop =>
      context.log.info("DeviceManager stopped")
      Behaviors.stopped
  }
}
