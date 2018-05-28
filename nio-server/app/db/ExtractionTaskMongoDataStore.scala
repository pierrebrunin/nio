package db

import javax.inject.{Inject, Singleton}
import models._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.{Cursor, QueryOpts, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.akkastream.cursorProducer

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExtractionTaskMongoDataStore @Inject()(reactiveMongoApi: ReactiveMongoApi)(
    implicit val ec: ExecutionContext) {

  def storedCollection(tenant: String): Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection(s"$tenant-extractionTasks"))

  implicit def format: Format[ExtractionTask] = ExtractionTask.fmt

  def insert(tenant: String, extractionTask: ExtractionTask) =
    storedCollection(tenant).flatMap(
      _.insert(format.writes(extractionTask).as[JsObject]).map(_.ok))

  def updateById(tenant: String,
                 id: String,
                 extractionTask: ExtractionTask): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.update(Json.obj("_id" -> id), extractionTask.asJson)
        .map(_.ok))
  }

  def findById(tenant: String, id: String) = {
    val query = Json.obj("_id" -> id)
    storedCollection(tenant).flatMap(_.find(query).one[ExtractionTask])
  }

  def findAll(tenant: String, page: Int, pageSize: Int) = {
    findAllByQuery(tenant, Json.obj(), page, pageSize)
  }

  def findAllByOrgKey(tenant: String,
                      orgKey: String,
                      page: Int,
                      pageSize: Int) = {
    findAllByQuery(tenant, Json.obj("orgKey" -> orgKey), page, pageSize)
  }

  def findAllByUserId(tenant: String,
                      userId: String,
                      page: Int,
                      pageSize: Int) = {
    findAllByQuery(tenant, Json.obj("userId" -> userId), page, pageSize)
  }

  private def findAllByQuery(tenant: String,
                             query: JsObject,
                             page: Int,
                             pageSize: Int) = {
    val options = QueryOpts(skipN = page * pageSize, pageSize)
    storedCollection(tenant).flatMap { coll =>
      for {
        count <- coll.count(Some(query))
        queryRes <- coll
          .find(query)
          .sort(Json.obj("lastUpdate" -> -1))
          .options(options)
          .cursor[ExtractionTask](ReadPreference.primaryPreferred)
          .collect[Seq](maxDocs = pageSize,
                        Cursor.FailOnError[Seq[ExtractionTask]]())
      } yield {
        (queryRes, count)
      }
    }
  }

  def init(tenant: String) = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

}
