package kgit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import java.nio.file.Paths

fun main(args: Array<String>) {
    KGitCli().subcommands(Init()).main(args)
}

class KGitCli : CliktCommand(help = "Simple Git-like VCS program") {
    override fun run() {/*wrapper command*/}
}

class Init : CliktCommand(help = "Initialize kgit repository") {
    override fun run() {
        Data.init()
        echo("Initialized empty kgit repository in ${Paths.get(".").toAbsolutePath().normalize()}/$KGIT_DIR")
    }
}