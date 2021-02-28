import cats.Applicative
import cats.effect.{IO, Resource}
import cats.implicits._
import com.google.protobuf.empty.Empty
import helloworld.helloworld.{GreeterGrpc, HelloReply, HelloRequest}
import io.grpc.ServerServiceDefinition
import lifecycleservice.lifecycleservice.DbClusterInfo.CLusterState.{ClusterNotFound, ClusterNotSharded, ClusterSharded}
import lifecycleservice.lifecycleservice.EmptyResponse.Response.Success
import lifecycleservice.lifecycleservice._

import scala.concurrent.{ExecutionContext, Future}

object services {

  class LifeCycleServiceImpl(implicit ec: ExecutionContext) extends LifeCycleServiceGrpc.LifeCycleService {

    override def getDbClusterInfo(request: DbClusterKey): Future[DbClusterInfo] =
      (request.key match {
        case "my-sharded-cluster" => DbClusterInfo(ClusterSharded)
        case "my-non-sharded-cluster" => DbClusterInfo(ClusterNotSharded)
        case _ => DbClusterInfo(ClusterNotFound)
      }).pure[Future]

    override def getDbClusterKeyForProjectCreation(request: Empty): Future[DbClusterKey] =
      DbClusterKey("rs6").pure[Future]

    override def initProject(request: ProjectKey): Future[EmptyResponse] = EmptyResponse(Success).pure[Future]

    override def updateLanguages(request: ProjectKey): Future[EmptyResponse] = EmptyResponse(Success).pure[Future]

    override def purgeProject(request: ProjectWithClusters): Future[EmptyResponse] = {
      val maybeEsClusterKey = request.dbClusters.map(_.esCluster.map(_.clusterKey))
      val maybeDbClusterKey = request.dbClusters.map(_.mongoCluster.map(_.clusterKey))

      val purgeEsCluster = ().pure[Future] // maybeEsClusterKey.fold(success)(_ => handle error)
      val purgeDbCluster = ().pure[Future] // maybeDbClusterKey.fold(success)(_ => handle error)

      Applicative[Future].map2(purgeEsCluster, purgeDbCluster) { case (_, _) => EmptyResponse(Success) }
    }
  }

  object LifeCycleServiceImpl {
    def resource(implicit ec: ExecutionContext): Resource[IO, ServerServiceDefinition] =
      Resource.make(LifeCycleServiceGrpc.bindService(new LifeCycleServiceImpl, ec).pure[IO])(_ => IO.unit)
  }

  class GreeterImpl(implicit ec: ExecutionContext) extends GreeterGrpc.Greeter {
    override def sayHello(req: HelloRequest): Future[HelloReply] =
      HelloReply(message = "Hello " + req.name).pure[Future]
  }

  object GreeterImpl {
    def resource(implicit ec: ExecutionContext): Resource[IO, ServerServiceDefinition] =
      Resource.make(GreeterGrpc.bindService(new GreeterImpl, ec).pure[IO])(_ => IO.unit)
  }
}
