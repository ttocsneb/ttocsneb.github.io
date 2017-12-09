package com.ttocsneb.webcompiler.json

/**
 * This is a serialization of a json object we will load when compiling the website
 */
class JsonConfig {

    // This should be locations of the featured .md file, omit @markdown part of the file
    val featured:List<String> = emptyList()
    val template:String = ""//the location of the template file
    val markdown:String = ""//the location of the .md posts
    val projects:String = ""//the location of the projects
    val blog:String = ""//the location of the html posts

    var featuredhash:Int = 0


}