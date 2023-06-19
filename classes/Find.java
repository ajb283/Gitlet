package gitlet;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Driver class for Find.
 *  @author Avery Bong
 */
public class Find implements Command {
    @Override
    public int run(Git s, String[] command) throws IOException {
        String message = command[0];
        List<Git.Node> nodes = s.findNodesWithMessage(message);
        if (nodes.size() == 0) {
            Main.error("Found no commit with that message.");
        }
        List<String> hashes = nodes.stream().map(x -> x._hash)
                .collect(Collectors.toList());
        for (String h : hashes) {
            System.out.println(h);
        }
        return 0;
    }
}
