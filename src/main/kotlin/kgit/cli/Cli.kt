package kgit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import kgit.base.KGit
import kgit.cli.commands.*
import kgit.data.ObjectDatabase
import kgit.diff.Diff
import kgit.remote.Remote


private val data = ObjectDatabase(workDir = ".")
private val diff = Diff(data)
private val kgit = KGit(data, diff)
private val remote = Remote(data, kgit)

fun main(args: Array<String>) {
    KGitCli()
        .subcommands(
            Init(kgit),
            HashObject(data),
            CatFile(data, kgit),
            WriteTree(kgit),
            ReadTree(kgit),
            CommitCmd(kgit),
            Log(data, kgit),
            Checkout(kgit),
            Tag(kgit),
            K(data, kgit),
            Branch(kgit),
            Status(kgit, diff, data),
            Reset(kgit),
            Show(kgit, diff),
            DiffCommand(kgit, diff),
            Merge(kgit),
            MergeBase(kgit),
            Fetch(remote),
            Push(remote)
        ).main(args)
}

class KGitCli : CliktCommand(help = "Simple Git-like VCS program") {
    override fun run() {
        /*wrapper command*/
    }
}

