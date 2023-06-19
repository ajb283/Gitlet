package gitlet;

import java.io.IOException;

/** Driver class for Merge.
 *  @author Avery Bong
 */
public class Merge implements Command {
    @Override
    public int run(Git s, String[] command) throws IOException {
        if (s._stage.size() > 0 || s._removal.size() > 0) {
            Main.error("You have uncommitted changes.");
        }
        String branchName = command[0];
        if (!s.branches().containsKey(branchName)) {
            Main.error("A branch with that name does not exist.");
        }
        s.merge(branchName);
        return 0;
    }
}
