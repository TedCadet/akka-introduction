package iotsystem

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import iotsystem.DeviceManager.{DeviceRegistered, ReplyDeviceList, ReplyGroupList, RequestDeviceList, RequestGroupList, RequestTrackDevice}
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  val managerActor: ActorRef[DeviceManager.Command] = spawn(DeviceManager())
  val registeredProbe: TestProbe[DeviceRegistered] = createTestProbe[DeviceRegistered]()
  val replyDeviceProbe: TestProbe[ReplyDeviceList] = createTestProbe[ReplyDeviceList]()
  val replyGroupProbe: TestProbe[ReplyGroupList] = createTestProbe[ReplyGroupList]()
  val group1: String = "group1"
  val group2: String = "group2"
  val device1: String = "device1"
  val device2: String = "device2"

  "be able to register differents devices actor" in {
    managerActor ! RequestTrackDevice(groupId = group1, deviceId = device1, replyTo = registeredProbe.ref)
    val deviceOne = registeredProbe.receiveMessage()

    managerActor ! RequestTrackDevice(groupId = group1, deviceId = device2, replyTo = registeredProbe.ref)
    val deviceTwo = registeredProbe.receiveMessage()

    deviceOne should !==(deviceTwo)
  }

  "return same actor for same deviceId" in {
    managerActor ! RequestTrackDevice(groupId = group1, deviceId = device1, replyTo = registeredProbe.ref)
    val deviceOne = registeredProbe.receiveMessage()

    managerActor ! RequestTrackDevice(groupId = group1, deviceId = device1, replyTo = registeredProbe.ref)
    val deviceTwo = registeredProbe.receiveMessage()

    deviceOne should ===(deviceTwo)
  }

  "be able to list active groups" in {
    val ids = Set(group1, group2)
    managerActor ! RequestTrackDevice(groupId = group1, deviceId = device1, replyTo = registeredProbe.ref)
    managerActor ! RequestTrackDevice(groupId = group2, deviceId = device2, replyTo = registeredProbe.ref)

    managerActor ! RequestGroupList(requestId = 1, replyTo = replyGroupProbe.ref)
    replyGroupProbe.expectMessage(ReplyGroupList(1, ids))
  }

  "be able to list active devices for a group" in {
    val device3 = "device3"
    val device4 = "device4"

    managerActor ! RequestTrackDevice(groupId = group1, deviceId = device1, replyTo = registeredProbe.ref)
    managerActor ! RequestTrackDevice(groupId = group1, deviceId = device2, replyTo = registeredProbe.ref)
    managerActor ! RequestTrackDevice(groupId = group2, deviceId = device3, replyTo = registeredProbe.ref)
    managerActor ! RequestTrackDevice(groupId = group2, deviceId = device4, replyTo = registeredProbe.ref)

    managerActor ! RequestDeviceList(requestId = 1, groupId = group1, replyTo = replyDeviceProbe.ref)
    replyDeviceProbe.expectMessage(ReplyDeviceList(1, Set(device1, device2)))

    managerActor ! RequestDeviceList(requestId = 2, groupId = group2, replyTo = replyDeviceProbe.ref)
    replyDeviceProbe.expectMessage(ReplyDeviceList(2, Set(device3, device4)))

  }
}
