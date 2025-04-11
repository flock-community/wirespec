type Role {
    name: String[]
}

type User {
    firstName: String?,
    lastName: String?,
    username: String,
    roles: Role[]
}
