package com.ttocsneb.webcompiler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ttocsneb.webcompiler.json.JsonConfig
import java.io.*


/**
 * This is the main entry point for the program, as well as most of the processing.
 */
class Main {

    class args {
        var loadAll = false
        var clean = false

        fun processargs(args:Array<String>):Boolean {
            var bool = true
            for(arg in args) {
                if(arg.startsWith("-")) {
                    val a = arg.substring(1).toLowerCase()
                    when(a){
                        "a" -> loadAll = true
                        "c", "-clean" -> {
                            loadAll = true
                            clean = true
                        }
                        "h", "-help" -> {
                            println("\t-a\t\t\tload all found files even if they have not changed\n" +
                                    "\t-c\t--clean\tclean up any output directory of extra unused stuff.  Note that this deletes the entire directory, then recompiles everythin\n" +
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

            val time:Long = System.currentTimeMillis()

            val arg:args = args()
            if(!arg.processargs(args))return

            //Create a gson with pretty print enabled.
            val gson:Gson = GsonBuilder().setPrettyPrinting().create()

            //load the config file

            val config = gson.fromJson(readFile(configFile), JsonConfig::class.java)

            if(arg.clean) {
                delete(File(config.blog))
            }

            val blog = Blog()
            blog.compile(arg, config)

            val main = MainPage()
            main.compile(arg, config)

            val rss = Rss()
            rss.compile(arg, config)

            println("Finished in ${(System.currentTimeMillis()-time)/1000.0} seconds")


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

        @Throws(IOException::class)
        @JvmStatic fun delete(f: File) {
            if (f.isDirectory) {
                for (c in f.listFiles()!!)
                    delete(c)
            }
            if (!f.delete())
                throw FileNotFoundException("Failed to delete file: " + f)
        }
    }

}