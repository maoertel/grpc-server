import cats.effect.{ExitCode, IO, IOApp, Resource}
import services.{GreeterImpl, LifeCycleServiceImpl}

import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Main extends IOApp {

  implicit private val logger: Logger = Logger.getLogger(classOf[GrpcTestServer].getName)
  implicit private val ec: ExecutionContextExecutor = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {

    val resource: Resource[IO, ApplicationConfig] = for {
      config <- ServerConfig.resource

      greeterService <- GreeterImpl.resource
      lifeCycleService <- LifeCycleServiceImpl.resource

      server <- GrpcTestServer.resource(config.port, greeterService, lifeCycleService)
    } yield ApplicationConfig(server, config.port)

    resource.use { config =>
      for {
        _ <- config.server.initialize(s"gRPC server started, listening on ${config.port}")
        _ <- IO.never
      } yield ExitCode.Success
    }
  }
}

case class ApplicationConfig(server: GrpcTestServer, port: Int)
