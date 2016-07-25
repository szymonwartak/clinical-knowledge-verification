import awscala.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType

import com.amazonaws.services.dynamodbv2.model.{CreateTableRequest, KeySchemaElement, _}
import com.amazonaws.services.{dynamodbv2 => aws}
import com.google.gson.{Gson, GsonBuilder}
import awscala._, awscala.dynamodbv2._

import scala.collection.JavaConversions._

object DynamoDb {

  // read stuff from NLP dudes
  val gson: Gson = new GsonBuilder().setPrettyPrinting.create
  val jsonStr = scala.io.Source.fromFile("relations_human_annotations_22072016.json").getLines().mkString
  loadRelations(gson.fromJson(jsonStr, classOf[Array[Relation]]))

  def loadRelations(relations: Seq[Relation]): Unit = {
    // table-name, access key, secret key
    val creds: List[String] = scala.io.Source.fromFile("/Users/szymon.wartak/dynamodb").getLines().mkString.split(",").toList

    implicit val dynamoDB = DynamoDB.local()
  //  implicit val region = Region.EU_WEST_1
  //  implicit val dynamoDB = DynamoDB(creds(1), creds(2))

    val tableName: String = creds(0)//"clinical-knowledge-verification"
    val req =
      new CreateTableRequest(
        List(
          new AttributeDefinition("status", AttributeType.String),
          new AttributeDefinition("type", AttributeType.String),
          new AttributeDefinition("id", AttributeType.Number)
        ),
        tableName, List(new KeySchemaElement("id", aws.model.KeyType.HASH)),
        new ProvisionedThroughput(10l, 5l))
      .withGlobalSecondaryIndexes(
        GlobalSecondaryIndex("status", Seq(KeySchema("status", aws.model.KeyType.HASH)),
          Projection(aws.model.ProjectionType.INCLUDE, Seq("status")), ProvisionedThroughput(10l, 5l)),
        new GlobalSecondaryIndex("type", Seq(KeySchema("type", aws.model.KeyType.HASH)),
          Projection(aws.model.ProjectionType.INCLUDE, Seq("type")), ProvisionedThroughput(100l, 100l)))

    dynamoDB.createTable(req)

    // table attrs: status (approved rejected ignored), type, knowledge, decision (who when comments)
    val table = dynamoDB.table(tableName).get

    (dynamoDB.describe(tableName).get.itemCount to 10 /*relations.size*/).foreach { i =>
      val entry = relations(i)
      table.put(i,
        ("status", "open"),
        ("type", gson.toJson(entry.knowledge.`type`)),
        ("knowledge", gson.toJson(entry))
      )
    }

    println(s"loaded: ${table.scan(Seq("status" -> Condition.eq("open"))).map(_.attributes.map(x => (x.name, x.value))).size} entries from NLP")
  }


}

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



// https://github.com/seratch/awscala/blob/master/src/test/scala/awscala/DynamoDBV2Spec.scala
trait DymanoDbAccess extends DBaccess {

}


trait DBaccess {
  def read()
  def write()
}

class RestApi {
  // user login
  // endpoint: NLP receive
  // endpoint: doctor read / update
}


/**
 * Batch process to write to knowledge base
 */
object WriteToKnowledgeBase extends App {

}