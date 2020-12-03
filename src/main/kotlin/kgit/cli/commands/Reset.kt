package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.base.KGit
import kgit.data.toOid

class Reset(private val kgit: KGit) : CliktCommand(help = "Move HEAD to an OID of choice") {

    private val commit: String by argument()

    override fun run() {
        kgit.reset(commit.toOid())
    }
}