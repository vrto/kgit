package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.base.KGit
import kgit.data.toOid

class MergeBase(private val kgit: KGit)
    : CliktCommand(name = "merge-base", help = "Compute a common ancestor of two commits") {

    private val commit1: String by argument("Commit 1 to compare")
    private val commit2: String by argument("Commit 2 to compare")

    override fun run() {
        val base = kgit.getMergeBase(commit1.toOid(), commit2.toOid())
        echo(base)
    }
}