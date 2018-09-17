import * as vscode from 'vscode'
import * as fs from 'fs'
import * as path from 'path'
import { LanguageClient } from 'vscode-languageclient'

import { Commands } from './commands'

import { ListTestsRequest, TestStatusNotification } from './extensions/protocol'
import { TestIdentifier, TestStatus, ListTestsParams } from './extensions/types'

// We use strings for keys instead of `TestIdentifier` because `Map` in
// Javascript does not use value equality.
type TestIdentifierFullName = string

export class TestProvider implements vscode.TreeDataProvider<TestIdentifier> {
  // <TestIdentifier>
  // map[TestIdentifier, TestStatus] // add unknown status
  // TestIdentifier should have list of ids, prefix: TestIdentifier?, name: String

  private statusMap: Map<TestIdentifierFullName, TestStatus> = new Map()

  private childrenMap: Map<TestIdentifierFullName, TestIdentifier[]> = new Map()

	private _onDidChangeTreeData: vscode.EventEmitter<TestIdentifier | undefined> = new vscode.EventEmitter()
	readonly onDidChangeTreeData: vscode.Event<TestIdentifier | undefined> = this._onDidChangeTreeData.event

	constructor(private client: LanguageClient, private workspaceRoot: string, private outputChannel: vscode.OutputChannel) {
	}

  updateTestStatus(status: TestStatus): void {
    console.log(`setStatus`)
    console.log(status)

    const test = status.id
    this.statusMap.set(TestIdentifier.fullName(test), status)

    const parent = TestIdentifier.parent(test)
    console.log(`parent`)
    console.log(parent)
    if (parent !== undefined) {
      const parentKey = TestIdentifier.fullName(parent)
      const siblings = this.childrenMap.get(parentKey)
      if (siblings === undefined) {
        this.childrenMap.set(parentKey, [ test ])
      } else {
        const updatedChildren = siblings
          .filter(sibling => TestIdentifier.fullName(sibling) != TestIdentifier.fullName(test))
          .concat( [ test ] )
        this.childrenMap.set(parentKey, updatedChildren)
      }
    }
    console.log("childrenMap")
    console.log(Array.from(this.childrenMap))

		this._onDidChangeTreeData.fire(status.id)
  }

	refresh(): void {
    console.log("refresh")

    this.statusMap.clear()
    this.childrenMap.clear()
		this._onDidChangeTreeData.fire()
	}

	getTreeItem(element: TestIdentifier): vscode.TreeItem {
    // console.log(`getTreeItem`)
    // console.log(element)
    //   console.log("AAAA")

    if (TestIdentifier.isRoot(element)) {
      return new TestNode(
        "All tests",
        ".",
        vscode.TreeItemCollapsibleState.Collapsed,
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
        TestIdentifier.name(element),
        TestIdentifier.fullName(element),
        element.hasChildrenTests ?
          vscode.TreeItemCollapsibleState.Collapsed : vscode.TreeItemCollapsibleState.None,
        {
          command: Commands.BSP_RUN_TESTS,
          title: '',
          arguments: [ element ]
        }
      )
      // console.log("YYY")
      // console.log(x)
      // console.log("ZZZ")
      return x
    }
  }

  getChildren(element?: TestIdentifier): Thenable<TestIdentifier[]> | TestIdentifier[] {
    console.log(`getChildren`)
    console.log(element)
    // console.log("childrenMap")
    // console.log(this.childrenMap)

    if (element === undefined) {
      return [TestIdentifier.root]
    } else if (element.path.length > 1) {
      const children = this.childrenMap.get(TestIdentifier.fullName(element))
      return children === undefined ? [] : Array.from(children)
    } else {
      return this.client.sendRequest(ListTestsRequest.type, {
        parents: [ element ]
      }).then(result => result.tests)
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
  }

  getParent(element: TestIdentifier): TestIdentifier | undefined {
    // console.log(`getParent`)
    // console.log(element)
    const parent = TestIdentifier.parent(element)
    // console.log(`parent`)
    // console.log(parent)
    return parent
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
