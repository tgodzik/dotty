package dotty.tools.buildprotocol.services;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.*;

import dotty.tools.buildprotocol.*;

public interface BuildClient extends LanguageClient {
	@JsonNotification("dotty/testStatus")
	void testStatus(TestStatus status);
}
