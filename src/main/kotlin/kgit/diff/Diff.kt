package kgit.diff

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import kgit.base.KGit
import kgit.base.Tree
import kgit.data.ObjectDatabase
import kgit.data.Oid
import kgit.data.TYPE_BLOB

class Diff(private val kgit: KGit, private val data: ObjectDatabase) {

    fun diffTrees(orig: Tree, changed: Tree): List<String> {
        val fromState = orig.parseState("./", kgit::getTree).map { it.path to it.oid }.toMap()
        val toState = changed.parseState("./", kgit::getTree).map { it.path to it.oid }.toMap()
        val allPaths = (fromState.map { it.key } + toState.map { it.key }).toSet()
        return allPaths
            .filter { fromState[it] != toState[it] }
            .map { diffBlobs(orig = fromState[it], changed = toState[it], path = it) }
            .flatten()
    }

    private fun diffBlobs(orig: Oid?, changed: Oid?, path: String): List<String> {
        val origLines = orig.readLines()
        val changedLines = changed.readLines()
        val patch = DiffUtils.diff(origLines, changedLines)
        return UnifiedDiffUtils.generateUnifiedDiff(path, path, origLines, patch, 0)
    }

    private fun Oid?.readLines() = this?.let {
        data.getObject(it, TYPE_BLOB).split("\n")
    } ?: emptyList()
}
