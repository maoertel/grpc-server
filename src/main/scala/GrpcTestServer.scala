import cats.effect.{IO, Resource}
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

import scala.concurrent.ExecutionContext

class GrpcTestServer(
  services: List[ServerServiceDefinition],
  port: Int
)(implicit executionContext: ExecutionContext) {

  private def startServer: IO[Server] =
    for {
      server <- IO(
        services
          .foldLeft(ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance())) {
            case (builder, serviceDefinition) => builder.addService(serviceDefinition)
          }
          .build()
          .start())
      _ = sys.addShutdownHook(server.shutdown())
    } yield server
}

object GrpcTestServer {

  def resource(
    port: Int,
    services: ServerServiceDefinition*
  )(implicit executionContext: ExecutionContext): Resource[IO, Server] =
    Resource.make(new GrpcTestServer(services.toList, port).startServer)(server => IO(server.awaitTermination()))
}
