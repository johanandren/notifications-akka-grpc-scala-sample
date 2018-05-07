enablePlugins(AkkaGrpcPlugin, JavaAgent)

// needed until we publish to maven central
resolvers += Resolver.bintrayRepo(
  "akka",
  "maven")


scalaVersion := "2.12.4"

// ALPN agent needed to do HTTP2
javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.7" % "runtime"

fork in run := true
connectInput in run := true

akkaGrpcGeneratedSources := Seq(AkkaGrpc.Server)

libraryDependencies ++= Seq(
  // To get access to some dummy cert etc
  "io.grpc" % "grpc-testing" % "1.11.0"
)