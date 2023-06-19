package gitlet;

import java.io.File;

/** Driver class for Add.
 *  @author Avery Bong
 */
public class Add implements Command {

    @Override
    public int run(Git s, String[] command) {
        if (command.length != 1) {
            throw new GitletException("Incorrect # of args");
        }
        File file = Utils.join(Main.getCwd(), command[0]);
        if (!file.exists()) {
            Main.error("File does not exist.");
        }
        s.stage(file);
        return 0;
    }
}
