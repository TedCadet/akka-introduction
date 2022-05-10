package iotsystem

import akka.actor.typed.ActorSystem

object IotApp extends App {
  ActorSystem[Nothing](IotSupervisor(), "iot-system")
}
