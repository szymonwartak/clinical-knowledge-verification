package com.babylonhealth.util

import org.scalatest._


class DynamoDbTest extends FlatSpec with Matchers with BeforeAndAfter {
  val dynamoDb = DynamoDb.get
  val jsonStr = scala.io.Source.fromURL(getClass.getResource("/relations_human_annotations_test.json")).getLines().mkString
  dynamoDb.loadRelations(jsonStr)

  // TODO: figure out independent way of running Dynamo tests to prevent hacks like this
  "getNextRelations" should "return multiple of 5 ( -1 from update) added from test data" in {
    val relations = dynamoDb.getRelations()
    assert(relations.size % 5 == 4)
  }

  "updateRelationStatus" should "do what it says" in {
    dynamoDb.updateRelationStatus(0, VerificationStatus.Approved)
    dynamoDb.getById(0) match {
      case Some(item) =>
        assert(item.attributes.find(_.name ==  KnowledgeTableColumns.status.toString).get.value.s.get ==
          VerificationStatus.Approved.toString)
    }
  }

}
