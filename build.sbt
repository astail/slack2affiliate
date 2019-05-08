organization := "net.astail"

name := "slack2affiliate"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.3",
  "com.github.slack-scala-client" %% "slack-scala-client" % "0.2.6",
  "ch.qos.logback" % "logback-classic" % "1.2.1",
  "org.slf4j" % "slf4j-api" % "1.7.12"
)

enablePlugins(JavaAppPackaging)
