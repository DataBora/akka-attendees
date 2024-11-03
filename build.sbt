import Dependencies._

ThisBuild / scalaVersion     := "2.13.15"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "rs.expertgroup"
ThisBuild / organizationName := "ExpertGroup"

val akkaVersion = "2.7.1"

lazy val root = (project in file("."))
  .settings(
    name := "actors",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test, 
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
       "ch.qos.logback" % "logback-classic" % "1.4.5",
       "com.github.nscala-time" %% "nscala-time" % "2.34.0"
    )
  )
