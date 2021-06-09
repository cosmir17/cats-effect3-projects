package modules

import modules.HashHandler.Hashes

trait HashesTestTrait {
  protected val normalText   = "abcdefg_string"
  protected val normalSha1   = "72900d8bb6c7f8156ff70cd53b5c600cd1e80708"
  protected val normalSha256 = "078c67d095ad4643b45c67630dc1b78b2a1cb5f79bfb4b2f4a1f8ca1a0d2a257"
  protected val normalMd5    = "003e6c84ef4a5cdc271aed02440dc1be"
  protected val normalCrc32  = "297f3723"
  protected val normalHashes = Hashes(normalSha1, normalSha256, normalMd5, normalCrc32)
}
