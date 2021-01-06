package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.remote.Remote

class Fetch(private val remote: Remote) : CliktCommand(help = "Fetch remote refs") {

    private val remotePath: String by argument(help = "Remote repository to fetch")

    override fun run() {
        val refs = remote.fetch(remotePath)
        echo("Fetched the following refs: $refs")
    }
}