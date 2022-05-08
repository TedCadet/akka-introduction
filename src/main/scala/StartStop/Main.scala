package StartStop

import akka.actor.typed.ActorSystem

object Main extends App {
  val startStopMain: ActorSystem[String] = ActorSystem(StartStopMain(), "StartStopMain")
  startStopMain ! "stop"
}
