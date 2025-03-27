package community.flock.wirespec.compiler.core.emit.shared

data object PythonShared : Shared {
    override val packageString: String = "shared"

    override val source = """
        |from abc import ABC, abstractmethod
        |from dataclasses import dataclass
        |from enum import Enum
        |from typing import Any, Generic, List, Dict, Optional, TypeVar, NoReturn
        |
        |T = TypeVar('T', bound=Any)
        |RAW = TypeVar('RAW', bound=Any)
        |Req = TypeVar('Req', bound='Request')
        |Res = TypeVar('Res', bound='Response')
        |
        |
        |class Wirespec:
        |
        |    class Endpoint(ABC): pass
        |
        |    class Refined(ABC):
        |        @property
        |        @abstractmethod
        |        def value(self) -> str: pass
        |
        |    class Handler(ABC): pass
        |
        |    class ServerEdge(Generic[Req, Res], ABC):
        |        @abstractmethod
        |        def from_request(self, request: 'RawRequest') -> Req: pass
        |
        |        @abstractmethod
        |        def to_response(self, response: Res) -> 'RawResponse': pass
        |
        |    class ClientEdge(Generic[Req, Res], ABC):
        |        @abstractmethod
        |        def to_request(self, request: Req) -> 'RawRequest': pass
        |
        |        @abstractmethod
        |        def from_response(self, response: 'RawResponse') -> Res: pass
        |
        |    class Method(Enum):
        |        GET = "GET"
        |        PUT = "PUT"
        |        POST = "POST"
        |        DELETE = "DELETE"
        |        OPTIONS = "OPTIONS"
        |        HEAD = "HEAD"
        |        PATCH = "PATCH"
        |        TRACE = "TRACE"
        |
        |    class Request(Generic[T], ABC):
        |
        |        class Path(ABC): pass
        |
        |        class Queries(ABC): pass
        |
        |        class Headers(ABC): pass
        |
        |        @property
        |        @abstractmethod
        |        def path(self) -> Path: pass
        |
        |        @property
        |        @abstractmethod
        |        def method(self) -> 'Method': pass
        |
        |        @property
        |        @abstractmethod
        |        def queries(self) -> Queries: pass
        |
        |        @property
        |        @abstractmethod
        |        def headers(self) -> Headers: pass
        |
        |        @property
        |        @abstractmethod
        |        def body(self) -> T: pass
        |
        |    class Response(Generic[T], ABC):
        |
        |        class Headers(ABC): pass
        |
        |        @property
        |        @abstractmethod
        |        def status(self) -> int: pass
        |
        |        @property
        |        @abstractmethod
        |        def headers(self) -> Headers: pass
        |
        |        @property
        |        @abstractmethod
        |        def body(self) -> T: pass
        |
        |    class Serializer(ABC):
        |        @abstractmethod
        |        def serialize_param(self, value: T, t: type) -> List[str]: pass
        |        @abstractmethod
        |        def serialize(self, value: T, t: type) -> str: pass
        |
        |    class Deserializer(ABC):
        |        @abstractmethod
        |        def deserialize_param(self, value: List[str], t): pass
        |        @abstractmethod
        |        def deserialize(self, value: str, t: type) -> T: pass
        |
        |    class Serialization(Serializer, Deserializer):
        |        @abstractmethod
        |        def serialize_param(self, value: T, t: type) -> List[str]: pass
        |        @abstractmethod
        |        def serialize(self, value: T, t: type) -> str: pass
        |        @abstractmethod
        |        def deserialize_param(self, value: List[str], t): pass
        |        @abstractmethod
        |        def deserialize(self, value: str, t): pass
        |
        |    @dataclass
        |    class RawRequest:
        |        method: str
        |        path: List[str]
        |        queries: Dict[str, List[str]]
        |        headers: Dict[str, List[str]]
        |        body: Optional[str]
        |
        |
        |    @dataclass
        |    class RawResponse:
        |        status_code: int
        |        headers: Dict[str, List[str]]
        |        body: Optional[str]
        |
    """.trimMargin()
}
