package kgit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import kgit.base.KGit
import kgit.cli.commands.*
import kgit.data.ObjectDatabase
import kgit.diff.Diff


private val objectDb = ObjectDatabase(workDir = ".")
private val kgit = KGit(objectDb)
private val diff = Diff(objectDb)

fun main(args: Array<String>) {
    KGitCli()
        .subcommands(
            Init(kgit),
            HashObject(objectDb),
            CatFile(objectDb, kgit),
            WriteTree(kgit),
            ReadTree(kgit),
            CommitCmd(kgit),
            Log(objectDb, kgit),
            Checkout(kgit),
            Tag(kgit),
            K(objectDb, kgit),
            Branch(kgit),
            Status(kgit, diff),
            Reset(kgit),
            Show(kgit, diff),
            DiffCommand(kgit, diff)
        ).main(args)
}

class KGitCli : CliktCommand(help = "Simple Git-like VCS program") {
    override fun run() {
        /*wrapper command*/
    }
}

