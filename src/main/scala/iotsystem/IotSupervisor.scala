package iotsystem

import akka.NotUsed
import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object IotSupervisor {
  //  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing](
  //    context => new IotSupervisor(context)
  //  )
  def apply(): Behavior[NotUsed] = Behaviors.setup {
    (context) => {
      context.log.info("IoT Application started")

      Behaviors.receiveSignal {
        case (context, PostStop) =>
          context.log.info("Iot Application stopped")
          Behaviors.same
      }
    }
  }
}

//class IotSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
//  context.log.info("IoT Application started")
//
//  override def onMessage(msg: Nothing): Behavior[Nothing] = {
//    // No need to handle any messages
//    Behaviors.unhandled
//  }
//
//  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
//    case PostStop =>
//      context.log.info("Iot Application stopped")
//      this
//  }
//
//}
