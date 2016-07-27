package com.babylonhealth.util

import java.net.URL

import awscala.Region
import awscala.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model.{Condition => JCondition, CreateTableRequest, KeySchemaElement}
import com.amazonaws.services.{dynamodbv2 => aws}
import com.google.gson.{Gson, GsonBuilder}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer


// classes for reading JSON sent from NLP guys
case class Relation(source: Source, meta_data: MetaData, knowledge: Knowledge)
case class Source(date: String, `type`: String, payload: PayloadUrl)
case class PayloadUrl(url: String)
case class MetaData(annotated_on: String, pipeline_id: String, responsible: String)
case class Knowledge(`type`: String, payload: BodypartProcedure)
case class BodypartProcedure(bodypart: ListLabel, procedure: ListLabel)
case class ListLabel(id_list: Array[Entry], label: Label)
case class Entry(lang: String, code: String, id: String)
case class Label(lang: String, text: String)


object VerificationStatus extends Enumeration {
  type Status = Value
  val Approved, Rejected, Ignored, Open = Value
}
object KnowledgeTableColumns extends Enumeration {
  type KnowledgeTableColumns = Value
  val status, `type`, id, knowledge, decision = Value
}


class DynamoDb(isTest: Boolean = true) {
//  import com.babylonhealth.util._

  import KnowledgeTableColumns._
  import VerificationStatus._

  // table-name, access key, secret key
  val creds: List[String] = scala.io.Source.fromURL(getClass.getResource("/dynamodb")).getLines().mkString.split(",").toList
  implicit val dynamoDB = {
    if (isTest)
      DynamoDB.local()
    else {
      implicit val region = Region.EU_WEST_1
      DynamoDB(creds(1), creds(2))
    }
  }

  val knowledgeTableName = creds(0) //"clinical-knowledge-verification"
  val knowledgeTable: Table = dynamoDB.table(knowledgeTableName).getOrElse(createKnowledgeTable())

  // TODO: solve (threading?) issues - this doesn't initialize properly with spray
  def gson: Gson = new GsonBuilder().setPrettyPrinting.create

  def updateRelationStatus(id: Int, newStatus: VerificationStatus.Value): Unit = {
    dynamoDB.putAttributes(knowledgeTable, id, attributes = (status.toString, newStatus.toString))
  }
  def getById(id: Int) = dynamoDB.get(knowledgeTable, id)
  def getNextRelations(limit: Int = 10): Array[Relation] = {
    dynamoDB.scan(knowledgeTable, filterOpen, limit = limit)
      .flatMap(_.attributes.filter(_.name == knowledge.toString))
      .flatMap(_.value.s)
      .map(x => gson.fromJson(x, classOf[Relation])).toArray
  }
  def loadRelations(jsonStr: String): Unit = {
//    val jsonStr = scala.io.Source.fromURL(resource).getLines().mkString
    val relations = gson.fromJson(jsonStr, classOf[Array[Relation]])
    val countBefore = dynamoDB.describe(knowledgeTable).get.itemCount

    val startIndex = dynamoDB.describe(knowledgeTableName).get.itemCount
    (0 until relations.size).map(_ + startIndex).foreach { i =>
      val entry = relations(i.toInt)
      println(s"putting: ${i} with status ${Open.toString}")
      knowledgeTable.putItem(hashPK = i,
        attributes = (status.toString, Open.toString),
        (`type`.toString, gson.toJson(entry.knowledge.`type`)),
        (knowledge.toString, gson.toJson(entry))
      )
    }
    val countAfter = dynamoDB.describe(knowledgeTable).get.itemCount
    println(s"added ${relations.size}, committed: ${countAfter - countBefore}, current size: ${countAfter}")
  }


  private def createKnowledgeTable(): Table = {
    println("creating knowledge table....")
    // table attrs: status (approved rejected ignored), type, knowledge, decision (who when comments)
    val req =
      new CreateTableRequest(
        List(
          new AttributeDefinition(status.toString, AttributeType.String),
          new AttributeDefinition(`type`.toString, AttributeType.String),
          new AttributeDefinition(id.toString, AttributeType.Number)
        ),
        knowledgeTableName, List(new KeySchemaElement(id.toString, aws.model.KeyType.HASH)),
        new ProvisionedThroughput(10l, 5l))
        .withGlobalSecondaryIndexes(
          GlobalSecondaryIndex(status.toString, Seq(KeySchema(status.toString, aws.model.KeyType.HASH)),
            Projection(aws.model.ProjectionType.INCLUDE, Seq(status.toString)), ProvisionedThroughput(10l, 5l)),
          new GlobalSecondaryIndex(`type`.toString, Seq(KeySchema(`type`.toString, aws.model.KeyType.HASH)),
            Projection(aws.model.ProjectionType.INCLUDE, Seq(`type`.toString)), ProvisionedThroughput(100l, 100l)))
    dynamoDB.createTable(req)
    Thread.sleep(1000l)
    dynamoDB.table(knowledgeTableName).get
  }
  private def filterOpen: Seq[(String, JCondition)] = Seq((status.toString, Condition.eq(Open.toString)))

}
