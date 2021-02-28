import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import com.typesafe.config.ConfigFactory

case class ServerConfig(port: Int)
object ServerConfig {
  def resource: Resource[IO, ServerConfig] = Resource.make {
    for {
      config <- ConfigFactory.load("server.conf").pure[IO]
      port = config.getInt("lifecycleService.port")
    } yield ServerConfig(port)
  }(_ => IO.unit)
}
