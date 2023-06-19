package gitlet;

import java.io.IOException;

public class GlobalLog implements Command {

    @Override
    public int run(Git s, String[] command) throws IOException {
        if (command.length != 0) {
            throw new GitletException("Incorrect # of args");
        }
        s.globalLog();
        return 0;
    }
}
