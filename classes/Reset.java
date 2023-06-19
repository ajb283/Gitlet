package gitlet;

import java.io.IOException;

/** Driver class for Reset.
 *  @author Avery Bong
 */
public class Reset implements Command {

    @Override
    public int run(Git s, String[] command) throws IOException {
        String hash = command[0];
        s.checkoutNode(command[0]);
        s.branches().replace(s.currentBranch(), s.findNode(hash));
        s._stage.clear();
        s._removal.clear();
        return 0;
    }
}
