package community.flock.wirespec.example.maven.custom.app.user;

import community.flock.wirespec.example.maven.custom.app.common.Converter;
import community.flock.wirespec.generated.java.model.AuthenticationPassword;
import community.flock.wirespec.generated.java.model.AuthenticationToken;
import community.flock.wirespec.generated.java.model.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserConverter implements Converter<User, UserDto> {
    @Override
    public User internalize(final UserDto userDto) {
        return switch (userDto.authentication()) {
            case AuthenticationPassword password -> new User(userDto.name(), password.secret());
            case AuthenticationToken token -> throw new UnsupportedOperationException("Token authentication not yet supported.");
            case null -> throw new IllegalStateException("Authentication cannot be null");
        };
    }


    @Override
    public UserDto externalize(final User user) {
        return new UserDto(user.name(), new AuthenticationPassword(user.password()));
    }
}
