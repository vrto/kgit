package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.base.KGit

class Add(private val kgit: KGit) : CliktCommand(help = "Add file (or directory) to the index.") {

    private val file: String by argument(help = "File to add")

    override fun run() {
        kgit.add(file)
    }
}