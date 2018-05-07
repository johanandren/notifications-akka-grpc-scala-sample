/**
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
package notifications

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}

import scala.concurrent.Future

final class NotificationServiceImpl(system: ActorSystem, mat: Materializer) extends grpc.NotificationService {

  private val notificationSupervisor = system.actorOf(NotificationSupervisor.props(), "notification-supervisor")
  (1 to 10).foreach(n =>
    notificationSupervisor ! NotificationSupervisor.CreateNotificationTarget(n.toLong)
  )

  def notify(in: grpc.NotifyRequest): Future[grpc.NotifyResult] = {
    system.actorSelection(pathFor(in.recipientId)) ! NotificationTarget.Notify(in.senderId, in.message)
    Future.successful(new grpc.NotifyResult())
  }

  def getNotifications(in: grpc.GetNotificationsRequest): Source[grpc.Notification, NotUsed] = {
    Source.actorRef[NotificationTarget.Notification](bufferSize = 10, overflowStrategy = OverflowStrategy.fail)
      .map(notification =>
        new grpc.Notification(notification.timestamp.toEpochMilli, notification.from, notification.message)
      )
      .mapMaterializedValue { sourceRef =>
        // subscribe to actor when materialized
        system.actorSelection(pathFor(in.recipientId)).tell(NotificationTarget.Subscribe, sourceRef)
        NotUsed
      }
  }

  private def pathFor(id: Long): String = s"/user/notification-supervisor/$id"
}
