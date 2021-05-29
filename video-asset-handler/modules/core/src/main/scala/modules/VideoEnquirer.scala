package modules

import cats.effect._
import cats.syntax.all._
import domain.metadata.MetaData

import java.io.{BufferedInputStream, ByteArrayInputStream}
import cats.effect.Resource
import cats.effect.std.Console
import config.environments.AppEnvironment
import config.environments.AppEnvironment.Test
import domain.video.VideoCorrupted
import modules.HashConverter.Hashes
import scodec.bits.ByteVector

import java.nio.file.{Files, Paths}

object VideoEnquirer {
  def make[F[_]: Async](
      clients: HttpClients[F],
      appEnv: AppEnvironment
  ): VideoEnquirer[F] =
    new VideoEnquirer[F](clients, appEnv) {}
}

class VideoEnquirer[F[_]: Async] private (
    clients: HttpClients[F],
    appEnv: AppEnvironment
) {

  def downloadAndCheckIntegrity(assetId: String): F[Resource[F, BufferedInputStream]] =
    for {
      (video, metaData)                 <- Async[F].both(download(assetId), queryMetadata(assetId))
      videoResource                     =  entityBodyToResource(video)
      Hashes(sha1, sha256, md5, crc32)  <- videoResource.use(HashConverter.convert[F])
      _                                 <- if (sha1 == metaData.sha1.value.value) validCase("sha1") else invalidCase("sha1")
      _                                 <- if (sha256 == metaData.sha256.value.value) validCase("sha256") else invalidCase("sha256")
      _                                 <- if (md5 == metaData.md5.value.value) validCase("md5") else invalidCase("md5")
      _                                 <- if (crc32 == metaData.crc32.value.value) validCase("crc32") else invalidCase(s"crc32")
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

  private def validCase(hash: String): F[Unit] =
    Sync[F].delay(println(s"the video is valid according to $hash hash validation")) *> Sync[F].unit

  private def invalidCase(hash: String): F[Unit] =
    Sync[F].raiseError(VideoCorrupted(s"the video's is invalid as it's hash doesn't match to " +
      s"the $hash hash in the metadata response, the program exits")) *> Sync[F].unit
}
