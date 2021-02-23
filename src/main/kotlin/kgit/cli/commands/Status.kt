package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import kgit.base.KGit
import kgit.data.ObjectDatabase
import kgit.diff.Diff

class Status(private val kgit: KGit, private val diff: Diff, private val data: ObjectDatabase)
    : CliktCommand(help = "Print current work dir status") {

    override fun run() {
        when (val branch = kgit.getBranchName()) {
            null -> echo("HEAD detached at ${kgit.getOid("@")}")
            else -> echo("On branch $branch")
        }

        data.getMergeHead()?.let {
            echo("Merging with $it")
        }

        val headTree = kgit.getComparableTree(kgit.getCommit(kgit.getOid("@")).treeOid)
        val workingTree = kgit.getWorkingTree()
        val indexTree = data.getIndex().asComparableTree()

        val staged = diff.listFileChanges(headTree, indexTree)
        echo("Changes to be committed:")
        staged.forEach { println("${it.action}: ${it.path}") }

        val unstaged = diff.listFileChanges(indexTree, workingTree)
        echo("\nChanges not staged for commit:")
        unstaged.forEach { println("${it.action}: ${it.path}") }
    }
}