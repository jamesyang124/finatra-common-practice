name := "finatra-integrations"

version := "1.0"

scalaVersion := "2.12.2"

lazy val noPublishSettings = Seq(
  publish := ((): Unit),
  publishLocal := ((): Unit),
  publishArtifact := false
)

lazy val micrositeSettings = Seq(
  micrositeName := "finatra integrations document",
  micrositeDescription := "A document for integrating grafter, docker-it-scala, and common practice with finatra 2.11",
  micrositeBaseUrl := "finatra-integrations",
  micrositeDocumentationUrl := "/finatra-integrations/docs/",
  micrositeGithubOwner := "jamesyang124",
  micrositeGithubRepo := "finatra-integrations",
  micrositeGitterChannel := false,
  micrositeGithubToken := Option(System.getenv().get("GITHUB_TOKEN")),
  micrositePushSiteWith := GitHub4s
)

lazy val docs = (project in file("docs"))
  .settings(moduleName := "docs")
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(TutPlugin)
