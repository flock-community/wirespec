type UserId = String(/^u-[0-9]{3}$/g)

enum Role {
    ADMIN, MEMBER
}

type Address {
    street: String,
    number: Integer?
}

type User {
    id: UserId,
    name: String,
    role: Role,
    address: Address
}

type NotFound {
    reason: String
}

rpc GetUser {
    id: UserId
} -> User ! NotFound

rpc FindUsers {
    role: Role?,
    limit: Integer(1, 50)
} -> User[]

rpc Ping {} -> String
