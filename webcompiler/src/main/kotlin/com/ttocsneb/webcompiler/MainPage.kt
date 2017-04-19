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
import java.util.*

/**
 * Compile the MainPage
 */
class MainPage {

    var blogs:String = ""
    var carousel:String = ""
    var carouselInd:String = ""
    var template:String = ""
    var file:String = ""
    var templatecfg:JsonTemplate = JsonTemplate()
    var templatefile:String = ""
    var hashID:Int = 0

    fun compile(args: Main.args, config: JsonConfig) {
        println("Compiling MainPage..")
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        //Load the template for the mainpage
        for(f in Main.getFiles(config.template, "json")) {
            val tmp = gson.fromJson(Main.readFile(f), JsonTemplate::class.java)
            if(tmp.template == "frontpage") {
                templatecfg = tmp
                templatefile = f
            }
        }
        //load the items in the configuration.
        for(i in templatecfg.items.indices) {
            val tmp = templatecfg.items[i]
            when(tmp.name) {
                "blogs" -> blogs = tmp.value
                "carousel" -> carousel = tmp.value
                "carouselInd" -> carouselInd = tmp.value
                "file" -> file = tmp.value
            }
        }

        //load the template file
        template = Main.readFile(File(templatefile).parentFile.path + "/" + templatecfg.file)

        //check whether the mainpage needs to be recompiled, note it will be recompiled anyway
        val changed = template.hashCode() != templatecfg.hash || config.featured.hashCode() != config.featuredhash
                || args.loadAll
        if(changed) {
            if(template.hashCode() != templatecfg.hash)println("Template has changed")
            if(config.featured.hashCode() != config.featuredhash)println("Featured items have changed")

            //Update the hashes

            templatecfg.hash = template.hashCode()
            config.featuredhash = config.featured.hashCode()
            Main.saveFile(Main.configFile, gson.toJson(config))
            Main.saveFile(templatefile, gson.toJson(templatecfg))
        }

        //compile the featured bar

        var temp = ""
        for(i in config.featured.indices) {
            val conf = gson.fromJson(Main.readFile(config.markdown + "/" + config.featured[i]).split("};")[0]+"}", JsonMD::class.java)
            val f = File(config.markdown + "/" + config.featured[i])
            //get the link to the featured post
            val file = "\\" + f.parentFile.path.replace(config.markdown, config.blog) + "\\" + f.nameWithoutExtension + "\\"

            //create the html code for the carousel
            temp +=  (if (i%3 == 0) ("<div class=\"item" + (if(i==0) " active" else "") + "\">\n") else "") +
                    "\t<div class=\"col-xs-4\">\n\t\t<h5><a href=\"" + file + "\">" + conf.title + "</a></h5>\n\t\t<h6>" +
                    conf.date + "</h6>\n\t</div>\n" + (if(i%3 == 2) "</div>\n" else "")
        }
        if(config.featured.size%3 != 2) {
            temp += "</div>\n"
        }
        template = template.replace(carousel, temp)

        //create the proper number of carousel buttons
        temp = ""
        var i=0
        while(i<Math.ceil(config.featured.size/3.0)) {
            temp += "<li data-target=\"#carousel-featured\" data-slide-to=\"$i\"" + (if(i == 0)" class=\"active\"" else "") +"></li>\n"
            i++
        }
        template = template.replace(carouselInd, temp)


        val mditems:Array<mditem> = Array(5, { mditem() })

        //compile the post previews
        for(f in Main.getFiles(config.markdown, "md")) {
            //parse the md file
            var cont: List<String> = Main.readFile(f).split("};")
            if(cont.size == 1) {
                cont = listOf("{\"title\": \"null\",\"date\":\"null\"", cont[0])
            }
            val mdcfg = gson.fromJson(cont[0] + "}", JsonMD::class.java)

            //get the date in millis
            val date: List<String> = mdcfg.date.split("/")
            val cal = Calendar.getInstance()
            try {
                cal.set(Calendar.YEAR, Integer.parseInt(date[2])+2000)
                cal.set(Calendar.MONTH, Integer.parseInt(date[0]))
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date[1]))
            } catch (e:Exception) {
                println("Invalid Date!")
                continue
            }
            val t = cal.timeInMillis

            //see if the post is in the top 5 newest posts
            i = mditems.size-1
            var element = -1
            //compare each top 5 element
            while(i>=0) {
                if(t > mditems[i].date) {
                    element = i
                }
                i--
            }
            //sqeeze the post in its rightful place
            if(element != -1) {
                i = mditems.size-1
                //move each item below the post down one
                while(i>element) {
                    mditems[i] = mditems[i-1]
                    i--
                }
                //place the post in the now open spot
                val md = mditem()
                md.content = cont[1]
                md.json = mdcfg
                md.date = t
                md.file = f
                mditems[element] = md
            }

        }

        //Compile the newest 5 elements

        //load up the markdown processor
        val options: MutableDataHolder = MutableDataSet()
        options.setFrom(ParserEmulationProfile.MARKDOWN)
        val p: Parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()

        temp = ""

        for(m in mditems) {

            if(m.file == "")break

            val dir = File(m.file)
            val file = File(dir.parentFile.path.replace(config.markdown, config.blog) + "/" + dir.nameWithoutExtension + "/")
            val text = renderer.render(p.parse(m.content.substring(0, Math.min(300, m.content.length)))) +  "... <a href=\"$file\">see more</a>"

            temp += "<div class=\"row\">\n\t<h3><a href=\"$file\">${m.json.title}</a></h3>\n\t<h6>${m.json.date}</h6>\n\t$text\n</div>\n"

        }

        Main.saveFile(file, template.replace(blogs, temp))
        println("done compiling FrontPage")

    }

    class mditem {
        var content:String = ""
        var json:JsonMD = JsonMD()
        var date:Long = 0
        var file:String = ""
    }
}