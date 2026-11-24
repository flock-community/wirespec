type User { id: String, name: String, email: String? }
type UserNotFound { id: String }

rpc GetUser { id: String } -> User ! UserNotFound
rpc FindUsers { name: String? } -> User[]
rpc CreateUser { name: String, email: String? } -> User

@JsonRpcMethod("users/rename")
rpc RenameUser { id: String, name: String } -> User ! UserNotFound
