package kgit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.io.File

val DYNAMIC_STRUCTURE = "src/test/resources/dynamic-structure"
val STATIC_STRUCTURE = "src/test/resources/test-structure"

fun ensureStaticTestStructure(): File {
    val dirToWrite = File(STATIC_STRUCTURE)
    assertThat(dirToWrite.exists()).isTrue()
    return dirToWrite
}

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

fun modifyCurrentWorkingDirFiles() {
    File("$DYNAMIC_STRUCTURE/flat.txt").writeText("changed content")
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