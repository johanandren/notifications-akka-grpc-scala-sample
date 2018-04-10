/**
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
import akka.actor.ActorSystem
import akka.stream.Materializer
import io.grpc.CallOptions
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.shaded.io.grpc.netty.{GrpcSslContexts, NegotiationType, NettyChannelBuilder}
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext

object NotificationsClient {


  def apply(host: String, port: Int)(implicit system: ActorSystem, materializer: Materializer): notifications.grpc.NotificationServiceApiClient = {

    val sslContext: SslContext =
      GrpcSslContexts
        .forClient
        .trustManager(TestUtils.loadCert("ca.pem"))
        .build()

    system.log.info(s"Connecting GRPC client to $host:$port")
    val channelBuilder =
      NettyChannelBuilder
        .forAddress(host, port)
        .flowControlWindow(65 * 1024)
        .negotiationType(NegotiationType.TLS)
        .sslContext(sslContext)

    channelBuilder.overrideAuthority("foo.test.google.fr")

    val channel = channelBuilder.build()
    import system.dispatcher

    val callOptions = CallOptions.DEFAULT

    new notifications.grpc.NotificationServiceApiClient(channel, callOptions)
  }
}
