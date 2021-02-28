import cats.effect.{IO, Resource}
import cats.syntax.applicative._
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

import java.util.logging.Logger
import scala.concurrent.ExecutionContext

class GrpcTestServer(
  services: List[ServerServiceDefinition],
  port: Int
)(implicit
  logger: Logger,
  executionContext: ExecutionContext
) {

  def initialize(log: String): IO[Server] =
    for {
      server <- buildServer
      _ = server.start()
      _ = logger.info(log)
      _ = sys.addShutdownHook(server.shutdown())
      _ <- blockUntilShutdown(server)
    } yield server

  private def buildServer: IO[Server] =
    for {
      server <- services
        .foldLeft(ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance())) {
          case (builder, serviceDefinition) => builder.addService(serviceDefinition)
        }
        .build()
        .pure[IO]
    } yield server

  private def blockUntilShutdown(server: Server): IO[Unit] = server.awaitTermination().pure[IO]
}

object GrpcTestServer {

  def resource(
    port: Int,
    services: ServerServiceDefinition*
  )(implicit logger: Logger, executionContext: ExecutionContext): Resource[IO, GrpcTestServer] =
    Resource.make(new GrpcTestServer(services.toList, port).pure[IO])(_ => IO.unit)

}
