import actors.GreeterMain
import actors.GreeterMain.SayHello
import akka.actor.typed.ActorSystem

object AkkaQuickstart extends App {
  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "akkaQuickStart")

  // send main message
  greeterMain ! SayHello("Charles")
}
