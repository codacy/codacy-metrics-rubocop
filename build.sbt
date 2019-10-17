import com.typesafe.sbt.packager.docker.{Cmd, DockerAlias}

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
organization := "com.codacy"
scalaVersion := "2.13.1"
name := "codacy-metrics-rubocop"
// App Dependencies
libraryDependencies ++= Seq("com.codacy" %% "codacy-metrics-scala-seed" % "0.2.0",
                            "org.specs2" %% "specs2-core" % "4.8.0" % Test)

mappings in Universal ++= {
  (resourceDirectory in Compile).map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value ++ Seq(
  (file("Gemfile"), "/setup/Gemfile"),
  (file("Gemfile.lock"), "/setup/Gemfile.lock"),
  (file(".rubocop-version"), "/setup/.rubocop-version")
)

Docker / packageName := packageName.value
dockerBaseImage := "openjdk:8-jre-alpine"
Docker / daemonUser := "docker"
Docker / daemonGroup := "docker"
dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}")
val installAll: String =
  s"""apk add --no-cache bash ruby ruby-dev ruby-irb ruby-rake ruby-io-console ruby-bigdecimal
     |ruby-json ruby-bundler libstdc++ tzdata bash ca-certificates libc-dev gcc make
     |&& echo 'gem: --no-document' > /etc/gemrc
     |&& cd setup
     |&& gem install bundler
     |&& bundle install
     |&& gem cleanup""".stripMargin.replaceAll(System.lineSeparator(), " ")
dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("ADD", _) =>
    List(Cmd("RUN", "adduser -u 2004 -D docker"),
         cmd,
         Cmd("RUN", installAll),
         Cmd("RUN", "mv /opt/docker/docs /docs"))

  case other => List(other)
}
