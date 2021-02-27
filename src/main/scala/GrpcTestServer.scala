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
//      serverBuilder <- getServerBuilder
//      _ <- IO(services.map(service =>
//        for {
//          builder <- serverBuilder.get
//          updated = builder.addService(service).asInstanceOf[ServerBuilder[NettyServerBuilder]]
//          done <- serverBuilder.set(updated)
//        } yield done
//      ))
//      updatedServerBuilder <- serverBuilder.get
//      server = updatedServerBuilder.build().start
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

//  private def getServerBuilder = Ref.of[IO, ServerBuilder[NettyServerBuilder]] {
//    ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance()).asInstanceOf[ServerBuilder[NettyServerBuilder]]
//  }

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
