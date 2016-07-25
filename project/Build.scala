import sbt.Keys._
import sbt._
import com.localytics.sbt.dynamodb.DynamoDBLocalKeys._

object BabylonBuild extends Build {
  startDynamoDBLocal <<= startDynamoDBLocal.dependsOn(compile in Test)
  test in Test <<= (test in Test).dependsOn(startDynamoDBLocal)
  testOptions in Test <+= dynamoDBLocalTestCleanup

  val coreSettings = Seq(

    version := "1.0",
    scalaVersion := "2.11.8",
    resolvers ++= Seq(
      Resolver.mavenLocal
    ),
    libraryDependencies ++= Seq(
      "com.github.seratch" %% "awscala" % "0.3.+",
      "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
      "com.google.code.gson" % "gson" % "2.7"
    )
  )

  lazy val root = Project("clinical_knowledge_verification", file(".")).settings(
    coreSettings : _*
  )

//  lazy val restApi = Project("restApi", file("restApi"))
//    .settings(coreSettings : _*)

}
