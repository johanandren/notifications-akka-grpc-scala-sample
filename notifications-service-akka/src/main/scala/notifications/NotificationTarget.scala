/**
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
package notifications

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}


object NotificationTarget {
  case class Notify(from: Long, message: String)

  case class Notification(id: String, timestamp: Instant, from: Long, message: String)
  case object Subscribe

  def props() = Props(new NotificationTarget)
}

class NotificationTarget extends Actor with ActorLogging {
  import NotificationTarget._

  var unseenNotifications: List[Notification] = Nil
  var currentSubscriptions: List[ActorRef] = Nil

  def receive: Receive = {
    case Notify(from, message) =>
      log.info(s"Notification from $from: $message")
      val notification = Notification(nextId, Instant.now(), from, message)
      if (currentSubscriptions.isEmpty) {
        unseenNotifications = notification :: unseenNotifications
      } else {
        currentSubscriptions.foreach(_ ! notification)
      }

    case Subscribe =>
      log.info(s"Subscriber added")
      val newSubscriber = sender()
      context.watch(newSubscriber)
      currentSubscriptions = newSubscriber :: currentSubscriptions
      unseenNotifications.foreach(n => newSubscriber ! n)
      unseenNotifications = Nil


    case Terminated(subscriber) =>
      log.info(s"Subscriber stopped")
      currentSubscriptions = currentSubscriptions.filterNot(_ == subscriber)

  }


  def nextId: String = UUID.randomUUID().toString
}
