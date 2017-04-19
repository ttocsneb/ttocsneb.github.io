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

/**
 * Compile Blog
 */
class Blog {

    var blog:String = ""
    var carousel:String = ""
    var title:String = ""
    var carouselInd:String = ""
    var template:String = ""

    /**
     * Compile the blogs
     */
    fun compile(args: Main.args, config: JsonConfig) {
        println("Compiling Blogs..")
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        //find the template for blogs
        var t:JsonTemplate? = null
        var templateFile:String = ""
        for(f in Main.getFiles(config.template, "json")) {
            val temp = gson.fromJson(Main.readFile(f), JsonTemplate::class.java)
            if(temp.template == "blog") {
                t = temp
                templateFile = f
                break
            }
        }
        if(t == null) t = JsonTemplate()
        val templateconfig = t

        //load the template
        for(i in templateconfig.items) {
            when(i.name) {
                "blog" -> blog = i.value
                "carousel" -> carousel = i.value
                "title" -> title = i.value
                "carouselInd" -> carouselInd = i.value
            }
        }
        template = Main.readFile(File(templateFile).parentFile.path + "/" + templateconfig.file)

        //check if everything needs to be compiled.
        val changed = template.hashCode() != templateconfig.hash || config.featured.hashCode() != config.featuredhash || args.loadAll
        if(changed) {
            if(template.hashCode() != templateconfig.hash)println("Template has changed")
            if(config.featured.hashCode() != config.featuredhash)println("Featured items have changed")
            //modify the saved hash to the current hash
            templateconfig.hash = template.hashCode()
            config.featuredhash = config.featured.hashCode()
            Main.saveFile(Main.configFile, gson.toJson(config))
            Main.saveFile(templateFile, gson.toJson(templateconfig))
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
            var cont: List<String> = Main.readFile(f).split("};")
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
                Main.saveFile(f, gson.toJson(mdconfig) + ";" + cont[1])
                //if the json was uncreated/invalid, don't compile it, give an error, and skip the file
                if(mdconfig.title == "null") {
                    println("\tError: invalid json at: $f\n\t\tCreating Json..\n\t\tSkipping $f")
                    continue
                }
            }

            println("\tCompiling: " + f)

            val dir = File(f)
            val file = File(dir.parentFile.path.replace(config.markdown, config.blog) + "/" + dir.nameWithoutExtension + "/")

            //create the directory, if it couldn't be created, give an error and skip the file
            if(!file.mkdirs()) {
                if(!file.exists()) {
                    println("\tUnable to create directory: " + file.path)
                    println("\t\tI'm not sure what happened, maybe a folder in the path is a file :/")
                    println("\tskipping..")
                    continue
                } else {
                    if(!file.isDirectory) {
                        println("\tUnable to create directory: " + file.path)
                        println("\t\tIt is already created as a file!")
                        println("\tskipping..")
                        continue
                    }
                }
            }

            //compile the post into html
            val node = p.parse(cont[1])
            val post = "<h1>" + mdconfig.title + "</h1>\n<h6>" + mdconfig.date + "</h6>\n" + renderer.render(node)
            val text = template.replace(blog, post).replace(title, mdconfig.title)
            Main.saveFile(file.path + "/index.html", text)
            println("\tCopying files..")
            mdconfig.files.forEach {
                println("\t\t$it")
                Files.copy(File(dir.parentFile.path + "/$it").toPath(), File(file.path + "/$it").toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            println("\tDone Copying files")
        }

        println("Done Compiling Blogs")

    }

    /**
     * Compile the template with anything that would apply to the entire website
     */
    private fun precompile(config: JsonConfig, gson: Gson, t:String):String {
        var temp = ""
        var template = t
        //compile the Featured bar into the template
        for(i in config.featured.indices) {
            val conf = gson.fromJson(Main.readFile(config.markdown + "/" + config.featured[i]).split("};")[0]+"}", JsonMD::class.java)
            val f = File(config.markdown + "/" + config.featured[i])
            //get the link to the featured post
            val file = "\\" + f.parentFile.path.replace(config.markdown, config.blog) + "\\" + f.nameWithoutExtension + "\\"

            //Create the html code for the carousel
            temp +=  (if (i%3 == 0) ("<div class=\"item" + (if(i==0) " active" else "") + "\">\n") else "") +
                    "\t<div class=\"col-xs-4\">\n\t\t<h5><a href=\"" + file + "\">" + conf.title + "</a></h5>\n\t\t<h6>" +
                    conf.date + "</h6>\n\t</div>\n" + (if((i+1)%3 == 0) "</div>\n" else "")
        }
        //Add the final div to the carousel if it hasn't already been created
        if(config.featured.size%3 != 0) {
            temp += "</div>\n"
        }
        //Add the carousel content to the template
        template = template.replace(carousel, temp)

        //create the proper number of carousel button things for the template
        temp = ""
        var i=0
        while(i< Math.ceil(config.featured.size / 3.0)) {
            temp += "<li data-target=\"#carousel-featured\" data-slide-to=\"$i\"" + (if(i == 0)" class=\"active\"" else "") +"></li>\n"
            i++
        }
        template = template.replace(carouselInd, temp)
        return template
    }
}