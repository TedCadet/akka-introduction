package StartStop

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object StartStopMain {
  def apply(): Behavior[String] = Behaviors.setup {
    context =>
      val first = context.spawn(StartStopActor1(), "StartStopActor1")
      first ! "stop"
      Behaviors.same
  }


}
