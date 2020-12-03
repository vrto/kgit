package kgit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import kgit.base.KGit
import kgit.cli.commands.*
import kgit.data.ObjectDatabase


private val objectDb = ObjectDatabase(workDir = ".")
private val kgit = KGit(objectDb)

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
            Status(kgit),
            Reset(kgit),
        ).main(args)
}

class KGitCli : CliktCommand(help = "Simple Git-like VCS program") {
    override fun run() {
        /*wrapper command*/
    }
}

