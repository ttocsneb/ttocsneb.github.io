package com.ttocsneb.webcompiler.json

/**
 * Contains the tokens for the template
 */
class JsonTemplate {

    val file:String = ""
    val template:String = ""
    val items:Array<Item> = emptyArray()
    var hash:Int = 0

    class Item {
        val name:String = ""
        var value:String = ""

    }
}