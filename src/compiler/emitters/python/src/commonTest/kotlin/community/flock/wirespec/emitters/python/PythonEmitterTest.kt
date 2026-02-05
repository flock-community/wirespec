package community.flock.wirespec.emitters.python

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.test.CompileChannelTest
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

class PythonEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(PythonEmitter())
    }

    @Test
    fun testEmitterRefined() {
        val expected = listOf(
            """
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class UUID(Wirespec.Refined):
            |  value: str
            |
            |  def validate(self) -> bool:
            |    return bool(re.match(r"/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/", self.value))
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.refined)
        res shouldBe expected
    }

    @Test
    fun compileFullEndpointTest() {
        val python = """
        |import re
        |
        |from abc import abstractmethod
        |from dataclasses import dataclass
        |from typing import List, Optional
        |from enum import Enum
        |
        |from ..wirespec import T, Wirespec
        |
        |@dataclass
        |class PotentialTodoDto:
        |  name: 'str'
        |  done: 'bool'
        |
        |
        |import re
        |
        |from abc import abstractmethod
        |from dataclasses import dataclass
        |from typing import List, Optional
        |from enum import Enum
        |
        |from ..wirespec import T, Wirespec
        |
        |@dataclass
        |class Token:
        |  iss: 'str'
        |
        |
        |import re
        |
        |from abc import abstractmethod
        |from dataclasses import dataclass
        |from typing import List, Optional
        |from enum import Enum
        |
        |from ..wirespec import T, Wirespec
        |
        |@dataclass
        |class TodoDto:
        |  id: 'str'
        |  name: 'str'
        |  done: 'bool'
        |
        |
        |import re
        |
        |from abc import abstractmethod
        |from dataclasses import dataclass
        |from typing import List, Optional
        |from enum import Enum
        |
        |from ..wirespec import T, Wirespec
        |
        |@dataclass
        |class Error:
        |  code: 'int'
        |  description: 'str'
        |
        |
        |import re
        |
        |from abc import abstractmethod
        |from dataclasses import dataclass
        |from typing import List, Optional
        |from enum import Enum
        |
        |from ..wirespec import T, Wirespec
        |
        |from ..model.Token import Token
        |from ..model.Token import Token
        |from ..model.PotentialTodoDto import PotentialTodoDto
        |from ..model.TodoDto import TodoDto
        |from ..model.Error import Error
        |
        |class PutTodo (Wirespec.Endpoint):
        |  @dataclass
        |  class Request(Wirespec.Request[PotentialTodoDto]):
        |    @dataclass
        |    class Path (Wirespec.Request.Path):
        |        id: str
        |    @dataclass
        |    class Queries (Wirespec.Request.Queries):
        |        done: 'bool'
        |        name: 'Optional[str]'
        |    @dataclass
        |    class Headers (Wirespec.Request.Headers):
        |        token: 'Token'
        |        RefreshToken: 'Optional[Token]'
        |
        |    @property
        |    def body(self) -> PotentialTodoDto:
        |      return self._body
        |
        |    @property
        |    def path(self) -> Path:
        |      return self._path
        |
        |    @property
        |    def queries(self) -> Queries:
        |      return self._queries
        |
        |    @property
        |    def headers(self) -> Headers:
        |      return self._headers
        |
        |    _body:  PotentialTodoDto
        |    _headers: Headers
        |    _queries: Queries
        |    _path: Path
        |    method: Wirespec.Method = Wirespec.Method.PUT
        |
        |    def __init__(self, id: str, done: bool, name: Optional[str], token: Token, RefreshToken: Optional[Token], body: PotentialTodoDto):
        |      self._path = PutTodo.Request.Path(id = id)
        |      self._queries =PutTodo.Request.Queries(  done = done,
        |        name = name)
        |      self._headers = PutTodo.Request.Headers(  token = token,
        |        RefreshToken = RefreshToken)
        |      self._body = body
        |
        |  @dataclass
        |  class Response200(Wirespec.Response[TodoDto]):
        |    @dataclass
        |    class Headers (Wirespec.Response.Headers): pass
        |
        |    @property
        |    def headers(self) -> Headers:
        |      return self._headers
        |
        |    @property
        |    def body(self) -> TodoDto:
        |      return self._body
        |
        |    _body: TodoDto
        |    _headers: Headers
        |    status: int = 200
        |
        |    def __init__(self, body: TodoDto):
        |      self._headers = PutTodo.Response200.Headers()
        |      self._body = body
        |
        |  @dataclass
        |  class Response201(Wirespec.Response[TodoDto]):
        |    @dataclass
        |    class Headers (Wirespec.Response.Headers):
        |        token: 'Token'
        |        refreshToken: 'Optional[Token]'
        |
        |    @property
        |    def headers(self) -> Headers:
        |      return self._headers
        |
        |    @property
        |    def body(self) -> TodoDto:
        |      return self._body
        |
        |    _body: TodoDto
        |    _headers: Headers
        |    status: int = 201
        |
        |    def __init__(self, token: Token, refreshToken: Optional[Token], body: TodoDto):
        |      self._headers = PutTodo.Response201.Headers(  token = token,
        |        refreshToken = refreshToken)
        |      self._body = body
        |
        |  @dataclass
        |  class Response500(Wirespec.Response[Error]):
        |    @dataclass
        |    class Headers (Wirespec.Response.Headers): pass
        |
        |    @property
        |    def headers(self) -> Headers:
        |      return self._headers
        |
        |    @property
        |    def body(self) -> Error:
        |      return self._body
        |
        |    _body: Error
        |    _headers: Headers
        |    status: int = 500
        |
        |    def __init__(self, body: Error):
        |      self._headers = PutTodo.Response500.Headers()
        |      self._body = body
        |
        |  Response = Response200 | Response201 | Response500
        |
        |  class Handler(Wirespec.Endpoint.Handler):
        |    @abstractmethod
        |    def PutTodo(self, req: 'PutTodo.Request') -> 'PutTodo.Response': pass
        |
        |  class Convert(Wirespec.Endpoint.Convert[Request, Response]):
        |    @staticmethod
        |    def to_raw_request(serialization: Wirespec.Serializer, request: 'PutTodo.Request') -> Wirespec.RawRequest:
        |      return Wirespec.RawRequest(
        |        path = ["todos", str(request.path.id)],
        |        method = request.method.value,
        |        queries = {"done": serialization.serialize_param(request.queries.done, bool),
        |    "name": serialization.serialize_param(request.queries.name, str)},
        |        headers = {"token": serialization.serialize_param(request.headers.token, Token),
        |    "Refresh-Token": serialization.serialize_param(request.headers.RefreshToken, Token)},
        |        body = serialization.serialize(request.body, PotentialTodoDto),
        |      )
        |
        |    @staticmethod
        |    def from_raw_request(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest) -> 'PutTodo.Request':
        |      return PutTodo.Request(
        |          id = serialization.deserialize(request.path[1], str),
        |    done = serialization.deserialize_param(request.queries.get("done".lower()), bool),
        |    name = serialization.deserialize_param(request.queries.get("name".lower()), str),
        |    token = serialization.deserialize_param(request.headers.get("token".lower()), Token),
        |    RefreshToken = serialization.deserialize_param(request.headers.get("Refresh-Token".lower()), Token),
        |          body = serialization.deserialize(request.body, PotentialTodoDto),
        |    )
        |
        |    @staticmethod
        |    def to_raw_response(serialization: Wirespec.Serializer, response: 'PutTodo.Response') -> Wirespec.RawResponse:
        |      match response:
        |        case PutTodo.Response200():
        |          return Wirespec.RawResponse(
        |            status_code = response.status,
        |            headers = {},
        |            body = serialization.serialize(response.body, TodoDto),
        |          )
        |        case PutTodo.Response201():
        |          return Wirespec.RawResponse(
        |            status_code = response.status,
        |            headers = {"token": serialization.serialize_param(response.headers.token, Token), "refreshToken": serialization.serialize_param(response.headers.refreshToken, Token)},
        |            body = serialization.serialize(response.body, TodoDto),
        |          )
        |        case PutTodo.Response500():
        |          return Wirespec.RawResponse(
        |            status_code = response.status,
        |            headers = {},
        |            body = serialization.serialize(response.body, Error),
        |          )
        |        case _:
        |          raise Exception("Cannot match response with status: " + str(response.status))
        |    @staticmethod
        |    def from_raw_response(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse) -> 'PutTodo.Response':
        |      match response.status_code:
        |        case 200:
        |          return PutTodo.Response200(
        |            body = serialization.deserialize(response.body, TodoDto),
        |          )
        |        case 201:
        |          return PutTodo.Response201(
        |            body = serialization.deserialize(response.body, TodoDto),
        |            token = serialization.deserialize_param(response.headers.get("token".lower()), Token),
        |            refreshToken = serialization.deserialize_param(response.headers.get("refreshToken".lower()), Token)
        |          )
        |        case 500:
        |          return PutTodo.Response500(
        |            body = serialization.deserialize(response.body, Error),
        |          )
        |        case _:
        |          raise Exception("Cannot match response with status: " + str(response.status_code))
        |
        |
        |
        |from . import model
        |from . import endpoint
        |from . import wirespec
        |
        """.trimMargin()
        CompileFullEndpointTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileMinimalEndpointTest() {
        val python = """
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TodoDto:
            |  description: 'str'
            |
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |from ..model.TodoDto import TodoDto
            |
            |class GetTodos (Wirespec.Endpoint):
            |  @dataclass
            |  class Request(Wirespec.Request[None]):
            |    @dataclass
            |    class Path (Wirespec.Request.Path): pass
            |    @dataclass
            |    class Queries (Wirespec.Request.Queries): pass
            |    @dataclass
            |    class Headers (Wirespec.Request.Headers): pass
            |
            |    @property
            |    def body(self) -> None:
            |      return self._body
            |
            |    @property
            |    def path(self) -> Path:
            |      return self._path
            |
            |    @property
            |    def queries(self) -> Queries:
            |      return self._queries
            |
            |    @property
            |    def headers(self) -> Headers:
            |      return self._headers
            |
            |    _body:  None
            |    _headers: Headers
            |    _queries: Queries
            |    _path: Path
            |    method: Wirespec.Method = Wirespec.Method.GET
            |
            |    def __init__(self, ):
            |      self._path = GetTodos.Request.Path()
            |      self._queries =GetTodos.Request.Queries()
            |      self._headers = GetTodos.Request.Headers()
            |      self._body = None
            |
            |  @dataclass
            |  class Response200(Wirespec.Response[List[TodoDto]]):
            |    @dataclass
            |    class Headers (Wirespec.Response.Headers): pass
            |
            |    @property
            |    def headers(self) -> Headers:
            |      return self._headers
            |
            |    @property
            |    def body(self) -> List[TodoDto]:
            |      return self._body
            |
            |    _body: List[TodoDto]
            |    _headers: Headers
            |    status: int = 200
            |
            |    def __init__(self, body: List[TodoDto]):
            |      self._headers = GetTodos.Response200.Headers()
            |      self._body = body
            |
            |  Response = Response200
            |
            |  class Handler(Wirespec.Endpoint.Handler):
            |    @abstractmethod
            |    def GetTodos(self, req: 'GetTodos.Request') -> 'GetTodos.Response': pass
            |
            |  class Convert(Wirespec.Endpoint.Convert[Request, Response]):
            |    @staticmethod
            |    def to_raw_request(serialization: Wirespec.Serializer, request: 'GetTodos.Request') -> Wirespec.RawRequest:
            |      return Wirespec.RawRequest(
            |        path = ["todos"],
            |        method = request.method.value,
            |        queries = {},
            |        headers = {},
            |        body = serialization.serialize(request.body, type(None)),
            |      )
            |
            |    @staticmethod
            |    def from_raw_request(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest) -> 'GetTodos.Request':
            |      return GetTodos.Request()
            |
            |    @staticmethod
            |    def to_raw_response(serialization: Wirespec.Serializer, response: 'GetTodos.Response') -> Wirespec.RawResponse:
            |      match response:
            |        case GetTodos.Response200():
            |          return Wirespec.RawResponse(
            |            status_code = response.status,
            |            headers = {},
            |            body = serialization.serialize(response.body, List[TodoDto]),
            |          )
            |        case _:
            |          raise Exception("Cannot match response with status: " + str(response.status))
            |    @staticmethod
            |    def from_raw_response(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse) -> 'GetTodos.Response':
            |      match response.status_code:
            |        case 200:
            |          return GetTodos.Response200(
            |            body = serialization.deserialize(response.body, List[TodoDto]),
            |          )
            |        case _:
            |          raise Exception("Cannot match response with status: " + str(response.status_code))
            |
            |
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()
        CompileMinimalEndpointTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileRefinedTest() {
        val python = """
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TodoId(Wirespec.Refined):
            |  value: str
            |
            |  def validate(self) -> bool:
            |    return bool(re.match(r"/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g", self.value))
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TodoNoRegex(Wirespec.Refined):
            |  value: str
            |
            |  def validate(self) -> bool:
            |    return True
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TestInt(Wirespec.Refined):
            |  value: int
            |
            |  def validate(self) -> bool:
            |    return True
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TestInt0(Wirespec.Refined):
            |  value: int
            |
            |  def validate(self) -> bool:
            |    return True
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TestInt1(Wirespec.Refined):
            |  value: int
            |
            |  def validate(self) -> bool:
            |    return 0 < self.value
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TestInt2(Wirespec.Refined):
            |  value: int
            |
            |  def validate(self) -> bool:
            |    return 3 < self.value and self.value < 1
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TestNum(Wirespec.Refined):
            |  value: float
            |
            |  def validate(self) -> bool:
            |    return True
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TestNum0(Wirespec.Refined):
            |  value: float
            |
            |  def validate(self) -> bool:
            |    return True
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TestNum1(Wirespec.Refined):
            |  value: float
            |
            |  def validate(self) -> bool:
            |    return self.value < 0.5
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TestNum2(Wirespec.Refined):
            |  value: float
            |
            |  def validate(self) -> bool:
            |    return -0.2 < self.value and self.value < 0.5
            |
            |  def __str__(self) -> str:
            |    return str(self.value)
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()

        CompileRefinedTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileChannelTest() {
        val result = CompileChannelTest.compiler { PythonEmitter() }
        val expect =
            //language=python
            """
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            """.trimMargin()
        result shouldBeRight expect
    }

    @Test
    fun compileEnumTest() {
        val result = CompileEnumTest.compiler { PythonEmitter() }
        val expect =
            //language=python
            """
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |class MyAwesomeEnum(str, Enum):
            |  ONE = "ONE"
            |  Two = "Two"
            |  THREE_MORE = "THREE_MORE"
            |  UnitedKingdom = "UnitedKingdom"
            |
            |  @property
            |  def label(self) -> str:
            |    return self.value
            |
            |  def __str__(self) -> str:
            |    return self.value
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            """.trimMargin()
        result shouldBeRight expect
    }

    @Test
    fun compileNegativeEnumTest() {
        val result = CompileEnumTest.negativeCompiler { PythonEmitter() }
        val expect =
            //language=python
            """
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |class InnerErrorCode(str, Enum):
            |  _0 = "0"
            |  _1 = "1"
            |  __1 = "-1"
            |  _2 = "2"
            |  __999 = "-999"
            |
            |  @property
            |  def label(self) -> str:
            |    return self.value
            |
            |  def __str__(self) -> str:
            |    return self.value
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            """.trimMargin()
        result shouldBeRight expect
    }

    @Test
    fun compileUnionTest() {
        val result = CompileUnionTest.compiler { PythonEmitter() }
        val expect =
            //language=python
            """
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class UserAccountPassword:
            |  username: 'str'
            |  password: 'str'
            |
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class UserAccountToken:
            |  token: 'str'
            |
            |
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class User:
            |  username: 'str'
            |  account: 'UserAccount'
            |
            |from ..model.UserAccount import UserAccount
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |class UserAccount(ABC):
            |  pass
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            """.trimMargin()
        result shouldBeRight expect
    }

    @Test
    fun compileTypeTest() {
        val result = CompileTypeTest.compiler { PythonEmitter() }
        val expect =
            //language=python
            """
            |import re
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |from enum import Enum
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class Request:
            |  type: 'str'
            |  url: 'str'
            |  BODY_TYPE: 'Optional[str]'
            |  params: 'List[str]'
            |  headers: 'Dict[str, str]'
            |  body: 'Optional[Dict[str, Optional[List[Optional[str]]]]]'
            |
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            """.trimMargin()
        result shouldBeRight expect
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
