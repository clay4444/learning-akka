name := """akkademaid-scala"""

version := "1.0"

scalaVersion := "2.11.8"

lazy val akkaVersion = "2.5.16"
lazy val akkaHttpVersion = "10.0.10"


// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
//  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-M4",
//  "com.typesafe.akka" % "akka-http-experimental_2.11" % "1.0-M4",
//  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-M4",
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" % "akka-http-core_2.12" % akkaHttpVersion,
//  "com.akkademy-db"   %% "akkademy-db-scala"     % "0.0.1-SNAPSHOT",
  "com.syncthemall" % "boilerpipe" % "1.2.2",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)
// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

