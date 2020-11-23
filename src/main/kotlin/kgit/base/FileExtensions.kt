package kgit.base

import java.io.File
import java.io.IOException

fun File.isIgnored(): Boolean = this.path.contains(".kgit") //TODO use constant

fun File.createNewFileWithinHierarchy() =
    try {
        createNewFile()
    } catch (e: IOException) {
        parentFile.mkdirs()
        createNewFile()
    }

fun File.emptyDir() {
    // delete all files
    this.walk()
        .filter { !it.isIgnored() }
        .filter { it.isFile }
        .forEach { it.delete() }

    // clean up dirs & subdirs
    this.walk()
        .filter { !it.isIgnored() }
        .filter { it.isDirectory }
        .filter { it.path != this.path } // no self-delete
        .forEach { it.delete() }
}