package com.babylonhealth.util

import java.io.{PrintWriter, File}

import awscala.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model.{Condition => JCondition, ComparisonOperator, CreateTableRequest, KeySchemaElement}
import com.amazonaws.services.{dynamodbv2 => aws}
//import aws.model.Condition
import com.babylonhealth.kver._
import com.google.gson.{Gson, GsonBuilder}

import scala.collection.JavaConversions._


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


class DynamoDb {
//  import com.babylonhealth.util._

  import KnowledgeTableColumns._
  import VerificationStatus._

  // table-name, access key, secret key
  val creds: List[String] = scala.io.Source.fromURL(getClass.getResource("/dynamodb")).getLines().mkString.split(",").toList
  implicit val dynamoDB = {
    DynamoDB.local()
    //  implicit val region = Region.EU_WEST_1
    //  implicit val dynamoDB = DynamoDB(creds(1), creds(2))
  }

  val knowledgeTableName = creds(0) //"clinical-knowledge-verification"
  val knowledgeTable: Table = dynamoDB.table(knowledgeTableName).getOrElse(createKnowledgeTable())

  // read stuff from NLP dudes
  loadRelations()

  // TODO: solve (threading?) issues - this doesn't initialize properly with spray
  def gson: Gson = new GsonBuilder().setPrettyPrinting.create


  def createKnowledgeTable(): Table = {
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

//  def eqS(values: String*): JCondition = {
//    new JCondition().withComparisonOperator(ComparisonOperator.EQ)
//      .withAttributeValueList(
//        values.flatMap(v => List(new AttributeValue(s = Option(v), ss = Seq(v)))))
//  }
  def filterOpen: Seq[(String, JCondition)] = Seq((status.toString, Condition.eq(Open.toString)))
  def getNextRelations(limit: Int = 10): Seq[Relation] = {
    val scan = dynamoDB.scan(knowledgeTable, filterOpen, limit = limit)
    println(s"scan: ${scan.size}")
    scan
      .flatMap(_.attributes.filter(_.name == knowledge.toString))
      .flatMap(_.value.s)
      .map(x => gson.fromJson(x, classOf[Relation]))
  }
  def loadRelations(): Unit = {
    val jsonStr = scala.io.Source.fromURL(getClass.getResource("/relations_human_annotations_22072016.json")).getLines().mkString
    val relations = gson.fromJson(jsonStr, classOf[Array[Relation]])
    val countBefore = dynamoDB.describe(knowledgeTable).get.itemCount

    (dynamoDB.describe(knowledgeTableName).get.itemCount until 10 /*relations.size*/).foreach { i =>
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

//    val scan = knowledgeTable.scan(Seq((status.toString, Condition.eq(Open.toString))))
//    println(s"loaded: ${scan.map(_.attributes.map(x => (x.name, x.value))).size} entries from NLP")

  }


}
