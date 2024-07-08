/*
 * @(#) JSONPointerTest.kt
 *
 * kjson-pointer  JSON Pointer for Kotlin
 * Copyright (c) 2021, 2022, 2023 Peter Wall
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

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.pointer.JSONPointer.Companion.encodeJSONPointerToken
import io.kjson.pointer.JSONPointer.Companion.decodeJSONPointerToken

class JSONPointerTest {

    @Test fun `should escape correctly on toString`() {
        expect("") { JSONPointer("").toString() }
        expect("/foo") { JSONPointer("/foo").toString() }
        expect("/foo/0") { JSONPointer("/foo/0").toString() }
        expect("/") { JSONPointer("/").toString() }
        expect("/a~1b") { JSONPointer("/a~1b").toString() }
        expect("/c%d") { JSONPointer("/c%d").toString() }
        expect("/e^f") { JSONPointer("/e^f").toString() }
        expect("/g|h") { JSONPointer("/g|h").toString() }
        expect("/i\\j") { JSONPointer("/i\\j").toString() }
        expect("/ ") { JSONPointer("/ ").toString() }
        expect("/m~0n") { JSONPointer("/m~0n").toString() }
    }

    @Test fun `should create correct URI fragment`() {
        expect("") { JSONPointer("").toURIFragment() }
        expect("/foo") { JSONPointer("/foo").toURIFragment() }
        expect("/foo/0") { JSONPointer("/foo/0").toURIFragment() }
        expect("/") { JSONPointer("/").toURIFragment() }
        expect("/a~1b") { JSONPointer("/a~1b").toURIFragment() }
        expect("/c%25d") { JSONPointer("/c%d").toURIFragment() }
        expect("/e%5Ef") { JSONPointer("/e^f").toURIFragment() }
        expect("/g%7Ch") { JSONPointer("/g|h").toURIFragment() }
        expect("/i%5Cj") { JSONPointer("/i\\j").toURIFragment() }
        expect("/k%22l") { JSONPointer("/k\"l").toURIFragment() }
        expect("/%20") { JSONPointer("/ ").toURIFragment() }
        expect("/m~0n") { JSONPointer("/m~0n").toURIFragment() }
        expect("/o%2Ap") { JSONPointer("/o*p").toURIFragment() }
        expect("/q%2Br") { JSONPointer("/q+r").toURIFragment() }
    }

    @Test fun `should correctly decode URI fragment`() {
        expect(JSONPointer("")) { JSONPointer.fromURIFragment("") }
        expect(JSONPointer("/foo")) { JSONPointer.fromURIFragment("/foo") }
        expect(JSONPointer("/foo/0")) { JSONPointer.fromURIFragment("/foo/0") }
        expect(JSONPointer("/")) { JSONPointer.fromURIFragment("/") }
        expect(JSONPointer("/a~1b")) { JSONPointer.fromURIFragment("/a~1b") }
        expect(JSONPointer("/c%d")) { JSONPointer.fromURIFragment("/c%25d") }
        expect(JSONPointer("/e^f")) { JSONPointer.fromURIFragment("/e%5Ef") }
        expect(JSONPointer("/g|h")) { JSONPointer.fromURIFragment("/g%7Ch") }
        expect(JSONPointer("/i\\j")) { JSONPointer.fromURIFragment("/i%5Cj") }
        expect(JSONPointer("/k\"l")) { JSONPointer.fromURIFragment("/k%22l") }
        expect(JSONPointer("/ ")) { JSONPointer.fromURIFragment("/%20") }
        expect(JSONPointer("/m~0n")) { JSONPointer.fromURIFragment("/m~0n") }
        expect(JSONPointer("/o*p")) { JSONPointer.fromURIFragment("/o%2Ap") }
        expect(JSONPointer("/q+r")) { JSONPointer.fromURIFragment("/q%2Br") }
    }

    @Test fun `should fail on invalid pointer string`() {
        assertFailsWith<JSONPointerException> { JSONPointer("abc") }.let {
            expect("Illegal JSON Pointer - \"abc\"") { it.message }
            assertNull(it.pointer)
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { JSONPointer("/~") }.let {
            expect("Illegal token in JSON Pointer - \"~\"") { it.message }
            assertNull(it.pointer)
            assertNotNull(it.cause) {
                expect("Incomplete escape sequence") { it.message }
            }
        }
    }

    @Test fun `should navigate correctly to child`() {
        val basePointer = JSONPointer("")
        expect("") { basePointer.toString() }
        val childPointer1 = basePointer.child("foo")
        expect("/foo") { childPointer1.toString() }
        val childPointer2 = childPointer1.child(0)
        expect("/foo/0") { childPointer2.toString() }
        val childPointer3 = childPointer1.child(1)
        expect("/foo/1") { childPointer3.toString() }
    }

    @Test fun `should navigate correctly to parent`() {
        val startingPointer = JSONPointer("/foo/1")
        expect("/foo/1") { startingPointer.toString() }
        val parentPointer1 = startingPointer.parent()
        expect("/foo") { parentPointer1.toString() }
        val parentPointer2 = parentPointer1.parent()
        assertSame(JSONPointer.root, parentPointer2)
    }

    @Test fun `should combine pointers using child pointer`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        val childPointer = JSONPointer("/jkl/mno")
        expect("/abc/def/ghi/jkl/mno") { startingPointer.child(childPointer).toString() }
    }

    @Test fun `should combine pointers using parent pointer`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        val parentPointer = JSONPointer("/jkl/mno")
        expect("/jkl/mno/abc/def/ghi") { startingPointer.withParent(parentPointer).toString() }
    }

    @Test fun `should combine pointers using parent element name`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        expect("/jkl/abc/def/ghi") { startingPointer.withParent("jkl").toString() }
    }

    @Test fun `should combine pointers using parent element index`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        expect("/0/abc/def/ghi") { startingPointer.withParent(0).toString() }
    }

    @Test fun `should combine pointers using plus operator`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        expect("/abc/def/ghi/xyz") { (startingPointer + "xyz").toString() }
        expect("/abc/def/ghi/3") { (startingPointer + 3).toString() }
        expect("/abc/def/ghi/jkl/mno") { (startingPointer + JSONPointer("/jkl/mno")).toString() }
        expect("/abc/def/ghi") { (startingPointer + JSONPointer.root).toString() }
    }

    @Test fun `should truncate JSONPointer`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        assertSame(startingPointer, startingPointer.truncate(3))
        expect(JSONPointer("/abc/def")) { startingPointer.truncate(2) }
        expect(JSONPointer("/abc")) { startingPointer.truncate(1) }
        assertSame(JSONPointer.root, startingPointer.truncate(0))
    }

    @Test fun `should throw exception on invalid truncate of JSONPointer`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        assertFailsWith<JSONPointerException> { startingPointer.truncate(4) }.let {
            expect("Illegal truncate (4), at /abc/def/ghi") { it.message }
            assertSame(startingPointer, it.pointer)
            assertNull(it.cause)
        }
        assertFailsWith<JSONPointerException> { startingPointer.truncate(-1) }.let {
            expect("Illegal truncate (-1), at /abc/def/ghi") { it.message }
            assertSame(startingPointer, it.pointer)
            assertNull(it.cause)
        }
    }

    @Test fun `should return valid root pointer`() {
        expect(JSONPointer("")) { JSONPointer.root }
    }

    @Test fun `should get current token`() {
        expect("second") { JSONPointer("/first/second").current }
        expect("first") { JSONPointer("/first/second").parent().current }
        expect("2") { JSONPointer("/first/2").current }
        assertNull(JSONPointer.root.current)
    }

    @Test fun `should correctly unescape pointer string`() {
        val array1 = JSONPointer.parseString("/abc/def")
        expect(2) { array1.size }
        expect("abc") { array1[0] }
        expect("def") { array1[1] }
        val array2 = JSONPointer.parseString("/ab~0")
        expect(1) { array2.size }
        expect("ab~") { array2[0] }
        val array3 = JSONPointer.parseString("/ab~1")
        expect(1) { array3.size }
        expect("ab/") { array3[0] }
        val array4 = JSONPointer.parseString("/ab~1~0cd")
        expect(1) { array4.size }
        expect("ab/~cd") { array4[0] }
    }

    @Test fun `should map JSON Pointer characters correctly`() {
        val unchanged = "unchanged"
        assertSame(unchanged, unchanged.encodeJSONPointerToken())
        expect("a~1b") { "a/b".encodeJSONPointerToken() }
        expect("a~1~0b") { "a/~b".encodeJSONPointerToken() }
        expect("abc") { "abc".encodeJSONPointerToken() }
        expect("ab~0") { "ab~".encodeJSONPointerToken() }
        expect("ab~1") { "ab/".encodeJSONPointerToken() }
        expect("ab~1~0cd") { "ab/~cd".encodeJSONPointerToken() }
    }

    @Test fun `should unmap JSON Pointer characters correctly`() {
        val unchanged = "unchanged"
        assertSame(unchanged, unchanged.decodeJSONPointerToken())
        expect("a/b") { "a~1b".decodeJSONPointerToken() }
        expect("a/~b") { "a~1~0b".decodeJSONPointerToken() }
    }

    @Test fun `should fail on incorrect JSON Pointer string`() {
        assertFailsWith<JSONPointerException> { "~".decodeJSONPointerToken() }.let {
            expect("Illegal token in JSON Pointer - \"~\"") { it.message }
            assertNull(it.pointer)
            assertNotNull(it.cause) {
                expect("Incomplete escape sequence") { it.message }
            }
        }
        assertFailsWith<JSONPointerException> { "abc~9".decodeJSONPointerToken() }.let {
            expect("Illegal token in JSON Pointer - \"abc~9\"") { it.message }
            assertNull(it.pointer)
            assertNotNull(it.cause) {
                expect("Invalid escape sequence in \"abc~9\"") { it.message }
            }
        }
    }

    @Test fun `should throw exception when child index is negative`() {
        assertFailsWith<JSONPointerException> { JSONPointer.root.child(-1) }.let {
            expect("JSON Pointer index -1 must not be negative") { it.message }
            expect(JSONPointer.root) { it.pointer }
            assertNull(it.cause)
        }
    }

    @Test fun `should throw exception when trying to navigate to parent of root pointer`() {
        assertFailsWith<JSONPointerException> { JSONPointer.root.parent() }.let {
            expect("Can't get parent of root JSON Pointer") { it.message }
            expect(JSONPointer.root) { it.pointer }
            assertNull(it.cause)
        }
    }

    @Test fun `should create JSON Pointer from a vararg list of strings`() {
        val pointer = JSONPointer.of("abc", "0")
        expect(JSONPointer("/abc/0")) { pointer }
    }

    @Test fun `should return root JSON Pointer for empty vararg list`() {
        val pointer = JSONPointer.of()
        assertSame(JSONPointer.root, pointer)
    }

    @Test fun `should create JSON Pointer from a pointer string`() {
        val pointer = JSONPointer.from("/abc/0")
        expect(JSONPointer("/abc/0")) { pointer }
    }

    @Test fun `should return root JSON Pointer for empty string`() {
        val pointer = JSONPointer.from("")
        assertSame(JSONPointer.root, pointer)
    }

    @Test fun `should create JSON Pointer from array`() {
        val array = arrayOf("abc", "0")
        val pointer = JSONPointer.from(array)
        expect(JSONPointer("/abc/0")) { pointer }
    }

    @Test fun `should return root JSON Pointer for empty array`() {
        val array = emptyArray<String>()
        val pointer = JSONPointer.from(array)
        assertSame(JSONPointer.root, pointer)
    }

    @Test fun `should create JSON Pointer from list`() {
        val list = listOf("abc", "0")
        val pointer = JSONPointer.from(list)
        expect(JSONPointer("/abc/0")) { pointer }
    }

    @Test fun `should return root JSON Pointer for empty list`() {
        val list = emptyList<String>()
        val pointer = JSONPointer.from(list)
        assertSame(JSONPointer.root, pointer)
    }

    @Test fun `should return tokens as array`() {
        val pointer = JSONPointer("/abc/def/ghi")
        val array = pointer.tokensAsArray()
        expect(3) { array.size }
        expect("abc") { array[0] }
        expect("def") { array[1] }
        expect("ghi") { array[2] }
        array[0] = "xxx"
        val array2 = pointer.tokensAsArray() // confirm that modifying the array doesn't affect pointer
        expect(3) { array2.size }
        expect("abc") { array2[0] }
        expect("def") { array2[1] }
        expect("ghi") { array2[2] }
    }

    @Test fun `should return tokens as list`() {
        val pointer = JSONPointer("/abc/def/ghi")
        val list = pointer.tokensAsList()
        expect(3) { list.size }
        expect("abc") { list[0] }
        expect("def") { list[1] }
        expect("ghi") { list[2] }
    }

    @Test fun `should return depth of pointer`() {
        val pointer = JSONPointer("/abc/def/ghi")
        expect(3) { pointer.depth }
        expect(0) { JSONPointer.root.depth }
    }

    @Test fun `should get an individual token`() {
        val pointer = JSONPointer("/abc/def/ghi")
        expect("abc") { pointer.getToken(0) }
        expect("def") { pointer.getToken(1) }
        expect("ghi") { pointer.getToken(2) }
    }

    @Test fun `should correctly report when pointer is root`() {
        assertTrue(JSONPointer.root.isRoot)
        assertFalse(JSONPointer("/abc").isRoot)
    }

    @Test fun `should output string with specified number of tokens`() {
        val pointer = JSONPointer("/abc/def/ghi")
        expect("") { pointer.toString(0) }
        expect("/abc") { pointer.toString(1) }
        expect("/abc/def") { pointer.toString(2) }
        expect("/abc/def/ghi") { pointer.toString(3) }
    }

    @Test fun `should throw exception with specified text`() {
        val pointer = JSONPointer("/abc/def")
        assertFailsWith<JSONPointerException> {
            pointer.throwPointerException("Pointer operation failed")
        }.let {
            expect("Pointer operation failed, at /abc/def") { it.message }
            expect("Pointer operation failed") { it.text }
            assertSame(pointer, it.pointer)
            assertNull(it.cause)
        }
    }

}
