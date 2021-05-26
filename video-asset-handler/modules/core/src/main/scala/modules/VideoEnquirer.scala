package modules

import cats.effect._
import cats.syntax.all._
import domain.metadata.MetaData

import org.apache.commons.codec.digest.DigestUtils

import java.io.{ BufferedInputStream, ByteArrayInputStream }
import cats.effect.Resource
import cats.effect.std.Console
import config.environments.AppEnvironment
import config.environments.AppEnvironment.Test
import domain.video.VideoCorrupted
import scodec.bits.ByteVector

import java.nio.file.{ Files, Paths }

object VideoEnquirer {
  def make[F[_]: Sync](
      clients: HttpClients[F],
      appEnv: AppEnvironment
  ): VideoEnquirer[F] =
    new VideoEnquirer[F](clients, appEnv) {}
}

class VideoEnquirer[F[_]: Sync] private (
    clients: HttpClients[F],
    appEnv: AppEnvironment
) {

  def downloadAndCheckIntegrity(assetId: String): F[Resource[F, BufferedInputStream]] =
    for {
      video         <- download(assetId)
      videoResource = entityBodyToResource(video)
      videoMd5      <- videoResource.use(i => Sync[F].delay(DigestUtils.md5Hex(i)))
      metaData      <- queryMetadata(assetId)
      _             <- if (videoMd5 == metaData.md5.value.value) validCase() else invalidCase()
    } yield videoResource

  def saveAsFile(videoResource: Resource[F, BufferedInputStream]): F[Unit] =
    for {
      _        <- Sync[F].delay(println(s"Please enter a file name you desire. the format would be 'mov' automatically"))
      fileName <- appEnv match { case Test => Sync[F].delay("test_video_file"); case _ => Console.make.readLine }
      _        <- videoResource.use(i => Sync[F].delay(Files.copy(i, Paths.get(fileName + ".mov"))))
      _        <- Sync[F].delay(println(s"File saved as $fileName.mov"))
    } yield ()

  def entityBodyToResource(b: ByteVector): Resource[F, BufferedInputStream] =
    Resource.make {
      Sync[F].blocking(new BufferedInputStream(new ByteArrayInputStream(b.toArray))) // build
    } { inStream =>
      Sync[F].blocking(inStream.close()).handleErrorWith(_ => Sync[F].unit) // release
    }

  private def download(assetId: String): F[ByteVector] = clients.downloader.download(assetId)

  private def queryMetadata(assetId: String): F[MetaData] = clients.metaDataQuerier.query(assetId)

  private def validCase(): F[Unit] =
    Sync[F].delay(println(s"the video is valid")) *> Sync[F].unit

  private def invalidCase(): F[Unit] =
    Sync[F].raiseError(VideoCorrupted("the video is invalid, the program exits")) *> Sync[F].unit
}
