package community.flock.wirespec.examples.app.user

import community.flock.wirespec.examples.app.common.Converter
import community.flock.wirespec.generated.kotlin.UserDto

object UserConverter : Converter<User, UserDto> {
    override fun UserDto.internalize() = User(name = name)

    override fun User.externalize() = UserDto(name = name)

}
