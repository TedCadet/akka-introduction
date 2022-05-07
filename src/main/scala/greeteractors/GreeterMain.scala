package greeteractors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

/**
 * guardian actor. it will bootstrap all the other actors of the application
 */
object GreeterMain {
  // message
  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] = {
    // setup bootstrap the application
    Behaviors.setup { context =>
      // create greeter actor
      val greeter = context.spawn(Greeter(), "greeter")

      Behaviors.receiveMessage { message =>
        // create greeterBot actor
        val replyTo = context.spawn(GreeterBot(max = 3), message.name)
        greeter ! Greeter.Greet(message.name, replyTo)
        Behaviors.same
      }
    }
  }
}
