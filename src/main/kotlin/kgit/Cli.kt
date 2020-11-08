package kgit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kgit.base.KGit
import kgit.data.KGIT_DIR
import kgit.data.ObjectDatabase
import kgit.data.TYPE_BLOB
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    KGitCli()
        .subcommands(Init(), HashObject(), CatFile(), WriteTree(), ReadTree(), CommitCmd())
        .main(args)
}

class KGitCli : CliktCommand(help = "Simple Git-like VCS program") {
    override fun run() {
        /*wrapper command*/
    }
}

class Init : CliktCommand(help = "Initialize kgit repository") {
    override fun run() {
        ObjectDatabase.init()
        echo("Initialized empty kgit repository in ${Paths.get(".").toAbsolutePath().normalize()}/$KGIT_DIR")
    }
}

class HashObject : CliktCommand(name = "hash-object", help = "Store an arbitrary BLOB into the Object Database") {

    private val fileName: String by argument(help = "File to hash")
    private val type: String by option(help = "expected type to match").default(TYPE_BLOB)

    override fun run() {
        val data = Files.readAllBytes(Path.of(fileName))
        val oid = ObjectDatabase.hashObject(data, type)
        echo(oid)
    }
}

class CatFile : CliktCommand(name = "cat-file", help = "Print hashed object") {

    private val oid: String by argument(help = "OID to print")
    private val expected: String by option(help = "expected type to match").default(TYPE_BLOB)

    override fun run() {
        echo(ObjectDatabase.getObject(oid, expected))
    }
}

class WriteTree : CliktCommand(
    name = "write-tree",
    help = "Recursively write directory with its contents into the Object Database"
) {
    override fun run() {
        echo(KGit.writeTree())
    }
}

class ReadTree : CliktCommand(
    name = "read-tree",
    help = "Read the tree structure and write them into the working directory"
) {

    private val tree: String by argument(help = "Tree OID to read")

    override fun run() {
        KGit.readTree(tree)
        echo("Tree $tree has been restored into the working directory.")
    }
}

class CommitCmd : CliktCommand(
    name = "commit",
    help = "Create a new commit"
) {

    private val message: String by option("-m", "--message").required()

    override fun run() {
        val commitId = KGit.commit(message)
        echo(commitId)
    }
}