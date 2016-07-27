package com.babylonhealth.kver

import spray.http._
import spray.httpx.SprayJsonSupport._
import akka.actor.ActorSystem
import akka.util.Timeout
import com.babylonhealth.util._
import com.google.gson.{Gson, GsonBuilder}
import spray.httpx.marshalling._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import spray.routing.{HttpService, SimpleRoutingApp}

import scala.collection.immutable.HashMap
import scala.concurrent.duration._
import scala.util.Try

// request param classes
case class DoctorUpdate(id: Int, verificationStatus: String)

object JsonDeser extends DefaultJsonProtocol {
  implicit val m1: RootJsonFormat[DoctorUpdate] = jsonFormat2(DoctorUpdate.apply)
}

object RestApi extends SimpleRoutingApp with CKRouting with App { //with SSLConfiguration {
  implicit val system = ActorSystem("restapi")
  implicit val timeout = Timeout(5 seconds)

  startServer(interface = "0.0.0.0", port = 8080) (routes)
}

trait CKRouting extends HttpService with MetaToResponseMarshallers {
  import JsonDeser._

  val gson: Gson = new GsonBuilder().setPrettyPrinting.create

  val dynamoDb = new DynamoDb

  // user login
  // endpoint: NLP receive
  // endpoint: doctor read / update
  lazy val routes = {
    path("nlp-receive") {
      post {
        decompressRequest() {
          entity(as[String]) { data =>
            complete {
              println("nlp-receive")
              val relations = gson.fromJson(data, classOf[Array[Relation]])
              dynamoDb.loadRelations(data)
              relations.size.toString
            }
          }
        }
      }
    } ~
    path("doctor-update") {
      post {
        entity(as[DoctorUpdate]) { du =>
          detach() {
            complete {
              println("doctor-update")
              dynamoDb.updateRelationStatus(du.id, VerificationStatus.withName(du.verificationStatus))
              "ok"
            }
          }
        }
      }
    } ~
    path("doctor-read") {
      get {
        decompressRequest() {
          detach() {
            complete {
              println("doctor-read")
              gson.toJson(dynamoDb.getNextRelations())
            }
          }
        }
      }
    }
  }
}
/**
 * Batch process to write to knowledge base
 */
object WriteToKnowledgeBase extends App {

}