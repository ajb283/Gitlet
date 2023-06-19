package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/** Core class for Gitlet.
 *  @author Avery Bong
 */
public class Git implements Serializable {

    public Git() {
        _currentBranch = "master";
        _nodes = new HashMap<>();
        _branches = new HashMap<>();
        _stage = new HashMap<>();
        _removal = new HashMap<>();
        _remotes = new HashMap<>();
        _conflicted = new ArrayList<>();
        makeCommit(_currentBranch, "initial commit",
                INITIAL_COMMIT_TIME, false);
    }

    /** Stage the file F. */
    public void stage(File f) {
        File backup = Main.backup(f);
        Node latest = _branches.get(currentBranch());
        if (_removal.containsKey(f.getName())
                && branches().get(currentBranch())
                ._files.containsValue(backup)) {
            _removal.remove(f.getName());
            _stage.put(f.getName(), null);
        } else if (latest._files.containsKey(f.getName())
                && latest._files.get(f.getName()).equals(backup)) {
            return;
        } else if (!_stage.containsKey(f.getName())
                && !backup.equals(_stage.getOrDefault(f.getName(), null))) {
            _stage.put(f.getName(), backup);
        }
    }

    /** Remove FILENAME. */
    public void remove(String fileName) {
        boolean inCommit = _branches.get(_currentBranch)
                ._files.containsKey(fileName);
        File file = Utils.join(Main.getCwd(), fileName);
        if (inCommit) {
            file.delete();
        }
        if (_stage.containsKey(fileName)) {
            _stage.remove(fileName);
        } else {
            _removal.put(fileName, null);
        }
    }

    /** Make a new commit at BRANCH, with the MESSAGE,
     * given the time DATE and a boolean HASPARENT. */
    public void makeCommit(String branch, String message,
                           ZonedDateTime date, boolean hasParent) {
        Node newCommit = new Node(message, date, hasParent);
        _branches.put(_currentBranch, newCommit);
        _nodes.put(newCommit._hash, newCommit);
        _stage.clear();
        _removal.clear();
        for (Node n : newCommit._pred) {
            n._next.add(newCommit);
        }
    }

    /** Make a merge commit merging nodes N1 and N2
     * from the branch given by NAME, and with the
     * files changed specified by ADD and REM. */
    public void mergeCommit(Node n1, Node n2, String name, Map<String,
            File> add, Map<String, File> rem) {
        String message = "Merged " + name + " into " + currentBranch() + ".";
        Node newCommit = new Node(message, ZonedDateTime.now(), n2, add, rem);
        _branches.replace(currentBranch(), newCommit);
        _nodes.put(newCommit._hash, newCommit);
    }

    /** Make a new branch with NEWNAME. */
    public void makeBranch(String newName) {
        Node currNode = _branches.get(_currentBranch);
        _branches.put(newName, currNode);
    }

    /** Set the active branch to NEWCURR. */
    public void setBranch(String newCurr) {
        _currentBranch = newCurr;
    }

    /** Checkout the node given by the HASH. */
    public void checkoutNode(String hash) throws IOException {
        Node toCheckout = findNode(hash);
        Node latest = branches().get(currentBranch());
        List<String> filesInDir =  Arrays.asList(Main.getCwd().list());
        for (Map.Entry<String, File> e : toCheckout._files.entrySet()) {
            if (!latest._files.containsKey(e.getKey())
                    && filesInDir.contains(e.getKey())
                    && !_removal.containsKey(e.getKey())) {
                Main.error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
            checkoutFile(hash, e.getKey());
        }
        for (Map.Entry<String, File> e : latest._files.entrySet()) {
            if (!toCheckout._files.containsKey(e.getKey())) {
                File f = Utils.join(Main.getCwd(), e.getKey());
                f.delete();
            }
        }
    }

    /** Checkout the BRANCH. */
    public void checkoutBranch(String branch) throws IOException {
        Node check = branches().get(branch);
        Node latest = branches().get(_currentBranch);
        List<String> filesInDir = Arrays.asList(Main.getCwd().list());
        for (Map.Entry<String, File> e : check._files.entrySet()) {
            if (!latest._files.containsKey(e.getKey())
                    && !latest._files.containsValue(e.getValue())
                    && filesInDir.contains(e.getKey())) {
                Main.error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
            checkoutFile(check._hash, e.getKey());
        }
        for (Map.Entry<String, File> e : latest._files.entrySet()) {
            if (!check._files.containsKey(e.getKey())) {
                File inDir = Utils.join(Main.getCwd(), e.getKey());
                inDir.delete();
            }
        }
        setBranch(branch);
        _stage.clear();
        _removal.clear();
    }

    /** @return the node given by its NODEHASH. */
    public Node findNode(String nodeHash) {
        Node toUse = null;
        for (Map.Entry<String, Node> e : _nodes.entrySet()) {
            String sub = e.getKey().substring(0, nodeHash.length());
            if (sub.equals(nodeHash)) {
                toUse = e.getValue();
            }
        }
        if (toUse == null) {
            Main.error("No commit with that id exists.");
        }
        return toUse;
    }

    /** Checkout the file given by FILENAME from the node given by NODEHASH. */
    public void checkoutFile(String nodeHash, String filename)
            throws IOException {
        Node toUse = findNode(nodeHash);
        toUse.checkout(filename);
    }

    /** @return the nodes with the commit MESSAGE. */
    public List<Node> findNodesWithMessage(String message) {
        List<Node> res = new ArrayList<>();
        for (Map.Entry<String, Node> e : _nodes.entrySet()) {
            if (e.getValue()._message.equals(message)) {
                res.add(e.getValue());
            }
        }
        return res;
    }

    /** Print the git log. */
    public void printLog() {
        StringBuilder res = new StringBuilder();
        List<Node> toLog = new ArrayList<>();
        Node self;
        for (self = _branches.get(_currentBranch);
             self._pred != null && self._pred.size() != 0;
             self = self._pred.get(0)) {
            toLog.add(self);
        }
        toLog.add(self);
        for (Node n : toLog) {
            res.append("\n===\n")
                    .append(n.toString())
                    .append("\n");
        }
        System.out.println(res.toString().substring(1, res.length() - 1));
    }

    /** Print the git log. */
    public void globalLog() {
        StringBuilder res = new StringBuilder();
        for (Node n : _nodes.values()) {
            res.append("\n===\n")
                    .append(n.toString())
                    .append("\n");
        }
        System.out.println(res.toString().substring(1, res.length() - 1));
    }

    /** Print the git status. */
    public void printStatus() {
        String res =
                "=== Branches ===\n"
                        + "%s\n"
                        + "=== Staged Files ===\n"
                        + "%s\n"
                        + "=== Removed Files ===\n"
                        + "%s\n"
                        + "=== Modifications Not Staged For Commit ===\n"
                        + "%s\n"
                        + "=== Untracked Files ==="
                        + "%s\n";
        String branches = branchesString();
        String stagedFiles = stagedString();
        String removedFiles = rmString();
        String modifications = modifiedString();
        String untracked = untrackedString();
        System.out.println(String.format(res, branches, stagedFiles,
                removedFiles, modifications, untracked));
    }

    /** Return a string describing modified files. */
    public String modifiedString() {
        Map<String, Integer> map = modifiedOrUntracked();
        StringBuilder res = new StringBuilder();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getValue() == 0 && !_conflicted.contains(e.getKey())) {
                res.append(e.getKey() + " (modified)\n");
            } else if (e.getValue() == 1 && _conflicted.size() == 0) {
                res.append(e.getKey() + " (deleted)\n");
            }
        }
        return res.toString();
    }

    /** Return a string describing untracked files. */
    public String untrackedString() {
        Map<String, Integer> map = modifiedOrUntracked();
        StringBuilder res = new StringBuilder();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getValue() == 2) {
                res.append("\n" + e.getKey());
            }
        }
        return res.toString();
    }

    /** Return a map of modified or untracked files.
     * 0 -> modified, 1 -> deleted, 2 -> untracked. */
    public Map<String, Integer> modifiedOrUntracked() {
        HashMap<String, Integer> map = new HashMap<>();
        for (File file : Main.getCwd().listFiles()) {
            if (file.isFile() && !file.isHidden()) {
                String hashOfFile = Main.backup(file).getName();
                File staged = _stage.getOrDefault(file.getName(), null);
                File rmed = _removal.getOrDefault(file.getName(), null);
                File committed = branches().get(currentBranch())
                        ._files.getOrDefault(file.getName(), null);
                if (staged == null && rmed == null && committed == null) {
                    map.put(file.getName(), 2);
                } else if ((staged != null
                        && !hashOfFile.equals(staged.getName()))
                        || (committed != null && !hashOfFile
                        .equals(committed.getName()))) {
                    map.put(file.getName(), 0);
                }
            }
        }
        List<String> cwdFilenames = Arrays
                .stream(Main.getCwd().list()).toList();
        for (String fileName : _stage.keySet()) {
            if (!cwdFilenames.contains(fileName)) {
                map.put(fileName, 1);
            }
        }
        for (String fileName : branches().get(currentBranch())
                ._files.keySet()) {
            if (!cwdFilenames.contains(fileName)
                    && !_removal.containsKey(fileName)) {
                map.put(fileName, 1);
            }
        }
        return map;

    }

    /** Merge the current branch with the branch given by BRANCHNAME. */
    public void merge(String branchName) throws IOException {
        if (branchName.equals(_currentBranch)) {
            Main.error("Cannot merge a branch with itself.");
        }
        boolean mc = false;
        Node l = branches().get(currentBranch());
        Node m = branches().get(branchName); Node d = toDiff(l, m);
        if (allPreds(m).contains(l)) {
            checkoutBranch(branchName);
            Main.error("Current branch fast-forwarded.");
        } else if (m._hash.equals(d._hash)) {
            Main.error("Cannot merge a branch with itself.");
        } else if (allPreds(l).contains(m)) {
            Main.error("Given branch is an ancestor of the current branch.");
        }
        Map<String, File> toProcess = new HashMap<>(m._files),
                add = new HashMap<>(), rem = new HashMap<>();
        toProcess.putAll(l._files);
        for (Map.Entry<String, File> e : toProcess.entrySet()) {
            File f = Utils.join(Main.getCwd(), e.getKey());
            if (f.exists() && !l._files.containsKey(f.getName())) {
                _conflicted.add(f.getName());
                Main.save(Utils.join(Main.getStorageFolder(), "system"), this);
                Main.error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
            File f1 = l._files.getOrDefault(e.getKey(), null),
                    f2 = m._files.getOrDefault(e.getKey(), null),
                    dFile = d._files.getOrDefault(e.getKey(), null);
            String h1 = f1 == null ? "" : f1.getName(),
                    h2 = f2 == null ? "" : f2.getName(),
                    hD = dFile == null ? "" : dFile.getName();
            if (hD.length() == 0 && h1.length() > 0 && h2.length() == 0) {
                continue;
            } else if (hD.length() == 0 && h2.length() > 0
                    && h1.length() == 0) {
                checkoutFile(m._hash, e.getKey());
                add.put(e.getKey(), e.getValue());
            } else if (hD.length() > 0 && hD.equals(h1)
                    && h2.length() == 0) {
                f.delete();
                rem.put(e.getKey(), e.getValue());
            } else if (hD.length() > 0 && hD.equals(h2)
                    && h1.length() == 0) {
                continue;
            } else if (dFile != null && !dFile.equals(e.getValue())
                    && hD.equals(h1)) {
                Main.exportFile(e.getValue(), f);
            } else {
                File merged = mergeConflict(f, f1, f2);
                mc = true;
                _conflicted.add(merged.getName());
                add.put(e.getKey(), merged);
            }
        }
        mergeCommit(l, m, branchName, add, rem);
        if (mc && !branchName.contains("/")) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Resolve and return F, the file conflicting
     * from contents at H1 and H2. */
    public File mergeConflict(File f, File h1, File h2) {
        String t1 = h1 != null ? Utils.readContentsAsString(h1) : "",
                t2 = h2 != null ? Utils.readContentsAsString(h2) : "";
        StringBuilder res = new StringBuilder();
        res.append("<<<<<<< HEAD\n")
                .append(t1)
                .append("=======\n")
                .append(t2)
                .append(">>>>>>>\n");
        Utils.writeContents(f, res.toString());
        return f;
    }

    /** Returns the latest common ancestor between NODE1 and NODE2. */
    private Node toDiff(Node node1, Node node2) {
        List<Node> pred1 = allPreds(node1), pred2 = allPreds(node2);
        int i;
        for (i = 0; i < Math.min(pred1.size(), pred2.size()); i++) {
            if (!pred1.get(i)._hash.equals(pred2.get(i)._hash)) {
                break;
            }
        }
        return pred1.get(i - 1);
    }

    /** Return a list of all of N's predecessors. */
    private List<Node> allPreds(Node n) {
        List<Node> queue,
                res = new ArrayList<>(),
                par = n._pred;
        queue = new ArrayList<>(par);
        while (queue.size() > 0) {
            Node self = queue.remove(0);
            if (!res.contains(self)) {
                res.add(0, self);
            }
            if (self._pred != null) {
                queue.addAll(self._pred);
            }
        }
        if (res.size() > 4) {
            Collections.sort(res, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return o1._commitTime.compareTo(o2._commitTime);
                }
            });
        }
        return res;
    }

    /** Add a new remote.
     * @param name
     * @param loc */
    public void addRemote(String name, File loc) {
        if (_remotes.containsKey(name)) {
            Main.error("A remote with that name already exists.");
        }
        _remotes.put(name, loc);
    }

    /** Remove an existing remote.
     * @param name */
    public void rmRemote(String name) {
        if (!_remotes.containsKey(name)) {
            Main.error("A remote with that name does not exist.");
        }
        _remotes.remove(name);
    }

    /** Push changes to an existing remote.
     * @param name
     * @param branch */
    public void push(String name, String branch) throws IOException {
        if (!_remotes.containsKey(name)) {
            Main.error("Remote directory not found.");
        }
        File remoteLoc = _remotes.get(name);
        Git remoteGitlet = Main.readSystem(remoteLoc);
        Node localHead = branches().get(currentBranch());
        Node remoteHead = remoteGitlet.branches().get(branch);
        List<String> preds = allPreds(localHead)
                .stream().map(x -> x._hash).toList();
        if (!preds.contains(remoteHead._hash)) {
            Main.error("Please pull down remote changes before pushing.");
        }
        remoteGitlet._branches.replace(branch, localHead);
        remoteGitlet._nodes.putAll(_nodes);
        for (File f : Main.getStorageFolder().listFiles()) {
            if (!f.getName().equals("system")) {
                Files.copy(
                        f.toPath(),
                        Utils.join(remoteLoc, f.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
        Main.save(Utils.join(remoteLoc, "system"), remoteGitlet);
    }

    /** Fetch changes from an existing remote.
     * @param name
     * @param branch */
    public void fetch(String name, String branch) throws IOException {
        if (!_remotes.containsKey(name)) {
            Main.error("Remote directory not found.");
        }
        File remoteLoc = _remotes.get(name);
        Git remoteGitlet = Main.readSystem(remoteLoc);
        if (!remoteGitlet.branches().containsKey(branch)) {
            Main.error("That remote does not have that branch.");
        }
        String newBranch = name + "/" + branch;
        Node headOfBranch = remoteGitlet.branches()
                .get(remoteGitlet.currentBranch());
        _branches.put(newBranch, headOfBranch);
        _nodes.putAll(remoteGitlet._nodes);
        for (File f : remoteLoc.listFiles()) {
            if (!f.getName().equals("system")) {
                Files.copy(
                        f.toPath(),
                        Utils.join(Main.getStorageFolder(),
                                f.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
    }

    public void pull(String name, String branch) throws IOException {
        _pull = true;
        fetch(name, branch);
        merge(name + "/" + branch);
    }

    public class Node implements Serializable {

        public Node(String message, ZonedDateTime date, boolean hasParent) {
            _message = message;
            _commitTime = date;
            if (hasParent) {
                _files = new HashMap<>(_branches.get(_currentBranch)._files);
                _pred = new ArrayList<>() {{
                        add(_branches.get(_currentBranch));
                    }};
            } else {
                _files = new HashMap<>();
                _pred = new ArrayList<>();
            }
            _next = new ArrayList<>();
            if (!_stage.isEmpty()) {
                for (Map.Entry<String, File> e : _stage.entrySet()) {
                    if (e.getValue() == null) {
                        continue;
                    } else if (!_files.containsKey(e.getKey())) {
                        _files.put(e.getKey(), e.getValue());
                    } else {
                        _files.replace(e.getKey(), e.getValue());
                    }
                }
            }
            if (!_removal.isEmpty()) {
                for (String e : _removal.keySet()) {
                    _files.remove(e);
                }
            }
            _hash = hash();
        }

        public Node(String message, ZonedDateTime date, Node other,
                    Map<String, File> add, Map<String, File> rem) {
            _message = message;
            _commitTime = date;
            _pred = new ArrayList<>() {{
                    add(_branches.get(currentBranch()));
                    add(other);
                }};
            _next = new ArrayList<>();
            _files = new HashMap<>(_branches.get(currentBranch())._files);
            _files.putAll(add);
            for (Map.Entry<String, File> e : rem.entrySet()) {
                _files.remove(e.getKey());
            }
            _hash = hash();
        }

        /** Checkout the FILENAME from this node. */
        public void checkout(String filename) throws IOException {
            File file = Utils.join(Main.getCwd(), filename);
            if (!_files.containsKey(filename)) {
                Main.error("File does not exist in that commit.");
            }
            Main.exportFile(_files.get(filename), file);
        }

        /** Return the hash of this node's unique properties. */
        public String hash() {
            StringBuilder props = new StringBuilder();
            props.append(_message);
            for (Map.Entry<String, File> e : _files.entrySet()) {
                props.append(e.getKey());
                props.append(e.getValue().toString());
            }
            props.append(_commitTime.toString());
            return Utils.sha1(props.toString());
        }

        @Override
        public String toString() {
            List<Node> parents = _pred;
            DateTimeFormatter format = DateTimeFormatter.ofPattern(
                    "EEE LLL dd HH:mm:ss yyyy Z"
            );
            StringBuilder res = new StringBuilder();
            res.append("commit ")
                    .append(_hash)
                    .append("\n");
            if (parents != null && parents.size() > 1) {
                res.append("Merge: ")
                        .append(parents.get(0)._hash.substring(0, 7))
                        .append(" ")
                        .append(parents.get(1)._hash.substring(0, 7))
                        .append("\n");
            }
            res.append("Date: ")
                    .append(_commitTime.format(format))
                    .append("\n")
                    .append(_message);
            return res.toString();
        }

        /** The hash for this commit. */
        protected String _hash;

        /** The commit message. */
        protected String _message;

        /** The time this commit was made. */
        protected ZonedDateTime _commitTime;

        /** The hashmap relating file names to their hash locations. */
        protected Map<String, File> _files;

        /** The predecessor set of nodes. */
        protected List<Node> _pred;

        /** The successor set of nodes. */
        protected List<Node> _next;

    }

    /** Return the current branch. */
    public String currentBranch() {
        return _currentBranch;
    }

    /** Return the hashmap of all branches. */
    public Map<String, Node> branches() {
        return _branches;
    }

    /** Return the representation of all the branches for status. */
    public String branchesString() {
        StringBuilder res = new StringBuilder();
        for (String b : _branches.keySet().stream().sorted().toList()) {
            if (b.equals(_currentBranch)) {
                res.append("*");
            }
            res.append(b)
                    .append("\n");
        }
        return res.toString();
    }

    /** Return the representation of all the staged files for status. */
    public String stagedString() {
        StringBuilder res = new StringBuilder();
        for (Map.Entry<String, File> e : _stage.entrySet()) {
            if (e.getValue() != null) {
                res.append(e.getKey())
                        .append("\n");
            }
        }
        return res.toString();
    }

    /** Return the representation of all the removed files for status. */
    public String rmString() {
        StringBuilder res = new StringBuilder();
        for (String b : _removal.keySet()) {
            res.append(b)
                    .append("\n");
        }
        return res.toString();
    }

    /** All nodes indexed by their hash. */
    protected Map<String, Node> _nodes;

    /** All branches indexed by their name. */
    protected Map<String, Node> _branches;

    /** All staged files indexed by their name. */
    protected Map<String, File> _stage;

    /** All removed files indexed by their name. */
    protected Map<String, File> _removal;

    /** All remote files indexed by their name. */
    protected Map<String, File> _remotes;

    /** List of conflict errors. */
    protected List<String> _conflicted;

    /** The active branch. */
    protected String _currentBranch;

    /** Trigger if happened. */
    protected boolean _pull = false;

    /** The initial commit time of Jan 1, 1970. */
    static final ZonedDateTime INITIAL_COMMIT_TIME =
            ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
}
