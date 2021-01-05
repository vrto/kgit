package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.base.KGit
import kgit.base.MergeResult.MERGED

class Merge(private val kgit: KGit) : CliktCommand(help = "Merge one branch into another") {

    private val commit: String by argument(help = "Commit to merge into the current branch")

    override fun run() {
        val result = kgit.merge(kgit.getOid(commit))

        if (result == MERGED)
            echo("Merged in working tree\nPlease commit")
        else
            echo("Fast-forward merge, no need to commit")
    }
}