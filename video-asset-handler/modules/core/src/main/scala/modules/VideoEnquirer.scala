package modules

import cats.effect._
import cats.syntax.all._
import domain.metadata.MetaData

import java.io.{BufferedInputStream, ByteArrayInputStream}
import cats.effect.Resource
import cats.effect.std.Console
import config.environments.AppEnvironment
import config.environments.AppEnvironment.Test
import org.typelevel.log4cats.Logger
import scodec.bits.ByteVector

import java.nio.file.{Files, Paths}

object VideoEnquirer {
  def make[F[_]: Async : Logger](
      clients: HttpClients[F],
      appEnv: AppEnvironment,
      hashHandler: HashHandler[F]
  ): VideoEnquirer[F] =
    new VideoEnquirer[F](clients, appEnv, hashHandler) {}
}

class VideoEnquirer[F[_]: Async : Logger] private (
    clients: HttpClients[F],
    appEnv: AppEnvironment,
    hashHandler: HashHandler[F]
) {

  def downloadAndCheckIntegrity(assetId: String): F[Resource[F, BufferedInputStream]] =
    for {
      (video, metaData)    <- Async[F].both(download(assetId), queryMetadata(assetId))
      videoResource        =  entityBodyToResource(video)
      hashes               <- videoResource.use(hashHandler.convert)
      _                    <- hashHandler.compare(hashes, metaData)
    } yield videoResource

  def saveAsFile(videoResource: Resource[F, BufferedInputStream]): F[Unit] =
    for {
      _        <- Logger[F].info(s"Please enter a file name you desire. the format would be 'mov' automatically")
      fileName <- appEnv match { case Test => Sync[F].delay("test_video_file"); case _ => Console.make.readLine }
      _        <- videoResource.use(i => Sync[F].delay(Files.copy(i, Paths.get(fileName + ".mov"))))
      _        <- Logger[F].info(s"File saved as $fileName.mov")
    } yield ()

  def entityBodyToResource(b: ByteVector): Resource[F, BufferedInputStream] =
    Resource.make {
      Sync[F].blocking(new BufferedInputStream(new ByteArrayInputStream(b.toArray))) // build
    } { inStream =>
      Sync[F].blocking(inStream.close()).handleErrorWith(_ => Sync[F].unit) // release
    }

  private def download(assetId: String): F[ByteVector] = clients.downloader.download(assetId)

  private def queryMetadata(assetId: String): F[MetaData] = clients.metaDataQuerier.query(assetId)
}
