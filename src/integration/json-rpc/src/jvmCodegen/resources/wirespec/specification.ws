// The methods of the examples in section 7 of the JSON-RPC 2.0 specification: https://www.jsonrpc.org/specification
rpc Subtract { minuend: Integer, subtrahend: Integer } -> Integer
rpc Sum { a: Integer, b: Integer, c: Integer } -> Integer
rpc Update { a: Integer, b: Integer, c: Integer, d: Integer, e: Integer } -> Unit

@JsonRpcMethod("notify_hello")
rpc NotifyHello { value: Integer } -> Unit

@JsonRpcMethod("notify_sum")
rpc NotifySum { a: Integer, b: Integer, c: Integer } -> Unit

@JsonRpcMethod("get_data")
rpc GetData {} -> Any[]
