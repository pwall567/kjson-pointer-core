# kjson-pointer-core

[![Build Status](https://github.com/pwall567/kjson-pointer-core/actions/workflows/build.yml/badge.svg)](https://github.com/pwall567/kjson-pointer-core/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.9.24&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.9.24)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/kjson-pointer-core?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%22kjson-pointer-core%22)

[JSON Pointer](https://tools.ietf.org/html/rfc6901) core functionality for Kotlin.

## Background

[JSON Pointer](https://tools.ietf.org/html/rfc6901) is a standard for defining the location of an individual element in
a JSON structure.
This library provides functionality to create `JSONPointer` objects as an internal representation of a JSON Pointer, and
to use those objects to navigate the JSON Structure &ndash; that is, to create new pointer objects referencing child or
parent elements in the same JSON Structure.
A `JSONPointer` object is immutable, so all of the navigation operations return new `JSONPointer` pointing to the
selected element.

The library started life as a part of the [kjson-pointer](https://github.com/pwall567/kjson-pointer) library.
It has been split out into a separate library to allow its use independently of the
[kjson-core](https://github.com/pwall567/kjson-core) library which provides parsing and an internal representation of a
JSON structure.



## Quick Start

### Creating a `JSONPointer`

To create a pointer from a JSON Pointer string (which may include encoding of &ldquo;`/`&rdquo; and &ldquo;`~`&rdquo;
characters, as described in the [JSON Pointer Specification](https://tools.ietf.org/html/rfc6901)):
```kotlin
    val pointer = JSONPointer("/prop1/0")
```
This creates a pointer to the 0th element of the &ldquo;prop1&rdquo; property (of whatever JSON value is addressed).

To create a pointer from a `vararg` list of pointer tokens (**not** encoded):
```kotlin
    val pointer = JSONPointer.of("prop1", "0")
```
This creates a pointer identical to the one above.

To create a pointer from pointer string (encoded):
```kotlin
    val pointer = JSONPointer.from("/prop1/0")
```
This also creates a pointer identical to the one above (the reason for using this function rather than the constructor
is that `from` returns the constant `root` if the string is empty).

To create a pointer from an array of pointer tokens (**not** encoded):
```kotlin
    val pointer = JSONPointer.from(arrayOf("prop1", "0"))
```
This also creates a pointer identical to the one above.

To create a pointer from a `List` of pointer tokens (**not** encoded):
```kotlin
    val pointer = JSONPointer.from(listOf("prop1", "0"))
```
This again creates a pointer identical to the one above.

The root element of any JSON structure may be located using the constant `root`:
```kotlin
    val rootPointer = JSONPointer.root
```

### Navigating a JSON Structure

Given an initial `JSONPointer`, it is then possible to navigate to child or parent nodes of the JSON structure.
All of the following functions return a new `JSONPointer` (the original pointer is immutable, and may be used for
further navigation operations).
No checking is performed as to whether the resulting pointer is valid; that will depend on the JSON structure to which
it is applied.

To create a pointer to a child property of the element addressed by the current pointer:
```kotlin
    val newPointer = rootPointer.child("prop1")
```

To create a pointer to a child array element of the element addressed by the current pointer:
```kotlin
    val newPointer2 = newPointer.child(0)
```
The result of the last two operations is a pointer equivalent to `JSONPointer("/prop1/0")`, the pointer in the pointer
creation examples.

It is also possible to combine two `JSONPointer`s.
Suppose that a sub-structure of a JSON object is located at pointer `/abc/def`, and a function examining that
sub-structure has located an element at pointer `/ghi/jkl` within it.
The two pointers may be combined to create a pointer relative to the outer structure:
```kotlin
    val outerPointer = substructurePointer.child(innerPointer)
```

Any of the three overloaded forms of the `child()` function (taking `String` or `Int` or `JSONPointer`) may be applied
to any `JSONPointer`; whether the resulting pointer is valid or not will be determined only when it is applied to a
specific structure.

For those who prefer the syntax, all three of the `child()` operations may be expressed using a `+` (plus) operator:
```kotlin
    val newPointer = rootPointer + "prop1"
```
```kotlin
    val newPointer2 = newPointer + 0
```
```kotlin
    val outerPointer = substructurePointer + innerPointer
```

The plus operation works only when the left-hand side is a `JSONPointer`; to prepend a property name or an array index
or another `JSONPointer`, the `withParent()` functions are available:
```kotlin
    val outerPointer = innerPointer.withParent("inner")
```
```kotlin
    val outerPointer = innerPointer.withParent(0)
```
```kotlin
    val outerPointer = innerPointer.withParent(substructurePointer)
```

And lastly, to navigate to the parent of a non-root node:
```kotlin
    val parentNode = pointer.parent()
```
Attempting to navigate to the parent of the root node will result in an exception being thrown.


### Output

The `toString()` function will return a string in the encoded form specified in the
[JSON Pointer Specification](https://tools.ietf.org/html/rfc6901).
Note that the string representation of the root pointer is an empty string.



## Dependency Specification

The latest version of the library is 1.2, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson-pointer-core</artifactId>
      <version>1.2</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:kjson-pointer-core:1.2'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson-pointer-core:1.2")
```

Peter Wall

2024-07-10
