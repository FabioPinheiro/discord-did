// val scala3Version = "3.7.2"
inThisBuild(
  Seq(
    scalaVersion := "3.3.6", // Also update docs/publishWebsite.sh and any ref to scala-3.3.6
  )
)

inThisBuild(
  Seq(
    Test / publishArtifact := false,
    // pomIncludeRepository := (_ => false),
    organization := "app.fmgp",
    homepage := Some(url("https://github.com/FabioPinheiro/discord-did")),
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/FabioPinheiro/scala-did"),
        "scm:git:git@github.com:FabioPinheiro/scala-did.git"
      )
    ),
    developers := List(
      Developer("FabioPinheiro", "Fabio Pinheiro", "fabiomgpinheiro@gmail.com", url("http://fmgp.app"))
    ),
    versionScheme := Some("early-semver"), // https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme
  )
)

lazy val V = new {
  val scalajsJavaSecureRandom = "1.0.0"
  val munit = "1.1.1"
  val zio = "2.1.20"
  val jda = "5.6.1"
}
lazy val D = new {
  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val jda = Def.setting("net.dv8tion" % "JDA" % V.jda)
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "discord-did",
    libraryDependencies += D.zio.value,
    libraryDependencies += D.jda.value,
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
    // fork := true,
  )
