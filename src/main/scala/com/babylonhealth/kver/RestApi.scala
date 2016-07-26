package com.babylonhealth.kver

import akka.actor.ActorSystem
import akka.util.Timeout
import com.babylonhealth.util._
import com.google.gson.{Gson, GsonBuilder}
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.marshalling.MetaToResponseMarshallers
import spray.httpx.unmarshalling._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import spray.routing.SimpleRoutingApp

import scala.concurrent.duration._
import scala.util.Try

// request param classes
case class DoctorUpdate(id: Int, verificationStatus: String)

object JsonDeser extends DefaultJsonProtocol {
  implicit val m1: RootJsonFormat[DoctorUpdate] = jsonFormat2(DoctorUpdate.apply)
}

object RestApi extends App with SimpleRoutingApp with MetaToResponseMarshallers{ //with SSLConfiguration {
  import JsonDeser._

  val gson: Gson = new GsonBuilder().setPrettyPrinting.create
  val dynamoDb = new DynamoDb

  implicit val system = ActorSystem("restapi")
  implicit val timeout = Timeout(5 seconds)

  // sbt "startDynamodbLocal ~re-start"
  startServer(interface = "0.0.0.0", port = 8080) {
    // curl -X POST localhost:8080/nlp-receive
    path("nlp-receive") {
      post {
        decompressRequest() {
          detach() { req =>
            complete {
              Try(req.request.entity)
                .map { case (HttpEntity.NonEmpty(contentType, data)) =>
                  val relations = gson.fromJson(data.asString, classOf[Array[Relation]])
                  println(s"received ${relations.size} relations")
                  "ok"
                }

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
              dynamoDb.updateRelationStatus(du.id, VerificationStatus.withName(du.verificationStatus))
              "updated"
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
              gson.toJson(dynamoDb.getNextRelations())
            }
          }
        }
      }
    }
  }
  // user login
  // endpoint: NLP receive
  // endpoint: doctor read / update
}


/**
 * Batch process to write to knowledge base
 */
object WriteToKnowledgeBase extends App {

}