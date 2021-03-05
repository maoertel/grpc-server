import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.applicative._
import services.{GreeterImpl, LifeCycleServiceImpl}

import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Main extends IOApp {

  implicit private val logger: Logger = Logger.getLogger("gRPC_server")
  implicit private val ec: ExecutionContextExecutor = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {

    val resource: Resource[IO, Int] = for {
      config <- ServerConfig.resource

      greeterService <- GreeterImpl.resource
      lifeCycleService <- LifeCycleServiceImpl.resource

      _ <- GrpcTestServer.resource(config.port, greeterService, lifeCycleService)
    } yield config.port

    resource.use(port => s"gRPC server started, listening on $port".pure[IO] map logger.info as ExitCode.Success)
  }
}
