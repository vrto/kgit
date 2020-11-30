package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import kgit.base.KGit

class Status(private val kgit: KGit) : CliktCommand(help = "Print current work dir status") {

    override fun run() {
        when (val branch = kgit.getBranchName()) {
            null -> echo("HEAD detached at ${kgit.getOid("@")}")
            else -> echo("On branch $branch")
        }
    }
}