package kgit.diff

import kgit.DynamicStructureAware

abstract class TreeAwareTest : DynamicStructureAware() {

    protected fun createTrees(treeModifier: Runnable? = null): Pair<ComparableTree, ComparableTree> {
        kgit.add(".")
        val orig = kgit.writeTree()

        treeModifier?.run()
        kgit.add(".")
        val changed = kgit.writeTree()

        val from = kgit.getComparableTree(orig)
        val to = kgit.getComparableTree(changed)
        return from to to
    }

    data class TreesWithBase(val base: ComparableTree, val from: ComparableTree, val to: ComparableTree)

    protected fun createTwoTreesWithBase(treeModifier: Runnable? = null): TreesWithBase {
        kgit.add(".")
        val orig = kgit.writeTree()

        treeModifier?.run()
        kgit.add(".")
        val changed = kgit.writeTree()

        return TreesWithBase(
            base = kgit.getComparableTree(orig), // first OID is base for two trees only
            from = kgit.getComparableTree(orig),
            to = kgit.getComparableTree(changed)
        )
    }

    protected fun createThreeTreesWithBase(firstTreeChange: Runnable, secondTreeChange: Runnable): TreesWithBase {
        kgit.add(".")
        val orig = kgit.writeTree()

        firstTreeChange.run()
        kgit.add(".")
        val changed1 = kgit.writeTree()

        secondTreeChange.run()
        kgit.add(".")
        val changed2 = kgit.writeTree()

        return TreesWithBase(
            base = kgit.getComparableTree(orig), // first OID is base for two trees only
            from = kgit.getComparableTree(changed1),
            to = kgit.getComparableTree(changed2)
        )
    }

}