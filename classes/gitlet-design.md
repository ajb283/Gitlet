# Gitlet Design Document
author: Avery Bong

## 1. Classes and Data Structures

### Main class
The Main class is the initial point of execution. We use polymorphism
and a HashMap mapping each command to a custom class where processing
and execution takes place. This excludes the need for a too long
`switch` statement.

Main is also the place where all imaging, copying and exporting of
files occurs. All methods relevant to these functionalities is written
here.

### Command classes (Add, Commit, Checkout, etc)
There is a Command interface, and many classes which inherit from it
and have a single `run` method. The `run` method takes in a Git object
and an array which consists of the rest of the arguments passed into
Gitlet at runtime. In each of these classes, argument format is checked
for the right number and type of arguments, and it calls on the relevant
method in the Git class to perform the required task.

For example, in the `Add` class:

```
@Override
public int run(Git s, String[] command) {
    if (command.length != 1) {
        throw new GitletException("Incorrect # of args");
    }
    File file = Utils.join(Main.getCwd(), command[0]);
    s.stage(file);
    return 0;
}
```

### Git class
This is where all actual execution occurs. The Git class represesnts a Git object, with a full staging area, commit graph, branch logic and
printing.
Fields:
* `_nodes` is a HashMap with each node's unique 40-digit SHA-1 serialization as keys to the node objects.
* `_branches` is a HashMap with each unique branch having an entry mapped to the newest commit in that branch.
* `_stage` is a HashMap with each file that has been staged for addition being mapped to the location of its unique blob in the `/.gitlet` folder.
* `_removal` is the same, but for files staged for removal.
* `_currentBranch` is the current active branch.
* `INITIAL_COMMIT_TIME` is the January 1, 1970, 00:00 commit date represented as an object.

#### Node Subclass

This subclass represents a single commit. It has functionality to
checkout files as well.

Fields:
* `_hash` is this commit's unique SHA-1.
* `_commitTime` is the time this commit was made.
* `_message` is the commit message.
* `_files` is the HashMap with each file backed up by this commit's name mapped to the location of its unique blob.
* `_pred` and `_next` are the parents and children of this commit. The logic is handled by the Git class.


## 2. Algorithms

### Main class

* `main` takes in a String array as usual. Arguments passed in live in this `args` array. It throws an IOException as methods call by it also throw it. Command processing, saving and loading instances of Gitlet for persistence, and some basic command verification occurs here. Most calls to `main` will call the `run` function within instances of the `Command` subclass.
* `readSystem` loads the existing instance of `.gitlet` if available.
* `backup` reads, serializes a file F, stores its blob and returns the location of its blob.
* `exportFile` takes the location of a blob and copies its contents to `DST`.
* `getCwd` is an accessor method for the File object representing the current working directory.

### Commands classes

* `run` is a function where verification for each command's format occurs. This was discussed above.

### Git class

* `stage` is a method to add a file F for staging.
* `makeCommit` is a method to add a new commit. In addition to creating a new `Node` object, it adds this node object to the relevant fields (`_nodes`, `_branches`), clears the staging area, and adds said node to the list of `_next` in its parent(s).
* `makeBranch` is a function to add a new branch.
* `checkoutBranch` is a function to perform the checkout functionality for branches.
* `checkoutFile` is a function to checkout a file given by FILENAME in a node given by its NODEHASH. It throws an IOException as it handles writing, copying and overwriting of files.
* `printLog` is a function to perform the log functionality.
* `currentBranch` is an accessor method for the current branch.
* `branches` is an accessor method for the hashmap of branch names to their nodes.

#### Node subclass

* `checkout` is a method to check out a file given by FILENAME from this commit node specifically.
* `hash` is a function to return the unique serialization of this node's properties. We exclude `_pred` and `_next` from this calculation as these values are not final.
* `toString` produces the string representation of this node as seen in logs.

## 3. Persistence

The `Main` class handles all persistence issues. The entire `Git` object is
loaded and saved by Main at the beginning and end of each run as necessary,
or a new one is created if it does not yet exist. We use the methods
provided in `Utils` to read and write the Git object. No other relevant
functionality in other classes needs to persist for our system to work.

In addition, the blobs (copies of files) are stored in the `.gitlet` folder
as well. Their SHA-1 of their contents are their filenames, and the original
contents are stored inside.

## 4. Design Diagram

![](Gitlet-v3.jpg)

