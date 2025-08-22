package fmgp.did.discord

object Secrets {
  val mongoDBConnectionPreprodReadOnly: String = ???
  val mongoDBConnectionPreprodReadAndWrite: String = ???
  val mongoDBConnectionPreprodReadAndWriteLocalhost: String = ???
  def mongoDBConnection = mongoDBConnectionPreprodReadAndWriteLocalhost
  val blockfrostToken = ???
}
