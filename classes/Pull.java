package gitlet;

import java.io.IOException;

/** Driver class for Pull.
 *  @author Avery Bong
 */
public class Pull implements Command {
    @Override
    public int run(Git s, String[] command) throws IOException {
        s.pull(command[0], command[1]);
        return 0;
    }
}
