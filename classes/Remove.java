package gitlet;

import java.io.IOException;

/** Driver class for Remove.
 *  @author Avery Bong
 */
public class Remove implements Command {

    @Override
    public int run(Git s, String[] command) throws IOException {
        if (command.length != 1) {
            Main.error("Follow proper structure.");
        }
        String name = command[0];
        if (!s._stage.containsKey(name)
                && !s.branches().get(s.currentBranch())
                ._files.containsKey(name)) {
            Main.error("No reason to remove the file.");
        }
        s.remove(name);
        return 0;
    }
}
