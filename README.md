# kgit

This is a small DIY Git-like tool based on [ugit](https://www.leshenko.net/p/ugit/#) tutorial.

The original tutorial was written in Python, this is a Kotlin flavor that more-or-less follows the original structure.

### Running

`./gradlew clean build`

the tool can be then used via `kgit` executable.

Sample output:

```
Usage: kgit-cli [OPTIONS] COMMAND [ARGS]...

  Simple Git-like VCS program

Options:
  -h, --help  Show this message and exit

Commands:
  init         Initialize kgit repository
  hash-object  Store an arbitrary BLOB into the Object Database
  cat-file     Print hashed object
  write-tree   Recursively write directory with its contents into the Object
               Database
  read-tree    Read the tree structure and write them into the working
               directory
  commit       Create a new commit
  log          Walk the list of commits and print them
  checkout     Read tree using the given OID and move HEAD
  tag          Tag a commit
  k            Print refs
  branch       Create new branch
  status       Print current work dir status
  reset        Move HEAD to an OID of choice
  show         Compare current working tree with the specified commit
  diff         Compare working tree to a commit
  merge        Merge one branch into another
  merge-base   Computer a common ancestor of two commits
  fetch        Fetch remote refs
  push         Upload objects to remote repository
  add          Add file (or directory) to the index

```
