package sbt
package dottyplugin

import sbt._
import sbt.Keys._

import sbt.internal.server._

import sjsonnew.JsonFormat

/** Contains code that calls into `private[sbt]` values and thus can only be
 *  accessed a class defined in the `sbt` package.

 *  FIXME: Eventually this file shouldn't be needed.
 */
object Restricted {
  lazy val exchange = StandardMain.exchange

  def jsonRpcRespond[A: JsonFormat](lsp: LanguageServerProtocol, event: A, execId: Option[String]): Unit =
    lsp.jsonRpcRespond(event, execId)

  def jsonRpcNotify[A: JsonFormat](lsp: LanguageServerProtocol, method: String, params: A): Unit =
    lsp.jsonRpcNotify(method, params)

  val dummyLayout = sbt.util.LogExchange.dummyLayout

  // def notifyEvent[A: JsonFormat](method: String, params: A): Unit =
  //   StandardMain.exchange.notifyEvent(method, params)
}
