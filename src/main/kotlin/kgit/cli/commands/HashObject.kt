package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kgit.data.ObjectDatabase
import kgit.data.TYPE_BLOB
import java.nio.file.Files
import java.nio.file.Path

class HashObject(private val objectDb: ObjectDatabase)
    : CliktCommand(name = "hash-object", help = "Store an arbitrary BLOB into the Object Database") {

    private val fileName: String by argument(help = "File to hash")
    private val type: String by option(help = "expected type to match").default(TYPE_BLOB)

    override fun run() {
        val data = Files.readAllBytes(Path.of(fileName))
        val oid = objectDb.hashObject(data, type)
        echo(oid)
    }
}