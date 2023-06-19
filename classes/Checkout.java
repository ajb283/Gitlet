package gitlet;

import java.io.IOException;

/** Driver class for Checkout.
 *  @author Avery Bong
 */
public class Checkout implements Command {

    @Override
    public int run(Git s, String[] command) throws IOException {
        switch (command.length) {
        case 1:
            String branch = command[0];
            if (!s.branches().containsKey(branch)) {
                Main.error("No such branch exists.");
            } else if (s.currentBranch().equals(branch)) {
                Main.error("No need to checkout the current branch.");
            }
            s.checkoutBranch(command[0]);
            break;
        case 2:
            Git.Node curr = s.branches().get(s.currentBranch());
            s.checkoutFile(curr.hash(), command[1]);
            break;
        case 3:
            if (command[1].equals("++")) {
                Main.error("Incorrect operands.");
            }
            s.checkoutFile(command[0], command[2]);
            break;
        default:
            throw new GitletException("Incorrect # of args");
        }
        return 0;
    }
}
