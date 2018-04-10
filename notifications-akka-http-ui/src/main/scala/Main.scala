/**
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString
import spray.json._
import DefaultJsonProtocol._
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.ws.TextMessage
import notifications.grpc

import scala.io.StdIn
import scala.util.{Failure, Success}

object Main extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val client = NotificationsClient("127.0.0.1", 8081)

  val r = concat(
    path("view" / LongNumber) { id =>
      get {
        concat(
          handleWebSocketMessages(
            Flow.fromSinkAndSource(
              Sink.ignore,
              client.getNotifications(grpc.GetNotificationsRequest(id))
                .map(notification => TextMessage(notification.message)))),
          // serve page from same url as actual websocket if request is not a ws: request
          getFromResource("view.html")
        )
      }
    },

    path(PathEnd) {
      get {
        getFromResource("index.html")
      }
    },
    path("notify") {
      post {
        formFields("targetId".as[Long], "notification") { (id, notification) =>
          onComplete(client.notify(grpc.NotifyRequest(id, 0L, notification))) {
            case Success(_) => redirect(Uri("/"), StatusCodes.SeeOther)
            case Failure(ex) => failWith(ex)
          }
        }
      }
    }
  )

  Http(system).bindAndHandle(r, "127.0.0.1", 8080)
    .foreach(binding => println(s"HTTP server bound to ${binding.localAddress}, enter to kill"))

  StdIn.readLine()
  system.terminate()
}
