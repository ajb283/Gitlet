package gitlet;

/** Driver class for Log.
 *  @author Avery Bong
 */
public class Log implements Command {

    @Override
    public int run(Git s, String[] command) {
        if (command.length != 0) {
            throw new GitletException("Incorrect # of args");
        }
        s.printLog();
        return 0;
    }
}
