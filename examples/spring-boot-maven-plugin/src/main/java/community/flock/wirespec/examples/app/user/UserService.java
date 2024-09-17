package community.flock.wirespec.examples.app.user;

import java.util.List;

interface UserContext extends
        UserAdapter.Module {

    class UserService {
        List<User> getAllUsers(UserContext ctx) {
            return ctx.getUserAdapter().getAllUsers();
        }

        User getUserByName(UserContext ctx, String name) {
            return ctx.getUserAdapter().getUserByName(name);
        }

        User saveUser(UserContext ctx, User user) {
            return ctx.getUserAdapter().saveUser(user);
        }

        User deleteUserByName(UserContext ctx, String name) {
            return ctx.getUserAdapter().deleteUserByName(name);
        }
    }
}
