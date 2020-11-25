package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import kgit.data.ObjectDatabase
import java.nio.file.Paths

class Init(private val objectDb: ObjectDatabase)
    : CliktCommand(help = "Initialize kgit repository") {

    override fun run() {
        objectDb.init()
        echo("Initialized empty kgit repository in ${Paths.get(".").toAbsolutePath().normalize()}/.kgit")
    }
}