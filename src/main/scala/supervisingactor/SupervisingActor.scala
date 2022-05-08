package supervisingactor

import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object SupervisingActor {
  def apply(): Behavior[String] = Behaviors.setup(
    context => new SupervisingActor(context)
  )
}

class SupervisingActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {

  private val child = context.spawn(Behaviors.supervise(
    SupervisedActor()).onFailure(SupervisorStrategy.restart),
    "supervised-actor")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "failChild" =>
        child ! "fail"
        this
    }
}



