package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import kgit.base.KGit
import kgit.data.ObjectDatabase
import java.io.File

// K like gitk
class K(private val objectDb: ObjectDatabase, private val kgit: KGit)
    : CliktCommand(name = "k", help = "Print refs") {

    override fun run() {
        val graph = Factory.mutGraph("commits").setDirected(true)

        val oids = objectDb.iterateRefs()
                .onEach { namedRef -> graph.add(Factory.mutNode(namedRef.name).add(Shape.NOTE).addLink(Factory.mutNode(namedRef.ref.value))) }
                .map { it.ref }

        val allCommits = kgit.listCommitsAndParents(oids)
        allCommits.forEach { oid ->
            val commit = kgit.getCommit(oid)
            graph.add(Factory.mutNode(oid.value).add(Shape.BOX).add(Style.FILLED).add(Label.of(oid.value.substring(0, 10))))
            commit.parentOid?.let {
                graph.add(Factory.mutNode(oid.value).addLink(Factory.mutNode(it.value)))
            }
        }

        val fileName = "kgit-k/${System.currentTimeMillis()}"
        Graphviz.fromGraph(graph).render(Format.PNG).toFile(File(fileName))

        echo("Run: open $fileName.png")
    }
}