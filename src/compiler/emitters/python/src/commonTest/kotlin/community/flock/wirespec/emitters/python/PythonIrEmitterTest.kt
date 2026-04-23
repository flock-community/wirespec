package community.flock.wirespec.emitters.python

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PythonIrEmitterTest {

    @Test
    fun compileFullEndpointTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class PotentialTodoDto(Wirespec.Model):
            |    name: str
            |    done: bool
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class Token(Wirespec.Model):
            |    iss: str
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TodoDto(Wirespec.Model):
            |    id: str
            |    name: str
            |    done: bool
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class Error(Wirespec.Model):
            |    code: int
            |    description: str
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Token import Token
            |from ..model.PotentialTodoDto import PotentialTodoDto
            |from ..model.TodoDto import TodoDto
            |from ..model.Error import Error
            |@dataclass
            |class Path(Wirespec.Path):
            |    id: str
            |@dataclass
            |class Queries(Wirespec.Queries):
            |    done: bool
            |    name: Optional[str]
            |@dataclass
            |class RequestHeaders(Wirespec.Request.Headers):
            |    token: Token
            |    refreshToken: Optional[Token]
            |@dataclass
            |class Request(Wirespec.Request[PotentialTodoDto]):
            |    path: Path
            |    method: Wirespec.Method
            |    queries: Queries
            |    headers: RequestHeaders
            |    body: PotentialTodoDto
            |    def __init__(
            |        self,
            |        id: str,
            |        done: bool,
            |        name: Optional[str],
            |        token: Token,
            |        refreshToken: Optional[Token],
            |        body: PotentialTodoDto,
            |    ):
            |        self.path = Path(id=id)
            |        self.method = Wirespec.Method.PUT
            |        self.queries = Queries(done=done, name=name)
            |        self.headers = RequestHeaders(token=token, refreshToken=refreshToken)
            |        self.body = body
            |class Response(Wirespec.Response[T], Generic[T]):
            |    pass
            |class Response2XX(Response[T], Generic[T]):
            |    pass
            |class Response5XX(Response[T], Generic[T]):
            |    pass
            |class ResponseTodoDto(Response[TodoDto]):
            |    pass
            |class ResponseError(Response[Error]):
            |    pass
            |@dataclass
            |class Response200Headers(Wirespec.Response.Headers):
            |    pass
            |@dataclass
            |class Response200(Response2XX[TodoDto], ResponseTodoDto):
            |    status: int
            |    headers: Response200Headers
            |    body: TodoDto
            |    def __init__(
            |        self,
            |        body: TodoDto,
            |    ):
            |        self.status = 200
            |        self.headers = Response200Headers()
            |        self.body = body
            |@dataclass
            |class Response201Headers(Wirespec.Response.Headers):
            |    token: Token
            |    refreshToken: Optional[Token]
            |@dataclass
            |class Response201(Response2XX[TodoDto], ResponseTodoDto):
            |    status: int
            |    headers: Response201Headers
            |    body: TodoDto
            |    def __init__(
            |        self,
            |        token: Token,
            |        refreshToken: Optional[Token],
            |        body: TodoDto,
            |    ):
            |        self.status = 201
            |        self.headers = Response201Headers(token=token, refreshToken=refreshToken)
            |        self.body = body
            |@dataclass
            |class Response500Headers(Wirespec.Response.Headers):
            |    pass
            |@dataclass
            |class Response500(Response5XX[Error], ResponseError):
            |    status: int
            |    headers: Response500Headers
            |    body: Error
            |    def __init__(
            |        self,
            |        body: Error,
            |    ):
            |        self.status = 500
            |        self.headers = Response500Headers()
            |        self.body = body
            |class PutTodo(Wirespec.Endpoint):
            |    @staticmethod
            |    def toRawRequest(serialization: Wirespec.Serializer, request: Request) -> Wirespec.RawRequest:
            |        return Wirespec.RawRequest(method=request.method.value, path=['todos', serialization.serializePath(request.path.id, str)], queries={'done': serialization.serializeParam(request.queries.done, bool), 'name': serialization.serializeParam(request.queries.name, str) if request.queries.name is not None else []}, headers={'token': serialization.serializeParam(request.headers.token, Token), 'Refresh-Token': serialization.serializeParam(request.headers.refreshToken, Token) if request.headers.refreshToken is not None else []}, body=serialization.serializeBody(request.body, PotentialTodoDto))
            |    @staticmethod
            |    def fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest) -> Request:
            |        return Request(id=serialization.deserializePath(request.path[1], str), done=serialization.deserializeParam(request.queries['done'], bool) if request.queries['done'] is not None else _raise('Param done cannot be null'), name=serialization.deserializeParam(request.queries['name'], str) if request.queries['name'] is not None else None, token=serialization.deserializeParam(next((v for k, v in request.headers.items() if k.lower() == 'token'.lower()), None), Token) if next((v for k, v in request.headers.items() if k.lower() == 'token'.lower()), None) is not None else _raise('Param token cannot be null'), refreshToken=serialization.deserializeParam(next((v for k, v in request.headers.items() if k.lower() == 'Refresh-Token'.lower()), None), Token) if next((v for k, v in request.headers.items() if k.lower() == 'Refresh-Token'.lower()), None) is not None else None, body=serialization.deserializeBody(request.body, PotentialTodoDto) if request.body is not None else _raise('body is null'))
            |    @staticmethod
            |    def toRawResponse(serialization: Wirespec.Serializer, response: Response[Any]) -> Wirespec.RawResponse:
            |        match response:
            |            case Response200() as r:
            |                return Wirespec.RawResponse(statusCode=r.status, headers={}, body=serialization.serializeBody(r.body, TodoDto))
            |            case Response201() as r:
            |                return Wirespec.RawResponse(statusCode=r.status, headers={'token': serialization.serializeParam(r.headers.token, Token), 'refreshToken': serialization.serializeParam(r.headers.refreshToken, Token) if r.headers.refreshToken is not None else []}, body=serialization.serializeBody(r.body, TodoDto))
            |            case Response500() as r:
            |                return Wirespec.RawResponse(statusCode=r.status, headers={}, body=serialization.serializeBody(r.body, Error))
            |            case _:
            |                raise Exception(('Cannot match response with status: ' + str(response.status)))
            |    @staticmethod
            |    def fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse) -> Response[Any]:
            |        match response.statusCode:
            |            case 200:
            |                return Response200(body=serialization.deserializeBody(response.body, TodoDto) if response.body is not None else _raise('body is null'))
            |            case 201:
            |                return Response201(token=serialization.deserializeParam(next((v for k, v in response.headers.items() if k.lower() == 'token'.lower()), None), Token) if next((v for k, v in response.headers.items() if k.lower() == 'token'.lower()), None) is not None else _raise('Param token cannot be null'), refreshToken=serialization.deserializeParam(next((v for k, v in response.headers.items() if k.lower() == 'refreshToken'.lower()), None), Token) if next((v for k, v in response.headers.items() if k.lower() == 'refreshToken'.lower()), None) is not None else None, body=serialization.deserializeBody(response.body, TodoDto) if response.body is not None else _raise('body is null'))
            |            case 500:
            |                return Response500(body=serialization.deserializeBody(response.body, Error) if response.body is not None else _raise('body is null'))
            |            case _:
            |                raise Exception(('Cannot match response with status: ' + str(response.statusCode)))
            |    class Handler(Wirespec.Handler, ABC):
            |        @abstractmethod
            |        async def put_todo(self, request: Request) -> Response[Any]:
            |            ...
            |    class Call(Wirespec.Call, ABC):
            |        @abstractmethod
            |        async def put_todo(self, id: str, done: bool, name: Optional[str], token: Token, refreshToken: Optional[Token], body: PotentialTodoDto) -> Response[Any]:
            |            ...
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Token import Token
            |from ..model.PotentialTodoDto import PotentialTodoDto
            |from ..model.TodoDto import TodoDto
            |from ..model.Error import Error
            |from ..endpoint.PutTodo import *
            |@dataclass
            |class PutTodoClient(PutTodo.Call):
            |    serialization: Wirespec.Serialization
            |    transportation: Wirespec.Transportation
            |    async def put_todo(self, id: str, done: bool, name: Optional[str], token: Token, refreshToken: Optional[Token], body: PotentialTodoDto) -> Response[Any]:
            |        request = Request(id=id, done=done, name=name, token=token, refreshToken=refreshToken, body=body)
            |        rawRequest = PutTodo.toRawRequest(serialization=self.serialization, request=request)
            |        rawResponse = await self.transportation.transport(rawRequest)
            |        return PutTodo.fromRawResponse(serialization=self.serialization, response=rawResponse)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class PotentialTodoDtoGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> PotentialTodoDto:
            |        return PotentialTodoDto(name=generator.generate((str(path) + 'name'), PotentialTodoDto, Wirespec.GeneratorFieldString(regex=None)), done=generator.generate((str(path) + 'done'), PotentialTodoDto, Wirespec.GeneratorFieldBoolean()))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TokenGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Token:
            |        return Token(iss=generator.generate((str(path) + 'iss'), Token, Wirespec.GeneratorFieldString(regex=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TodoDtoGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TodoDto:
            |        return TodoDto(id=generator.generate((str(path) + 'id'), TodoDto, Wirespec.GeneratorFieldString(regex=None)), name=generator.generate((str(path) + 'name'), TodoDto, Wirespec.GeneratorFieldString(regex=None)), done=generator.generate((str(path) + 'done'), TodoDto, Wirespec.GeneratorFieldBoolean()))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class ErrorGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Error:
            |        return Error(code=generator.generate((str(path) + 'code'), Error, Wirespec.GeneratorFieldInteger(min=None, max=None)), description=generator.generate((str(path) + 'description'), Error, Wirespec.GeneratorFieldString(regex=None)))
            |
            |from . import model
            |from . import endpoint
            |from . import client
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from .wirespec import T, Wirespec, _raise
            |from .model.Token import Token
            |from .model.PotentialTodoDto import PotentialTodoDto
            |from .model.TodoDto import TodoDto
            |from .model.Error import Error
            |from .endpoint.PutTodo import *
            |from .client.PutTodoClient import PutTodoClient
            |@dataclass
            |class Client(PutTodo.Call):
            |    serialization: Wirespec.Serialization
            |    transportation: Wirespec.Transportation
            |    async def put_todo(self, id: str, done: bool, name: Optional[str], token: Token, refreshToken: Optional[Token], body: PotentialTodoDto) -> Response[Any]:
            |        return await PutTodoClient(serialization=self.serialization, transportation=self.transportation).put_todo(id, done, name, token, refreshToken, body)
            |
        """.trimMargin()

        CompileFullEndpointTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileChannelTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class Queue(Wirespec.Channel, ABC):
            |    @abstractmethod
            |    def invoke(self, message: str) -> None:
            |        ...
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
        """.trimMargin()

        CompileChannelTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileEnumTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class MyAwesomeEnum(Wirespec.Enum, enum.Enum):
            |    ONE = "ONE"
            |    Two = "Two"
            |    THREE_MORE = "THREE_MORE"
            |    UnitedKingdom = "UnitedKingdom"
            |    _1 = "-1"
            |    _0 = "0"
            |    _10 = "10"
            |    _999 = "-999"
            |    _88 = "88"
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class MyAwesomeEnumGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> MyAwesomeEnum:
            |        return MyAwesomeEnum.valueOf(generator.generate((str(path) + 'value'), MyAwesomeEnum, Wirespec.GeneratorFieldEnum(values=['ONE', 'Two', 'THREE_MORE', 'UnitedKingdom', '-1', '0', '10', '-999', '88'])))
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
        """.trimMargin()

        CompileEnumTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileMinimalEndpointTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TodoDto(Wirespec.Model):
            |    description: str
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TodoDto import TodoDto
            |@dataclass
            |class Path(Wirespec.Path):
            |    pass
            |@dataclass
            |class Queries(Wirespec.Queries):
            |    pass
            |@dataclass
            |class RequestHeaders(Wirespec.Request.Headers):
            |    pass
            |@dataclass
            |class Request(Wirespec.Request[None]):
            |    path: Path
            |    method: Wirespec.Method
            |    queries: Queries
            |    headers: RequestHeaders
            |    body: None
            |    def __init__(self):
            |        self.path = Path()
            |        self.method = Wirespec.Method.GET
            |        self.queries = Queries()
            |        self.headers = RequestHeaders()
            |        self.body = None
            |class Response(Wirespec.Response[T], Generic[T]):
            |    pass
            |class Response2XX(Response[T], Generic[T]):
            |    pass
            |class ResponseListTodoDto(Response[list[TodoDto]]):
            |    pass
            |@dataclass
            |class Response200Headers(Wirespec.Response.Headers):
            |    pass
            |@dataclass
            |class Response200(Response2XX[list[TodoDto]], ResponseListTodoDto):
            |    status: int
            |    headers: Response200Headers
            |    body: list[TodoDto]
            |    def __init__(
            |        self,
            |        body: list[TodoDto],
            |    ):
            |        self.status = 200
            |        self.headers = Response200Headers()
            |        self.body = body
            |class GetTodos(Wirespec.Endpoint):
            |    @staticmethod
            |    def toRawRequest(serialization: Wirespec.Serializer, request: Request) -> Wirespec.RawRequest:
            |        return Wirespec.RawRequest(method=request.method.value, path=['todos'], queries={}, headers={}, body=None)
            |    @staticmethod
            |    def fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest) -> Request:
            |        return Request()
            |    @staticmethod
            |    def toRawResponse(serialization: Wirespec.Serializer, response: Response[Any]) -> Wirespec.RawResponse:
            |        match response:
            |            case Response200() as r:
            |                return Wirespec.RawResponse(statusCode=r.status, headers={}, body=serialization.serializeBody(r.body, list[TodoDto]))
            |            case _:
            |                raise Exception(('Cannot match response with status: ' + str(response.status)))
            |    @staticmethod
            |    def fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse) -> Response[Any]:
            |        match response.statusCode:
            |            case 200:
            |                return Response200(body=serialization.deserializeBody(response.body, list[TodoDto]) if response.body is not None else _raise('body is null'))
            |            case _:
            |                raise Exception(('Cannot match response with status: ' + str(response.statusCode)))
            |    class Handler(Wirespec.Handler, ABC):
            |        @abstractmethod
            |        async def get_todos(self, request: Request) -> Response[Any]:
            |            ...
            |    class Call(Wirespec.Call, ABC):
            |        @abstractmethod
            |        async def get_todos(self) -> Response[Any]:
            |            ...
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TodoDto import TodoDto
            |from ..endpoint.GetTodos import *
            |@dataclass
            |class GetTodosClient(GetTodos.Call):
            |    serialization: Wirespec.Serialization
            |    transportation: Wirespec.Transportation
            |    async def get_todos(self) -> Response[Any]:
            |        request = Request()
            |        rawRequest = GetTodos.toRawRequest(serialization=self.serialization, request=request)
            |        rawResponse = await self.transportation.transport(rawRequest)
            |        return GetTodos.fromRawResponse(serialization=self.serialization, response=rawResponse)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TodoDtoGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TodoDto:
            |        return TodoDto(description=generator.generate((str(path) + 'description'), TodoDto, Wirespec.GeneratorFieldString(regex=None)))
            |
            |from . import model
            |from . import endpoint
            |from . import client
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from .wirespec import T, Wirespec, _raise
            |from .model.TodoDto import TodoDto
            |from .endpoint.GetTodos import *
            |from .client.GetTodosClient import GetTodosClient
            |@dataclass
            |class Client(GetTodos.Call):
            |    serialization: Wirespec.Serialization
            |    transportation: Wirespec.Transportation
            |    async def get_todos(self) -> Response[Any]:
            |        return await GetTodosClient(serialization=self.serialization, transportation=self.transportation).get_todos()
            |
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileRefinedTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TodoId(Wirespec.Refined[str]):
            |    value: str
            |    def validate(self) -> bool:
            |        return bool(re.match(r"/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g", self.value))
            |    def __str__(self) -> str:
            |        return self.value
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TodoNoRegex(Wirespec.Refined[str]):
            |    value: str
            |    def validate(self) -> bool:
            |        return True
            |    def __str__(self) -> str:
            |        return self.value
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TestInt(Wirespec.Refined[int]):
            |    value: int
            |    def validate(self) -> bool:
            |        return True
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TestInt0(Wirespec.Refined[int]):
            |    value: int
            |    def validate(self) -> bool:
            |        return True
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TestInt1(Wirespec.Refined[int]):
            |    value: int
            |    def validate(self) -> bool:
            |        return 0 <= self.value
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TestInt2(Wirespec.Refined[int]):
            |    value: int
            |    def validate(self) -> bool:
            |        return 1 <= self.value and self.value <= 3
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TestNum(Wirespec.Refined[float]):
            |    value: float
            |    def validate(self) -> bool:
            |        return True
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TestNum0(Wirespec.Refined[float]):
            |    value: float
            |    def validate(self) -> bool:
            |        return True
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TestNum1(Wirespec.Refined[float]):
            |    value: float
            |    def validate(self) -> bool:
            |        return self.value <= 0.5
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class TestNum2(Wirespec.Refined[float]):
            |    value: float
            |    def validate(self) -> bool:
            |        return -0.2 <= self.value and self.value <= 0.5
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TodoIdGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TodoId:
            |        return TodoId(value=generator.generate((str(path) + 'value'), TodoId, Wirespec.GeneratorFieldString(regex='^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}')))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TodoNoRegexGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TodoNoRegex:
            |        return TodoNoRegex(value=generator.generate((str(path) + 'value'), TodoNoRegex, Wirespec.GeneratorFieldString(regex=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TestIntGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TestInt:
            |        return TestInt(value=generator.generate((str(path) + 'value'), TestInt, Wirespec.GeneratorFieldInteger(min=None, max=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TestInt0Generator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TestInt0:
            |        return TestInt0(value=generator.generate((str(path) + 'value'), TestInt0, Wirespec.GeneratorFieldInteger(min=None, max=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TestInt1Generator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TestInt1:
            |        return TestInt1(value=generator.generate((str(path) + 'value'), TestInt1, Wirespec.GeneratorFieldInteger(min=0, max=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TestInt2Generator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TestInt2:
            |        return TestInt2(value=generator.generate((str(path) + 'value'), TestInt2, Wirespec.GeneratorFieldInteger(min=1, max=3)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TestNumGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TestNum:
            |        return TestNum(value=generator.generate((str(path) + 'value'), TestNum, Wirespec.GeneratorFieldNumber(min=None, max=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TestNum0Generator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TestNum0:
            |        return TestNum0(value=generator.generate((str(path) + 'value'), TestNum0, Wirespec.GeneratorFieldNumber(min=None, max=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TestNum1Generator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TestNum1:
            |        return TestNum1(value=generator.generate((str(path) + 'value'), TestNum1, Wirespec.GeneratorFieldNumber(min=None, max=0.5)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TestNum2Generator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> TestNum2:
            |        return TestNum2(value=generator.generate((str(path) + 'value'), TestNum2, Wirespec.GeneratorFieldNumber(min=-0.2, max=0.5)))
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
        """.trimMargin()

        CompileRefinedTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileUnionTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class UserAccountPassword(Wirespec.Model, UserAccount):
            |    username: str
            |    password: str
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class UserAccountToken(Wirespec.Model, UserAccount):
            |    token: str
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from .UserAccount import UserAccount
            |@dataclass
            |class User(Wirespec.Model):
            |    username: str
            |    account: UserAccount
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class UserAccount:
            |    pass
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class UserAccountPasswordGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> UserAccountPassword:
            |        return UserAccountPassword(username=generator.generate((str(path) + 'username'), UserAccountPassword, Wirespec.GeneratorFieldString(regex=None)), password=generator.generate((str(path) + 'password'), UserAccountPassword, Wirespec.GeneratorFieldString(regex=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class UserAccountTokenGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> UserAccountToken:
            |        return UserAccountToken(token=generator.generate((str(path) + 'token'), UserAccountToken, Wirespec.GeneratorFieldString(regex=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class UserGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> User:
            |        return User(username=generator.generate((str(path) + 'username'), User, Wirespec.GeneratorFieldString(regex=None)), account=UserAccountGenerator.generate((str(path) + 'account'), generator))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class UserAccountGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> UserAccount:
            |        variant = generator.generate((str(path) + 'variant'), UserAccount, Wirespec.GeneratorFieldUnion(variants=['UserAccountPassword', 'UserAccountToken']))
            |        match variant:
            |            case 'UserAccountPassword':
            |                return UserAccountPasswordGenerator.generate((str(path) + 'UserAccountPassword'), generator)
            |            case 'UserAccountToken':
            |                return UserAccountTokenGenerator.generate((str(path) + 'UserAccountToken'), generator)
            |        raise Exception('Unknown variant')
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
        """.trimMargin()

        CompileUnionTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileTypeTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class Request(Wirespec.Model):
            |    type: str
            |    url: str
            |    BODY_TYPE: Optional[str]
            |    params: list[str]
            |    headers: dict[str, str]
            |    body: Optional[dict[str, Optional[list[Optional[str]]]]]
            |    def validate(self) -> list[str]:
            |        return []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class RequestGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Request:
            |        return Request(type=generator.generate((str(path) + 'type'), Request, Wirespec.GeneratorFieldString(regex=None)), url=generator.generate((str(path) + 'url'), Request, Wirespec.GeneratorFieldString(regex=None)), BODY_TYPE=(None if generator.generate((str(path) + 'BODY_TYPE'), Request, Wirespec.GeneratorFieldNullable(inner=Wirespec.GeneratorFieldString(regex=None))) else generator.generate((str(path) + 'BODY_TYPE'), Request, Wirespec.GeneratorFieldString(regex=None))), params=generator.generate((str(path) + 'params'), Request, Wirespec.GeneratorFieldArray(inner=Wirespec.GeneratorFieldString(regex=None))), headers=generator.generate((str(path) + 'headers'), Request, Wirespec.GeneratorFieldDict(key=None, value=Wirespec.GeneratorFieldString(regex=None))), body=(None if generator.generate((str(path) + 'body'), Request, Wirespec.GeneratorFieldNullable(inner=Wirespec.GeneratorFieldDict(key=None, value=None))) else generator.generate((str(path) + 'body'), Request, Wirespec.GeneratorFieldDict(key=None, value=None))))
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
        """.trimMargin()

        CompileTypeTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileNestedTypeTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class DutchPostalCode(Wirespec.Refined[str]):
            |    value: str
            |    def validate(self) -> bool:
            |        return bool(re.match(r"/^([0-9]{4}[A-Z]{2})${'$'}/g", self.value))
            |    def __str__(self) -> str:
            |        return self.value
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from .DutchPostalCode import DutchPostalCode
            |@dataclass
            |class Address(Wirespec.Model):
            |    street: str
            |    houseNumber: int
            |    postalCode: DutchPostalCode
            |    def validate(self) -> list[str]:
            |        return (['postalCode'] if not self.postalCode.validate() else [])
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from .Address import Address
            |@dataclass
            |class Person(Wirespec.Model):
            |    name: str
            |    address: Address
            |    tags: list[str]
            |    def validate(self) -> list[str]:
            |        return [f"address.{e}" for e in self.address.validate()]
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class DutchPostalCodeGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> DutchPostalCode:
            |        return DutchPostalCode(value=generator.generate((str(path) + 'value'), DutchPostalCode, Wirespec.GeneratorFieldString(regex='^([0-9]{4}[A-Z]{2})${'$'}')))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class AddressGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Address:
            |        return Address(street=generator.generate((str(path) + 'street'), Address, Wirespec.GeneratorFieldString(regex=None)), houseNumber=generator.generate((str(path) + 'houseNumber'), Address, Wirespec.GeneratorFieldInteger(min=None, max=None)), postalCode=DutchPostalCodeGenerator.generate((str(path) + 'postalCode'), generator))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class PersonGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Person:
            |        return Person(name=generator.generate((str(path) + 'name'), Person, Wirespec.GeneratorFieldString(regex=None)), address=AddressGenerator.generate((str(path) + 'address'), generator), tags=generator.generate((str(path) + 'tags'), Person, Wirespec.GeneratorFieldArray(inner=Wirespec.GeneratorFieldString(regex=None))))
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
        """.trimMargin()

        CompileNestedTypeTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileComplexModelTest() {
        val python = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class Email(Wirespec.Refined[str]):
            |    value: str
            |    def validate(self) -> bool:
            |        return bool(re.match(r"/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}/g", self.value))
            |    def __str__(self) -> str:
            |        return self.value
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class PhoneNumber(Wirespec.Refined[str]):
            |    value: str
            |    def validate(self) -> bool:
            |        return bool(re.match(r"/^\+[1-9]\d{1,14}${'$'}/g", self.value))
            |    def __str__(self) -> str:
            |        return self.value
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class Tag(Wirespec.Refined[str]):
            |    value: str
            |    def validate(self) -> bool:
            |        return bool(re.match(r"/^[a-z][a-z0-9-]{0,19}${'$'}/g", self.value))
            |    def __str__(self) -> str:
            |        return self.value
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |@dataclass
            |class EmployeeAge(Wirespec.Refined[int]):
            |    value: int
            |    def validate(self) -> bool:
            |        return 18 <= self.value and self.value <= 65
            |    def __str__(self) -> str:
            |        return str(self.value)
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from .Email import Email
            |from .PhoneNumber import PhoneNumber
            |@dataclass
            |class ContactInfo(Wirespec.Model):
            |    email: Email
            |    phone: Optional[PhoneNumber]
            |    def validate(self) -> list[str]:
            |        return (['email'] if not self.email.validate() else []) + (['phone'] if not self.phone.validate() else []) if self.phone is not None else []
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from .EmployeeAge import EmployeeAge
            |from .ContactInfo import ContactInfo
            |from .Tag import Tag
            |@dataclass
            |class Employee(Wirespec.Model):
            |    name: str
            |    age: EmployeeAge
            |    contactInfo: ContactInfo
            |    tags: list[Tag]
            |    def validate(self) -> list[str]:
            |        return (['age'] if not self.age.validate() else []) + [f"contactInfo.{e}" for e in self.contactInfo.validate()] + [item for i, el in enumerate(self.tags) for item in ([f"tags[{i}]"] if not el.validate() else [])]
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from .Employee import Employee
            |@dataclass
            |class Department(Wirespec.Model):
            |    name: str
            |    employees: list[Employee]
            |    def validate(self) -> list[str]:
            |        return [item for i, el in enumerate(self.employees) for item in [f"employees[{i}].{e}" for e in el.validate()]]
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from .Department import Department
            |@dataclass
            |class Company(Wirespec.Model):
            |    name: str
            |    departments: list[Department]
            |    def validate(self) -> list[str]:
            |        return [item for i, el in enumerate(self.departments) for item in [f"departments[{i}].{e}" for e in el.validate()]]
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class EmailGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Email:
            |        return Email(value=generator.generate((str(path) + 'value'), Email, Wirespec.GeneratorFieldString(regex='^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}')))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class PhoneNumberGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> PhoneNumber:
            |        return PhoneNumber(value=generator.generate((str(path) + 'value'), PhoneNumber, Wirespec.GeneratorFieldString(regex='^\+[1-9]\d{1,14}${'$'}')))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class TagGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Tag:
            |        return Tag(value=generator.generate((str(path) + 'value'), Tag, Wirespec.GeneratorFieldString(regex='^[a-z][a-z0-9-]{0,19}${'$'}')))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class EmployeeAgeGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> EmployeeAge:
            |        return EmployeeAge(value=generator.generate((str(path) + 'value'), EmployeeAge, Wirespec.GeneratorFieldInteger(min=18, max=65)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class ContactInfoGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> ContactInfo:
            |        return ContactInfo(email=EmailGenerator.generate((str(path) + 'email'), generator), phone=(None if generator.generate((str(path) + 'phone'), ContactInfo, Wirespec.GeneratorFieldNullable(inner=None)) else PhoneNumberGenerator.generate((str(path) + 'phone'), generator)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class EmployeeGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Employee:
            |        return Employee(name=generator.generate((str(path) + 'name'), Employee, Wirespec.GeneratorFieldString(regex=None)), age=EmployeeAgeGenerator.generate((str(path) + 'age'), generator), contactInfo=ContactInfoGenerator.generate((str(path) + 'contactInfo'), generator), tags=generator.generate((str(path) + 'tags'), Employee, Wirespec.GeneratorFieldArray(inner=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class DepartmentGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Department:
            |        return Department(name=generator.generate((str(path) + 'name'), Department, Wirespec.GeneratorFieldString(regex=None)), employees=generator.generate((str(path) + 'employees'), Department, Wirespec.GeneratorFieldArray(inner=None)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class CompanyGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Company:
            |        return Company(name=generator.generate((str(path) + 'name'), Company, Wirespec.GeneratorFieldString(regex=None)), departments=generator.generate((str(path) + 'departments'), Company, Wirespec.GeneratorFieldArray(inner=None)))
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
            |
            |
            |
            |
            |
            |
        """.trimMargin()

        CompileComplexModelTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun sharedOutputTest() {
        val expected = """
            |from __future__ import annotations
            |
            |import enum
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, Optional, Type, TypeVar
            |
            |T = TypeVar('T')
            |
            |
            |def _raise(msg: str) -> Any:
            |    raise Exception(msg)
            |
            |# package shared
            |class Wirespec:
            |    class Model(ABC):
            |        @abstractmethod
            |        def validate(self) -> list[str]:
            |            ...
            |    class Enum(ABC):
            |        label: str
            |    class Endpoint(ABC):
            |        pass
            |    class Channel(ABC):
            |        pass
            |    class Refined(ABC, Generic[T]):
            |        value: T
            |        @abstractmethod
            |        def validate(self) -> bool:
            |            ...
            |    class Path(ABC):
            |        pass
            |    class Queries(ABC):
            |        pass
            |    class Headers(ABC):
            |        pass
            |    class Handler(ABC):
            |        pass
            |    class Call(ABC):
            |        pass
            |    class Method(enum.Enum):
            |        GET = "GET"
            |        PUT = "PUT"
            |        POST = "POST"
            |        DELETE = "DELETE"
            |        OPTIONS = "OPTIONS"
            |        HEAD = "HEAD"
            |        PATCH = "PATCH"
            |        TRACE = "TRACE"
            |    class Request(ABC, Generic[T]):
            |        path: Wirespec.Path
            |        method: Wirespec.Method
            |        queries: Wirespec.Queries
            |        headers: Headers
            |        body: T
            |        class Headers(ABC):
            |            pass
            |    class Response(ABC, Generic[T]):
            |        status: int
            |        headers: Headers
            |        body: T
            |        class Headers(ABC):
            |            pass
            |    class BodySerializer(ABC):
            |        @abstractmethod
            |        def serializeBody(self, t: T, type: type[T]) -> bytes:
            |            ...
            |    class BodyDeserializer(ABC):
            |        @abstractmethod
            |        def deserializeBody(self, raw: bytes, type: type[T]) -> T:
            |            ...
            |    class BodySerialization(BodySerializer, BodyDeserializer, ABC):
            |        pass
            |    class PathSerializer(ABC):
            |        @abstractmethod
            |        def serializePath(self, t: T, type: type[T]) -> str:
            |            ...
            |    class PathDeserializer(ABC):
            |        @abstractmethod
            |        def deserializePath(self, raw: str, type: type[T]) -> T:
            |            ...
            |    class PathSerialization(PathSerializer, PathDeserializer, ABC):
            |        pass
            |    class ParamSerializer(ABC):
            |        @abstractmethod
            |        def serializeParam(self, value: T, type: type[T]) -> list[str]:
            |            ...
            |    class ParamDeserializer(ABC):
            |        @abstractmethod
            |        def deserializeParam(self, values: list[str], type: type[T]) -> T:
            |            ...
            |    class ParamSerialization(ParamSerializer, ParamDeserializer, ABC):
            |        pass
            |    class Serializer(BodySerializer, PathSerializer, ParamSerializer, ABC):
            |        pass
            |    class Deserializer(BodyDeserializer, PathDeserializer, ParamDeserializer, ABC):
            |        pass
            |    class Serialization(Serializer, Deserializer, ABC):
            |        pass
            |    @dataclass
            |    class RawRequest:
            |        method: str
            |        path: list[str]
            |        queries: dict[str, list[str]]
            |        headers: dict[str, list[str]]
            |        body: Optional[bytes]
            |    @dataclass
            |    class RawResponse:
            |        statusCode: int
            |        headers: dict[str, list[str]]
            |        body: Optional[bytes]
            |    class Transportation(ABC):
            |        @abstractmethod
            |        async def transport(self, request: Wirespec.RawRequest) -> Wirespec.RawResponse:
            |            ...
            |    class GeneratorField(ABC, Generic[T]):
            |        pass
            |    @dataclass
            |    class GeneratorFieldString(GeneratorField[str]):
            |        regex: Optional[str]
            |    @dataclass
            |    class GeneratorFieldInteger(GeneratorField[int]):
            |        min: Optional[int]
            |        max: Optional[int]
            |    @dataclass
            |    class GeneratorFieldNumber(GeneratorField[float]):
            |        min: Optional[float]
            |        max: Optional[float]
            |    @dataclass
            |    class GeneratorFieldBoolean(GeneratorField[bool]):
            |        pass
            |    @dataclass
            |    class GeneratorFieldBytes(GeneratorField[bytes]):
            |        pass
            |    @dataclass
            |    class GeneratorFieldEnum(GeneratorField[str]):
            |        values: list[str]
            |    @dataclass
            |    class GeneratorFieldUnion(GeneratorField[str]):
            |        variants: list[str]
            |    @dataclass
            |    class GeneratorFieldArray(GeneratorField[int]):
            |        inner: Optional[Wirespec.GeneratorField[Any]]
            |    @dataclass
            |    class GeneratorFieldNullable(GeneratorField[bool]):
            |        inner: Optional[Wirespec.GeneratorField[Any]]
            |    @dataclass
            |    class GeneratorFieldDict(GeneratorField[int]):
            |        key: Optional[Wirespec.GeneratorField[Any]]
            |        value: Optional[Wirespec.GeneratorField[Any]]
            |    class Generator(ABC):
            |        @abstractmethod
            |        def generate(self, path: list[str], type: type[T], field: Wirespec.GeneratorField[T]) -> T:
            |            ...
            |
        """.trimMargin()

        val emitter = PythonIrEmitter()
        emitter.shared.source shouldBe expected
    }

    private fun emitGeneratorSource(node: Definition, fileNameSubstring: String): String {
        val emitter = PythonIrEmitter()
        val ast = AST(
            nonEmptyListOf(
                Module(
                    FileUri(""),
                    nonEmptyListOf(node),
                ),
            ),
        )
        val emitted = emitter.emit(ast, noLogger)
        val match = emitted.toList().first { it.file.contains(fileNameSubstring) }
        return match.result
    }

    @Test
    fun testEmitGeneratorForType() {
        val address = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Address"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("street"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("number"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer(constraint = null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class AddressGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Address:
            |        return Address(street=generator.generate((str(path) + 'street'), Address, Wirespec.GeneratorFieldString(regex=None)), number=generator.generate((str(path) + 'number'), Address, Wirespec.GeneratorFieldInteger(min=None, max=None)))
            |
        """.trimMargin()

        emitGeneratorSource(address, "AddressGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForEnum() {
        val color = Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Color"),
            entries = setOf("RED", "GREEN", "BLUE"),
        )

        val expected = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class ColorGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Color:
            |        return Color.valueOf(generator.generate((str(path) + 'value'), Color, Wirespec.GeneratorFieldEnum(values=['RED', 'GREEN', 'BLUE'])))
            |
        """.trimMargin()

        emitGeneratorSource(color, "ColorGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForUnion() {
        val shape = Union(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Shape"),
            entries = setOf(
                Reference.Custom(value = "Circle", isNullable = false),
                Reference.Custom(value = "Square", isNullable = false),
            ),
        )

        val expected = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class ShapeGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Shape:
            |        variant = generator.generate((str(path) + 'variant'), Shape, Wirespec.GeneratorFieldUnion(variants=['Circle', 'Square']))
            |        match variant:
            |            case 'Circle':
            |                return CircleGenerator.generate((str(path) + 'Circle'), generator)
            |            case 'Square':
            |                return SquareGenerator.generate((str(path) + 'Square'), generator)
            |        raise Exception('Unknown variant')
            |
        """.trimMargin()

        emitGeneratorSource(shape, "ShapeGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForRefined() {
        val uuid = Refined(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("UUID"),
            reference = Reference.Primitive(
                type = Reference.Primitive.Type.String(
                    Reference.Primitive.Type.Constraint.RegExp("/^[0-9a-f]{8}${'$'}/g"),
                ),
                isNullable = false,
            ),
        )

        val expected = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class UUIDGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> UUID:
            |        return UUID(value=generator.generate((str(path) + 'value'), UUID, Wirespec.GeneratorFieldString(regex='^[0-9a-f]{8}${'$'}')))
            |
        """.trimMargin()

        emitGeneratorSource(uuid, "UUIDGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForArrayField() {
        val inventory = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Inventory"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("items"),
                        annotations = emptyList(),
                        reference = Reference.Iterable(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(constraint = null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class InventoryGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Inventory:
            |        return Inventory(items=generator.generate((str(path) + 'items'), Inventory, Wirespec.GeneratorFieldArray(inner=Wirespec.GeneratorFieldInteger(min=None, max=None))))
            |
        """.trimMargin()

        emitGeneratorSource(inventory, "InventoryGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForDictField() {
        val lookup = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Lookup"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("entries"),
                        annotations = emptyList(),
                        reference = Reference.Dict(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(constraint = null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class LookupGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Lookup:
            |        return Lookup(entries=generator.generate((str(path) + 'entries'), Lookup, Wirespec.GeneratorFieldDict(key=None, value=Wirespec.GeneratorFieldInteger(min=None, max=None))))
            |
        """.trimMargin()

        emitGeneratorSource(lookup, "LookupGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForNullableField() {
        val person = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Person"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("nickname"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |class PersonGenerator:
            |    @staticmethod
            |    def generate(path: list[str], generator: Wirespec.Generator) -> Person:
            |        return Person(nickname=(None if generator.generate((str(path) + 'nickname'), Person, Wirespec.GeneratorFieldNullable(inner=Wirespec.GeneratorFieldString(regex=None))) else generator.generate((str(path) + 'nickname'), Person, Wirespec.GeneratorFieldString(regex=None))))
            |
        """.trimMargin()

        emitGeneratorSource(person, "PersonGenerator") shouldBe expected
    }
}
