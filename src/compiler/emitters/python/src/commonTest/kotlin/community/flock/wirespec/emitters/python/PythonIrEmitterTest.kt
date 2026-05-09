package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.ir.core.RawElement
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
            |from ..model.PotentialTodoDto import PotentialTodoDto
            |class PotentialTodoDtoGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> PotentialTodoDto:
            |        return PotentialTodoDto(name=generator.generate(path + ['name'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), done=generator.generate(path + ['done'], Wirespec.GeneratorFieldBoolean(annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Token import Token
            |class TokenGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Token:
            |        return Token(iss=generator.generate(path + ['iss'], Wirespec.GeneratorFieldString(regex=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TodoDto import TodoDto
            |class TodoDtoGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TodoDto:
            |        return TodoDto(id=generator.generate(path + ['id'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), name=generator.generate(path + ['name'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), done=generator.generate(path + ['done'], Wirespec.GeneratorFieldBoolean(annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Error import Error
            |class ErrorGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Error:
            |        return Error(code=generator.generate(path + ['code'], Wirespec.GeneratorFieldInteger(min=None, max=None, annotations=[])), description=generator.generate(path + ['description'], Wirespec.GeneratorFieldString(regex=None, annotations=[])))
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
            |from ..model.MyAwesomeEnum import MyAwesomeEnum
            |class MyAwesomeEnumGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> MyAwesomeEnum:
            |        return MyAwesomeEnum.valueOf(generator.generate(path + ['value'], Wirespec.GeneratorFieldEnum(values=['ONE', 'Two', 'THREE_MORE', 'UnitedKingdom', '-1', '0', '10', '-999', '88'], annotations=[], type=MyAwesomeEnum)))
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
            |from ..model.TodoDto import TodoDto
            |class TodoDtoGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TodoDto:
            |        return TodoDto(description=generator.generate(path + ['description'], Wirespec.GeneratorFieldString(regex=None, annotations=[])))
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
            |from ..model.TodoId import TodoId
            |class TodoIdGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TodoId:
            |        return TodoId(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldString(regex='^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}', annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TodoNoRegex import TodoNoRegex
            |class TodoNoRegexGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TodoNoRegex:
            |        return TodoNoRegex(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldString(regex=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TestInt import TestInt
            |class TestIntGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TestInt:
            |        return TestInt(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldInteger(min=None, max=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TestInt0 import TestInt0
            |class TestInt0Generator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TestInt0:
            |        return TestInt0(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldInteger(min=None, max=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TestInt1 import TestInt1
            |class TestInt1Generator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TestInt1:
            |        return TestInt1(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldInteger(min=0, max=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TestInt2 import TestInt2
            |class TestInt2Generator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TestInt2:
            |        return TestInt2(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldInteger(min=1, max=3, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TestNum import TestNum
            |class TestNumGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TestNum:
            |        return TestNum(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldNumber(min=None, max=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TestNum0 import TestNum0
            |class TestNum0Generator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TestNum0:
            |        return TestNum0(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldNumber(min=None, max=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TestNum1 import TestNum1
            |class TestNum1Generator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TestNum1:
            |        return TestNum1(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldNumber(min=None, max=0.5, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.TestNum2 import TestNum2
            |class TestNum2Generator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> TestNum2:
            |        return TestNum2(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldNumber(min=-0.2, max=0.5, annotations=[])))
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
            |from ..model.UserAccountPassword import UserAccountPassword
            |class UserAccountPasswordGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> UserAccountPassword:
            |        return UserAccountPassword(username=generator.generate(path + ['username'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), password=generator.generate(path + ['password'], Wirespec.GeneratorFieldString(regex=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.UserAccountToken import UserAccountToken
            |class UserAccountTokenGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> UserAccountToken:
            |        return UserAccountToken(token=generator.generate(path + ['token'], Wirespec.GeneratorFieldString(regex=None, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.User import User
            |from ..model.UserAccount import UserAccount
            |from .UserAccountGenerator import UserAccountGenerator
            |class UserGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> User:
            |        return User(username=generator.generate(path + ['username'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), account=generator.generate(path + ['account'], Wirespec.GeneratorFieldShape(annotations={}, generate=lambda p0: UserAccountGenerator.generate(generator, p0), type=UserAccount)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.UserAccount import UserAccount
            |from .UserAccountPasswordGenerator import UserAccountPasswordGenerator
            |from .UserAccountTokenGenerator import UserAccountTokenGenerator
            |class UserAccountGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> UserAccount:
            |        variant = generator.generate(path + ['variant'], Wirespec.GeneratorFieldUnion(variants=['UserAccountPassword', 'UserAccountToken'], annotations=[], type=UserAccount))
            |        match variant:
            |            case 'UserAccountPassword':
            |                return UserAccountPasswordGenerator.generate(generator, path + ['UserAccountPassword'])
            |            case 'UserAccountToken':
            |                return UserAccountTokenGenerator.generate(generator, path + ['UserAccountToken'])
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
            |from ..model.Request import Request
            |class RequestGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Request:
            |        return Request(type=generator.generate(path + ['type'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), url=generator.generate(path + ['url'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), BODY_TYPE=generator.generate(path + ['BODY_TYPE'], Wirespec.GeneratorFieldNullable(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldString(regex=None, annotations=[])))), params=generator.generate(path + ['params'], Wirespec.GeneratorFieldArray(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldString(regex=None, annotations=[])))), headers=generator.generate(path + ['headers'], Wirespec.GeneratorFieldDict(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldString(regex=None, annotations=[])))), body=generator.generate(path + ['body'], Wirespec.GeneratorFieldNullable(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldDict(generate=lambda p1: generator.generate(p1, Wirespec.GeneratorFieldArray(generate=lambda p2: generator.generate(p2, Wirespec.GeneratorFieldString(regex=None, annotations=[])))))))))
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
            |from ..model.DutchPostalCode import DutchPostalCode
            |class DutchPostalCodeGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> DutchPostalCode:
            |        return DutchPostalCode(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldString(regex='^([0-9]{4}[A-Z]{2})${'$'}', annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Address import Address
            |from ..model.DutchPostalCode import DutchPostalCode
            |from .DutchPostalCodeGenerator import DutchPostalCodeGenerator
            |class AddressGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Address:
            |        return Address(street=generator.generate(path + ['street'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), houseNumber=generator.generate(path + ['houseNumber'], Wirespec.GeneratorFieldInteger(min=None, max=None, annotations=[])), postalCode=generator.generate(path + ['postalCode'], Wirespec.GeneratorFieldShape(annotations={}, generate=lambda p0: DutchPostalCodeGenerator.generate(generator, p0), type=DutchPostalCode)))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Person import Person
            |from ..model.Address import Address
            |from .AddressGenerator import AddressGenerator
            |class PersonGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Person:
            |        return Person(name=generator.generate(path + ['name'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), address=generator.generate(path + ['address'], Wirespec.GeneratorFieldShape(annotations={'street': [], 'houseNumber': [], 'postalCode': []}, generate=lambda p0: AddressGenerator.generate(generator, p0), type=Address)), tags=generator.generate(path + ['tags'], Wirespec.GeneratorFieldArray(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldString(regex=None, annotations=[])))))
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
            |from ..model.Email import Email
            |class EmailGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Email:
            |        return Email(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldString(regex='^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}', annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.PhoneNumber import PhoneNumber
            |class PhoneNumberGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> PhoneNumber:
            |        return PhoneNumber(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldString(regex='^\+[1-9]\d{1,14}${'$'}', annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Tag import Tag
            |class TagGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Tag:
            |        return Tag(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldString(regex='^[a-z][a-z0-9-]{0,19}${'$'}', annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.EmployeeAge import EmployeeAge
            |class EmployeeAgeGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> EmployeeAge:
            |        return EmployeeAge(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldInteger(min=18, max=65, annotations=[])))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.ContactInfo import ContactInfo
            |from ..model.Email import Email
            |from ..model.PhoneNumber import PhoneNumber
            |from .EmailGenerator import EmailGenerator
            |from .PhoneNumberGenerator import PhoneNumberGenerator
            |class ContactInfoGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> ContactInfo:
            |        return ContactInfo(email=generator.generate(path + ['email'], Wirespec.GeneratorFieldShape(annotations={}, generate=lambda p0: EmailGenerator.generate(generator, p0), type=Email)), phone=generator.generate(path + ['phone'], Wirespec.GeneratorFieldNullable(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldShape(annotations={}, generate=lambda p1: PhoneNumberGenerator.generate(generator, p1), type=PhoneNumber)))))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Employee import Employee
            |from ..model.EmployeeAge import EmployeeAge
            |from ..model.ContactInfo import ContactInfo
            |from ..model.Tag import Tag
            |from .EmployeeAgeGenerator import EmployeeAgeGenerator
            |from .ContactInfoGenerator import ContactInfoGenerator
            |from .TagGenerator import TagGenerator
            |class EmployeeGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Employee:
            |        return Employee(name=generator.generate(path + ['name'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), age=generator.generate(path + ['age'], Wirespec.GeneratorFieldShape(annotations={}, generate=lambda p0: EmployeeAgeGenerator.generate(generator, p0), type=EmployeeAge)), contactInfo=generator.generate(path + ['contactInfo'], Wirespec.GeneratorFieldShape(annotations={'email': [], 'phone': []}, generate=lambda p0: ContactInfoGenerator.generate(generator, p0), type=ContactInfo)), tags=generator.generate(path + ['tags'], Wirespec.GeneratorFieldArray(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldShape(annotations={}, generate=lambda p1: TagGenerator.generate(generator, p1), type=Tag)))))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Department import Department
            |from ..model.Employee import Employee
            |from .EmployeeGenerator import EmployeeGenerator
            |class DepartmentGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Department:
            |        return Department(name=generator.generate(path + ['name'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), employees=generator.generate(path + ['employees'], Wirespec.GeneratorFieldArray(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldShape(annotations={'name': [], 'age': [], 'contactInfo': [], 'tags': []}, generate=lambda p1: EmployeeGenerator.generate(generator, p1), type=Employee)))))
            |
            |from __future__ import annotations
            |import re
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |from ..wirespec import T, Wirespec, _raise
            |from ..model.Company import Company
            |from ..model.Department import Department
            |from .DepartmentGenerator import DepartmentGenerator
            |class CompanyGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Company:
            |        return Company(name=generator.generate(path + ['name'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), departments=generator.generate(path + ['departments'], Wirespec.GeneratorFieldArray(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldShape(annotations={'name': [], 'employees': []}, generate=lambda p1: DepartmentGenerator.generate(generator, p1), type=Department)))))
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
            |        annotations: list[dict[str, Any]]
            |    @dataclass
            |    class GeneratorFieldInteger(GeneratorField[int]):
            |        min: Optional[int]
            |        max: Optional[int]
            |        annotations: list[dict[str, Any]]
            |    @dataclass
            |    class GeneratorFieldNumber(GeneratorField[float]):
            |        min: Optional[float]
            |        max: Optional[float]
            |        annotations: list[dict[str, Any]]
            |    @dataclass
            |    class GeneratorFieldBoolean(GeneratorField[bool]):
            |        annotations: list[dict[str, Any]]
            |    @dataclass
            |    class GeneratorFieldBytes(GeneratorField[bytes]):
            |        annotations: list[dict[str, Any]]
            |    @dataclass
            |    class GeneratorFieldEnum(GeneratorField[str]):
            |        values: list[str]
            |        annotations: list[dict[str, Any]]
            |        type: type[T]
            |    @dataclass
            |    class GeneratorFieldUnion(GeneratorField[str]):
            |        variants: list[str]
            |        annotations: list[dict[str, Any]]
            |        type: type[T]
            |    @dataclass
            |    class GeneratorFieldArray(GeneratorField[list[T]], Generic[T]):
            |        generate: Callable[[list[str]], T]
            |    @dataclass
            |    class GeneratorFieldNullable(GeneratorField[Optional[T]], Generic[T]):
            |        generate: Callable[[list[str]], T]
            |    @dataclass
            |    class GeneratorFieldShape(GeneratorField[T], Generic[T]):
            |        annotations: dict[str, list[dict[str, Any]]]
            |        generate: Callable[[list[str]], T]
            |        type: type[T]
            |    @dataclass
            |    class GeneratorFieldDict(GeneratorField[dict[str, V]], Generic[V]):
            |        generate: Callable[[list[str]], V]
            |    class Generator(ABC):
            |        @abstractmethod
            |        def generate(self, path: list[str], field: Wirespec.GeneratorField[T]) -> T:
            |            ...
            |
            """.trimMargin()

        val emitter = PythonIrEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.elements?.filterIsInstance<RawElement>()?.joinToString("") { it.code } shouldBe expected
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
            |from ..model.Address import Address
            |class AddressGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Address:
            |        return Address(street=generator.generate(path + ['street'], Wirespec.GeneratorFieldString(regex=None, annotations=[])), number=generator.generate(path + ['number'], Wirespec.GeneratorFieldInteger(min=None, max=None, annotations=[])))
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
            |from ..model.Color import Color
            |class ColorGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Color:
            |        return Color.valueOf(generator.generate(path + ['value'], Wirespec.GeneratorFieldEnum(values=['RED', 'GREEN', 'BLUE'], annotations=[], type=Color)))
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
            |from ..model.Shape import Shape
            |from .CircleGenerator import CircleGenerator
            |from .SquareGenerator import SquareGenerator
            |class ShapeGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Shape:
            |        variant = generator.generate(path + ['variant'], Wirespec.GeneratorFieldUnion(variants=['Circle', 'Square'], annotations=[], type=Shape))
            |        match variant:
            |            case 'Circle':
            |                return CircleGenerator.generate(generator, path + ['Circle'])
            |            case 'Square':
            |                return SquareGenerator.generate(generator, path + ['Square'])
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
            |from ..model.UUID import UUID
            |class UUIDGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> UUID:
            |        return UUID(value=generator.generate(path + ['value'], Wirespec.GeneratorFieldString(regex='^[0-9a-f]{8}${'$'}', annotations=[])))
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
            |from ..model.Inventory import Inventory
            |class InventoryGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Inventory:
            |        return Inventory(items=generator.generate(path + ['items'], Wirespec.GeneratorFieldArray(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldInteger(min=None, max=None, annotations=[])))))
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
            |from ..model.Lookup import Lookup
            |class LookupGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Lookup:
            |        return Lookup(entries=generator.generate(path + ['entries'], Wirespec.GeneratorFieldDict(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldInteger(min=None, max=None, annotations=[])))))
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
            |from ..model.Person import Person
            |class PersonGenerator:
            |    @staticmethod
            |    def generate(generator: Wirespec.Generator, path: list[str]) -> Person:
            |        return Person(nickname=generator.generate(path + ['nickname'], Wirespec.GeneratorFieldNullable(generate=lambda p0: generator.generate(p0, Wirespec.GeneratorFieldString(regex=None, annotations=[])))))
            |
            """.trimMargin()

        emitGeneratorSource(person, "PersonGenerator") shouldBe expected
    }
}
