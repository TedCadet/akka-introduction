package iotsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  import Device._

  "Device actor" must {
    // device actor
    val deviceActor = spawn(Device("group", "device"))
    // probes
    val readProbe = createTestProbe[RespondTemperature]()
    val recordProbe = createTestProbe[TemperatureRecorded]()

    "reply with empty reading if no temperature is known" in {
      deviceActor ! Device.ReadTemperature(requestId = 42, replyTo = readProbe.ref)
      val responseEmpty = readProbe.receiveMessage()

      responseEmpty.requestId should ===(42)
      responseEmpty.value should ===(None)
    }

    "reply with latest temperature reading" in {
      // record a temp
      deviceActor ! Device.RecordTemperature(requestId = 1, value = 24.0, replyTo = recordProbe.ref)
      // assert it was acknowledge
      recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 1))

      // read the temp
      deviceActor ! Device.ReadTemperature(requestId = 2, replyTo = readProbe.ref)
      val responseWithReading1 = readProbe.receiveMessage()

      // assert the value
      responseWithReading1.requestId should ===(2)
      responseWithReading1.value should ===(Some(24.0))

      // record a new temp
      deviceActor ! Device.RecordTemperature(requestId = 3, value = 22, replyTo = recordProbe.ref)
      // assert it was acknowledge
      recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 3))

      // read the new temp
      deviceActor ! Device.ReadTemperature(requestId = 4, replyTo = readProbe.ref)
      val responseReadingNewTemp = readProbe.receiveMessage()
      // assert the new temp
      responseReadingNewTemp.requestId should ===(4)
      responseReadingNewTemp.value should ===(Some(22))
    }
  }

}
