package kgit.diff

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import kgit.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class MergingTest : TreeAwareTest() {

    private val diff = Diff(data)

    @Nested
    inner class Basic {

        @Test
        fun `merging two identical contents shouldn't change the content of files`() {
            val (base, from, to) = createTwoTreesWithBase()

            val merged = diff.mergeTrees(base, from, to)
            assertThat(merged).containsExactly(
                FileMerge(path = "$DYNAMIC_STRUCTURE/flat.txt", content = "orig content\n"),
                FileMerge(path = "$DYNAMIC_STRUCTURE/subdir/nested.txt", content = "orig nested content\n")
            )
        }

        @Test
        fun `should merge two trees with one file change`() {
            val (base, from, to) = createTwoTreesWithBase {
                modifyOneFile()
            }

            val merged = diff.mergeTrees(base, from, to)
            assertThat(merged).containsExactly(
                FileMerge(path = "$DYNAMIC_STRUCTURE/flat.txt", content = """
                    changed content
                    
                    """.trimIndent()
                ),
                FileMerge(path = "$DYNAMIC_STRUCTURE/subdir/nested.txt", content = "orig nested content\n")
            )
        }

        @Test
        fun `should merge two trees with several file changes`() {
            val (base, from, to) = createTwoTreesWithBase {
                modifyCurrentWorkingDirFiles()
            }

            val merged = diff.mergeTrees(base, from, to)
            assertThat(merged).containsExactly(
                FileMerge(path = "$DYNAMIC_STRUCTURE/flat.txt", content = """
                    changed content
                    
                    """.trimIndent()
                ),
                FileMerge(path = "$DYNAMIC_STRUCTURE/subdir/nested.txt", content = """
                    changed nested content
                    
                    """.trimIndent()
                ),
                FileMerge(path = "$DYNAMIC_STRUCTURE/new_file", content = """
                    new content
                
                """.trimIndent()
                )
            )
        }

        @Test
        fun `should merge two trees with several file changes and a base ancestor`() {
            val (base, from, to) = createThreeTreesWithBase(
                firstTreeChange = ::modifyOneFile,
                secondTreeChange = ::modifyAndDeleteCurrentWorkingDirFiles
            )

            val merged = diff.mergeTrees(base, from, to)
            assertThat(merged).containsExactly(
                FileMerge(path = "$DYNAMIC_STRUCTURE/flat.txt", content = """
                    <<<<<<< BASE
                    orig content
                    =======
                    changed content
                    >>>>>>> MERGE_HEAD
                    
                    """.trimIndent()
                ),
                FileMerge(path = "$DYNAMIC_STRUCTURE/subdir/nested.txt", content = """
                    changed nested content
                    
                    """.trimIndent()
                ),
                FileMerge(path = "$DYNAMIC_STRUCTURE/new_file", content = """
                    new content
                
                """.trimIndent()
                )
            )
        }
    }

    @Nested
    inner class Conflicts {

        @Test
        fun `should print a merge conflict if the ancestor isn't known`() {
            val firstChangeText = """
                def be_a_cat ():
                    print ("Sleep")
                    return True
    
                def be_a_dog ():
                    print ("Bark!")
                    return False
            """.trimIndent()

            val secondChangeText = """
                def be_a_cat ():
                    print ("Meow")
                    return True
    
                def be_a_dog ():
                    print ("Eat homework")
                    return False
            """.trimIndent()

            val (base, from, to) = createThreeTreesWithBase(
                firstTreeChange = { modifyOneSourceFile(firstChangeText) },
                secondTreeChange = { modifyOneSourceFile(secondChangeText) }
            )

            val merged = diff.mergeTrees(base, from, to)
            assertThat(merged).contains(
                FileMerge(path = "$DYNAMIC_STRUCTURE/source.py", content = """
                    <<<<<<< HEAD
                    def be_a_cat ():
                        print ("Sleep")
                        return True
                    ||||||| BASE
                    =======
                    def be_a_cat ():
                        print ("Meow")
                        return True
                    >>>>>>> MERGE_HEAD
                    
                    <<<<<<< HEAD
                    def be_a_dog ():
                        print ("Bark!")
                        return False
                    ||||||| BASE
                    =======
                    def be_a_dog ():
                        print ("Eat homework")
                        return False
                    >>>>>>> MERGE_HEAD
                
                """.trimIndent()
                )
            )
        }

        @Test
        fun `should auto-merge if the ancestor is known`() {
            val ancestorText = """
                def be_a_cat ():
                    print ("Meow")
                    return True
    
                def be_a_dog ():
                    print ("Bark!")
                    return False
            """.trimIndent()

            val firstChangeText = """
                def be_a_cat ():
                    print ("Sleep")
                    return True
    
                def be_a_dog ():
                    print ("Bark!")
                    return False
            """.trimIndent()

            val secondChangeText = """
                def be_a_cat ():
                    print ("Meow")
                    return True
    
                def be_a_dog ():
                    print ("Eat homework")
                    return False
            """.trimIndent()

            modifyOneSourceFile(ancestorText)

            val (base, from, to) = createThreeTreesWithBase(
                firstTreeChange = { modifyOneSourceFile(firstChangeText) },
                secondTreeChange = { modifyOneSourceFile(secondChangeText) }
            )

            val merged = diff.mergeTrees(base, from, to)
            assertThat(merged).contains(
                FileMerge(path = "$DYNAMIC_STRUCTURE/source.py", content = """
                    def be_a_cat ():
                        print ("Sleep")
                        return True
                    
                    def be_a_dog ():
                        print ("Eat homework")
                        return False
                
                """.trimIndent()
                )
            )
        }
    }

}