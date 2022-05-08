package supervisingactor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object SupervisingMain {
  def apply(): Behavior[String] = Behaviors.setup {
    context =>
      val supervisingActor = context.spawn(SupervisingActor(), "supervising-actor")
      supervisingActor ! "failChild"
      Behaviors.same
  }


}
