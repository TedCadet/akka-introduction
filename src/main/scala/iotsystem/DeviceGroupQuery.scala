package iotsystem

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.FiniteDuration

object DeviceGroupQuery {

  def apply(
             deviceIdToActor: Map[String, ActorRef[Device.Command]],
             requestId: Long,
             requester: ActorRef[DeviceManager.RespondAllTemperatures],
             timeout: FiniteDuration
           ): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new DeviceGroupQuery(deviceIdToActor, requestId, requester, timeout, context, timers)
      }
    }
  }

  // msgs Command
  trait Command

  private case object CollectionTimeout extends Command

  final case class WrappedRespondTemperature(response: Device.RespondTemperature) extends Command

  private final case class DeviceTerminated(deviceId: String) extends Command
}

class DeviceGroupQuery(deviceIdToActor: Map[String, ActorRef[Device.Command]],
                       requestId: Long,
                       requester: ActorRef[DeviceManager.RespondAllTemperatures],
                       timeout: FiniteDuration,
                       context: ActorContext[DeviceGroupQuery.Command],
                       timers: TimerScheduler[DeviceGroupQuery.Command])
  extends AbstractBehavior[DeviceGroupQuery.Command](context) {

  import DeviceGroupQuery._
  import DeviceManager.RespondAllTemperatures
  import DeviceManager.Temperature
  import DeviceManager.TemperatureReading
  import DeviceManager.TemperatureNotAvailable
  import DeviceManager.DeviceNotAvailable
  import DeviceManager.DeviceTimedOut

  context.log.info("query started")
  timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)

  // ActorRef that will receive the RespondTemperature for each device
  // the respond is wrapped in a DeviceGroupQuery.Command
  // is this ref is this actor?
  private val respondTemperatureAdapter = context.messageAdapter(WrappedRespondTemperature.apply)

  private var receivedResponses = Map.empty[String, DeviceManager.TemperatureReading]

  // initiate it with all the ids since they all haven't received everything
  private var stillWaiting = deviceIdToActor.keySet
  
  deviceIdToActor.foreach {
    case (deviceId, device) =>
      context.watchWith(device, DeviceTerminated(deviceId))
      // increment requestId?
      device ! Device.ReadTemperature(0, respondTemperatureAdapter)
  }


  override def onMessage(msg: DeviceGroupQuery.Command): Behavior[DeviceGroupQuery.Command] = {
    msg match {
      case WrappedRespondTemperature(response) => onRespondTemperature(response)
      case DeviceTerminated(deviceId) => onDeviceTerminated(deviceId)
      case CollectionTimeout => onCollectionTimeout()
    }
  }

  def respondAllCollected(): Behavior[Command] = {
    if (stillWaiting.isEmpty) {
      requester ! RespondAllTemperatures(requestId = requestId, temperatures = receivedResponses)
      Behaviors.stopped
    } else {
      this
    }
  }

  def onRespondTemperature(response: Device.RespondTemperature): Behavior[Command] = {
    val reading = response.value match {
      case Some(value) => Temperature(value)
      case None => TemperatureNotAvailable
    }

    val deviceId: String = response.deviceId
    receivedResponses += deviceId -> reading
    stillWaiting -= deviceId

    respondAllCollected()
  }

  def onDeviceTerminated(deviceId: String): Behavior[DeviceGroupQuery.Command] = {
    if (stillWaiting(deviceId)) {
      receivedResponses += deviceId -> DeviceNotAvailable
      stillWaiting -= deviceId
    }
    respondAllCollected()
  }

  def onCollectionTimeout(): Behavior[Command] = {
    receivedResponses ++= stillWaiting.map(deviceId => deviceId -> DeviceTimedOut)
    stillWaiting = Set.empty
    respondAllCollected()
  }
}
