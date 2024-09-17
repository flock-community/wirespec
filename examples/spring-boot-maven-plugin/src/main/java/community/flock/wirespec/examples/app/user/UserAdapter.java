package community.flock.wirespec.examples.app.user;

import java.util.List;

public interface UserAdapter {
    interface Module {
        UserAdapter userAdapter();
    }

    List<User> getAllUsers();

    User getUserByName(final String name);

    User saveUser(final User user);

    User deleteUserByName(final String name);
}
