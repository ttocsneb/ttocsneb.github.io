package com.ttocsneb.webcompiler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.ttocsneb.webcompiler.json.JsonConfig
import com.ttocsneb.webcompiler.json.JsonMD
import com.ttocsneb.webcompiler.json.JsonTemplate
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


/**
 * This is the main entry point for the program, as well as most of the processing.
 */
class Main {

    /**
     * Holds program arguments.
     */
    class Args {
        var loadAll = false
        var clean = false

        /**
         * Process the Arguments from the program
         *
         * @param args Arguments given from the main method
         *
         * @return Whether the arguments were properly processed
         */
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


        @JvmStatic fun main(args: Array<String>) {

            val time:Long = System.currentTimeMillis()

            //Get the arguments for the program
            val arg = Args()
            if(!arg.processargs(args))return

            //Create a Gson object with pretty print enabled.
            val gson:Gson = GsonBuilder().setPrettyPrinting().create()

            //load the config file
            val config = gson.fromJson(readFile(configFile), JsonConfig::class.java)

            //Delete all possible output if the clean argument is enabled.
            if(arg.clean) {
                delete(File(config.blog))
            }

            //Compile the Blog
            val blog = Blog()
            blog.compile(arg, config)

            //Compile the Main Page
            val main = MainPage()
            main.compile(arg, config)

            val projects = Projects()
            projects.compile(arg, config)

            //Compile the RSS
            val rss = Rss()
            rss.compile(arg, config)

            println("Finished in ${(System.currentTimeMillis()-time)/1000.0} seconds")


        }

        /**
         * Find all files in a folder with a specific extension
         *
         * @param directory The location to look in
         * @param extension What type of file should be found.
         * @param recursive Whether the search should be recursive
         *
         * @return a list of file paths
         */
        @JvmStatic fun getFiles(directory: String, extension: String, recursive: Boolean): List<String> {
            val mdfiles:MutableList<String> = mutableListOf()
            //go through each child of the directory
            for(f in File(directory).list()) {
                //get the path of the file
                val file = directory + "/" + f
                if(f.endsWith("." + extension) && File(file).isFile) {
                    //if this is an md file, add it to the list of files
                    mdfiles.add(file)
                } else if(File(file).isDirectory && recursive) {
                    //if this is a directory, add all files from that directory to the list of files
                    mdfiles.addAll(getFiles(file, extension, recursive))
                }
            }
            return mdfiles.toList()
        }

        /**
         * Find all files in a folder with a specific extension
         *
         * This will recursively search
         *
         * @param directory The location to look in
         * @param extension What type of file should be found.
         *
         * @return a list of file paths
         */
        @JvmStatic fun getFiles(directory: String, extension: String): List<String> {
            return getFiles(directory, extension, true)
        }

        /**
         * Get a specific template
         *
         * Specify the directory to find templates, and an ID to look for.
         *
         * @param directory Directory to look in
         * @param templateType JsonTemplate#template ID to look for
         * @param gson Gson Parser to use
         *
         * @return Location of template or null
         */
        @JvmStatic fun getTemplate(directory: String, templateType: String, gson: Gson): String? {
            var templateFile = ""

            //Go through each found json file in the templates folder
            getFiles(directory, "json").forEach { f ->
                try {
                    //try to load the json as a JsonTemplate.
                    val temp = gson.fromJson(readFile(f), JsonTemplate::class.java)

                    //If the template has the correct ID, return that, else, continue the search
                    if(temp.template == templateType) {
                        return f
                    }
                } catch (e: JsonSyntaxException) {
                    println(f + " is not of type JsonTemplate.. Skipping")
                }
            }

            return null
        }

        /**
         * Compile the site specific items in a template
         *
         * @param config Global Configuration
         * @param templateCfg template Configuration
         * @param gson Gson parser
         * @param template Template to compile
         *
         * @return partially compiled Template
         */
        @JvmStatic fun preCompile(config: JsonConfig, templateCfg: JsonTemplate, gson: Gson, template:String): String {
            var temp = ""
            var t = template

            var carousel: String = ""
            var carouselInd: String = ""

            //Load the tags
            templateCfg.items.forEach { item ->
                when(item.name) {
                    "carousel" -> carousel = item.value
                    "carouselInd" -> carouselInd = item.value
                }
            }


            //compile the Featured bar into the template
            for(i in config.featured.indices) {
                val conf = gson.fromJson(Main.readFile(config.markdown + "/" + config.featured[i]).split("};")[0]+"}", JsonMD::class.java)
                val f = File(config.markdown + "/" + config.featured[i])
                //get the link to the featured post
                val file = "\\" + f.parentFile.path.replace(config.markdown, config.blog) + "\\" + f.nameWithoutExtension + "\\"

                //Create the html code for the carousel
                temp +=  (if (i%3 == 0) ("<div class=\"item" + (if(i==0) " active" else "") + "\">\n") else "") +
                        "\t<div class=\"col-xs-4\">\n\t\t<h5><a href=\"" + file + "\">" + conf.title + "</a></h5>\n\t\t<h6>" +
                        SimpleDateFormat("MMM d, yyyy").format(Date(conf.unix)) + "</h6>\n\t</div>\n" + (if((i+1)%3 == 0) "</div>\n" else "")
            }
            //Add the final div to the carousel if it hasn't already been created
            if(config.featured.size%3 != 0) {
                temp += "</div>\n"
            }
            //Load the featured items into the template
            t = t.replace(carousel, temp)

            //create the proper number of carousel button things for the template
            temp = ""
            var i=0
            while(i< Math.ceil(config.featured.size / 3.0)) {
                temp += "<li data-target=\"#carousel-featured\" data-slide-to=\"$i\"" + (if(i == 0)" class=\"active\"" else "") +"></li>\n"
                i++
            }
            t = t.replace(carouselInd, temp)


            return t
        }

        /**
         * Read a file into a string
         *
         * @param file The file to read
         *
         * @return Contents of the file
         */
        @JvmStatic fun readFile(file: String): String {
            val f = FileReader(File(file))
            val str = f.readText()
            f.close()
            return str
        }

        /**
         * Save a file.
         *
         * @param file file to write
         * @param content what to write to the file
         */
        @JvmStatic fun saveFile(file: String, content: String) {
            val fw = FileWriter(file)
            val bw = BufferedWriter(fw)
            bw.write(content)
            bw.close()
            fw.close()
        }

        /**
         * Delete a file
         *
         * @param file file to delete
         *
         * @throws IOException
         */
        @Throws(IOException::class)
        @JvmStatic fun delete(file: File) {
            if (file.isDirectory) {
                for (c in file.listFiles()!!)
                    delete(c)
            }
            if (!file.delete())
                throw FileNotFoundException("Failed to delete file: " + file)
        }
    }

}