package kgit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    KGitCli().subcommands(Init(), HashObject(), CatFile()).main(args)
}

class KGitCli : CliktCommand(help = "Simple Git-like VCS program") {
    override fun run() {/*wrapper command*/}
}

class Init : CliktCommand(help = "Initialize kgit repository") {
    override fun run() {
        Data.init()
        echo("Initialized empty kgit repository in ${Paths.get(".").toAbsolutePath().normalize()}/$KGIT_DIR")
    }
}

class HashObject : CliktCommand(name = "hash-object", help = "store an arbitrary BLOB into an Object Database") {

    private val fileName: String by argument(help = "File to hash")
    private val type: String by option(help = "expected type to match").default("blob")

    override fun run() {
        val data = Files.readAllBytes(Path.of(fileName))
        val oid = Data.hashObject(data, type)
        echo(oid)
    }
}

class CatFile : CliktCommand(name = "cat-file", help = "Print hashed object") {

    private val oid: String by argument(help = "OID to print")
    private val expected: String by option(help = "expected type to match").default("blob")

    override fun run() {
        echo(Data.getObject(oid, expected))
    }
}