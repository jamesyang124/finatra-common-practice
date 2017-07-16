name := "finatra-integrations"

version := "1.0"

scalaVersion := "2.12.2"

micrositeName := "Finatra 2.11 Integrations Example"
micrositeDescription := "A document site for showing the examples of integrating grafter, docker-it-scala, and other tool sets."

micrositeBaseUrl := "finatra-integrations"
micrositeDocumentationUrl := "/finatra-integrations/docs/"
micrositeDataDirectory := (resourceDirectory in Compile).value / "microsite" / "data"

micrositeAuthor := "James Yang"
micrositeGithubOwner := "jamesyang124"
micrositeGithubRepo := "finatra-integrations"
micrositeGitterChannel := false
micrositeGithubToken := Option(System.getenv().get("GITHUB_TOKEN"))
micrositePushSiteWith := GitHub4s
includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md"


enablePlugins(MicrositesPlugin)
enablePlugins(TutPlugin)
