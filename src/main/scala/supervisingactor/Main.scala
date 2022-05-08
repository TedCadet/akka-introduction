package supervisingactor

import akka.actor.typed.ActorSystem

object Main extends App {
  val supervisingActor = ActorSystem[String](SupervisingMain(), "supervising-main")
  supervisingActor ! "start"
}
