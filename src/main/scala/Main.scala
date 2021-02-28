import java.util.logging.Logger

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.applicative._
import com.typesafe.config.ConfigFactory
import helloworld.helloworld.GreeterGrpc
import lifecycleservice.lifecycleservice.LifeCycleServiceGrpc
import services.{GreeterImpl, LifeCycleServiceImpl}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Main extends IOApp {

  private implicit val logger: Logger = Logger.getLogger(classOf[GrpcTestServer].getName)
  private implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- ConfigFactory.load("server.conf").pure[IO]
      port = config.getInt("lifecycleService.port")
      services =
        GreeterGrpc.bindService(new GreeterImpl, ec) ::
        LifeCycleServiceGrpc.bindService(new LifeCycleServiceImpl, ec) :: Nil

      server <- GrpcTestServer.create(services, port)
      _ <- server.start
    } yield ExitCode.Success
}
