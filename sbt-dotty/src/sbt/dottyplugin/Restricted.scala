package sbt
package dottyplugin

import sbt._
import sbt.Keys._

import sjsonnew.JsonFormat

/** Contains code that calls into `private[sbt]` values and thus can only be
 *  accessed a class defined in the `sbt` package.

 *  FIXME: Eventually this file shouldn't be needed.
 */
object Restricted {
  def notifyEvent[A: JsonFormat](method: String, params: A): Unit =
    StandardMain.exchange.notifyEvent(method, params)
}
