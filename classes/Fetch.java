package gitlet;

import java.io.IOException;

/** Driver class for Fetch.
 *  @author Avery Bong
 */
public class Fetch implements Command {
    @Override
    public int run(Git s, String[] command) throws IOException {
        s.fetch(command[0], command[1]);
        return 0;
    }
}
