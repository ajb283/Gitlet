package gitlet;

import java.io.IOException;

/** Driver class for RmRemote.
 *  @author Avery Bong
 */
public class RmRemote implements Command {

    @Override
    public int run(Git s, String[] command) throws IOException {
        s.rmRemote(command[0]);
        return 0;
    }
}
