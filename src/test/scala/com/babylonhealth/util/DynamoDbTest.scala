package com.babylonhealth.util

import org.scalatest._


class DynamoDbTest extends FlatSpec with Matchers with BeforeAndAfter {
  val dynamoDb = new DynamoDb
  dynamoDb.loadRelations(getClass.getResource("/relations_human_annotations_test.json"))

  "getNextRelations" should "return 5 added from test data" in {
    val relations = dynamoDb.getNextRelations()
    assert(relations.size == 5)
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
