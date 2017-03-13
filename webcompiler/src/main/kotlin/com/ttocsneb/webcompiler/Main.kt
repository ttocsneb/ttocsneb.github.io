package com.ttocsneb.webcompiler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ttocsneb.webcompiler.json.JsonConfig
import com.ttocsneb.webcompiler.json.JsonMD
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * This is the main entry point for the program, as well as most of the processing.
 */
class Main {


    companion object {
        val configFile:String = "config.json"

        @JvmStatic  fun main(args: Array<String>) {

            //Create a gson with pretty print enabled.
            val gson:Gson = GsonBuilder().setPrettyPrinting().create()

            //load the config file
            val configReader = FileReader(File(configFile))
            val config = gson.fromJson(configReader.readText(), JsonConfig::class.java)
            configReader.close()

            //Check if the template has changed
            val templateReader = FileReader(File(config.template))
            val template = templateReader.readText()
            val changed = template.hashCode() != config.templatehash
            if(changed) {
                //modify the saved hash to the current hash
                config.templatehash = template.hashCode()
                val fw = FileWriter(configFile)
                val bw = BufferedWriter(fw)
                bw.write(gson.toJson(config))
                bw.close()
                fw.close()
                println("The Template has changed, everything will be compiled..")
            }

            for (f in getFilesToCompile(config.markdown)) {
                val mdReader = FileReader(File(f))
                var cont: List<String> = mdReader.readText().split("};")
                //account for an uncreated json, by creating a json with 'null' values
                if(cont.size == 1) {
                    cont = listOf("{\"title\": \"null\",\"date\":\"null\"", cont[0])
                }
                val mdconfig = gson.fromJson(cont.get(0) + "}", JsonMD::class.java)


                //if the file has not changed since last compile, and we are not compiling everything, skip this file
                if(mdconfig.hash == cont[1].hashCode()) {
                    if(!changed)
                        continue
                } else {
                    //modify the hash value, and save it to file.
                    mdconfig.hash = cont[1].hashCode()
                    val fw = FileWriter(f)
                    val bw = BufferedWriter(fw)
                    bw.write(gson.toJson(mdconfig) + ";" + cont[1])
                    bw.close()
                    fw.close()
                    //if the json was uncreated/invalid, don't compile it, give an error, and skip the file
                    if(mdconfig.title == "null") {
                        println("Error: undefined json at: $f\n\tCreating Json..\n\tSkipping $f")
                        continue
                    }
                }

                println("Found: " + f)
            }




        }

        @JvmStatic fun getFilesToCompile(directory:String):List<String> {
            val mdfiles:MutableList<String> = mutableListOf()
            //go through each child of the directory
            for(f in File(directory).list()) {
                val file = directory + "/" + f
                if(f.endsWith(".md") && File(file).isFile) {
                    //if this is an md file, add it to the list of files
                    mdfiles.add(file)
                } else if(File(file).isDirectory) {
                    //if this is a directory, add all md files from that directory to the list of files
                    mdfiles.addAll(getFilesToCompile(file))
                }
            }
            return mdfiles.toList()
        }
    }

}