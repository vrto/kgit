package kgit

import kgit.diff.ComparableTree

abstract class TreeAwareTest : DynamicStructureAware() {

    protected fun createTrees(treeModifier: Runnable? = null): Pair<ComparableTree, ComparableTree> {
        val orig = kgit.writeTree()
        treeModifier?.run()
        val changed = kgit.writeTree()

        val from = kgit.getComparableTree(orig)
        val to = kgit.getComparableTree(changed)
        return from to to
    }

}