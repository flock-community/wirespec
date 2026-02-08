package community.flock.wirespec.emitters.typescript

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEndpointWithRefinedTypeTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TypeScriptEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(TypeScriptEmitter())
    }

    @Test
    fun testEmitterRefined() {
        val expected = listOf(
            """
            |import {Wirespec} from '../Wirespec'
            |
            |export type UUID = string;
            |export const validateUUID = (value: string): value is UUID => {
            |  return /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/.test(value);
            |}
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.refined)
        res shouldBe expected
    }

    @Test
    fun compileFullEndpointTest() {
        val ts = """
            |import {Wirespec} from '../Wirespec'
            |
            |import {type Token} from '../model'
            |import {type PotentialTodoDto} from '../model'
            |import {type TodoDto} from '../model'
            |import {type Error} from '../model'
            |export namespace PutTodo {
            |  type Path = {
            |    "id": string,
            |  }
            |  type Queries = {
            |    "done": boolean,
            |    "name": string | undefined,
            |  }
            |  type Headers = {
            |    "token": Token,
            |    "Refresh-Token": Token | undefined,
            |  }
            |  export type Request = {
            |    path: Path
            |    method: "PUT"
            |    queries: Queries
            |    headers: Headers
            |    body: PotentialTodoDto
            |  }
            |  export type Response200 = {
            |    status: 200
            |    headers: {}
            |    body: TodoDto
            |  }
            |  export type Response201 = {
            |    status: 201
            |    headers: {"token": Token, "refreshToken": Token | undefined}
            |    body: TodoDto
            |  }
            |  export type Response500 = {
            |    status: 500
            |    headers: {}
            |    body: Error
            |  }
            |  export type Response = Response200 | Response201 | Response500
            |  export type RequestParams = {"id": string, "done": boolean, "name"?: string, "token": Token, "Refresh-Token"?: Token, "body": PotentialTodoDto}
            |  export const request = (params: RequestParams): Request => ({
            |    path: {"id": params["id"]},
            |    method: "PUT",
            |    queries: {"done": params["done"], "name": params["name"]},
            |    headers: {"token": params["token"], "Refresh-Token": params["Refresh-Token"]},
            |    body: params.body,
            |  })
            |  export type Response200Params = {"body": TodoDto}
            |  export const response200 = (params: Response200Params): Response200 => ({
            |    status: 200,
            |    headers: {},
            |    body: params.body,
            |  })
            |  export type Response201Params = {"token": Token, "refreshToken"?: Token, "body": TodoDto}
            |  export const response201 = (params: Response201Params): Response201 => ({
            |    status: 201,
            |    headers: {"token": params["token"], "refreshToken": params["refreshToken"]},
            |    body: params.body,
            |  })
            |  export type Response500Params = {"body": Error}
            |  export const response500 = (params: Response500Params): Response500 => ({
            |    status: 500,
            |    headers: {},
            |    body: params.body,
            |  })
            |  export type Handler = {
            |    putTodo: (request:Request) => Promise<Response>
            |  }
            |  export const client: Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    to: (it) => ({
            |      method: "PUT",
            |      path: ["todos", serialization.serialize(it.path["id"])],
            |      queries: {"done": serialization.serialize(it.queries["done"]), "name": serialization.serialize(it.queries["name"])},
            |      headers: {"token": serialization.serialize(it.headers["token"]), "Refresh-Token": serialization.serialize(it.headers["Refresh-Token"])},
            |      body: serialization.serialize(it.body)
            |    }),
            |    from: (it) => {
            |      switch (it.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {},
            |            body: serialization.deserialize<TodoDto>(it.body)
            |          };
            |        case 201:
            |          return {
            |            status: 201,
            |            headers: {"token": serialization.deserialize(Object.entries(it.headers).find(([key]) => key.toLowerCase() === "token")?.[1]), "refreshToken": serialization.deserialize(Object.entries(it.headers).find(([key]) => key.toLowerCase() === "refreshtoken")?.[1])},
            |            body: serialization.deserialize<TodoDto>(it.body)
            |          };
            |        case 500:
            |          return {
            |            status: 500,
            |            headers: {},
            |            body: serialization.deserialize<Error>(it.body)
            |          };
            |        default:
            |          throw new Error(`Cannot internalize response with status: ${'$'}{it.status}`);
            |      }
            |    }
            |  })
            |  export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (it) => {
            |      return {
            |        method: "PUT",
            |        path: {
            |          "id": serialization.deserialize(it.path[1])
            |        },
            |        queries: {
            |          "done": serialization.deserialize(it.queries["done"]),      "name": serialization.deserialize(it.queries["name"])
            |        },
            |        headers: {
            |          "token": serialization.deserialize(Object.entries(it.headers).find(([key]) => key.toLowerCase() === "token")?.[1]),      "Refresh-Token": serialization.deserialize(Object.entries(it.headers).find(([key]) => key.toLowerCase() === "refresh-token")?.[1])
            |        },
            |        body: serialization.deserialize(it.body)
            |      }
            |    },
            |    to: (it) => {
            |      switch (it.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {} as Record<string, string>,
            |            body: serialization.serialize(it.body),
            |          };
            |        case 201:
            |          return {
            |            status: 201,
            |            headers: {"token": serialization.serialize(it.headers["token"]), "refreshToken": serialization.serialize(it.headers["refreshToken"])} as Record<string, string>,
            |            body: serialization.serialize(it.body),
            |          };
            |        case 500:
            |          return {
            |            status: 500,
            |            headers: {} as Record<string, string>,
            |            body: serialization.serialize(it.body),
            |          };
            |      }
            |    }
            |  })
            |  export const api = {
            |    name: "putTodo",
            |    method: "PUT",
            |    path: "todos/:id",
            |    server,
            |    client
            |  } as const
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type PotentialTodoDto = {
            |  "name": string,
            |  "done": boolean
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type Token = {
            |  "iss": string
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type TodoDto = {
            |  "id": string,
            |  "name": string,
            |  "done": boolean
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type Error = {
            |  "code": number,
            |  "description": string
            |}
            |
            |import {Wirespec} from "./Wirespec"
            |
            |import {PutTodo} from "./endpoint/PutTodo"
            |
            |import {type Token} from "./model/Token"
            |import {type PotentialTodoDto} from "./model/PotentialTodoDto"
            |import {type TodoDto} from "./model/TodoDto"
            |import {type Error} from "./model/Error"
            |
            |type RawHandler = (req: Wirespec.RawRequest) => Promise<Wirespec.RawResponse>
            |
            |export const client = (serialization: Wirespec.Serialization, handler: RawHandler) => ({
            |  PutTodo: async (params: {"id": string, "done": boolean, "name": string | undefined, "token": Token, "Refresh-Token": Token | undefined, "body": PotentialTodoDto}) => {
            |    const req = PutTodo.request(params)
            |    const rawRequest = PutTodo.client(serialization).to(req)
            |    const rawResponse = await handler(rawRequest)
            |    return PutTodo.client(serialization).from(rawResponse)
            |  },
            |})
            |
            |export {PutTodo} from './PutTodo'
            |export {PotentialTodoDto} from './PotentialTodoDto'
            |export {Token} from './Token'
            |export {TodoDto} from './TodoDto'
            |export {Error} from './Error'
        """.trimMargin()

        CompileFullEndpointTest.compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun compileMinimalEndpointTest() {
        val ts = """
            |import {Wirespec} from '../Wirespec'
            |
            |import {type TodoDto} from '../model'
            |export namespace GetTodos {
            |  type Path = {}
            |  type Queries = {}
            |  type Headers = {}
            |  export type Request = {
            |    path: Path
            |    method: "GET"
            |    queries: Queries
            |    headers: Headers
            |    body: undefined
            |  }
            |  export type Response200 = {
            |    status: 200
            |    headers: {}
            |    body: TodoDto[]
            |  }
            |  export type Response = Response200
            |  export type RequestParams = {}
            |  export const request = (): Request => ({
            |    path: {},
            |    method: "GET",
            |    queries: {},
            |    headers: {},
            |    body: undefined,
            |  })
            |  export type Response200Params = {"body": TodoDto[]}
            |  export const response200 = (params: Response200Params): Response200 => ({
            |    status: 200,
            |    headers: {},
            |    body: params.body,
            |  })
            |  export type Handler = {
            |    getTodos: (request:Request) => Promise<Response>
            |  }
            |  export const client: Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    to: (it) => ({
            |      method: "GET",
            |      path: ["todos"],
            |      queries: {},
            |      headers: {},
            |      body: serialization.serialize(it.body)
            |    }),
            |    from: (it) => {
            |      switch (it.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {},
            |            body: serialization.deserialize<TodoDto[]>(it.body)
            |          };
            |        default:
            |          throw new Error(`Cannot internalize response with status: ${'$'}{it.status}`);
            |      }
            |    }
            |  })
            |  export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (it) => {
            |      return {
            |        method: "GET",
            |        path: {
            |      
            |        },
            |        queries: {
            |  
            |        },
            |        headers: {
            |  
            |        },
            |        body: serialization.deserialize(it.body)
            |      }
            |    },
            |    to: (it) => {
            |      switch (it.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {} as Record<string, string>,
            |            body: serialization.serialize(it.body),
            |          };
            |      }
            |    }
            |  })
            |  export const api = {
            |    name: "getTodos",
            |    method: "GET",
            |    path: "todos",
            |    server,
            |    client
            |  } as const
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type TodoDto = {
            |  "description": string
            |}
            |
            |import {Wirespec} from "./Wirespec"
            |
            |import {GetTodos} from "./endpoint/GetTodos"
            |
            |import {type TodoDto} from "./model/TodoDto"
            |
            |type RawHandler = (req: Wirespec.RawRequest) => Promise<Wirespec.RawResponse>
            |
            |export const client = (serialization: Wirespec.Serialization, handler: RawHandler) => ({
            |  GetTodos: async () => {
            |    const req = GetTodos.request()
            |    const rawRequest = GetTodos.client(serialization).to(req)
            |    const rawResponse = await handler(rawRequest)
            |    return GetTodos.client(serialization).from(rawResponse)
            |  },
            |})
            |
            |export {GetTodos} from './GetTodos'
            |export {TodoDto} from './TodoDto'
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun compileChannelTest() {
        val ts = """
            |import {Wirespec} from '../Wirespec'
            |
            |// TODO("Not yet implemented")
            |
            |export {Queue} from './Queue'
        """.trimMargin()

        CompileChannelTest.compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun compileEnumTest() {
        val ts = """
            |import {Wirespec} from '../Wirespec'
            |
            |export type MyAwesomeEnum = "ONE" | "Two" | "THREE_MORE" | "UnitedKingdom"
            |
            |export {MyAwesomeEnum} from './MyAwesomeEnum'
        """.trimMargin()

        CompileEnumTest.compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun compileRefinedTest() {
        val ts = """
            |import {Wirespec} from '../Wirespec'
            |
            |export type TodoId = string;
            |export const validateTodoId = (value: string): value is TodoId => {
            |  return /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g.test(value);
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TodoNoRegex = string;
            |export const validateTodoNoRegex = (value: string): value is TodoNoRegex => {
            |  return true;
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TestInt = number;
            |export const validateTestInt = (value: number): value is TestInt => {
            |  return true;
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TestInt0 = number;
            |export const validateTestInt0 = (value: number): value is TestInt0 => {
            |  return true;
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TestInt1 = number;
            |export const validateTestInt1 = (value: number): value is TestInt1 => {
            |  return 0 < value;
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TestInt2 = number;
            |export const validateTestInt2 = (value: number): value is TestInt2 => {
            |  return 3 < value && value < 1;
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TestNum = number;
            |export const validateTestNum = (value: number): value is TestNum => {
            |  return true;
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TestNum0 = number;
            |export const validateTestNum0 = (value: number): value is TestNum0 => {
            |  return true;
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TestNum1 = number;
            |export const validateTestNum1 = (value: number): value is TestNum1 => {
            |  return value < 0.5;
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TestNum2 = number;
            |export const validateTestNum2 = (value: number): value is TestNum2 => {
            |  return -0.2 < value && value < 0.5;
            |}
            |
            |export {TodoId} from './TodoId'
            |export {TodoNoRegex} from './TodoNoRegex'
            |export {TestInt} from './TestInt'
            |export {TestInt0} from './TestInt0'
            |export {TestInt1} from './TestInt1'
            |export {TestInt2} from './TestInt2'
            |export {TestNum} from './TestNum'
            |export {TestNum0} from './TestNum0'
            |export {TestNum1} from './TestNum1'
            |export {TestNum2} from './TestNum2'
        """.trimMargin()

        CompileRefinedTest.compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun compileEndpointWithRefinedTypeTest() {
        val ts = """
            |import {Wirespec} from '../Wirespec'
            |
            |export type TodoId = string;
            |export const validateTodoId = (value: string): value is TodoId => {
            |  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}${'$'}/g.test(value);
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |import {type TodoId} from '../model'
            |import {type TodoDto} from '../model'
            |export namespace GetTodoById {
            |  type Path = {
            |    "id": TodoId,
            |  }
            |  type Queries = {}
            |  type Headers = {}
            |  export type Request = {
            |    path: Path
            |    method: "GET"
            |    queries: Queries
            |    headers: Headers
            |    body: undefined
            |  }
            |  export type Response200 = {
            |    status: 200
            |    headers: {}
            |    body: TodoDto
            |  }
            |  export type Response = Response200
            |  export type RequestParams = {"id": TodoId}
            |  export const request = (params: RequestParams): Request => ({
            |    path: {"id": params["id"]},
            |    method: "GET",
            |    queries: {},
            |    headers: {},
            |    body: undefined,
            |  })
            |  export type Response200Params = {"body": TodoDto}
            |  export const response200 = (params: Response200Params): Response200 => ({
            |    status: 200,
            |    headers: {},
            |    body: params.body,
            |  })
            |  export type Handler = {
            |    getTodoById: (request:Request) => Promise<Response>
            |  }
            |  export const client: Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    to: (it) => ({
            |      method: "GET",
            |      path: ["todos", serialization.serialize(it.path["id"])],
            |      queries: {},
            |      headers: {},
            |      body: serialization.serialize(it.body)
            |    }),
            |    from: (it) => {
            |      switch (it.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {},
            |            body: serialization.deserialize<TodoDto>(it.body)
            |          };
            |        default:
            |          throw new Error(`Cannot internalize response with status: ${'$'}{it.status}`);
            |      }
            |    }
            |  })
            |  export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (it) => {
            |      return {
            |        method: "GET",
            |        path: {
            |          "id": serialization.deserialize(it.path[1])
            |        },
            |        queries: {
            |  
            |        },
            |        headers: {
            |  
            |        },
            |        body: serialization.deserialize(it.body)
            |      }
            |    },
            |    to: (it) => {
            |      switch (it.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {} as Record<string, string>,
            |            body: serialization.serialize(it.body),
            |          };
            |      }
            |    }
            |  })
            |  export const api = {
            |    name: "getTodoById",
            |    method: "GET",
            |    path: "todos/:id",
            |    server,
            |    client
            |  } as const
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type TodoDto = {
            |  "description": string
            |}
            |
            |import {Wirespec} from "./Wirespec"
            |
            |import {GetTodoById} from "./endpoint/GetTodoById"
            |
            |import {type TodoId} from "./model/TodoId"
            |import {type TodoDto} from "./model/TodoDto"
            |
            |type RawHandler = (req: Wirespec.RawRequest) => Promise<Wirespec.RawResponse>
            |
            |export const client = (serialization: Wirespec.Serialization, handler: RawHandler) => ({
            |  GetTodoById: async (params: {"id": TodoId}) => {
            |    const req = GetTodoById.request(params)
            |    const rawRequest = GetTodoById.client(serialization).to(req)
            |    const rawResponse = await handler(rawRequest)
            |    return GetTodoById.client(serialization).from(rawResponse)
            |  },
            |})
            |
            |export {TodoId} from './TodoId'
            |export {TodoDto} from './TodoDto'
            |export {GetTodoById} from './GetTodoById'
        """.trimMargin()

        CompileEndpointWithRefinedTypeTest.compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun compileUnionTest() {
        val ts = """
            |import {Wirespec} from '../Wirespec'
            |
            |import {type UserAccountPassword} from '../model'
            |import {type UserAccountToken} from '../model'
            |export type UserAccount = UserAccountPassword | UserAccountToken
            |
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type UserAccountPassword = {
            |  "username": string,
            |  "password": string
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type UserAccountToken = {
            |  "token": string
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |import {UserAccount} from './UserAccount'
            |export type User = {
            |  "username": string,
            |  "account": UserAccount
            |}
            |
            |export {UserAccount} from './UserAccount'
            |export {UserAccountPassword} from './UserAccountPassword'
            |export {UserAccountToken} from './UserAccountToken'
            |export {User} from './User'
        """.trimMargin()

        CompileUnionTest.compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun compileTypeTest() {
        val ts = """
            |import {Wirespec} from '../Wirespec'
            |
            |
            |export type Request = {
            |  "type": string,
            |  "url": string,
            |  "BODY_TYPE": string | undefined,
            |  "params": string[],
            |  "headers": Record<string, string>,
            |  "body": Record<string, string | undefined[] | undefined> | undefined
            |}
            |
            |export {Request} from './Request'
        """.trimMargin()

        CompileTypeTest.compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    private fun EmitContext.emitFirst(node: Definition) = emitters.map {
        val ast = AST(
            nonEmptyListOf(
                Module(
                    FileUri(""),
                    nonEmptyListOf(node),
                ),
            ),
        )
        it.emit(ast, logger).first().result
    }
}
