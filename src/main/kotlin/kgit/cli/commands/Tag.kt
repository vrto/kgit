package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kgit.base.KGit

class Tag(private val kgit: KGit) : CliktCommand(help = "Tag a commit") {

    private val name: String by argument(help = "Tag name to use")
    private val oid: String? by argument(help = "OID to tag").optional()

    override fun run() {
        val resolved = kgit.getOid(oid ?: "@")
        kgit.tag(name, resolved)
    }
}