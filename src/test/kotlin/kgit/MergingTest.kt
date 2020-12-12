package kgit

import kgit.diff.Diff
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit


class MergingTest : DynamicStructureAware() {

    private val diff = Diff(objectDb)

    @Test
    fun `learning stuff`() {
        "diff -DHEAD flat.txt subdir/nested.txt".runCommand(File(DYNAMIC_STRUCTURE))
    }

    fun String.runCommand(workingDir: File) {
        ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(60, TimeUnit.MINUTES)
    }
}