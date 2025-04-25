package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.example.maven.custom.app.common.Converter;
import community.flock.wirespec.generated.java.model.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserConverter implements Converter<User, UserDto> {
    @Override
    public User internalize(final UserDto userDto) {
        return new User(userDto.name());
    }

    @Override
    public UserDto externalize(final User user) {
        return new UserDto(user.name());
    }
}
