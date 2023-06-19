package gitlet;

import java.io.IOException;

/** Driver class for Rm Branch.
 *  @author Avery Bong
 */
public class RmBranch implements Command {
    @Override
    public int run(Git s, String[] command) throws IOException {
        String branchName = command[0];
        if (!s.branches().containsKey(branchName)) {
            Main.error("A branch with that name does not exist.");
        } else if (s.currentBranch().equals(branchName)) {
            Main.error("Cannot remove the current branch.");
        }
        s.branches().remove(branchName);
        return 0;
    }
}
