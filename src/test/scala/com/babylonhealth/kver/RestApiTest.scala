package com.babylonhealth.kver

import akka.actor.ActorSystem
import akka.testkit._
import akka.util.Timeout
import com.babylonhealth.util.{Relation, VerificationStatus}
import org.scalatest.{FreeSpec, Matchers}
import spray.http.HttpEntity
import spray.http.MediaTypes._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._


class RestApiTest extends FreeSpec with Matchers with ScalatestRouteTest with CKRouting {
  def actorRefFactory = system
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.second.dilated(system))

  implicit val timeout = Timeout(5 seconds)
  val jsonStr = scala.io.Source.fromURL(getClass.getResource("/relations_human_annotations_test.json")).getLines().mkString
  val relations = gson.fromJson(jsonStr, classOf[Array[Relation]])

  // curl -X GET http://localhost:8080/doctor-read
  // curl -X POST http://localhost:8080/nlp-receive
  "read-update-read" - {
    "should be empty, load, return all, update, return all minus one" in {
      Get("/doctor-read") ~> routes ~> check {
        gson.fromJson(responseAs[String], classOf[Array[Relation]]).size should equal (0)
      }
      Post("/nlp-receive", HttpEntity(`application/json`, jsonStr)) ~> routes ~> check {
        responseAs[String].toInt should equal(gson.fromJson(jsonStr, classOf[Array[Relation]]).size)
      }
      Get("/doctor-read") ~> routes ~> check {
        gson.fromJson(responseAs[String], classOf[Array[Relation]]).size should equal (relations.size)
      }
      val approved0 = HttpEntity(`application/json`, gson.toJson(DoctorUpdate(0, VerificationStatus.Approved.toString)))
      Post("/doctor-update", approved0) ~> routes ~> check {
        responseAs[String] should equal("ok")
      }
      Get("/doctor-read") ~> routes ~> check {
        gson.fromJson(responseAs[String], classOf[Array[Relation]]).size should equal (relations.size - 1)
      }
    }
  }

}
