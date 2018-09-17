import * as vscode from 'vscode'
import * as fs from 'fs'
import * as path from 'path'
import { LanguageClient } from 'vscode-languageclient'

import { Commands } from './commands'

import { ListTestsRequest, RunTestsRequest, TestStatusNotification } from './extensions/protocol'
import { TestIdentifier, TestStatus, ListTestsParams } from './extensions/types'

// We use strings for keys instead of `TestIdentifier` itself because
// Javascript doesn't have a useful notion of value equality on objects.
type TestIdentifierHandle = string

// * Done
// - Expand "All tests" and test classes
// - Expanding test class should start test run
//   - Hopefully ok because children are cached

// * IDE
// - Actually use status:
//   - When running, show spinning wheel if possible and "Running..." in tooltip
//   - When finished, show green check mark or red cross mark, and actual error in tooltip
//     - Check what happens with exceptions
// - Don't discard the DLC output
// - Figure out if we can get the output tab to be shown when running tests
//     - vscode.commands.executeCommand("...") ? switchOutput probably
//   - ... and get the problem tab to be shown when saving
//     - vscode.commands.executeCommand("workbench.action.problems.focus")
// - Implement compile-on-save:
//   - save all files first
//   - configurable with a setting
//   - for now: compile everything
// - save sbt logs in lsp logs (embed in custom message, maybe telemety or traceEvent ?)
// - Figure out how to start sbt (probably just run sbt from DLS)
// - Strip ansi in sbt logMessage, then color with syntax extension
// - integrate worksheet mode, see gitter discussion
// - Test on Windows

// * Dotty
// - Scaladoc/Javadoc URL when asking for doc

// * Mooc
// - Merge compile and test project
// - Documentation:
//   - Take JDK/sbt page from progfun, cleanup
//   - Write intro to IDE and sbt at the same time
//     - Find recording software
//   - Write intro to worksheet

// Later stuff:
// - Figure out how to keep alive sbt (and the dls itself too)

export class TestProvider implements vscode.TreeDataProvider<TestIdentifierHandle> {
  // <TestIdentifier>
  // map[TestIdentifier, TestStatus] // add unknown status
  // TestIdentifier should have list of ids, prefix: TestIdentifier?, name: String

  private nodesMap: Map<TestIdentifierHandle, TestIdentifier> = new Map()

  private statusMap: Map<TestIdentifierHandle, TestStatus> = new Map()

  private childrenMap: Map<TestIdentifierHandle, Set<TestIdentifierHandle>> = new Map()

	private _onDidChangeTreeData: vscode.EventEmitter<TestIdentifierHandle | undefined> = new vscode.EventEmitter()
	readonly onDidChangeTreeData: vscode.Event<TestIdentifierHandle | undefined> = this._onDidChangeTreeData.event

	constructor(private client: LanguageClient, private workspaceRoot: string, private outputChannel: vscode.OutputChannel) {
	}

  // update(test: TestIdentifier): TestIdentifier {
  //   nodesMap.put(TestIdentifier.fullName(test), test)
  // }

  // element(name: TestIdentifierFullName): TestIdentifier {
  //   const res = nodesMap.get(name)
  //   if (res === undefined) {
      
  //   }
  // }

  handleName(test: TestIdentifier): TestIdentifierHandle {
    return TestIdentifier.isRoot(test) ?
      "." :
      TestIdentifier.fullName(test)
  }

  public getHandle(test: TestIdentifier): TestIdentifierHandle {
    const handle = this.handleName(test)

    if (this.nodesMap.get(handle) === undefined) {
      this.nodesMap.set(handle, test)
    }
    return handle
  }

  public getTest(handle: TestIdentifierHandle): TestIdentifier {
    const elem = this.nodesMap.get(handle)
    if (elem === undefined) {
      throw new TypeError()
    } else {
      return elem
    }
  }

  registerHandle(test: TestIdentifier): TestIdentifierHandle {
    const handle = this.handleName(test)
    this.nodesMap.set(handle, test)
    return handle
  }

  updateTestStatus(status: TestStatus): void {
    console.log(`setStatus`)
    console.log(status)

    const test = status.id
    const testHandle = this.registerHandle(test)
    this.statusMap.set(testHandle, status)

    const parent = TestIdentifier.parent(test)
    console.log(`parent`)
    console.log(parent)

    if (parent === undefined) {
      this._onDidChangeTreeData.fire()
    } else {
      const parentHandle = this.getHandle(parent)
      const siblings = this.childrenMap.get(parentHandle)
      if (siblings === undefined) {
        this.childrenMap.set(parentHandle, new Set( [ testHandle ] ))
      } else {
        siblings.add(testHandle)
      }
      console.log("childrenMap")
      console.log(Array.from(this.childrenMap))

      this._onDidChangeTreeData.fire(testHandle)
    }

    // const p2 = TestIdentifier.parent(parent as TestIdentifier) as TestIdentifier
    // console.log("duck")
    // console.log(p2)
		// this._onDidChangeTreeData.fire(p2)

    // doesn't work
		// this._onDidChangeTreeData.fire(status.id)
		// this._onDidChangeTreeData.fire(parent)

    // works
		// this._onDidChangeTreeData.fire(TestIdentifier.root)
		// this._onDidChangeTreeData.fire()
  }

	refresh(): void {
    console.log("refresh")

    this.nodesMap.clear()
    this.statusMap.clear()
    this.childrenMap.clear()
		this._onDidChangeTreeData.fire()
	}

	getTreeItem(element: TestIdentifierHandle): vscode.TreeItem {
    console.log(`getTreeItem`)
    console.log(element)
    const test = this.getTest(element)
    console.log(`test`)
    console.log(test)

    const state = test.hasChildrenTests ?
      (test.path.length === 0 ? // By default, expand the root and its children
       vscode.TreeItemCollapsibleState.Expanded :
       vscode.TreeItemCollapsibleState.Collapsed) :
      vscode.TreeItemCollapsibleState.None

    if (TestIdentifier.isRoot(test)) {
      return new TestNode(
        "All tests",
        element,
        state,
        {
          command: Commands.BSP_RUN_TESTS,
          title: '',
          arguments: []
        }
      )
    } else {
      // console.log("XXX")
      // console.log(TestIdentifier.name(element))
      // console.log(TestIdentifier.fullName(element))
      // console.log(element.hasChildrenTests ?
      //             vscode.TreeItemCollapsibleState.Collapsed : vscode.TreeItemCollapsibleState.None)
      
      const x = new TestNode(
        TestIdentifier.name(test),
        element,
        state,
        {
          command: Commands.BSP_RUN_TESTS,
          title: '',
          arguments: [ test ]
        }
      )
      // console.log("YYY")
      // console.log(x)
      // console.log("ZZZ")
      return x
    }
  }

  getChildrenInternal(element: TestIdentifierHandle, retry: boolean):
      Thenable<TestIdentifierHandle[]> | TestIdentifierHandle[] {
    const test = this.getTest(element)

    if (test.path.length > 1) {
      const children = this.childrenMap.get(element)
      console.log("children")
      console.log(children)
      if (children === undefined) {
        if (retry) {
          return this.client.sendRequest(RunTestsRequest.type, {
            tests: [ test ]
          }).then(result => this.getChildrenInternal(element, false))
        } else {
          return []
        }
      } else {
        return Array.from(children)
      }
    } else {
      return this.client.sendRequest(ListTestsRequest.type, {
        parents: [ test ]
      }).then(result => result.tests.map(test => this.registerHandle(test)))
    }
  }

  getChildren(element?: TestIdentifierHandle): Thenable<TestIdentifierHandle[]> | TestIdentifierHandle[] {
    console.log(`getChildren`)
    console.log(element)
    // console.log("childrenMap")
    // console.log(this.childrenMap)

    if (element === undefined) {
      const handle = this.registerHandle(TestIdentifier.root)
      console.log("handle")
      console.log(handle)
      return [ handle ]
    } else {
      return this.getChildrenInternal(element, true)
    }
  }
    //   if (element) {
    //     if (element.key === "root") {
    //       const params: ChildrenTestsParams = {
    //         targets: []
    //       }
    //       this.client.sendRequest(ListTestsRequest.type, params)
    //         .then(listTestsResults => {
    //           resolve(
    //             listTestsResults.items
    //               .map(res => new Test(res.id.name, res.id.name, vscode.TreeItemCollapsibleState.None, {
    //                 command: Commands.BSP_RUN_TESTS,
    //                 title: '',
    //                 arguments: [res.id.target.name, res.id.name]
    //               }))
    //           )
    //         })
    //     } else {
    // 		  resolve([])
    //     }
    // 	} else {
    //     resolve([new Test("Run all tests", "root", vscode.TreeItemCollapsibleState.Collapsed)])
    //     // resolve([new Test("Run all tests", vscode.TreeItemCollapsibleState.Collapsed, {
    //     //   command: Commands.BSP_LIST_TESTS,
    //     //   title: '',
    //     //   arguments: []
    //     // })])
    // 	}
    // })

  public getParent(element: TestIdentifierHandle): TestIdentifierHandle | undefined {
    console.log(`getParent`)
    console.log(element)
    const parent = TestIdentifier.parent(this.getTest(element))
    console.log(`parent`)
    console.log(parent)
    return parent === undefined ?
      undefined :
      this.getHandle(parent)
  }
}

class TestNode extends vscode.TreeItem {
  public constructor(
    public readonly label: string,
    public readonly id: string,
    public readonly collapsibleState: vscode.TreeItemCollapsibleState,
    public readonly command?: vscode.Command
  ) {
    super(label, collapsibleState)
  }

  // get tooltip(): string {
  // 	return `${this.label}`
  // }

  // iconPath = {
  // 	light: path.join(__filename, '..', '..', '..', 'resources', 'light', 'dependency.svg'),
  // 	dark: path.join(__filename, '..', '..', '..', 'resources', 'dark', 'dependency.svg')
  // }

  // contextValue = 'test'
}
