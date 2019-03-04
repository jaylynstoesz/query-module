name := "query-module"

version := "1.0.0"
      
lazy val `query-module` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.0"

val sparkVersion = "2.4.0"
val hadoopVersion = "2.7.2"
val fasterXmlVersion = "2.9.0"
val awsSdkVersion = "1.10.34"
val jodaVersion = "2.10"

libraryDependencies += "com.amazonaws" % "aws-java-sdk-pom" % awsSdkVersion pomOnly()
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-annotations" % fasterXmlVersion
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % fasterXmlVersion
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % fasterXmlVersion
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % fasterXmlVersion
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.5"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.5.14"
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.5.3"
libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided"
libraryDependencies += "javax.servlet" % "servlet-api" % "2.3" % "provided"
libraryDependencies += "joda-time" % "joda-time" % jodaVersion
libraryDependencies += "mysql" % "mysql-connector-java" % "6.0.6"
libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % hadoopVersion
libraryDependencies += "org.apache.hadoop" % "hadoop-common" % hadoopVersion
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.6"
libraryDependencies += "org.apache.httpcomponents" % "httpcore" % "4.4.10"
libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % "test"
libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )

pomIncludeRepository := { _ => false }
publishMavenStyle := true
publishArtifact in Test := false

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

import scalariform.formatter.preferences._
scalariformPreferences := scalariformPreferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AllowParamGroupsOnNewlines, true)
  .setPreference(DoubleIndentConstructorArguments, true)
  .setPreference(DanglingCloseParenthesis, Force)
  .setPreference(DoubleIndentConstructorArguments, true)
  .setPreference(FirstArgumentOnNewline, Force)
  .setPreference(FirstParameterOnNewline, Force)
  .setPreference(IndentPackageBlocks, true)
  .setPreference(IndentSpaces, 2)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  .setPreference(NewlineAtEndOfFile, true)
  .setPreference(SpaceInsideParentheses, false)
  .setPreference(SpaceInsideBrackets, false)
  .setPreference(SpacesWithinPatternBinders, false)
  .setPreference(SpacesAroundMultiImports, false)


      