package gitlet;

import java.io.IOException;

/** Driver class for Push.
 *  @author Avery Bong
 */
public class Push implements Command {
    @Override
    public int run(Git s, String[] command) throws IOException {
        s.push(command[0], command[1]);
        return 0;
    }
}
