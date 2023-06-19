package gitlet;

import java.time.ZonedDateTime;

/** Driver class for Commit.
 *  @author Avery Bong
 */
public class Commit implements Command {
    @Override
    public int run(Git s, String[] command) {
        String message = command[0];
        if (message.length() <= 0) {
            Main.error("Please enter a commit message.");
        } else if (s._stage.size() == 0 && s._removal.size() == 0) {
            Main.error("No changes added to the commit.");
        }
        s.makeCommit(s.currentBranch(), command[0], ZonedDateTime.now(), true);
        return 0;
    }
}
