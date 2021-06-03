package domain

import domain.AppExceptionHandler.AppException

object downloader {
  abstract class DownloaderException(msg: String)  extends AppException(msg)
}
