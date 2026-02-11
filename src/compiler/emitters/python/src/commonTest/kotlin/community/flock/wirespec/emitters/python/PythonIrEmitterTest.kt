package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PythonIrEmitterTest {

    @Test
    fun compileFullEndpointTest() {
        val python = """
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class PotentialTodoDto:
            |    name: str
            |    done: bool
            |
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class Token:
            |    iss: str
            |
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class TodoDto:
            |    id: str
            |    name: str
            |    done: bool
            |
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class Error:
            |    code: int
            |    description: str
            |
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
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
            |class Response200(Response2XX, ResponseTodoDto):
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
            |class Response201(Response2XX, ResponseTodoDto):
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
            |class Response500(Response5XX, ResponseError):
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
            |        return Wirespec.RawRequest(method=request.method.value, path=['todos', serialization.serializePath(request.path.id, str)], queries={'done': serialization.serializeParam(request.queries.done, bool), 'name': serialization.serializeParam(request.queries.name, str) if request.queries.name is not None else []}, headers={'token': serialization.serializeParam(request.headers.token, Token), 'refreshToken': serialization.serializeParam(request.headers.refreshToken, Token) if request.headers.refreshToken is not None else []}, body=serialization.serializeBody(request.body, PotentialTodoDto))
            |    @staticmethod
            |    def fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest) -> Request:
            |        return Request(id=serialization.deserializePath(request.path[1], str), done=serialization.deserializeParam(request.queries['done'], bool) if request.queries['done'] is not None else _raise('Param done cannot be null'), name=serialization.deserializeParam(request.queries['name'], str) if request.queries['name'] is not None else None, token=serialization.deserializeParam(request.headers['token'], Token) if request.headers['token'] is not None else _raise('Param token cannot be null'), refreshToken=serialization.deserializeParam(request.headers['refreshToken'], Token) if request.headers['refreshToken'] is not None else None, body=serialization.deserializeBody(request.body, PotentialTodoDto) if request.body is not None else _raise('body is null'))
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
            |                return Response201(token=serialization.deserializeParam(response.headers['token'], Token) if response.headers['token'] is not None else _raise('Param token cannot be null'), refreshToken=serialization.deserializeParam(response.headers['refreshToken'], Token) if response.headers['refreshToken'] is not None else None, body=serialization.deserializeBody(response.body, TodoDto) if response.body is not None else _raise('body is null'))
            |            case 500:
            |                return Response500(body=serialization.deserializeBody(response.body, Error) if response.body is not None else _raise('body is null'))
            |            case _:
            |                raise Exception(('Cannot match response with status: ' + str(response.statusCode)))
            |    class Handler(Wirespec.Handler, ABC):
            |        @abstractmethod
            |        async def putTodo(self, request: Request) -> Response[Any]:
            |            ...
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()

        CompileFullEndpointTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileChannelTest() {
        val python = """
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |class Queue(Wirespec.Channel, ABC):
            |    @abstractmethod
            |    def invoke(self, message: str) -> None:
            |        ...
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()

        CompileChannelTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileEnumTest() {
        val python = """
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |class MyAwesomeEnum(Wirespec.Enum, enum.Enum):
            |    ONE = "ONE"
            |    Two = "Two"
            |    THREE_MORE = "THREE_MORE"
            |    UnitedKingdom = "UnitedKingdom"
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()

        CompileEnumTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileMinimalEndpointTest() {
        val python = """
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class TodoDto:
            |    description: str
            |
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
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
            |class Response200(Response2XX, ResponseListTodoDto):
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
            |        async def getTodos(self, request: Request) -> Response[Any]:
            |            ...
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileRefinedTest() {
        val python = """
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class TodoId(Wirespec.Refined):
            |    value: str
            |    def validate(self) -> bool:
            |        return bool(re.match(r"/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g", self.value))
            |    def __str__(self) -> str:
            |        return self.value
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()

        CompileRefinedTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileUnionTest() {
        val python = """
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class UserAccountPassword(UserAccount):
            |    username: str
            |    password: str
            |
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class UserAccountToken(UserAccount):
            |    token: str
            |
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |from .UserAccount import UserAccount
            |@dataclass
            |class User:
            |    username: str
            |    account: UserAccount
            |
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |class UserAccount:
            |    pass
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()

        CompileUnionTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileTypeTest() {
        val python = """
            |from __future__ import annotations
            |
            |import re
            |
            |from abc import ABC, abstractmethod
            |from dataclasses import dataclass
            |from typing import Any, Generic, List, Optional
            |import enum
            |
            |from ..wirespec import T, Wirespec, _raise
            |
            |@dataclass
            |class Request:
            |    type: str
            |    url: str
            |    bODY_TYPE: Optional[str]
            |    params: list[str]
            |    headers: dict[str, str]
            |    body: Optional[dict[str, Optional[list[Optional[str]]]]]
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()

        CompileTypeTest.compiler { PythonIrEmitter() } shouldBeRight python
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
            |    class Enum(ABC):
            |        label: str
            |    class Endpoint(ABC):
            |        pass
            |    class Channel(ABC):
            |        pass
            |    class Refined(ABC):
            |        value: str
            |    class Path(ABC):
            |        pass
            |    class Queries(ABC):
            |        pass
            |    class Headers(ABC):
            |        pass
            |    class Handler(ABC):
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
            |
        """.trimMargin()

        val emitter = PythonIrEmitter()
        emitter.shared!!.source shouldBe expected
    }
}
