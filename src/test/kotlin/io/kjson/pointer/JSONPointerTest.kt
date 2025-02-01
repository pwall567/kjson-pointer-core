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

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeNonNull
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldThrow

import io.kjson.pointer.JSONPointer.Companion.decodeJSONPointerToken
import io.kjson.pointer.JSONPointer.Companion.encodeJSONPointerToken

class JSONPointerTest {

    @Test fun `should escape correctly on toString`() {
        JSONPointer("").toString() shouldBe ""
        JSONPointer("/foo").toString() shouldBe "/foo"
        JSONPointer("/foo/0").toString() shouldBe "/foo/0"
        JSONPointer("/").toString() shouldBe "/"
        JSONPointer("/a~1b").toString() shouldBe "/a~1b"
        JSONPointer("/c%d").toString() shouldBe "/c%d"
        JSONPointer("/e^f").toString() shouldBe "/e^f"
        JSONPointer("/g|h").toString() shouldBe "/g|h"
        JSONPointer("/i\\j").toString() shouldBe "/i\\j"
        JSONPointer("/ ").toString() shouldBe "/ "
        JSONPointer("/m~0n").toString() shouldBe "/m~0n"
    }

    @Test fun `should create correct URI fragment`() {
        JSONPointer("").toURIFragment() shouldBe ""
        JSONPointer("/foo").toURIFragment() shouldBe "/foo"
        JSONPointer("/foo/0").toURIFragment() shouldBe "/foo/0"
        JSONPointer("/").toURIFragment() shouldBe "/"
        JSONPointer("/a~1b").toURIFragment() shouldBe "/a~1b"
        JSONPointer("/c%d").toURIFragment() shouldBe "/c%25d"
        JSONPointer("/e^f").toURIFragment() shouldBe "/e%5Ef"
        JSONPointer("/g|h").toURIFragment() shouldBe "/g%7Ch"
        JSONPointer("/i\\j").toURIFragment() shouldBe "/i%5Cj"
        JSONPointer("/k\"l").toURIFragment() shouldBe "/k%22l"
        JSONPointer("/ ").toURIFragment() shouldBe "/%20"
        JSONPointer("/m~0n").toURIFragment() shouldBe "/m~0n"
        JSONPointer("/o*p").toURIFragment() shouldBe "/o%2Ap"
        JSONPointer("/q+r").toURIFragment() shouldBe "/q%2Br"
    }

    @Test fun `should correctly decode URI fragment`() {
        JSONPointer.fromURIFragment("") shouldBe JSONPointer("")
        JSONPointer.fromURIFragment("/foo") shouldBe JSONPointer("/foo")
        JSONPointer.fromURIFragment("/foo/0") shouldBe JSONPointer("/foo/0")
        JSONPointer.fromURIFragment("/") shouldBe JSONPointer("/")
        JSONPointer.fromURIFragment("/a~1b") shouldBe JSONPointer("/a~1b")
        JSONPointer.fromURIFragment("/c%25d") shouldBe JSONPointer("/c%d")
        JSONPointer.fromURIFragment("/e%5Ef") shouldBe JSONPointer("/e^f")
        JSONPointer.fromURIFragment("/g%7Ch") shouldBe JSONPointer("/g|h")
        JSONPointer.fromURIFragment("/i%5Cj") shouldBe JSONPointer("/i\\j")
        JSONPointer.fromURIFragment("/k%22l") shouldBe JSONPointer("/k\"l")
        JSONPointer.fromURIFragment("/%20") shouldBe JSONPointer("/ ")
        JSONPointer.fromURIFragment("/m~0n") shouldBe JSONPointer("/m~0n")
        JSONPointer.fromURIFragment("/o%2Ap") shouldBe JSONPointer("/o*p")
        JSONPointer.fromURIFragment("/q%2Br") shouldBe JSONPointer("/q+r")
    }

    @Test fun `should fail on invalid pointer string`() {
        shouldThrow<JSONPointerException>("Illegal JSON Pointer - \"abc\"") {
            JSONPointer("abc")
        }.let {
            it.pointer shouldBe null
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal token in JSON Pointer - \"~\"") {
            JSONPointer("/~")
        }.let {
            it.pointer shouldBe null
            it.cause.shouldBeNonNull().let { cause ->
                cause.message shouldBe "Incomplete escape sequence"
            }
        }
    }

    @Test fun `should navigate correctly to child`() {
        val basePointer = JSONPointer("")
        basePointer.toString() shouldBe ""
        val childPointer1 = basePointer.child("foo")
        childPointer1.toString() shouldBe "/foo"
        val childPointer2 = childPointer1.child(0)
        childPointer2.toString() shouldBe "/foo/0"
        val childPointer3 = childPointer1.child(1)
        childPointer3.toString() shouldBe "/foo/1"
    }

    @Test fun `should navigate correctly to parent`() {
        val startingPointer = JSONPointer("/foo/1")
        startingPointer.toString() shouldBe "/foo/1"
        val parentPointer1 = startingPointer.parent()
        parentPointer1.toString() shouldBe "/foo"
        val parentPointer2 = parentPointer1.parent()
        parentPointer2 shouldBeSameInstance JSONPointer.root
    }

    @Test fun `should combine pointers using child pointer`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        val childPointer = JSONPointer("/jkl/mno")
        startingPointer.child(childPointer).toString() shouldBe "/abc/def/ghi/jkl/mno"
    }

    @Test fun `should combine pointers using parent pointer`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        val parentPointer = JSONPointer("/jkl/mno")
        startingPointer.withParent(parentPointer).toString() shouldBe "/jkl/mno/abc/def/ghi"
    }

    @Test fun `should combine pointers using parent element name`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        startingPointer.withParent("jkl").toString() shouldBe "/jkl/abc/def/ghi"
    }

    @Test fun `should combine pointers using parent element index`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        startingPointer.withParent(0).toString() shouldBe "/0/abc/def/ghi"
    }

    @Test fun `should combine pointers using plus operator`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        (startingPointer + "xyz").toString() shouldBe "/abc/def/ghi/xyz"
        (startingPointer + 3).toString() shouldBe "/abc/def/ghi/3"
        (startingPointer + JSONPointer("/jkl/mno")).toString() shouldBe "/abc/def/ghi/jkl/mno"
        (startingPointer + JSONPointer.root).toString() shouldBe "/abc/def/ghi"
    }

    @Test fun `should truncate JSONPointer`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        startingPointer.truncate(3) shouldBeSameInstance startingPointer
        startingPointer.truncate(2) shouldBe JSONPointer("/abc/def")
        startingPointer.truncate(1) shouldBe JSONPointer("/abc")
        startingPointer.truncate(0) shouldBeSameInstance JSONPointer.root
    }

    @Test fun `should throw exception on invalid truncate of JSONPointer`() {
        val startingPointer = JSONPointer("/abc/def/ghi")
        shouldThrow<JSONPointerException>("Illegal truncate (4), at /abc/def/ghi") {
            startingPointer.truncate(4)
        }.let {
            it.pointer shouldBeSameInstance startingPointer
            it.cause shouldBe null
        }
        shouldThrow<JSONPointerException>("Illegal truncate (-1), at /abc/def/ghi") {
            startingPointer.truncate(-1)
        }.let {
            it.pointer shouldBeSameInstance startingPointer
            it.cause shouldBe null
        }
    }

    @Test fun `should return valid root pointer`() {
        JSONPointer.root shouldBe JSONPointer("")
    }

    @Test fun `should get current token`() {
        JSONPointer("/first/second").current shouldBe "second"
        JSONPointer("/first/second").parent().current shouldBe "first"
        JSONPointer("/first/2").current shouldBe "2"
        JSONPointer.root.current shouldBe null
    }

    @Test fun `should correctly unescape pointer string`() {
        val array1 = JSONPointer.parseString("/abc/def")
        array1.size shouldBe 2
        array1[0] shouldBe "abc"
        array1[1] shouldBe "def"
        val array2 = JSONPointer.parseString("/ab~0")
        array2.size shouldBe 1
        array2[0] shouldBe "ab~"
        val array3 = JSONPointer.parseString("/ab~1")
        array3.size shouldBe 1
        array3[0] shouldBe "ab/"
        val array4 = JSONPointer.parseString("/ab~1~0cd")
        array4.size shouldBe 1
        array4[0] shouldBe "ab/~cd"
    }

    @Test fun `should map JSON Pointer characters correctly`() {
        val unchanged = "unchanged"
        unchanged.encodeJSONPointerToken() shouldBeSameInstance unchanged
        "a/b".encodeJSONPointerToken() shouldBe "a~1b"
        "a/~b".encodeJSONPointerToken() shouldBe "a~1~0b"
        "abc".encodeJSONPointerToken() shouldBe "abc"
        "ab~".encodeJSONPointerToken() shouldBe "ab~0"
        "ab/".encodeJSONPointerToken() shouldBe "ab~1"
        "ab/~cd".encodeJSONPointerToken() shouldBe "ab~1~0cd"
    }

    @Test fun `should unmap JSON Pointer characters correctly`() {
        val unchanged = "unchanged"
        unchanged.decodeJSONPointerToken() shouldBeSameInstance unchanged
        "a~1b".decodeJSONPointerToken() shouldBe "a/b"
        "a~1~0b".decodeJSONPointerToken() shouldBe "a/~b"
    }

    @Test fun `should fail on incorrect JSON Pointer string`() {
        shouldThrow<JSONPointerException>("Illegal token in JSON Pointer - \"~\"") {
            "~".decodeJSONPointerToken()
        }.let {
            it.pointer shouldBe null
            it.cause.shouldBeNonNull().let { cause ->
                cause.message shouldBe "Incomplete escape sequence"
            }
        }
        shouldThrow<JSONPointerException>("Illegal token in JSON Pointer - \"abc~9\"") {
            "abc~9".decodeJSONPointerToken()
        }.let {
            it.pointer shouldBe null
            it.cause.shouldBeNonNull().let { cause ->
                cause.message shouldBe "Invalid escape sequence in \"abc~9\""
            }
        }
    }

    @Test fun `should throw exception when child index is negative`() {
        shouldThrow<JSONPointerException>("JSON Pointer index -1 must not be negative") {
            JSONPointer.root.child(-1)
        }.let {
            it.pointer shouldBe JSONPointer.root
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception when trying to navigate to parent of root pointer`() {
        shouldThrow<JSONPointerException>("Can't get parent of root JSON Pointer") {
            JSONPointer.root.parent()
        }.let {
            it.pointer shouldBe JSONPointer.root
            it.cause shouldBe null
        }
    }

    @Test fun `should create JSON Pointer from a vararg list of strings`() {
        val pointer = JSONPointer.of("abc", "0")
        pointer shouldBe JSONPointer("/abc/0")
    }

    @Test fun `should return root JSON Pointer for empty vararg list`() {
        val pointer = JSONPointer.of()
        pointer shouldBeSameInstance JSONPointer.root
    }

    @Test fun `should create JSON Pointer from a pointer string`() {
        val pointer = JSONPointer.from("/abc/0")
        pointer shouldBe JSONPointer("/abc/0")
    }

    @Test fun `should return root JSON Pointer for empty string`() {
        val pointer = JSONPointer.from("")
        pointer shouldBeSameInstance JSONPointer.root
    }

    @Test fun `should create JSON Pointer from array`() {
        val array = arrayOf("abc", "0")
        val pointer = JSONPointer.from(array)
        pointer shouldBe JSONPointer("/abc/0")
    }

    @Test fun `should return root JSON Pointer for empty array`() {
        val array = emptyArray<String>()
        val pointer = JSONPointer.from(array)
        pointer shouldBeSameInstance JSONPointer.root
    }

    @Test fun `should create JSON Pointer from list`() {
        val list = listOf("abc", "0")
        val pointer = JSONPointer.from(list)
        pointer shouldBe JSONPointer("/abc/0")
    }

    @Test fun `should return root JSON Pointer for empty list`() {
        val list = emptyList<String>()
        val pointer = JSONPointer.from(list)
        pointer shouldBeSameInstance JSONPointer.root
    }

    @Test fun `should return tokens as array`() {
        val pointer = JSONPointer("/abc/def/ghi")
        val array = pointer.tokensAsArray()
        array.size shouldBe 3
        array[0] shouldBe "abc"
        array[1] shouldBe "def"
        array[2] shouldBe "ghi"
        array[0] = "xxx"
        val array2 = pointer.tokensAsArray() // confirm that modifying the array doesn't affect pointer
        array2.size shouldBe 3
        array2[0] shouldBe "abc"
        array2[1] shouldBe "def"
        array2[2] shouldBe "ghi"
    }

    @Test fun `should return tokens as list`() {
        val pointer = JSONPointer("/abc/def/ghi")
        val list = pointer.tokensAsList()
        list.size shouldBe 3
        list[0] shouldBe "abc"
        list[1] shouldBe "def"
        list[2] shouldBe "ghi"
    }

    @Test fun `should return depth of pointer`() {
        val pointer = JSONPointer("/abc/def/ghi")
        pointer.depth shouldBe 3
        JSONPointer.root.depth shouldBe 0
    }

    @Test fun `should get an individual token`() {
        val pointer = JSONPointer("/abc/def/ghi")
        pointer.getToken(0) shouldBe "abc"
        pointer.getToken(1) shouldBe "def"
        pointer.getToken(2) shouldBe "ghi"
    }

    @Test fun `should correctly report when pointer is root`() {
        JSONPointer.root.isRoot shouldBe true
        JSONPointer("/abc").isRoot shouldBe false
    }

    @Test fun `should output string with specified number of tokens`() {
        val pointer = JSONPointer("/abc/def/ghi")
        pointer.toString(0) shouldBe ""
        pointer.toString(1) shouldBe "/abc"
        pointer.toString(2) shouldBe "/abc/def"
        pointer.toString(3) shouldBe "/abc/def/ghi"
    }

    @Test fun `should throw exception with specified text`() {
        val pointer = JSONPointer("/abc/def")
        shouldThrow<JSONPointerException>("Pointer operation failed, at /abc/def") {
            pointer.throwPointerException("Pointer operation failed")
        }.let {
            it.text shouldBe "Pointer operation failed"
            it.pointer shouldBeSameInstance pointer
            it.cause shouldBe null
        }
    }

}
