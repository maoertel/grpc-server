import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory

case class ServerConfig(port: Int)
object ServerConfig {
  def resource: Resource[IO, ServerConfig] = Resource.make {
    for {
      config <- IO(ConfigFactory.load("server.conf"))
      port = config.getInt("lifecycleService.port")
    } yield ServerConfig(port)
  }(_ => IO.unit)
}
