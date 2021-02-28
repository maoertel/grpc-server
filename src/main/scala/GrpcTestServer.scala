import cats.effect.{IO, Resource}
import cats.syntax.applicative._
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

import scala.concurrent.ExecutionContext

class GrpcTestServer(
  services: List[ServerServiceDefinition],
  port: Int
)(implicit executionContext: ExecutionContext) {

  private def startServer: IO[Server] =
    for {
      server <- services
        .foldLeft(ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance())) {
          case (builder, serviceDefinition) => builder.addService(serviceDefinition)
        }
        .build()
        .start()
        .pure[IO]
      _ = sys.addShutdownHook(server.shutdown())
    } yield server
}

object GrpcTestServer {

  def resource(
    port: Int,
    services: ServerServiceDefinition*
  )(implicit executionContext: ExecutionContext): Resource[IO, Server] =
    Resource.make(new GrpcTestServer(services.toList, port).startServer)(_ => IO.unit)

  implicit class serverWrapper(server: Server) {
    def waitForTermination: IO[Unit] = server.awaitTermination().pure[IO]
  }
}
