package com.ttocsneb.webcompiler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ttocsneb.webcompiler.json.JsonConfig
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

            


        }
    }

}