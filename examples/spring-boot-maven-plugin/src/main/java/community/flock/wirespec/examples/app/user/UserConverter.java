package community.flock.wirespec.examples.app.user;

import community.flock.wirespec.examples.app.common.Converter;
import community.flock.wirespec.generated.java.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserConverter implements Converter<User, UserDto> {
    @Override
    public User internalize(UserDto userDto) {
        return new User(userDto.name());
    }

    @Override
    public UserDto externalize(User user) {
        return new UserDto(user.name());
    }
}
