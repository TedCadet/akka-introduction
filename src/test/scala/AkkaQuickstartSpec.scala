import greeteractors.Greeter
import greeteractors.Greeter.{Greet, Greeted}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class AkkaQuickstartSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A Greeter" must {
    "reply to greeted" in {
      val underTest = spawn(Greeter())
      val replyProbe = createTestProbe[Greeted]()

      underTest ! Greet("Santa", replyProbe.ref)
      replyProbe.expectMessage(Greeted("Santa", underTest.ref))
    }
  }
}
