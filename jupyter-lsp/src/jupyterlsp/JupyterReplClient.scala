package jupyterlsp

import java.net._
import java.io._
import java.nio.file._
import java.util.concurrent.CompletableFuture
import java.util.function.Function

import org.eclipse.lsp4j
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.services._
import org.eclipse.lsp4j.jsonrpc.Launcher

import scala.collection._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.io.Codec
import scala.util.Properties

trait Server extends LanguageServer with ReplService

object JupyterReplClient {
  /** Create a new client connected to the REPL server at the given port number. */
  def apply(port: Int): JupyterReplClient = {
    val client = new JupyterReplClient

    // val writer = new PrintWriter(new File("lsp-client.log"))
    val writer = new PrintWriter(System.err, true)

    val socket = new Socket("localhost", port)

    val launcher = Launcher.createLauncher(client, classOf[Server],
      socket.getInputStream, socket.getOutputStream, /*validate =*/ false,  writer)
    launcher.startListening()
    val server = launcher.getRemoteProxy
    client.server = server

    val params = new InitializeParams
    server.initialize(params)

    client
  }
}

class JupyterReplClient extends ReplClient { thisClient =>

  import lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
  import lsp4j.jsonrpc.messages.{Either => JEither}

  private var server: Server = _

  override def logMessage(params: MessageParams): Unit = {}
  override def showMessage(params: MessageParams): Unit = {}
  override def showMessageRequest(params: ShowMessageRequestParams): CompletableFuture[MessageActionItem] = null
  override def publishDiagnostics(params: PublishDiagnosticsParams): Unit = {}
  override def telemetryEvent(params: Any): Unit = {}
}
