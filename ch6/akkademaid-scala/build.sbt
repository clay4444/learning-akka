name := "akkademaid-scala"

organization := "com.akkademy-db"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val akkaVersion = "2.5.16"

lazy val akkaHttpVersion = "10.0.10"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  // akka-contrib 是 akka-cluster 的客户端操作
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.syncthemall" % "boilerpipe" % "1.2.2",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

mappings in (Compile, packageBin) ~= { _.filterNot { case (_, name) =>
  Seq("application.conf").contains(name)
}}

