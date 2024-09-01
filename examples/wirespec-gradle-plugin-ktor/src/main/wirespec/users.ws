type UserDto {
    name: String
}

endpoint GetUsers GET /api/users -> {
    200 -> UserDto[]
}

endpoint GetUserByName GET /api/users/{name: String} -> {
    200 -> UserDto
}

endpoint PostUser POST UserDto /api/users -> {
    200 -> UserDto
}

endpoint DeleteUserByName DELETE /api/users/{name: String} -> {
    200 -> UserDto
}
