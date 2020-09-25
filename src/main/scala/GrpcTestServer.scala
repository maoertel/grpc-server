import java.util.logging.Logger

import cats.effect.IO
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

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
      server <- IO(ServerBuilder
        .forPort(port)
        .addService(ProtoReflectionService.newInstance())
        .addService(services.head)
        .addService(services.tail.head)
        .build()
        .start)
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
