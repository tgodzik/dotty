package dotty.tools
package languageserver

import java.net._
import java.io._
import java.nio.file._
import java.util.concurrent.CompletableFuture
import java.util.function.Function

import com.fasterxml.jackson.databind.ObjectMapper

import org.eclipse.lsp4j
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.services._
import org.eclipse.lsp4j.jsonrpc.Launcher

import buildprotocol._
import buildprotocol.services._

import org.scalasbt.ipcsocket._

import scala.collection._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.io.Codec

import config.SbtPortFile

trait SbtServer extends LanguageServer with BuildService

object SbtClient {
  def apply(languageServer: DottyLanguageServer): SbtClient = {
    val configFile = new File("/home/smarter/opt/dotty/project/target/active.json")

    val config = (new ObjectMapper).readValue(configFile, classOf[SbtPortFile])
    val uri = config.uri

    val socket = uri.getScheme match {
      case "local" => new UnixDomainSocket(uri.getSchemeSpecificPart)
      case _ => sys.error(s"Unsupported uri: $uri")
    }

    val client = new SbtClient(languageServer)

    val writer = new PrintWriter(new File("/home/smarter/opt/dotty/sbt-server.log"))
    // val writer = new PrintWriter(System.err, true)

    val launcher = Launcher.createLauncher(client, classOf[SbtServer],
      socket.getInputStream, socket.getOutputStream, /*validate =*/ false,  writer)
    launcher.startListening()
    val server = launcher.getRemoteProxy
    client.server = server

    val params = new InitializeParams
    server.initialize(params)

    client
  }

  // def main2(args: Array[String]): Unit = {
  //   val client = SbtClient(null)

  //   // val execParams = new SbtExecParams("compile")
  //   // // server.sbtExec(execParams)
  //   val testParams = new ListTestsParams(List(new BuildTargetIdentifier("dotty-compiler/test")).asJava)
  //   val r = client.server.listTests(testParams)
  //   println("r: " + r.get)
  //   // socket.close()
  //   // System.exit(0)
  // }
}

class SbtClient(val languageServer: DottyLanguageServer) extends BuildClient { thisClient =>

  import lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
  import lsp4j.jsonrpc.messages.{Either => JEither}
  // import lsp4j._

  var server: SbtServer = _

  private[this] var rootUri: String = _

  // private[this] var myDrivers: mutable.Map[ProjectConfig, InteractiveDriver] = _

  private[this] def computeAsync[R](fun: CancelChecker => R): CompletableFuture[R] =
    CompletableFutures.computeAsync { cancelToken =>
      // We do not support any concurrent use of the compiler currently.
      thisClient.synchronized {
        cancelToken.checkCanceled()
        try {
          fun(cancelToken)
        } catch {
          case NonFatal(ex) =>
            ex.printStackTrace
            throw ex
        }
      }
    }

  override def logMessage(params: MessageParams): Unit = {
    // languageServer.client.logMessage(params)
  }

  override def testStatus(status: TestStatus): Unit = {
    languageServer.client.testStatus(status)
  }

  override def publishDiagnostics(params: PublishDiagnosticsParams): Unit = {
    languageServer.client.publishDiagnostics(params)
  }
  override def showMessage(params: MessageParams): Unit = {
    languageServer.client.showMessage(params)
  }
  override def showMessageRequest(params: ShowMessageRequestParams): CompletableFuture[MessageActionItem] = null
  override def telemetryEvent(event: Any): Unit = {
    languageServer.client.telemetryEvent(event)
  }

  // override def initialize(params: InitializeParams) = computeAsync { cancelToken =>
  //   rootUri = params.getRootUri
  //   assert(rootUri != null)

  //   val c = new ServerCapabilities
  //   c.setTextDocumentSync(TextDocumentSyncKind.Full)
  //   c.setDocumentHighlightProvider(true)
  //   c.setDocumentSymbolProvider(true)
  //   c.setDefinitionProvider(true)
  //   c.setRenameProvider(true)
  //   c.setHoverProvider(true)
  //   c.setWorkspaceSymbolProvider(true)
  //   c.setReferencesProvider(true)
  //   c.setCompletionProvider(new CompletionOptions(
  //     /* resolveProvider = */ false,
  //     /* triggerCharacters = */ List(".").asJava))

  //   // Do most of the initialization asynchronously so that we can return early
  //   // from this method and thus let the client know our capabilities.
  //   CompletableFuture.supplyAsync(() => drivers)
  //     .exceptionally { (ex: Throwable) =>
  //       ex.printStackTrace
  //       sys.exit(1)
  //     }

  //   new InitializeResult(c)
  // }
}
