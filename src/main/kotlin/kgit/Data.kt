package kgit

import java.nio.file.Files
import java.nio.file.Path

const val KGIT_DIR = ".kgit"

object Data {

    fun init() {
        Files.createDirectory(Path.of(KGIT_DIR))
    }
}