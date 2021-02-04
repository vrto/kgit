package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kgit.remote.PushResult.*
import kgit.remote.Remote

class Push(private val remote: Remote) : CliktCommand(help = "Upload objects to remote repository") {

    private val remotePath: String by argument(help = "Remote repository to push")
    private val branch: String by argument(help = "Branch to push")

    override fun run() {
        val msg = when (remote.push(remotePath, "refs/heads/$branch")) {
            UNKNOWN_REF -> "Push canceled, unknown branch $branch"
            FORCE_PUSH_REJECTED -> "Force push rejected, fetch remote changes first"
            OK -> "$branch pushed to $remotePath"
        }
        echo(msg)
    }
}