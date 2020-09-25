import cats.Applicative
import cats.implicits._
import com.google.protobuf.empty.Empty
import helloworld.helloworld.{GreeterGrpc, HelloReply, HelloRequest}
import lifecycleservice.lifecycleservice.DbClusterInfo.CLusterState.{ClusterNotFound, ClusterNotSharded, ClusterSharded}
import lifecycleservice.lifecycleservice.EmptyResponse.Response.Success
import lifecycleservice.lifecycleservice._

import scala.concurrent.{ExecutionContext, Future}

object services {

  class LifeCycleServiceImpl(implicit ec: ExecutionContext) extends LifeCycleServiceGrpc.LifeCycleService {

    private def SuccF[A](value: A = ()) = Future.successful(value)

    override def getDbClusterInfo(request: DbClusterKey): Future[DbClusterInfo] =
      SuccF(request.key match {
        case "my-sharded-cluster" => DbClusterInfo(ClusterSharded)
        case "my-non-sharded-cluster" => DbClusterInfo(ClusterNotSharded)
        case _ => DbClusterInfo(ClusterNotFound)
      })

    override def getDbClusterKeyForProjectCreation(request: Empty): Future[DbClusterKey] = SuccF(DbClusterKey("rs6"))

    override def initProject(request: ProjectKey): Future[EmptyResponse] = SuccF(EmptyResponse(Success))

    override def updateLanguages(request: ProjectKey): Future[EmptyResponse] = SuccF(EmptyResponse(Success))

    override def purgeProject(request: ProjectWithClusters): Future[EmptyResponse] = {
      val maybeEsClusterKey = request.dbClusters.map(_.esCluster.map(_.clusterKey))
      val maybeDbClusterKey = request.dbClusters.map(_.mongoCluster.map(_.clusterKey))

      val purgeEsCluster = maybeEsClusterKey.fold(SuccF())(_ => SuccF())
      val purgeDbCluster = maybeDbClusterKey.fold(SuccF())(_ => SuccF())


      Applicative[Future].map2(purgeEsCluster, purgeDbCluster) { case (_, _) => EmptyResponse(Success) }
    }
  }

  class GreeterImpl(implicit ec: ExecutionContext) extends GreeterGrpc.Greeter {

    override def sayHello(req: HelloRequest): Future[HelloReply] =
      HelloReply(message = "Hello " + req.name).pure[Future]

  }

}
