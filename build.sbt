ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "AkkaAssignment"
  )
val akkaVersion="2.6.19"

libraryDependencies ++=Seq (
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.2.12",
  "org.apache.poi" % "poi" % "5.2.2",
  "org.apache.poi" % "poi-ooxml" % "5.2.2",
  "au.com.bytecode" % "opencsv" % "2.4"
)