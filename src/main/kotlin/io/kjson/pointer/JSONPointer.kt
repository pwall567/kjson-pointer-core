/*
 * @(#) JSONPointer.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2021, 2022, 2023, 2024 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kjson.pointer

import net.pwall.text.CharMapResult
import net.pwall.text.StringMapper.checkLength
import net.pwall.text.StringMapper.mapCharacters
import net.pwall.text.StringMapper.mapSubstrings
import net.pwall.text.URIStringMapper.decodeURI
import net.pwall.text.URIStringMapper.encodeURI
import net.pwall.text.UTF8StringMapper.decodeUTF8
import net.pwall.text.UTF8StringMapper.encodeUTF8
import net.pwall.util.ImmutableList

/**
 * JSON Pointer.
 *
 * @author  Peter Wall
 */
class JSONPointer internal constructor(internal val tokens: Array<String>) {

    // Note: It would be good to make this a value class, but that is not currently possible,  A value class is not
    // allowed to have equals() and hashCode() functions (the compiler uses the functions from the single property, and
    // arrays do not provide "deep equals" capability).

    /**
     * Construct a `JSONPointer` from the supplied `String`, which must consist of zero or more tokens representing
     * either an object property name or an array index, with each token preceded by a slash (`/`).
     */
    constructor(pointer: String) : this(parseString(pointer))

    /** The depth of the pointer (the number of tokens). */
    val depth: Int
        get() = tokens.size

    /** The last token of the `JSONPointer` (the current property name or array index). */
    val current: String?
        get() = tokens.lastOrNull()

    /** `true` if the pointer is pointing to root. */
    val isRoot: Boolean
        get() = tokens.isEmpty()

    /**
     * Get the tokens that make up this pointer as an [Array].
     */
    fun tokensAsArray(): Array<String> = if (tokens.isEmpty()) emptyArray() else tokens.copyOf()

    /**
     * Get the tokens that make up this pointer as a [List].
     */
    fun tokensAsList(): List<String> = ImmutableList.listOf(tokens)

    /**
     * Return a new `JSONPointer` referencing the parent object or array of the value referenced by this pointer.
     */
    fun parent(): JSONPointer = when (val len = tokens.size) {
        0 -> throwRootParentError()
        1 -> root
        else -> JSONPointer(tokens.copyOfRange(0, len - 1))
    }

    /**
     * Return a new `JSONPointer` referencing the nominated child property of the object referenced by this pointer.
     */
    fun child(string: String): JSONPointer = JSONPointer(tokens + string)

    /**
     * Return a new `JSONPointer` referencing the nominated child item of the array referenced by this pointer.
     */
    fun child(index: Int): JSONPointer {
        if (index < 0)
            throwPointerException("JSON Pointer index $index must not be negative")
        return child(index.toString())
    }

    /**
     * Return a new `JSONPointer` concatenating this pointer with a child pointer.
     */
    fun child(childPointer: JSONPointer): JSONPointer {
        if (isRoot)
            return childPointer
        if (childPointer.isRoot)
            return this
        return JSONPointer(tokens + childPointer.tokens)
    }

    /**
     * Return a new `JSONPointer` concatenating a parent element name with this pointer.
     */
    fun withParent(string: String): JSONPointer =
            JSONPointer(Array(tokens.size + 1) { i -> if (i == 0) string else tokens[i - 1] })

    /**
     * Return a new `JSONPointer` concatenating a parent array index with this pointer.
     */
    fun withParent(index: Int): JSONPointer {
        if (index < 0)
            throwPointerException("JSON Pointer index $index must not be negative")
        return withParent(index.toString())
    }

    /**
     * Return a new `JSONPointer` concatenating a parent pointer with this pointer.
     */
    fun withParent(parent: JSONPointer): JSONPointer {
        if (isRoot)
            return parent
        if (parent.isRoot)
            return this
        return JSONPointer(parent.tokens + tokens)
    }

    /**
     * Return a new `JSONPointer` concatenating this pointer with a child element name.
     */
    operator fun plus(string: String): JSONPointer = child(string)

    /**
     * Return a new `JSONPointer` concatenating this pointer with a child array index.
     */
    operator fun plus(index: Int): JSONPointer = child(index)

    /**
     * Return a new `JSONPointer` concatenating this pointer with a child pointer.
     */
    operator fun plus(childPointer: JSONPointer): JSONPointer = child(childPointer)

    /**
     * Truncate the pointer to the first _n_ tokens.
     */
    fun truncate(n: Int): JSONPointer = when (n) {
        0 -> root
        depth -> this
        in 1 until depth -> JSONPointer(tokens.copyOfRange(0, n))
        else -> throwPointerException("Illegal truncate ($n)")
    }

    /**
     * Convert the `JSONPointer` to a form suitable for use in a URI fragment.  This means applying not only the JSON
     * Pointer escaping rules, but also encoding into UTF-8 and then applying URI percent encoding.
     */
    fun toURIFragment(): String = buildString {
        for (token in tokens) {
            append('/')
            append(token.encodeJSONPointerToken().encodeUTF8().encodeURI())
        }
    }

    /**
     * Get the token at the specified index.
     */
    fun getToken(index: Int): String = tokens[index]

    /**
     * Throw a [JSONPointerException] with this pointer as the key.
     */
    fun throwPointerException(text: String): Nothing {
        throw JSONPointerException(text, this)
    }

    /**
     * Create the string form of this pointer using the first _n_ tokens.
     */
    fun toString(numTokens: Int): String = toString(tokens, numTokens)

    override fun equals(other: Any?): Boolean =
        this === other || other is JSONPointer && tokens.contentEquals(other.tokens)

    override fun hashCode(): Int = tokens.contentHashCode()

    override fun toString(): String = toString(tokens, tokens.size)

    companion object {

        /** The root `JSONPointer`. */
        val root = JSONPointer(emptyArray())

        @Suppress("ConstPropertyName")
        private const val emptyString = ""

        /**
         * Create a `JSONPointer` from a `vararg` list of tokens.
         */
        @Suppress("unchecked_cast")
        fun of(vararg tokens: String): JSONPointer =
            if (tokens.isEmpty()) root else JSONPointer((tokens as Array<String>).copyOf())

        /**
         * Create a `JSONPointer` from an array of tokens.
         */
        fun from(array: Array<String>): JSONPointer = if (array.isEmpty()) root else JSONPointer(array.copyOf())

        /**
         * Create a `JSONPointer` from a list of tokens.
         */
        fun from(list: List<String>): JSONPointer = if (list.isEmpty()) root else JSONPointer(list.toTypedArray())

        /**
         * Create the string form of a JSON pointer from the first _n_ tokens in the specified array.
         */
        fun toString(tokens: Array<String>, n: Int): String {
            if (n == 0)
                return emptyString
            return buildString {
                for (i in 0 until n) {
                    append('/')
                    append(tokens[i].encodeJSONPointerToken())
                }
            }
        }

        /**
         * Parse a JSON Pointer string into an array of string tokens.
         */
        fun parseString(string: String): Array<String> {
            if (string.isEmpty())
                return emptyArray()
            if (string[0] != '/')
                throw JSONPointerException("Illegal JSON Pointer - \"$string\"")
            return string.substring(1).split('/').map {
                try {
                    it.decodeJSONPointerToken()
                } catch (e: JSONPointerException) {
                    throw e
                } catch (e: Exception) {
                    throw JSONPointerException("Illegal token in JSON Pointer - \"$string\"").withCause(e)
                }
            }.toTypedArray()
        }

        /**
         * Create a `JSONPointer` from a URI fragment.
         */
        fun fromURIFragment(fragment: String): JSONPointer {
            val pointer: String = try {
                fragment.decodeURI().decodeUTF8()
            } catch (e: Exception) {
                throw JSONPointerException("Illegal URI fragment - \"$fragment\"").withCause(e)
            }
            return JSONPointer(pointer)
        }

        /**
         * Encode a string using the character substitutions specified for JSON pointer tokens.
         */
        fun String.encodeJSONPointerToken() = mapCharacters {
            when (it) {
                '~' -> "~0"
                '/' -> "~1"
                else -> null
            }
        }

        /**
         * Decode a string encoded using the character substitutions specified for JSON pointer tokens.
         */
        fun String.decodeJSONPointerToken() = mapSubstrings { index ->
            try {
                when (this[index]) {
                    '~' -> {
                        checkLength(this, index, 2)
                        when (this[index + 1]) {
                            '0' -> mapJSONPointerTilde
                            '1' -> mapJSONPointerSlash
                            else -> throw IllegalArgumentException("Invalid escape sequence in \"$this\"")
                        }
                    }
                    else -> null
                }
            }
            catch (e: Exception) {
                throw JSONPointerException("Illegal token in JSON Pointer - \"$this\"").withCause(e)
            }
        }

        private val mapJSONPointerTilde = CharMapResult(2, '~')
        private val mapJSONPointerSlash = CharMapResult(2, '/')

        /**
         * Throw exception for attempt to get parent of root pointer (used by other libraries).
         */
        fun throwRootParentError(): Nothing {
            throw JSONPointerException("Can't get parent of root JSON Pointer", root)
        }

    }

}
