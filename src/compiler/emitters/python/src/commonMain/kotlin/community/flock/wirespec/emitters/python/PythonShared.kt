package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.Shared

data object PythonShared : Shared {
    override val packageString: String = "shared"

    override val source = """
        |from abc import ABC, abstractmethod
        |from dataclasses import dataclass
        |from enum import Enum
        |from typing import Generic, List, Dict, Optional, TypeVar
        |
        |T = TypeVar('T')
        |REQ = TypeVar('REQ')
        |RES = TypeVar('RES')
        |
        |class Wirespec:
        |
        |    class Refined(ABC, Generic[T]):
        |        @property
        |        @abstractmethod
        |        def value(self) -> T: pass
        |
        |    class Endpoint(ABC):
        |
        |        class Handler(ABC):
        |            pass
        |
        |        class Convert(ABC, Generic[REQ, RES]):
        |            @staticmethod
        |            @abstractmethod
        |            def to_raw_request(serializer: 'Wirespec.Serializer', req: REQ) -> 'Wirespec.RawRequest': pass
        |
        |            @staticmethod
        |            @abstractmethod
        |            def from_raw_response(serializer: 'Wirespec.Deserializer', res: 'Wirespec.RawResponse') -> RES: pass
        |
        |            @staticmethod
        |            @abstractmethod
        |            def to_raw_response(serializer: 'Wirespec.Serializer', res: RES) -> 'Wirespec.RawResponse': pass
        |
        |            @staticmethod
        |            @abstractmethod
        |            def from_raw_request(serializer: 'Wirespec.Deserializer[T]', req: 'Wirespec.RawRequest') -> REQ: pass
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
        |        def serialize(self, value: T, t: type[T]) -> str: pass
        |        @abstractmethod
        |        def serialize_param(self, value: T, t: type[T]) -> List[str]: pass
        |
        |    class Deserializer(Generic[T], ABC):
        |        @abstractmethod
        |        def deserialize(self, value: str | None, t: type[T]) -> T: pass
        |        @abstractmethod
        |        def deserialize_param(self, value: List[str] | None, t: type[T]) -> T: pass
        |
        |    class Serialization(Serializer, Deserializer):
        |        @abstractmethod
        |        def serialize(self, value: T, t: type[T]) -> str: pass
        |        @abstractmethod
        |        def serialize_param(self, value: T, t: type[T]) -> List[str]: pass
        |        @abstractmethod
        |        def deserialize(self, value: str | None, t: type[T]) -> T: pass
        |        @abstractmethod
        |        def deserialize_param(self, value: List[str] | None, t: type[T]) -> T: pass
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
