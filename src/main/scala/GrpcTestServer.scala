import cats.effect.IO
import cats.syntax.applicative._
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

import java.util.logging.Logger
import scala.concurrent.ExecutionContext

class GrpcTestServer(
  services: List[ServerServiceDefinition],
  port: Int
)(
  implicit logger: Logger,
  executionContext: ExecutionContext
) {

  def start: IO[Unit] = init flatMap blockUntilShutdown

  private def init: IO[Server] =
    for {
      server: Server <- services
        .foldLeft(ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance())) {
          case (builder, serviceDefinition) => builder.addService(serviceDefinition)
        }
        .build()
        .start()
        .pure[IO]
      _ = logger.info("Server started, listening on " + port)
      _ = sys.addShutdownHook(server.shutdown())
    } yield server

  private def blockUntilShutdown(server: Server): IO[Unit] = IO(server.awaitTermination())
}

object GrpcTestServer {

  def create(
    services: List[ServerServiceDefinition],
    port: Int
  )(
    implicit logger: Logger,
    executionContext: ExecutionContext
  ): IO[GrpcTestServer] = IO(new GrpcTestServer(services, port))

}
