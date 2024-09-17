package community.flock.wirespec.examples.app.user;

import java.util.List;

public interface UserAdapter {
    interface Module {
        UserAdapter getUserAdapter();
    }

    List<User> getAllUsers();

    User getUserByName(String name);

    User saveUser(User user);

    User deleteUserByName(String name);
}
