package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.base.KGit

class Merge(private val kgit: KGit) : CliktCommand(help = "Merge one branch into another") {

    private val commit: String by argument(help = "Commit to merge into the current branch")

    override fun run() {
        kgit.merge(kgit.getOid(commit))
        echo("Merged in working tree\nPlease commit")
    }
}