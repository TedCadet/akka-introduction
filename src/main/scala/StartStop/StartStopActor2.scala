package StartStop

import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object StartStopActor2 {
  def apply(): Behavior[String] = Behaviors.setup(context => new StartStopActor2(context))
}

class StartStopActor2(context: ActorContext[String]) extends AbstractBehavior[String](context) {

  println("second started")

  // no msgs handled by this actor
  override def onMessage(msg: String): Behavior[String] = Behaviors.unhandled

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("second stopped")
      this
  }
}
