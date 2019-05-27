name := "twitter-trends"

version := "0.1"

scalaVersion := "2.12.4"

val akkaVersion = "2.5.20"
val akkaHttpVersion = "10.1.7"
val scalaTestVersion = "3.0.5"

libraryDependencies ++= Seq(
   "com.typesafe.akka" %% "akka-stream" % akkaVersion,
   "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
   "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
   "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
   "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
   "org.scalatest" %% "scalatest" % scalaTestVersion
)