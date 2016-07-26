import sbt.Keys._
import sbt._
import com.localytics.sbt.dynamodb.DynamoDBLocalKeys._
import spray.revolver.RevolverKeys

object BabylonBuild extends Build with RevolverKeys {

  val akkaV = "2.3.3"
  val sprayV = "1.3.2"

  val dynamoDbSettings = Seq(
    startDynamoDBLocal <<= startDynamoDBLocal.dependsOn(compile in Test),
    test in Test <<= (test in Test).dependsOn(startDynamoDBLocal),
    testOptions in Test <+= dynamoDBLocalTestCleanup
  )
  val coreSettings = Seq(
    name := "clinical_knowledge_verification",
    version := "1.0",
    scalaVersion := "2.11.8",
    javaOptions in reStart += "-Xmx2g",
    mainClass in reStart := Some("com.babylonhealth.kver.RestApi"),
    //    name := "nlp-magic",
    resolvers ++= Seq(
      Resolver.mavenLocal
    ),
    libraryDependencies ++= Seq(
      "com.github.seratch" %% "awscala" % "0.3.+",
      "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
      "com.google.code.gson" % "gson" % "2.7",
      "com.typesafe.akka" %% "akka-remote" % akkaV,
      "com.typesafe.akka" %% "akka-testkit" % akkaV,
      "org.scalatest" %% "scalatest" % "2.2.2" % "test",
      "io.spray" %% "spray-io" % sprayV,
      "io.spray" %% "spray-can" % sprayV,
      "io.spray" %% "spray-json" % sprayV,
      "io.spray" %% "spray-httpx" % sprayV,
      "io.spray" %% "spray-routing" % sprayV

    )
  )

  lazy val root = Project("clinical_knowledge_verification", file(".")).settings(
    coreSettings ++ dynamoDbSettings: _*
  )

//  lazy val restApi = Project("restApi", file("restApi"))
//    .settings(coreSettings : _*)

}
