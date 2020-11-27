package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import kgit.base.KGit
import java.nio.file.Paths

class Init(private val kgit: KGit) : CliktCommand(help = "Initialize kgit repository") {

    override fun run() {
        kgit.init()
        echo("Initialized empty kgit repository in ${Paths.get(".").toAbsolutePath().normalize()}/.kgit")
    }
}