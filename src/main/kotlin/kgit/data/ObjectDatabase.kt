package kgit.data

import kgit.base.createNewFileWithinHierarchy
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest

internal const val KGIT_DIR = ".kgit"
private const val OBJECTS_DIR = ".kgit/objects"
private const val REFS_DIR = ".kgit/refs"

const val TYPE_BLOB = "blob"
const val TYPE_TREE = "tree"
const val TYPE_COMMIT = "commit"

private const val nullByte = 0.toChar().toByte()

/**
 * Handles everything that directly touches the disk.
 */
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

    fun tryParseObject(oid: Oid, expectedType: String) = try {
        getObject(oid, expectedType)
    } catch (e: InvalidTypeException) {
        null
    }

    fun tryParseBlob(oid: Oid): String? = tryParseObject(oid, TYPE_BLOB)

    fun setHead(ref: RefValue, deref: Boolean = true) {
        updateRef("HEAD", ref, deref)
    }

    fun updateRef(refName: String, refValue: RefValue, deref: Boolean = true) {
        val ref = getRefInternal(refName, deref).name
        val toSave = if (refValue.symbolic) "ref: ${refValue.value}" else refValue.value
        val refPath = "$workDir/$KGIT_DIR/$ref"
        File(refPath).apply {
            createNewFileWithinHierarchy()
            writeText(toSave)
        }
    }

    fun getHead(deref: Boolean = true): RefValue = getRef("HEAD", deref)

    fun getMergeHead(deref: Boolean = true): RefValue? = getRef("MERGE_HEAD", deref).takeIf { it.oidOrNull != null }

    fun getRef(refName: String, deref: Boolean = true): RefValue =
        getRefInternal(refName, deref).ref

    private fun getRefInternal(refName: String, deref: Boolean): NamedRefValue {
        // contents of ref file or plain OID
        val value = File("$workDir/$KGIT_DIR/$refName")
            .takeIf { it.exists() }
            ?.readText()
            ?: refName
        return when {
            // recursive symbolic dereferencing
            deref && value.startsWith("ref:") -> getRefInternal(value.drop("ref: ".length), deref = true)
            !deref && value.startsWith("ref:") -> NamedRefValue(refName, RefValue(symbolic = true, value))
            else -> NamedRefValue(refName, RefValue(symbolic = false, value))
        }
    }

    fun iterateRefs(deref: Boolean = true): List<NamedRefValue> {
        val root = File("$workDir/$REFS_DIR")
        val names = root.walk().filter { it.isFile }.map { it.toRelativeString(root) }
        val refs = names.map {
            NamedRefValue(it, getRef("refs/$it", deref))
        }

        // prepend HEAD & MERGE_HEAD to list of all refs
        return mutableListOf<NamedRefValue>().apply {
            add(NamedRefValue("HEAD", getHead()))
            getMergeHead(deref)?.let { add(NamedRefValue("MERGE_HEAD", it)) }
        } + refs
    }

    fun deleteRef(refName: String, deref: Boolean = true) {
        val ref = getRefInternal(refName, deref).name
        File("$workDir/$KGIT_DIR/$ref").delete()
    }

    fun fetchObjectIfMissing(oid: Oid, remoteKGitDir: String) {
        createNewTargeIfNeeded(oid, "$workDir/$OBJECTS_DIR")?.let {
            val source = File("$remoteKGitDir/$OBJECTS_DIR/$oid")
            Files.copy(source.toPath(), it.toPath(), REPLACE_EXISTING)
        }
    }

    fun pushObject(oid: Oid, remoteKGitDir: String) {
        createNewTargeIfNeeded(oid, "$remoteKGitDir/$OBJECTS_DIR")?.let {
            val source = File("$workDir/$OBJECTS_DIR/$oid")
            Files.copy(source.toPath(), it.toPath(), REPLACE_EXISTING)
        }
    }

    private fun createNewTargeIfNeeded(oid: Oid, objectsDirPath: String): File? {
        val target = File("$objectsDirPath/$oid")

        return when {
            target.exists() -> null
            else -> target.also (File::createNewFileWithinHierarchy)
        }
    }
}

class InvalidTypeException(expected: String, actual: String)
    : RuntimeException("Expected $expected, got $actual")

inline class Oid(val value: String) {
    override fun toString() = value
    fun toDirectRef() = RefValue(symbolic = false, value = this.value)
}

data class RefValue(val symbolic: Boolean = false, val value: String) {
    // only to be called when we know we're dealing with OID, as the value can be a reference
    val oid get() = value.toOid()
    val oidOrNull get() = value.toOidOrNull()
    val oidValue get() = oid.value
}

data class NamedRefValue(val name: String, val ref: RefValue)

fun String.toOid(): Oid {
    require(length == 40) {
        "OID must be in SHA-1, but was: $this"
    }
    return Oid(this)
}

fun String.toOidOrNull(): Oid? = try {
    toOid()
} catch (e: IllegalArgumentException) {
    null
}
