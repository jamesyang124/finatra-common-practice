name := "finatra-common-practice"

version := "1.0"

scalaVersion := "2.12.2"

lazy val noPublishSettings = Seq(
  publish := ((): Unit),
  publishLocal := ((): Unit),
  publishArtifact := false
)

lazy val micrositeSettings = Seq(
  micrositeName := "finatra common practice",
  micrositeDescription := "Guideline for common practice in finatra 2.11",
  micrositeBaseUrl := "finatra-common-practice",
  micrositeDocumentationUrl := "/finatra-common-practice/docs/",
  micrositeGithubOwner := "jamesyang124",
  micrositeGithubRepo := "finatra-common-practice",
  micrositeGitterChannel := false,
  micrositeHighlightTheme := "github",
  micrositeGithubToken := Option(System.getenv().get("GITHUB_TOKEN")),
  micrositePushSiteWith := GitHub4s
)

lazy val docs = (project in file("docs"))
  .settings(moduleName := "docs")
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .enablePlugins(MicrositesPlugin)
  //.enablePlugins(TutPlugin)
