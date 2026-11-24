type User { name: String, age: Integer? }
type UserNotFound { id: String }

rpc GetUser { id: String } -> User ! UserNotFound
rpc FindUsers { name: String?, limit: Integer32? } -> User[]
@JsonRpcMethod("users/rename")
rpc RenameUser { id: String, name: String } -> Unit ! String
rpc Ping {} -> String
rpc Echo { user: User, tags: String[] } -> User?
