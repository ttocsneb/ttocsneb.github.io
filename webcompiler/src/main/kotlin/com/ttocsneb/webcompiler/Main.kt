package com.ttocsneb.webcompiler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ttocsneb.webcompiler.json.JsonConfig
import com.ttocsneb.webcompiler.json.JsonMD
import com.ttocsneb.webcompiler.json.JsonTemplate
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.ParserEmulationProfile
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.*
import java.lang.Math.ceil

/**
 * This is the main entry point for the program, as well as most of the processing.
 */
class Main {

    class args {
        var loadAll = false

        fun processargs(args:Array<String>):Boolean {
            var bool = true
            for(arg in args) {
                if(arg.startsWith("-")) {
                    val a = arg.substring(1).toLowerCase()
                    when(a){
                        "a" -> loadAll = true
                        "h", "-help" -> {
                            println("\t-a\t\t\tload all found files even if they have not changed\n" +
                                    "\t-h\t--help\tload this help screen")
                            bool = false
                        }
                    }
                }
            }
            return bool
        }
    }

    companion object {
        val configFile:String = "config.json"


        @JvmStatic  fun main(args: Array<String>) {

            val arg:args = args()
            if(!arg.processargs(args))return

            //Create a gson with pretty print enabled.
            val gson:Gson = GsonBuilder().setPrettyPrinting().create()

            //load the config file

            val config = gson.fromJson(readFile(configFile), JsonConfig::class.java)

            val blog = Blog()
            blog.compile(arg, config)


            println("Done!")


        }

        @JvmStatic fun getFiles(directory:String, extension:String):List<String> {
            val mdfiles:MutableList<String> = mutableListOf()
            //go through each child of the directory
            for(f in File(directory).list()) {
                //get the path of the file
                val file = directory + "/" + f
                if(f.endsWith("." + extension) && File(file).isFile) {
                    //if this is an md file, add it to the list of files
                    mdfiles.add(file)
                } else if(File(file).isDirectory) {
                    //if this is a directory, add all files from that directory to the list of files
                    mdfiles.addAll(getFiles(file, extension))
                }
            }
            return mdfiles.toList()
        }

        /**
         * Read a file into a string
         */
        @JvmStatic fun readFile(directory: String):String {
            val file = FileReader(File(directory))
            val str = file.readText()
            file.close()
            return str
        }

        /**
         * Save a file.
         */
        @JvmStatic fun saveFile(directory: String, content: String) {
            val fw = FileWriter(directory)
            val bw = BufferedWriter(fw)
            bw.write(content)
            bw.close()
            fw.close()
        }
    }

}