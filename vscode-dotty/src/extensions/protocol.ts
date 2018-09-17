import { RequestType, NotificationType } from 'vscode-jsonrpc'
// import { TextDocumentRegistrationOptions, StaticRegistrationOptions } from './protocol'
import { ListTestsParams, ListTestsResult, TestIdentifier, RunTestsParams, RunTestsResult, TestStatus } from './types'


export namespace ListTestsRequest {
	export const type = new RequestType<ListTestsParams, ListTestsResult, void, void>('dotty/listTests');
}

export namespace RunTestsRequest {
	export const type = new RequestType<RunTestsParams, RunTestsResult, void, void>('dotty/runTests');
}

export namespace TestStatusNotification {
	export const type = new NotificationType<TestStatus, void>('dotty/testStatus');
}
