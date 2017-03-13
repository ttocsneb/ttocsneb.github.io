package com.ttocsneb.webcompiler.json

/**
 * This Json goes at the begining of a blog post .md file.
 *
 * Note: a '~' should be placed at the end of the json portion to separate the json from the md.  If none is provided,
 * one will be generated and not compiled.
 */
class JsonMD {
    val title:String = ""
    val date:String = ""
    val compiled:Boolean = false
}