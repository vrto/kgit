package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.remote.Remote

class Push(private val remote: Remote) : CliktCommand(help = "Upload objects to remote repository") {

    private val remotePath: String by argument(help = "Remote repository to push")
    private val branch: String by argument(help = "Branch to push")

    override fun run() {
        remote.push(remotePath, "refs/heads/$branch")
    }
}