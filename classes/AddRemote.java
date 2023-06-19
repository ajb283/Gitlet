package gitlet;

import java.io.File;
import java.io.IOException;

/** Driver class for AddRemote.
 *  @author Avery Bong
 */
public class AddRemote implements Command {
    @Override
    public int run(Git s, String[] command) throws IOException {
        String name = command[0];
        String locString = command[1].replace("/", File.separator);
        File loc = new File(locString);
        s.addRemote(name, loc);
        return 0;
    }
}
