package com.ttocsneb.webcompiler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ttocsneb.webcompiler.json.JsonConfig
import com.ttocsneb.webcompiler.json.JsonMD
import com.ttocsneb.webcompiler.json.JsonTemplate
import java.io.File
import java.text.SimpleDateFormat
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

    /**
     * Compile the Main Page
     *
     * @param args Program Arguments
     * @param config Global Configuration
     */
    fun compile(args: Main.Args, config: JsonConfig) {
        println("Compiling MainPage..")
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()


        //Load the template for the mainpage
        templatefile = Main.getTemplate(config.template, "frontpage", gson)!!
        templatecfg = gson.fromJson(Main.readFile(templatefile), JsonTemplate::class.java)
        template = Main.readFile(File(templatefile).parentFile.path + "/" + templatecfg.file)


        //load the items in the configuration.
        templatecfg.items.forEach {
            when(it.name) {
                "blogs" -> blogs = it.value
                "carousel" -> carousel = it.value
                "carouselInd" -> carouselInd = it.value
                "file" -> file = it.value
            }
        }


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


        //compile the side bar
        template = Main.preCompile(config, templatecfg, gson, template)


        val mditems:Array<MdItem> = Array(5, { MdItem() })

        //compile the post previews
        for(f in Main.getFiles(config.markdown, "md")) {
            //parse the md file
            var cont: List<String> = Main.readFile(f).split("};")
            //create an empty json if there was none loaded
            if(cont.size == 1) {
                cont = listOf("{\"title\": \"null\",\"date\":\"null\"", cont[0])
            }
            val mdcfg = gson.fromJson(cont[0] + "}", JsonMD::class.java)

            //Order the posts from newest to oldest
            var i = mditems.size - 1
            var element = -1
            while (i >= 0) {
                if (mdcfg.unix > mditems[i].json.unix)
                    element = i
                i--
            }

            if (element != -1) {
                i = mditems.size - 1
                while (i > element) {
                    mditems[i] = mditems[i - 1]
                    i--
                }
                val md = MainPage.MdItem()
                md.json = mdcfg
                md.file = f
                md.content = cont[1]
                mditems[element] = md
            }

        }

        var temp = ""


        //Compile the newest 5 elements
        for(m in mditems) {

            if(m.file == "")break

            val dir = File(m.file)
            val file = File(dir.parentFile.path.replace(config.markdown, config.blog) + "/" + dir.nameWithoutExtension + "/").toString().replace(" ", "%20")

            temp += "<div class=\"row\">\n\t<h3><a href=\"$file\">${m.json.title}</a></h3>\n\t<h6>${SimpleDateFormat("MMM d, yyyy").format(Date(m.json.unix))}</h6>\n\t${m.json.description}\n</div>\n"
        }

        Main.saveFile(file, template.replace(blogs, temp))
        println("done compiling FrontPage")

    }

    class MdItem {
        var content:String = ""
        var json:JsonMD = JsonMD()
        var file:String = ""
    }
}