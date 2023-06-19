package gitlet;

import java.io.IOException;

/** Driver class for Status.
 *  @author Avery Bong
 */
public class Status implements Command  {

    @Override
    public int run(Git s, String[] command) throws IOException {
        s.printStatus();
        return 0;
    }
}
