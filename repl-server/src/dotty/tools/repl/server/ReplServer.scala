package dotty.tools
package repl
package server

import java.net.URI
import java.io._
import java.nio.file._
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap}
import java.util.function.Function

import org.eclipse.lsp4j

import scala.collection._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.io.Codec

import lsp4j.services._

import jupyterlsp.{ReplClient, ReplService}

/** A Language Server that runs an instance of the Dotty REPL.
 */
class ReplServer extends LanguageServer
    with TextDocumentService with WorkspaceService with ReplService { thisServer =>
  import lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
  import lsp4j.jsonrpc.messages.{Either => JEither}
  import lsp4j._


  private[this] var rootUri: String = _

  private[this] var myClient: ReplClient = _
  def client: ReplClient = myClient

  def connect(client: ReplClient): Unit = {
    myClient = client
  }

  override def exit(): Unit = {
    System.exit(0)
  }

  override def shutdown(): CompletableFuture[Object] = {
    CompletableFuture.completedFuture(new Object)
  }

  def computeAsync[R](fun: CancelChecker => R): CompletableFuture[R] =
    CompletableFutures.computeAsync { cancelToken =>
      // We do not support any concurrent use of the compiler currently.
      thisServer.synchronized {
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

  override def initialize(params: InitializeParams) = computeAsync { cancelToken =>
    rootUri = params.getRootUri
    assert(rootUri != null)

    val c = new ServerCapabilities
    c.setTextDocumentSync(TextDocumentSyncKind.Full)
    c.setHoverProvider(true)
    c.setDocumentHighlightProvider(false)
    c.setDocumentSymbolProvider(false)
    c.setDefinitionProvider(false)
    c.setRenameProvider(false)
    c.setWorkspaceSymbolProvider(false)
    c.setReferencesProvider(false)
    c.setCompletionProvider(new CompletionOptions(
      /* resolveProvider = */ false,
      /* triggerCharacters = */ List(".").asJava))

    new InitializeResult(c)
  }

  override def didOpen(params: DidOpenTextDocumentParams): Unit = thisServer.synchronized {
    val document = params.getTextDocument
    val uri = new URI(document.getUri)

    ???
  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = thisServer.synchronized {
    val document = params.getTextDocument
    val uri = new URI(document.getUri)

    ???
  }

  override def didClose(params: DidCloseTextDocumentParams): Unit = thisServer.synchronized {
    val document = params.getTextDocument
    val uri = new URI(document.getUri)

    ???
  }

  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit =
    /*thisServer.synchronized*/ {}

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit =
    /*thisServer.synchronized*/ {}

  override def didSave(params: DidSaveTextDocumentParams): Unit = {
    /*thisServer.synchronized*/ {}
  }

  override def completion(params: CompletionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)

    JEither.forRight(new CompletionList(
      /*isIncomplete = */ false, Nil.asJava))
  }


  override def hover(params: TextDocumentPositionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)

    val markup = new lsp4j.MarkupContent
    markup.setKind("markdown")
    markup.setValue("todo")

    new Hover(markup, null)
  }

  override def getTextDocumentService: TextDocumentService = this
  override def getWorkspaceService: WorkspaceService = this

  // Unimplemented features. If you implement one of them, you may need to add a
  // capability in `initialize`
  override def definition(params: TextDocumentPositionParams) = null
  override def references(params: ReferenceParams) = null
  override def rename(params: RenameParams) = null
  override def documentHighlight(params: TextDocumentPositionParams) = null
  override def documentSymbol(params: DocumentSymbolParams) = null
  override def symbol(params: WorkspaceSymbolParams) = null
  override def codeAction(params: CodeActionParams) = null
  override def codeLens(params: CodeLensParams) = null
  override def formatting(params: DocumentFormattingParams) = null
  override def rangeFormatting(params: DocumentRangeFormattingParams) = null
  override def onTypeFormatting(params: DocumentOnTypeFormattingParams) = null
  override def resolveCodeLens(params: CodeLens) = null
  override def resolveCompletionItem(params: CompletionItem) = null
  override def signatureHelp(params: TextDocumentPositionParams) = null
}
