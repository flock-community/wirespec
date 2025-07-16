type User {
    name: String
}

type UserError {
    code: Integer,
    description: String
}

endpoint GetUsers GET /api/users -> {
    200 -> User[]
    500 -> UserError
}

