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
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compile Blog
 */
class Blog {

    val TEMPLATE_BLOG = "blog"

    val BLOG:String = "blog"
    val CAROUSEL:String = "carousel"
    val TITLE:String = "title"
    val CAROUSEL_IND:String = "carouselInd"
    val DESCRIPTION:String = "desc"
    val TAGS:String = "keywords"
    val AUTHOR:String = "author"

    var blog:String = ""
    var carousel:String = ""
    var title:String = ""
    var carouselInd:String = ""
    var template:String = ""
    var description:String = ""
    var tags:String = ""
    var author:String = ""

    /**
     * Compile the blogs
     *
     * @param args processed arguments
     * @param config Global configuration
     */
    fun compile(args: Main.Args, config: JsonConfig) {
        println("Compiling Blogs..")
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        //find the template for blogs
        val templateFile: String = Main.getTemplate(config.template, TEMPLATE_BLOG, gson)!!
        val templateConfig: JsonTemplate = gson.fromJson(Main.readFile(templateFile), JsonTemplate::class.java)
        template = Main.readFile(File(templateFile).parentFile.path + "/" + templateConfig.file)


        //load the template tags
        for(i in templateConfig.items) {
            when(i.name) {
                BLOG -> blog = i.value
                CAROUSEL -> carousel = i.value
                TITLE -> title = i.value
                CAROUSEL_IND -> carouselInd = i.value
                DESCRIPTION -> description = i.value
                TAGS -> tags = i.value
                AUTHOR -> author = i.value
            }
        }


        //check if everything needs to be compiled.
        val changed = template.hashCode() != templateConfig.hash || config.featured.hashCode() != config.featuredhash || args.loadAll
        if(changed) {
            if(template.hashCode() != templateConfig.hash)println("Template has changed")
            if(config.featured.hashCode() != config.featuredhash)println("Featured items have changed")
            //modify the saved hash to the current hash
            templateConfig.hash = template.hashCode()
            config.featuredhash = config.featured.hashCode()
            Main.saveFile(Main.configFile, gson.toJson(config))
            Main.saveFile(templateFile, gson.toJson(templateConfig))
            println("Compiling All Blogs")
        }


        //pre-compile the template
        template = precompile(config, gson, template)


        //Setup the Markdown processor
        val options: MutableDataHolder = MutableDataSet()
        options.setFrom(ParserEmulationProfile.MARKDOWN)
        val p: Parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()


        //Go through each found file, and compile it
        println("Getting files and compiling..")
        for (f in Main.getFiles(config.markdown, "md")) {
            compileBlog(f, gson, changed, config, p, renderer)
        }

        println("Done Compiling Blogs")

    }

    /**
     * Compile a single Blog
     *
     * @param file blog location to compile
     * @param gson Gson Parser
     * @param forceCompile compile the blog, even if it hasn't changed
     * @param config Global Configuration
     * @param parser Parser
     * @param renderer Renderer
     */
    private fun compileBlog(file:String, gson:Gson, forceCompile:Boolean, config: JsonConfig, parser:Parser, renderer:HtmlRenderer) {
        //Split the file into the config, and markdown sections
        var cont: List<String> = Main.readFile(file).split("};")

        //account for an uncreated json, by creating a json with 'null' values
        if(cont.size == 1) {
            cont = listOf("{\"title\": \"null\",\"date\":\"null\"", cont[0])
        }

        val mdconfig = gson.fromJson(cont[0] + "}", JsonMD::class.java)


        //Parse the date into a timestamp
        var timestamp:Long = -1

        val formats = Array(3, {""})
        formats[0] = "MM/dd/yy HH:mm:ss.SSS"
        formats[1] = "MM/dd/yy HH:mm"
        formats[2] = "MM/dd/yy"

        //Try to format the timestamp from the three formats listed above
        for(x in formats) {
            try {
                timestamp = SimpleDateFormat(x).parse(mdconfig.date).time
                break
            } catch (e:ParseException) {
                //do nothing..

            }
        }
        if(timestamp.compareTo(-1) == 0) {
            println("\t\tThere was a problem formatting the date for ${mdconfig.title}")
        }


        //if the file has not changed since last compile, and we are not compiling everything, skip this file
        if(mdconfig.hash == cont[1].hashCode() && timestamp.compareTo(-1) == 0) {
            if(!forceCompile)
                return
        } else {
            //The files have changed, so we need to update the hashes
            mdconfig.hash = cont[1].hashCode()
            mdconfig.unix = timestamp
            Main.saveFile(file, gson.toJson(mdconfig) + ";" + cont[1])
            //if the json was uncreated/invalid, don't compile it, give an error, and skip the file
            if(mdconfig.title == "null") {
                println("\tError: invalid json at: $file\n\t\tCreating Json..\n\t\tSkipping $file")
                return
            }
        }

        println("\tCompiling: " + file)

        val dir = File(file)
        val f = File(dir.parentFile.path.replace(config.markdown, config.blog) + "/" + dir.nameWithoutExtension + "/")

        //create the directory, if it couldn't be created, give an error and skip the file
        if(!f.mkdirs()) {
            if(!f.exists()) {
                println("\tUnable to create directory: " + f.path)
                println("\t\tI'm not sure what happened, maybe a folder in the path is a file :/")
                println("\tskipping..")
                return
            } else {
                if(!f.isDirectory) {
                    println("\tUnable to create directory: " + f.path)
                    println("\t\tIt is already created as a file!")
                    println("\tskipping..")
                    return
                }
            }
        }

        //compile the post into html
        val node = parser.parse(cont[1])
        val post = "<h1>" + mdconfig.title + "</h1>\n<h6>" + SimpleDateFormat("MMM d, yyyy").format(Date(mdconfig.unix)) + "</h6>\n" + renderer.render(node)

        //Fill in the tags
        val text = template
                .replace(blog, post)
                .replace(title, mdconfig.title)
                .replace(description, mdconfig.description)
                .replace(author, mdconfig.author)
                .replace(tags, mdconfig.tags)

        Main.saveFile(f.path + "/index.html", text)
        println("\tCopying files..")
        mdconfig.files.forEach {
            println("\t\t$it")
            Files.copy(File(dir.parentFile.path + "/$it").toPath(), File(f.path + "/$it").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        println("\tDone Copying files")
    }

    /**
     * Compile the template with anything that would apply to the entire website
     *
     * @param config Global Config
     * @param gson Gson Parser
     * @param template Template to compile
     *
     * @return A precompiled template for every blog page.
     */
    private fun precompile(config: JsonConfig, gson: Gson, template:String):String {
        var temp = ""
        var t = template
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
        //Add the carousel content to the template
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
}