/**
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
package notifications

import akka.actor.{Actor, Props}
import notifications.NotificationSupervisor.CreateNotificationTarget

object NotificationSupervisor {
  case class CreateNotificationTarget(id: Long)

  def props(): Props = Props(new NotificationSupervisor)
}

class NotificationSupervisor extends Actor {

  def receive = {
    case CreateNotificationTarget(id) =>
      val name = id.toString
      if (context.child(name).isEmpty) {
        context.actorOf(NotificationTarget.props(), id.toString)
      }
  }

}
