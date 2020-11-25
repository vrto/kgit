package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kgit.base.KGit
import kgit.data.ObjectDatabase
import kgit.data.TYPE_BLOB

class CatFile(private val objectDb: ObjectDatabase, private val kgit: KGit)
    : CliktCommand(name = "cat-file", help = "Print hashed object") {

    private val oid: String by argument(help = "OID to print")
    private val expected: String by option(help = "expected type to match").default(TYPE_BLOB)

    override fun run() {
        val resolved = kgit.getOid(oid)
        echo(objectDb.getObject(resolved, expected))
    }
}