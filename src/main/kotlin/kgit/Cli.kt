package kgit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.mutGraph
import guru.nidi.graphviz.model.Factory.mutNode
import kgit.base.Commit
import kgit.base.KGit
import kgit.data.ObjectDatabase
import kgit.data.Oid
import kgit.data.TYPE_BLOB
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


private val objectDb = ObjectDatabase(workDir = ".")
private val kgit = KGit(objectDb)

fun main(args: Array<String>) {
    KGitCli()
        .subcommands(Init(), HashObject(), CatFile(), WriteTree(), ReadTree(),
                     CommitCmd(), Log(), Checkout(), Tag(), K())
        .main(args)
}

class KGitCli : CliktCommand(help = "Simple Git-like VCS program") {
    override fun run() {
        /*wrapper command*/
    }
}

class Init : CliktCommand(help = "Initialize kgit repository") {
    override fun run() {
        objectDb.init()
        echo("Initialized empty kgit repository in ${Paths.get(".").toAbsolutePath().normalize()}/.kgit")
    }
}

class HashObject : CliktCommand(name = "hash-object", help = "Store an arbitrary BLOB into the Object Database") {

    private val fileName: String by argument(help = "File to hash")
    private val type: String by option(help = "expected type to match").default(TYPE_BLOB)

    override fun run() {
        val data = Files.readAllBytes(Path.of(fileName))
        val oid = objectDb.hashObject(data, type)
        echo(oid)
    }
}

class CatFile : CliktCommand(name = "cat-file", help = "Print hashed object") {

    private val oid: String by argument(help = "OID to print")
    private val expected: String by option(help = "expected type to match").default(TYPE_BLOB)

    override fun run() {
        val resolved = kgit.getOid(oid)
        echo(objectDb.getObject(resolved, expected))
    }
}

class WriteTree : CliktCommand(
        name = "write-tree",
        help = "Recursively write directory with its contents into the Object Database"
) {
    override fun run() {
        echo(kgit.writeTree())
    }
}

class ReadTree : CliktCommand(
        name = "read-tree",
        help = "Read the tree structure and write them into the working directory"
) {

    private val tree: String by argument(help = "Tree OID to read")

    override fun run() {
        val resolved = kgit.getOid(tree)
        kgit.readTree(resolved)
        echo("Tree $tree has been restored into the working directory.")
    }
}

class CommitCmd : CliktCommand(
        name = "commit",
        help = "Create a new commit"
) {

    private val message: String by option("-m", "--message").required()

    override fun run() {
        val commitId = kgit.commit(message)
        echo(commitId)
    }
}

class Log : CliktCommand(help = "Walk the list of commits and print them") {

    private val start: String? by argument(help = "OID to start from").optional()

    override fun run() {
        var oid: Oid? = kgit.getOid(start ?: "@")
        while (oid != null) {
            val commit = kgit.getCommit(oid)
            commit.prettyPrint(oid)
            oid = commit.parentOid
        }
    }

    private fun Commit.prettyPrint(oid: Oid) {
        println("commit $oid")
        println("\t$message")
        println("")
    }
}

class Checkout : CliktCommand(help = "Read tree using the given OID and move HEAD") {

    private val oid: String by argument(help = "OID to checkout")

    override fun run() {
        val resolved = kgit.getOid(oid)
        kgit.checkout(resolved)
    }
}

class Tag : CliktCommand(help = "Tag a commit") {

    private val name: String by argument(help = "Tag name to use")
    private val oid: String? by argument(help = "OID to tag").optional()

    override fun run() {
        val resolved = kgit.getOid(oid ?: "@")
        kgit.tag(name, resolved)
    }
}

// K like gitk
class K : CliktCommand(name = "k", help = "Print refs") {

    override fun run() {
        val graph = mutGraph("commits").setDirected(true)

        val oids = objectDb.iterateRefs()
                .onEach { namedRef -> graph.add(mutNode(namedRef.name).add(Shape.NOTE).addLink(mutNode(namedRef.ref.value))) }
                .map { it.ref }

        val allCommits = kgit.listCommitsAndParents(oids)
        allCommits.forEach { oid ->
            val commit = kgit.getCommit(oid)
            graph.add(mutNode(oid.value).add(Shape.BOX).add(Style.FILLED).add(Label.of(oid.value.substring(0, 10))))
            commit.parentOid?.let {
                graph.add(mutNode(oid.value).addLink(mutNode(it.value)))
            }
        }

        val fileName = "kgit-k/${System.currentTimeMillis()}"
        Graphviz.fromGraph(graph).render(Format.PNG).toFile(File(fileName))

        echo("Run: open $fileName.png")
    }
}