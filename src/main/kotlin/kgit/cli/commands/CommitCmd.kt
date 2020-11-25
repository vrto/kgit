package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kgit.base.KGit

class CommitCmd(private val kgit: KGit)
    : CliktCommand(name = "commit", help = "Create a new commit") {

    private val message: String by option("-m", "--message").required()

    override fun run() {
        val commitId = kgit.commit(message)
        echo(commitId)
    }
}