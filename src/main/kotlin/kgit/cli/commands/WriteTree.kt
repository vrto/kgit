package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import kgit.base.KGit

class WriteTree(private val kgit: KGit)
    : CliktCommand(name = "write-tree", help = "Recursively write directory with its contents into the Object Database") {

    override fun run() {
        echo(kgit.writeTree())
    }
}