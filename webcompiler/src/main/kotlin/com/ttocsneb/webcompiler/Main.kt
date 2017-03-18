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


    companion object {
        val configFile:String = "config.json"
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

        @JvmStatic  fun main(args: Array<String>) {

            if(!processargs(args))return

            //Create a gson with pretty print enabled.
            val gson:Gson = GsonBuilder().setPrettyPrinting().create()

            //load the config file

            val config = gson.fromJson(readFile(configFile), JsonConfig::class.java)

            //Check if the template/featured has changed
            val templateconfig = gson.fromJson(readFile(config.template), JsonTemplate::class.java)
            var template = readFile(File(config.template).parentFile.path + "/" + templateconfig.file)

            val changed = template.hashCode() != templateconfig.hash || config.featured.hashCode() != config.featuredhash || loadAll
            if(changed) {
                //modify the saved hash to the current hash
                templateconfig.hash = template.hashCode()
                config.featuredhash = config.featured.hashCode()
                saveFile(configFile, gson.toJson(config))
                saveFile(config.template, gson.toJson(templateconfig))
                if(template.hashCode() != templateconfig.hash)println("Template has changed")
                if(config.featured.hashCode() != config.featuredhash)println("Featured items have changed")
                println("Loading everything")
                //println("The Template or Featured Section has changed, everything will be compiled..")
            }


            //pre-compile the template
            template = precompile(config, gson, template, templateconfig)

            //Setup the Markdown processor
            val options:MutableDataHolder = MutableDataSet()
            options.setFrom(ParserEmulationProfile.MARKDOWN)
            val p:Parser = Parser.builder(options).build()
            val renderer = HtmlRenderer.builder(options).build()

            //Go through each found file, and compile it
            println("Getting files and compiling..")
            for (f in getFilesToCompile(config.markdown)) {
                var cont: List<String> = readFile(f).split("};")
                //account for an uncreated json, by creating a json with 'null' values
                if(cont.size == 1) {
                    cont = listOf("{\"title\": \"null\",\"date\":\"null\"", cont[0])
                }
                val mdconfig = gson.fromJson(cont[0] + "}", JsonMD::class.java)


                //if the file has not changed since last compile, and we are not compiling everything, skip this file
                if(mdconfig.hash == cont[1].hashCode()) {
                    if(!changed)
                        continue
                } else {
                    //modify the hash value, and save it to file.
                    mdconfig.hash = cont[1].hashCode()
                    saveFile(f, gson.toJson(mdconfig) + ";" + cont[1])
                    //if the json was uncreated/invalid, don't compile it, give an error, and skip the file
                    if(mdconfig.title == "null") {
                        println("Error: undefined json at: $f\n\tCreating Json..\n\tSkipping $f")
                        continue
                    }
                }

                println("Compiling: " + f)

                val dir = File(f)
                val file = File(dir.parentFile.path.replace(config.markdown, config.blog) + "/" + dir.nameWithoutExtension + "/")

                //create the directory, if it couldn't be created, give an error and skip the file
                if(!file.mkdirs()) {
                    if(!file.exists()) {
                        println("Unable to create directory: " + file.path)
                        println("\tI'm not sure what happened, maybe a folder in the path is a file :/")
                        println("skipping..")
                        continue
                    } else {
                        if(!file.isDirectory) {
                            println("Unable to create directory: " + file.path)
                            println("\tIt is already created as a file!")
                            println("skipping..")
                            continue
                        }
                    }
                }

                //compile the post into html
                val node = p.parse(cont[1])
                val post = "<h1>" + mdconfig.title + "</h1>\n<h6>" + mdconfig.date + "</h6>\n" + renderer.render(node)
                val text = template.replace(templateconfig.blog, post).replace(templateconfig.title, mdconfig.title)
                saveFile(file.path + "/index.html", text)
            }

            println("Done!")


        }

        /**
         * Compile the template with anything that would apply to the entire website
         */
        private fun precompile(config: JsonConfig, gson: Gson, t:String, templateconfig: JsonTemplate):String {
            var temp = ""
            var template = t
            //compile the Featured bar into the template
            for(i in config.featured.indices) {
                val conf = gson.fromJson(readFile(config.markdown + "/" + config.featured[i]).split("};")[0]+"}", JsonMD::class.java)
                val f = File(config.markdown + "/" + config.featured[i])
                //get the link to the featured post
                val file = "\\" + f.parentFile.path.replace(config.markdown, config.blog) + "\\" + f.nameWithoutExtension + "\\"

                //Create the html code for the carousel
                temp +=  (if (i%3 == 0) ("<div class=\"item" + (if(i==0) " active" else "") + "\">\n") else "") +
                        "\t<div class=\"col-xs-4\">\n\t\t<h5><a href=\"" + file + "\">" + conf.title + "</a></h5>\n\t\t<h6>" +
                        conf.date + "</h6>\n\t</div>\n" + (if(i%3 == 2) "</div>\n" else "")
            }
            //Add the final div to the carousel if it hasn't already been created
            if(config.featured.size%3 != 2) {
                temp += "</div>\n"
            }
            //Add the carousel content to the template
            template = template.replace(templateconfig.carousel, temp)

            //create the proper number of carousel button things for the template
            temp = ""
            var i=0
            while(i<ceil(config.featured.size/3.0)) {
                temp += "<li data-target=\"#carousel-featured\" data-slide-to=\"$i\"" + (if(i == 0)" class=\"active\"" else "") +"></li>\n"
                i++
            }
            template = template.replace(templateconfig.carouselInd, temp)
            return template
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