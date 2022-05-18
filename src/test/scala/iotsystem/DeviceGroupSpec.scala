package iotsystem

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import iotsystem.Device.{Passivate, RecordTemperature, TemperatureRecorded}
import iotsystem.DeviceManager.{DeviceRegistered, ReplyDeviceList, RequestAllTemperatures, RequestDeviceList, RequestTrackDevice, RespondAllTemperatures, Temperature, TemperatureNotAvailable}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceGroupSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  // declare actors and probes and the groupId
  val groupId: String = "group"
  val groupActor: ActorRef[DeviceGroup.Command] = spawn(DeviceGroup("group"))
  val registeredProbe: TestProbe[DeviceRegistered] = createTestProbe[DeviceRegistered]()
  val recordProbe: TestProbe[TemperatureRecorded] = createTestProbe[TemperatureRecorded]()
  val listDeviceProbe: TestProbe[ReplyDeviceList] = createTestProbe[ReplyDeviceList]()

  "be able to register a device actor" in {

    // register a device
    val deviceId1 = "device1"
    groupActor ! RequestTrackDevice(groupId, deviceId1, registeredProbe.ref)
    val responseRegistered1 = registeredProbe.receiveMessage()
    val deviceActor1 = responseRegistered1.device

    // register a second device
    val deviceId2 = "device2"
    groupActor ! RequestTrackDevice(groupId, deviceId2, registeredProbe.ref)
    val responseRegistered2 = registeredProbe.receiveMessage()
    val deviceActor2 = responseRegistered2.device

    // assert that they're different
    deviceActor1 should !==(deviceActor2)

    //TODO: seperate the test cases

    // check that the device actors are working
    deviceActor1 ! RecordTemperature(requestId = 1, value = 22, replyTo = recordProbe.ref)
    // check ack received
    recordProbe.expectMessage(TemperatureRecorded(requestId = 1))

    deviceActor2 ! RecordTemperature(requestId = 2, value = 23, replyTo = recordProbe.ref)
    recordProbe.expectMessage(TemperatureRecorded(requestId = 2))
  }

  "ignore requests for wrong groupId" in {
    groupActor ! RequestTrackDevice("wrongGroup", "device1", registeredProbe.ref)
    registeredProbe.expectNoMessage(500.milliseconds)
  }

  "return same actor for same deviceId" in {
    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = "device1", replyTo = registeredProbe.ref)
    val registered1 = registeredProbe.receiveMessage()

    // registering same again should be idempotent
    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = "device1", replyTo = registeredProbe.ref)
    val registered2 = registeredProbe.receiveMessage()

    registered1.device should ===(registered2.device)
  }

  "be able to list active devices" in {
    val device1 = "device1"
    val device2 = "device2"
    val setIds = Set(device1, device2)

    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = device1, replyTo = registeredProbe.ref)
    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = device2, replyTo = registeredProbe.ref)
    groupActor ! RequestDeviceList(groupId = groupId, requestId = 1, replyTo = listDeviceProbe.ref)

    listDeviceProbe.expectMessage(ReplyDeviceList(requestId = 1, setIds))
  }

  "be able to list active devices after one shut down" in {
    val device1 = "device1"
    val device2 = "device2"
    val setIds = Set(device2)

    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = device1, replyTo = registeredProbe.ref)
    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = device2, replyTo = registeredProbe.ref)
    // receive the ref of the first device
    val deviceToShutdown = registeredProbe.receiveMessage().device

    deviceToShutdown ! Passivate
    registeredProbe.expectTerminated(deviceToShutdown, registeredProbe.remainingOrDefault)

    /// using awaitAssert to retry because it might take longer for the groupActor
    // to see the Terminated, that order is undefined
    registeredProbe.awaitAssert {
      groupActor ! RequestDeviceList(groupId = groupId, requestId = 1, replyTo = listDeviceProbe.ref)
      listDeviceProbe.expectMessage(ReplyDeviceList(requestId = 1, ids = setIds))
    }
  }

  "be able to collect temperatures from all active devices" in {
    val device1 = "device1"
    val device2 = "device2"
    val device3 = "device3"
    val AllTempProbe = createTestProbe[RespondAllTemperatures]()

    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = device1, replyTo = registeredProbe.ref)
    val deviceActor1 = registeredProbe.receiveMessage().device

    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = device2, replyTo = registeredProbe.ref)
    val deviceActor2 = registeredProbe.receiveMessage().device

    groupActor ! RequestTrackDevice(groupId = groupId, deviceId = device3, replyTo = registeredProbe.ref)
    val deviceActor3 = registeredProbe.receiveMessage().device

    deviceActor1 ! RecordTemperature(requestId = 0, value = 1, replyTo = recordProbe.ref)
    deviceActor2 ! RecordTemperature(requestId = 1, value = 2, replyTo = recordProbe.ref)

    groupActor ! RequestAllTemperatures(requestId = 0, groupId = groupId, replyTo = AllTempProbe.ref)

    AllTempProbe.expectMessage(RespondAllTemperatures(requestId = 0,
      temperatures = Map(device1 -> Temperature(1), device2 -> Temperature(2), device3 -> TemperatureNotAvailable)))


  }
}
