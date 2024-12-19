type UserDto {
    name: String
}

endpoint GetUsers GET /api/users
  ?{name: String}
  #{version: String} -> {
    200 -> UserDto[]
}

endpoint GetUserByName GET /api/users/{name: String} -> {
    200 -> UserDto
    404 -> Unit
}

endpoint PostUser POST UserDto /api/users -> {
    200 -> UserDto
    409 -> Unit
}

endpoint DeleteUserByName DELETE /api/users/{name: String} -> {
    200 -> UserDto
    404 -> Unit
}

endpoint UploadImage POST Bytes /api/users/{name: String}/image -> {
    201 -> Unit
    404 -> Unit
}
