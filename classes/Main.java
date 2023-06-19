package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Avery Bong
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        Git s;
        if (args.length == 0) {
            Main.error("Please enter a command.");
        }
        if (!STORAGE.exists()) {
            if (!args[0].equals("init")) {
                Main.error("Not in an initialized Gitlet directory.");
            }
            STORAGE.mkdir();
            s = new Git();
        } else {
            if (args[0].equals("init")) {
                Main.error("A Gitlet version-control system "
                        + "already exists in the current directory.");
            }
            s = readSystem(STORAGE);
        }
        Supplier<Command> supplier = COMMANDS.get(args[0]);
        if (supplier == null) {
            Main.error("No command with that name exists.");
        }
        Command command = supplier.get();
        command.run(s, args.length < 1
                ? new String[]{}
                : Arrays.copyOfRange(args, 1, args.length));
        save(SYSTEM, s);
    }

    /** @return the stored Git system if exists.
     * @param loc */
    public static Git readSystem(File loc) {
        if (!loc.exists()) {
            error("Remote directory not found.");
        }
        File sysLoc = Utils.join(loc, "system");
        return Utils.readObject(sysLoc, Git.class);
    }

    /** Saves the stored Git S to LOC. */
    public static void save(File loc, Git s) {
        Utils.writeObject(loc, s);
    }

    /** Throw an error with the MESSAGE and exit. */
    public static void error(String message) {
        System.out.println(message);
        System.exit(0);
    }

    /** Backup and return a file F. */
    public static File backup(File f) {
        String contents = Utils.readContentsAsString(f);
        String hash = Utils.sha1(contents);
        File backupLoc = Utils.join(STORAGE, hash);
        if (!backupLoc.exists()) {
            Utils.writeContents(backupLoc, contents);
        }
        return backupLoc;
    }

    /** Copy the contents of HASHED to DST. Can throw IOException. */
    public static void exportFile(File hashed, File dst) throws IOException {
        Files.copy(hashed.toPath(), dst.toPath(), COPY);
    }

    /** Map of commands to classes. */
    static final Map<String, Supplier<Command>> COMMANDS = new HashMap<>() {{
            put("init", Init::new);
            put("add", Add::new);
            put("commit", Commit::new);
            put("log", Log::new);
            put("global-log", GlobalLog::new);
            put("checkout", Checkout::new);
            put("branch", Branch::new);
            put("rm", Remove::new);
            put("rm-branch", RmBranch::new);
            put("find", Find::new);
            put("reset", Reset::new);
            put("status", Status::new);
            put("merge", Merge::new);
            put("add-remote", AddRemote::new);
            put("rm-remote", RmRemote::new);
            put("fetch", Fetch::new);
            put("push", Push::new);
            put("pull", Pull::new);
        }};

    /** Return CWD. */
    public static File getCwd() {
        return CWD;
    }

    /** Return CWD. */
    private static final File CWD = new File(".");

    /** Return STORE. */
    public static File getStorageFolder() {
        return STORAGE;
    }

    /** Return STORE. */
    private static final File STORAGE = Utils.join(CWD, "/.gitlet");

    /** Return SYSTEM. */
    private static final File SYSTEM = Utils.join(STORAGE, "/system");

    /** COPY OPTION. */
    private static final StandardCopyOption COPY =
            StandardCopyOption.REPLACE_EXISTING;
}
