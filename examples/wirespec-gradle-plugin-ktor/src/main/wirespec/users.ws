type UserDto {
    name: String
}

endpoint GetUsers GET /users -> {
    200 -> UserDto[]
}

endpoint GetUserByName GET /users/{name: String} -> {
    200 -> UserDto
}

endpoint PostUser POST UserDto /users -> {
    200 -> UserDto
}

endpoint DeleteUserByName DELETE /users/{name: String} -> {
    200 -> UserDto
}
