package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import kgit.base.KGit
import kgit.diff.Diff

class Status(private val kgit: KGit, private val diff: Diff) : CliktCommand(help = "Print current work dir status") {

    override fun run() {
        when (val branch = kgit.getBranchName()) {
            null -> echo("HEAD detached at ${kgit.getOid("@")}")
            else -> echo("On branch $branch")
        }

        val headTree = kgit.getComparableTree(kgit.getCommit(kgit.getOid("@")).treeOid)
        val changes = diff.listFileChanges(headTree, kgit.getWorkingTree())
        echo("Changes to be committed:")
        changes.forEach { println("${it.action}: ${it.path}") }
    }
}