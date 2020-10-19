package kgit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) {
    KGitCli().subcommands(Init()).main(args)
}

class KGitCli : CliktCommand(help = "Simple Git-like VCS program") {
    override fun run() {/*wrapper command*/}
}

class Init : CliktCommand(help = "Initialize kgit repository") {
    override fun run() {
        echo("Initializing kgit repository")
    }
}