package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.base.KGit

class ReadTree(private val kgit: KGit)
    : CliktCommand(name = "read-tree", help = "Read the tree structure and write them into the working directory") {

    private val tree: String by argument(help = "Tree OID to read")

    override fun run() {
        val resolved = kgit.getOid(tree)
        kgit.readTree(resolved)
        echo("Tree $tree has been restored into the working directory.")
    }
}