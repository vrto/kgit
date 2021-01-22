package kgit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import java.io.File

val DYNAMIC_STRUCTURE = "src/test/resources/dynamic-structure"
val DYNAMIC_REMOTE_STRUCTURE = "src/test/resources/remote/remote-structure"
val STATIC_STRUCTURE = "src/test/resources/test-structure"

fun createDynamicTestStructure(): File {
    // desired state:
    // ./dynamic-structure
    // ./dynamic-structure/flat.txt
    // ./dynamic-structure/subdir/nested.txt

    val dirToWrite = File(DYNAMIC_STRUCTURE)
    assertThat(dirToWrite.exists()).isFalse()
    require(dirToWrite.mkdir())

    File("$DYNAMIC_STRUCTURE/flat.txt").apply {
        require(createNewFile())
        writeText("orig content")
    }

    require(File("$DYNAMIC_STRUCTURE/subdir").mkdir())

    File("$DYNAMIC_STRUCTURE/subdir/nested.txt").apply {
        require(createNewFile())
        writeText("orig nested content")
    }

    return dirToWrite
}

fun createDynamicRemoteTestStructure(): File {
    // desired state:
    // ./remote-structure
    // ./remote-structure/flat.txt
    // ./remote-structure/subdir/nested2.txt

    val dirToWrite = File(DYNAMIC_REMOTE_STRUCTURE)
    assertThat(dirToWrite.exists()).isFalse()
    require(dirToWrite.mkdirs())

    File("$DYNAMIC_REMOTE_STRUCTURE/flat.txt").apply {
        require(createNewFile())
        writeText("orig content")
    }

    require(File("$DYNAMIC_REMOTE_STRUCTURE/subdir").mkdir())

    File("$DYNAMIC_REMOTE_STRUCTURE/subdir/nested2.txt").apply {
        require(createNewFile())
        writeText("orig nested content no 2")
    }

    return dirToWrite
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