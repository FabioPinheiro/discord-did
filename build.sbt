// val scala3Version = "3.7.2"
inThisBuild(
  Seq(
    scalaVersion := "3.3.7", // Also update docs/publishWebsite.sh and any ref to scala-3.3.6
  )
)

resolvers += "jitpack" at "https://jitpack.io"

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
    // ### https://docs.scala-lang.org/scala3/guides/migration/options-new.html
    // ### https://docs.scala-lang.org/scala3/guides/migration/options-lookup.html
    scalacOptions ++=
      Seq("-encoding", "UTF-8") ++ // source files are in UTF-8
        // Seq("-explain") ++ // Compile error Explanation
        Seq( // run all/Test/compile before each commit
          "-source",
          "future", // TODO user '3.9' // Note 3.10 will start to remove scala 2 logic like implicits
        ) ++ // preparation for scala 3.9
        Seq(
          "-deprecation", // warn about use of deprecated APIs
          "-unchecked", // warn about unchecked type parameters
          "-feature", // warn about misused language features (Note we are using 'language:implicitConversions')
          "-explain-cyclic",
          // TODO "-Yexplicit-nulls",
          // "-Ysafe-init", // https://dotty.epfl.ch/docs/reference/other-new-features/safe-initialization.html
          "-language:implicitConversions", // we can use with the flag '-feature'
        ) ++ {
          // https://docs.scala-lang.org/scala3/guides/migration/tooling-syntax-rewriting.html
          // scalac -help
          if (true) Seq("-Xfatal-warnings")
          else Seq("-rewrite", "-source", "future-migration") // preparation for scala 3.9 and 3.10
        }
  )
)

lazy val V = new {
  val scalajsJavaSecureRandom = "1.0.0"
  val munit = "1.1.1"
  val zio = "2.1.24"
  val zioHttp = "3.4.1"
  val zioConfig = "4.0.6"
  val jda = "6.3.0" // "5.6.1"
  val scalaDID = "0.1.0-M35"
}
lazy val D = new {
  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val ziohttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp)
  val zioConfig = Def.setting("dev.zio" %% "zio-config" % V.zioConfig)
  val zioConfigMagnolia = Def.setting("dev.zio" %% "zio-config-magnolia" % V.zioConfig) // For deriveConfig
  val zioConfigTypesafe = Def.setting("dev.zio" %% "zio-config-typesafe" % V.zioConfig) // For HOCON
  val jda = Def.setting("net.dv8tion" % "JDA" % V.jda)
  val qrcode = Def.setting("com.github.kenglxn.QRGen" % "javase" % "3.0.1")
  val did = Def.setting("app.fmgp" %% "did" % V.scalaDID)
  val didImp = Def.setting("app.fmgp" %% "did-imp" % V.scalaDID)
  val didFramework = Def.setting("app.fmgp" %% "did-framework" % V.scalaDID)
  // val didUniresolver = Def.setting("app.fmgp" %% "did-uniresolver" % V.scalaDID)
  val didProtocols = Def.setting("app.fmgp" %% "did-comm-protocols" % V.scalaDID)
  val didPrism = Def.setting("app.fmgp" %% "did-method-prism" % V.scalaDID)
  val didPeer = Def.setting("app.fmgp" %% "did-method-peer" % V.scalaDID)
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "discord-did",
    libraryDependencies += D.zio.value,
    libraryDependencies += D.ziohttp.value,
    libraryDependencies += D.jda.value,
    libraryDependencies += D.qrcode.value,
    libraryDependencies ++= Seq(
      D.zioConfig.value,
      D.zioConfigMagnolia.value,
      D.zioConfigTypesafe.value,
      D.did.value,
      D.didImp.value,
      D.didFramework.value,
      // D.didUniresolver.value,
      D.didProtocols.value,
      D.didPrism.value,
      D.didPeer.value,
    ),
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.4" % Test,
    // fork := true,
  )
