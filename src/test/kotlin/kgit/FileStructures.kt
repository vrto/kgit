package kgit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import java.io.File

val DYNAMIC_STRUCTURE = "src/test/resources/dynamic-structure"
val DYNAMIC_REMOTE_STRUCTURE = "src/test/resources/remote/remote-structure"
val STATIC_STRUCTURE = "src/test/resources/test-structure"

fun createDynamicTestStructure() = setupTestStructure(DYNAMIC_STRUCTURE)

fun createDynamicRemoteTestStructure() = setupTestStructure(DYNAMIC_REMOTE_STRUCTURE)

private fun setupTestStructure(root: String): File {
    // desired state:
    // ./$root
    // ./$root/flat.txt
    // ./$root/subdir/nested.txt

    val dirToWrite = File(root)
    assertThat(dirToWrite.exists()).isFalse()
    require(dirToWrite.mkdirs())

    File("$root/flat.txt").apply {
        require(createNewFile())
        writeText("orig content")
    }

    require(File("$root/subdir").mkdir())

    File("$root/subdir/nested.txt").apply {
        require(createNewFile())
        writeText("orig nested content")
    }

    return dirToWrite
}


fun addFileToLocalStructure(name: String) {
    File("$DYNAMIC_STRUCTURE/$name").apply {
        require(createNewFile())
        writeText(name)
    }
}

fun addFileToRemoteStructure(name: String) {
    File("$DYNAMIC_REMOTE_STRUCTURE/$name").apply {
        require(createNewFile())
        writeText(name)
    }
}

fun modifyOneFile() {
    File("$DYNAMIC_STRUCTURE/flat.txt").writeText("changed content")
}

fun modifyOneSourceFile(text: String) {
    File("$DYNAMIC_STRUCTURE/source.py").writeText(text)
}

fun modifyCurrentWorkingDirFiles() {
    File("$DYNAMIC_STRUCTURE/flat.txt").writeText("changed content")
    File("$DYNAMIC_STRUCTURE/subdir/nested.txt").writeText("changed nested content")
    File("$DYNAMIC_STRUCTURE/new_file").apply {
        require(createNewFile())
        writeText("new content")
    }
}

fun modifyAndDeleteCurrentWorkingDirFiles() {
    File("$DYNAMIC_STRUCTURE/flat.txt").delete()
    File("$DYNAMIC_STRUCTURE/subdir/nested.txt").writeText("changed nested content")
    File("$DYNAMIC_STRUCTURE/new_file").apply {
        require(createNewFile())
        writeText("new content")
    }
}

fun assertFilesChanged() {
    assertThat(File("$DYNAMIC_STRUCTURE/flat.txt").readText()).isEqualTo("changed content")
    assertThat(File("$DYNAMIC_STRUCTURE/subdir/nested.txt").readText()).isEqualTo("changed nested content")
}

fun assertFilesRestored() {
    assertThat(File("$DYNAMIC_STRUCTURE/flat.txt").readText()).isEqualTo("orig content")
    assertThat(File("$DYNAMIC_STRUCTURE/subdir/nested.txt").readText()).isEqualTo("orig nested content")
    assertThat(File("$DYNAMIC_STRUCTURE/new_file").exists()).isFalse()
}