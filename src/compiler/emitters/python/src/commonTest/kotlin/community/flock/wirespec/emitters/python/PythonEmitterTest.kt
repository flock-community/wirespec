package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class PythonEmitterTest {

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
        |        refreshToken: 'Optional[Token]'
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
        |    def __init__(self, id: str, done: bool, name: Optional[str], token: Token, refreshToken: Optional[Token], body: PotentialTodoDto):
        |      self._path = PutTodo.Request.Path(id = id)
        |      self._queries =PutTodo.Request.Queries(  done = done,
        |        name = name)
        |      self._headers = PutTodo.Request.Headers(  token = token,
        |        refreshToken = refreshToken)
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
        |    "refreshToken": serialization.serialize_param(request.headers.refreshToken, Token)},
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
        |    refreshToken = serialization.deserialize_param(request.headers.get("refreshToken".lower()), Token),
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
        |from . import endpoint
        |
        |from typing import List, Optional
        |
        |from .model.Token import Token
        |from .model.PotentialTodoDto import PotentialTodoDto
        |from .model.TodoDto import TodoDto
        |from .model.Error import Error
        |
        |class Client():
        |
        |  def __init__(self, serialization, handler):
        |    self.serialization = serialization
        |    self.handler = handler
        |
        |  def putTodo(self, id: str, done: bool, name: Optional[str], token: Token, refreshToken: Optional[Token], body: PotentialTodoDto):
        |     req = endpoint.PutTodo.Request(id, done, name, token, refreshToken, body)
        |     raw_req = endpoint.PutTodo.Convert.to_raw_request(self.serialization, req)
        |     raw_res = self.handler(raw_req)
        |     return endpoint.PutTodo.Convert.from_raw_response(self.serialization, raw_res)
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
            |from . import endpoint
            |
            |from typing import List, Optional
            |
            |from .model.TodoDto import TodoDto
            |
            |class Client():
            |
            |  def __init__(self, serialization, handler):
            |    self.serialization = serialization
            |    self.handler = handler
            |
            |  def getTodos(self, ):
            |     req = endpoint.GetTodos.Request
            |     raw_req = endpoint.GetTodos.Convert.to_raw_request(self.serialization, req)
            |     raw_res = self.handler(raw_req)
            |     return endpoint.GetTodos.Convert.from_raw_response(self.serialization, raw_res)
            |
        """.trimMargin()
        CompileMinimalEndpointTest.compiler { PythonEmitter() } shouldBeRight python
    }
}
