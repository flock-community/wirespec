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
        |        def method(self) -> 'Wirespec.Method': pass
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
        |    class Serializer(Generic[T], ABC):
        |        @abstractmethod
        |        def serialize(self, value: T, t: type) -> str: pass
        |        @abstractmethod
        |        def serialize_param(self, value: T, t: type) -> List[str]: pass
        |
        |    class Deserializer(Generic[T], ABC):
        |        @abstractmethod
        |        def deserialize(self, value: str | None, t: type) -> T: pass
        |        @abstractmethod
        |        def deserialize_param(self, value: List[str] | None, t): pass
        |
        |    class Serialization(Generic[T], Serializer, Deserializer):
        |        @abstractmethod
        |        def serialize(self, value: T, t: type) -> str: pass
        |        @abstractmethod
        |        def serialize_param(self, value: T, t: type) -> List[str]: pass
        |        @abstractmethod
        |        def deserialize(self, value: str | None, t: type) -> T: pass
        |        @abstractmethod
        |        def deserialize_param(self, value: List[str] | None, t): pass
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
