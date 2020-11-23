package kgit.data

import kgit.base.createNewFileWithinHierarchy
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private const val KGIT_DIR = ".kgit"
private const val OBJECTS_DIR = ".kgit/objects"
private const val REFS_DIR = ".kgit/refs"

const val TYPE_BLOB = "blob"
const val TYPE_TREE = "tree"
const val TYPE_COMMIT = "commit"

private const val nullByte = 0.toChar().toByte()

class ObjectDatabase(val workDir: String) {

    fun init() {
        Files.createDirectory(Path.of("$workDir/$KGIT_DIR"))
    }

    fun hashObject(data: ByteArray, type: String): Oid {
        ensureObjectsDirectory()

        val input =  type.encodeToByteArray() + nullByte + data
        val digest: ByteArray = MessageDigest.getInstance("SHA-1").digest(input)

        // OID is they key in the Object Database
        val oid = digest.joinToString(separator = "") { "%02x".format(it) }

        // write actual binary content using the OID
        File("$workDir/$OBJECTS_DIR/$oid").writeBytes(input)

        return Oid(oid)
    }

    private fun ensureObjectsDirectory() {
        val objectsDirectory = Path.of("$workDir/$OBJECTS_DIR")
        if (!Files.exists(objectsDirectory)) {
            Files.createDirectory(objectsDirectory)
        }
    }

    fun getObject(oid: Oid, expectedType: String): String {
        val obj = Path.of("$workDir/$OBJECTS_DIR/$oid")
        val allBytes = Files.readAllBytes(obj)

        // null byte separates the type from the content
        val separatorPos = allBytes.indexOfFirst { it == nullByte }

        val type = String(allBytes.filterIndexed { index, _ -> index < separatorPos }.toByteArray())
        if (type != expectedType) {
            throw InvalidTypeException(expected = expectedType, actual = type)
        }

        // plain content
        return String(allBytes.filterIndexed { index, _ -> index > separatorPos }.toByteArray())
    }

    fun setHead(oid: Oid) {
        updateRef("HEAD", oid)
    }

    fun updateRef(refName: String, oid: Oid) {
        File("$workDir/$KGIT_DIR/$refName").apply {
            createNewFileWithinHierarchy()
            writeText(oid.value)
        }
    }

    fun getHead(): Oid? = getRef("HEAD")

    fun getRef(refName: String): Oid? {
        val ref = File("$workDir/$KGIT_DIR/$refName")
        return when {
            ref.exists() -> ref.readText().toOid()
            else -> null
        }
    }

    fun iterateRefs(): List<NamedRef> {
        val root = File("$workDir/$REFS_DIR")
        val names = root.walk().filter { it.isFile }.map { it.toRelativeString(root) }
        val refs = names.map {
            NamedRef(it, getRef("refs/$it")!!)
        }
        return listOf(NamedRef("HEAD", getHead()!!)) + refs
    }
}

class InvalidTypeException(expected: String, actual: String)
    : RuntimeException("Expected $expected, got $actual")

inline class Oid(val value: String) {
    override fun toString() = value
}

fun String.toOid() = Oid(this)

data class NamedRef(val name: String, val ref: Oid) {
    override fun toString() = "$name $ref"
}