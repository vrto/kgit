package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.base.KGit

class Checkout(private val kgit: KGit)
    : CliktCommand(help = "Read tree using the given OID and move HEAD") {

    private val oid: String by argument(help = "OID to checkout")

    override fun run() {
        val resolved = kgit.getOid(oid)
        kgit.checkout(resolved)
    }
}