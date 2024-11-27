type User {
    name: String
}

type Error {
    code: Integer,
    description: String
}

endpoint GetUsers GET /api/users -> {
    200 -> User[]
}

