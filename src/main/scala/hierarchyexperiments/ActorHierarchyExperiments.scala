package hierarchyexperiments

import akka.actor.typed.ActorSystem

object ActorHierarchyExperiments extends App {
  val testSystem = ActorSystem(Main(), "testSystem")
  testSystem ! "start"
}
