package jupyterlsp

import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification

/**
 * A `LanguageClient` that supports repl-specific notifications.
 */
trait ReplClient extends LanguageClient {
  // See WorksheetClient for examples
}

