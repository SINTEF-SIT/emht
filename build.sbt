name := "emht"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.webjars" %% "webjars-play" % "2.2.1-2",
  "org.webjars" % "flot" % "0.8.0-1",
  "org.webjars" % "jqgrid" % "4.4.5",
  "org.webjars" % "jquery-ui" % "1.10.4-1",
  "org.webjars" % "bootstrap" % "3.2.0"
)     

play.Project.playJavaSettings
