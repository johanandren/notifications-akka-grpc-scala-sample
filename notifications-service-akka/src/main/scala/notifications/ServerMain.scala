/**
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
package notifications

import java.io.FileInputStream
import java.nio.file.{Files, Paths}
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, KeyStore, SecureRandom}
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http2, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import io.grpc.internal.testing.TestUtils
import javax.net.ssl.{KeyManagerFactory, SSLContext}

import scala.concurrent.Future
import scala.io.StdIn

object ServerMain extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val service: HttpRequest => Future[HttpResponse] =
    grpc.NotificationServiceApiHandler(new NotificationService(system, materializer))

  Http2().bindAndHandleAsync(
    service,
    interface = "127.0.0.1",
    port = 8081,
    httpsContext = serverHttpContext()
  )
  .foreach { binding =>
    println(s"GRPC server bound to: ${binding.localAddress}, enter to terminate")
  }

  StdIn.readLine()
  system.terminate()

  private def serverHttpContext(): HttpsConnectionContext = {
    val keyEncoded = new String(Files.readAllBytes(Paths.get(TestUtils.loadCert("server1.key").getAbsolutePath)), "UTF-8")
      .replace("-----BEGIN PRIVATE KEY-----\n", "")
      .replace("-----END PRIVATE KEY-----\n", "")
      .replace("\n", "")

    val decodedKey = Base64.getDecoder.decode(keyEncoded)

    val spec = new PKCS8EncodedKeySpec(decodedKey)

    val kf = KeyFactory.getInstance("RSA")
    val privateKey = kf.generatePrivate(spec)

    val fact = CertificateFactory.getInstance("X.509")
    val is = new FileInputStream(TestUtils.loadCert("server1.pem"))
    val cer = fact.generateCertificate(is)

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry("private", privateKey, Array.empty, Array(cer))

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)

    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

    new HttpsConnectionContext(context)
  }

}
