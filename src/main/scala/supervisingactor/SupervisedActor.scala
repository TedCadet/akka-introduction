package supervisingactor

import akka.actor.typed.{Behavior, PostStop, PreRestart, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object SupervisedActor {
  def apply(): Behavior[String] = Behaviors.setup(
    context => new SupervisedActor(context)
  )
}

class SupervisedActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("supervised actor started")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "fail" =>
        println("supervised actor fails now")
        throw new Exception("I failed!")
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PreRestart =>
      println("supervised actor will be restarted")
      this
    case PostStop =>
      println("supervised actor stopped")
      this
  }

}
