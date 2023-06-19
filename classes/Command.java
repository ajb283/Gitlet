package gitlet;

import java.io.IOException;

/** Driver class for Command.
 *  @author Avery Bong
 */
public interface Command {

    int run(Git s, String[] command) throws IOException;

}
