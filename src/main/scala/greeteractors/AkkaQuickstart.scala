package greeteractors

import akka.actor.typed.ActorSystem
import greeteractors.GreeterMain.SayHello

object AkkaQuickstart extends App {
  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "akkaQuickStart")

  // send main message
  greeterMain ! SayHello("Charles")
}
