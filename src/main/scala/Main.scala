import GrpcTestServer._
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.applicative._
import io.grpc.Server
import services.{GreeterImpl, LifeCycleServiceImpl}

import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Main extends IOApp {

  implicit private val logger: Logger = Logger.getLogger(classOf[GrpcTestServer].getName)
  implicit private val ec: ExecutionContextExecutor = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {

    val resource: Resource[IO, (Server, Int)] = for {
      config <- ServerConfig.resource

      greeterService <- GreeterImpl.resource
      lifeCycleService <- LifeCycleServiceImpl.resource

      server <- GrpcTestServer.resource(config.port, greeterService, lifeCycleService)
    } yield server -> config.port

    resource.use { case (server, port) =>
      for {
        _ <- logger.info(s"gRPC server started, listening on $port").pure[IO]
        _ <- server.waitForTermination
      } yield ExitCode.Success
    }
  }
}
