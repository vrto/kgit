package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.MutableGraph
import kgit.base.KGit
import kgit.data.NamedRefValue
import kgit.data.ObjectDatabase
import kgit.data.Oid
import java.io.File

// K like gitk
class K(private val objectDb: ObjectDatabase, private val kgit: KGit)
    : CliktCommand(name = "k", help = "Print refs") {

    override fun run() {
        val graph = Factory.mutGraph("commits").setDirected(true)

        val oids = objectDb.iterateRefs(deref = false)
            .onEach { graph.addRefNote(it) }
            .filter { !it.ref.symbolic }
            .map { it.ref.oid }

        val allCommits = kgit.listCommitsAndParents(oids)
        allCommits.forEach { oid ->
            val commit = kgit.getCommit(oid)
            graph.add(oid.toNode())
            commit.parentOids.forEach {
                graph.add(oid.linkToParent(it))
            }
        }

        val fileName = "kgit-k/${System.currentTimeMillis()}"
        graph.renderToFile(fileName)

        echo("Run: open $fileName.png")
    }

    private fun MutableGraph.addRefNote(namedRef: NamedRefValue) {
        add(Factory.mutNode(namedRef.name).add(Shape.NOTE).addLink(Factory.mutNode(namedRef.ref.value)))
    }

    private fun Oid.toNode() =
        Factory.mutNode(value).add(Shape.BOX).add(Style.FILLED).add(Label.of(value.substring(0, 10)))

    private fun Oid.linkToParent(parent: Oid) =
        Factory.mutNode(value).addLink(Factory.mutNode(parent.value))

    private fun MutableGraph.renderToFile(fileName: String) {
        Graphviz.fromGraph(this).render(Format.PNG).toFile(File(fileName))
    }
}