package iotsystem

import akka.NotUsed
import akka.actor.typed.ActorSystem

object IotApp extends App {
  ActorSystem[NotUsed](IotSupervisor(), "iot-system")
}
