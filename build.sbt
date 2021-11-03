enablePlugins(JavaAppPackaging)

scalaVersion := "2.13.7"

name := "codacy-metrics-rubocop"

libraryDependencies ++= Seq("com.codacy" %% "codacy-metrics-scala-seed" % "0.3.1")
