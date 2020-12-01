package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kgit.base.KGit

class Branch(private val kgit: KGit) : CliktCommand(help = "Create new branch") {

    private val name: String? by argument(help = "Name of the new branch").optional()
    private val startPoint: String by option("--start_point").default("@")

    override fun run() {
        if (name == null) {
            val current = kgit.getBranchName()
            kgit.listBranches().forEach {
                if (current == it) {
                    echo("* $it")
                } else {
                    echo(" $it")
                }
            }
        } else {
            val oid = kgit.getOid(startPoint)
            kgit.createBranch(name!!, oid)
            echo("Branch $name created at $oid")
        }
    }
}