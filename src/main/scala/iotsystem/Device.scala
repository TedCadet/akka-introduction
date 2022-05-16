package iotsystem

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

object Device {
  sealed trait Command

  // messages
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command

  final case class RespondTemperature(requestId: Long, deviceId: String, value: Option[Double])

  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded]) extends Command

  case object Passivate extends Command

  // message to acknowledge that the temp was processed
  final case class TemperatureRecorded(requestId: Long)

  def apply(groupId: String, deviceId: String): Behavior[Command] = Behaviors.setup(context => new Device(context, groupId, deviceId))
}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String) extends AbstractBehavior[Device.Command](context) {

  import Device._

  context.log.info2("Device actor {}-{} started", groupId, deviceId)
  var lastTemperatureReading: Option[Double] = None

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case ReadTemperature(id, replyTo) =>
        replyTo ! RespondTemperature(id, deviceId, lastTemperatureReading)
        this

      case RecordTemperature(id, value, replyTo) =>
        context.log.info2("Recorded temperature reading {} with {}", value, id)
        lastTemperatureReading = Some(value)
        replyTo ! TemperatureRecorded(id)
        this

      case Passivate =>
        context.log.info("device-{} stopped", deviceId)
        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info2("Device actor {}-{} stopped", groupId, deviceId)
      this
  }
}
