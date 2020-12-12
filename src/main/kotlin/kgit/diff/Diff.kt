package kgit.diff

import kgit.base.Tree
import kgit.data.ObjectDatabase
import kgit.data.Oid
import kgit.data.TYPE_BLOB
import kgit.diff.ChangeAction.*
import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect.PIPE
import java.util.concurrent.TimeUnit

class Diff(private val data: ObjectDatabase) {

    fun diffTrees(orig: ComparableTree, changed: ComparableTree): List<String> {
        val fromState = orig.asMap()
        val toState = changed.asMap()
        val allPaths = fromState.combinePaths(toState)
        return allPaths
            .filter { fromState[it] != toState[it] }
            .map { diffBlobs(orig = fromState[it], changed = toState[it], path = it) }
    }

    private fun Map<String, Oid>.combinePaths(other: Map<String, Oid>) = (map { it.key } + other.map { it.key }).toSet()

    private fun diffBlobs(orig: Oid?, changed: Oid?, path: String): String {
        val origTemp = File.createTempFile("orig", "comparable").apply {
            writeText(orig.readLines().joinToString(separator = "\n", postfix = "\n"))
            deleteOnExit()
        }
        val changedTemp = File.createTempFile("changed", "comparable").apply {
            writeText(changed.readLines().joinToString(separator = "\n", postfix = "\n"))
            deleteOnExit()
        }
        return "diff --unified --show-c-function --label $path $origTemp --label $path $changedTemp"
            .runCommand(File(data.workDir))
    }

    fun listFileChanges(orig: ComparableTree, changed: ComparableTree): List<FileChange> {
        val fromState = orig.asMap()
        val toState = changed.asMap()
        val allPaths = fromState.combinePaths(toState)
        return allPaths
            .map { FileChange(it, it.determineAction(fromState, toState)) }
            .filter { it.action != NO_CHANGE }
    }

    private fun Oid?.readLines() = this?.let {
        data.getObject(it, TYPE_BLOB).split("\n")
    } ?: emptyList()
}

private fun String.determineAction(originalState: Map<String, Oid>, newState: Map<String, Oid>) = when {
    originalState[this] == null -> NEW
    newState[this] == null -> DELETED
    originalState[this] != newState[this] -> MODIFIED
    else -> NO_CHANGE
}

typealias ComparableTree = List<Tree.FileState>
fun ComparableTree.asMap() = this.map { it.path to it.oid }.toMap()

data class FileChange(val path: String, val action: ChangeAction)
enum class ChangeAction {
    NEW, DELETED, MODIFIED, NO_CHANGE
}

fun String.runCommand(workingDir: File): String = try {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(PIPE)
        .redirectError(PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    proc.inputStream.bufferedReader().readText()
} catch(e: IOException) {
    e.printStackTrace()
    ""
}