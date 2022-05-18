package iotsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import iotsystem.DeviceManager.{DeviceNotAvailable, DeviceTimedOut, RespondAllTemperatures, Temperature, TemperatureNotAvailable}
import org.scalatest.wordspec.AnyWordSpecLike
import iotsystem.Device.Command
import iotsystem.DeviceGroupQuery.WrappedRespondTemperature
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

class DeviceGroupQuerySpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "a DeviceGroupQuery Actor" must {
    val requester = createTestProbe[RespondAllTemperatures]()

    val device1 = createTestProbe[Command]()
    val device2 = createTestProbe[Command]()

    val deviceId1 = "device1"
    val deviceId2 = "device2"

    val deviceIdToActor = Map(deviceId1 -> device1.ref, deviceId2 -> device2.ref)

    val queryActor = spawn(DeviceGroupQuery(deviceIdToActor = deviceIdToActor,
      requestId = 1,
      requester = requester.ref,
      timeout = 3.seconds))

    "return temperature value for working devices" in {
      // deviceGroupQuery will send ReadTemperature msg to the devices when spawned
      // here, we make sure they received that msg
      device1.expectMessageType[Device.ReadTemperature]
      device2.expectMessageType[Device.ReadTemperature]

      // simulate the responses to the queryActor
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0,
        deviceId = deviceId1,
        value = Some(1.0)))
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0,
        deviceId = deviceId2,
        value = Some(2.0)))

      // verify if the requester received all the temperatures
      RespondAllTemperatures(requestId = 1,
        temperatures = Map(deviceId1 -> Temperature(1.0), deviceId2 -> Temperature(2.0)))
    }

    "return TempratureNotAvailable fro devices with no readings" in {
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0,
        deviceId = deviceId1,
        value = None))
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0,
        deviceId = deviceId2,
        value = Some(2.0)))

      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map(deviceId1 -> TemperatureNotAvailable, deviceId2 -> Temperature(2))
        )
      )
    }

    "return DeviceNotAvailable if device stops before answering" in {
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0,
        deviceId = deviceId1,
        value = Some(2.0)))

      device2.stop()

      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map(deviceId1 -> Temperature(2), deviceId2 -> DeviceNotAvailable)
        )
      )
    }

    "return temperature reading even if device stops after answering" in {
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0,
        deviceId = deviceId1,
        value = Some(2.0)))
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0,
        deviceId = deviceId2,
        value = Some(1.0)))

      device2.stop()

      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map(deviceId1 -> Temperature(2), deviceId2 -> Temperature(1))
        )
      )
    }

    "return DeviceTimedOut if device doesn't answer in time" in {
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0,
        deviceId = deviceId1,
        value = Some(2.0)))

      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map(deviceId1 -> Temperature(2), deviceId2 -> DeviceTimedOut)
        )
      )
    }
  }
}
