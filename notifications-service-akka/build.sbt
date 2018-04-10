enablePlugins(AkkaGrpcPlugin, JavaAgent)

scalaVersion := "2.12.4"

// ALPN agent needed to do HTTP2
javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.7" % "runtime"

fork in run := true
connectInput in run := true

libraryDependencies ++= Seq(
  // To get access to some dummy cert etc
  "io.grpc" % "grpc-testing" % "1.11.0"
)