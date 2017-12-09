package com.ttocsneb.webcompiler.json

/**
 * This Json goes at the begining of a blog post .md file.
 *
 * Note: a '};' should be placed at the end of the json portion to separate the json from the md (the '}' is part of the
 * json, not a new one).  If none is provided, one will be generated and not compiled.
 */
class JsonProject {

    val icon:String = ""
    val title: String = ""
    val description: String = ""
    val tags: String = ""
    var hash:Int = 0

}