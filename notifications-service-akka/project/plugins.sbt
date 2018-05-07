// these two are needed until we publish to maven central
// plugin + dependencies
resolvers += Resolver.bintrayIvyRepo(
  "akka",
  "sbt-plugin-releases")
resolvers += Resolver.bintrayRepo(
  "akka",
  "maven")


addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "0.1")

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")