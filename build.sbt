name := "emht"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.webjars" %% "webjars-play" % "2.2.1-2",
  "org.webjars" % "flot" % "0.8.0-1",
  "org.webjars" % "bootstrap" % "3.1.0"
)     

play.Project.playJavaSettings
