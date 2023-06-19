package gitlet;

import java.io.IOException;

/** Driver class for Branch.
 *  @author Avery Bong
 */
public class Branch implements Command {

    @Override
    public int run(Git s, String[] command) throws IOException {
        String branch = command[0];
        if (s.branches().containsKey(branch)) {
            Main.error("A branch with that name already exists.");
        }
        s.makeBranch(branch);
        return 0;
    }
}
