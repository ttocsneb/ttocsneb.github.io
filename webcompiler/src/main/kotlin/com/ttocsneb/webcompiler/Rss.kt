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
 * Rss is the compiler for the site's rss feed
 */
class Rss {

    val location:String = "rss.xml"

    var templatecfg:JsonTemplate = JsonTemplate()
    var cfgfile:String = ""
    var template:String = ""
    var content:String = ""

    /**
     * Compile the rss file
     *
     * @param args Program arguments
     * @param config Global Configuration
     */
    fun compile(args: Main.Args, config: JsonConfig) {
        println("Compiling RSS..")
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        //Load the template for the mainpage
        cfgfile = Main.getTemplate(config.template, "rss", gson)!!
        templatecfg = gson.fromJson(Main.readFile(cfgfile), JsonTemplate::class.java)
        template = Main.readFile(File(cfgfile).parentFile.path + "/" + templatecfg.file)


        //Load template tags
        templatecfg.items.forEach {
            when(it.name) {
                "content" -> content = it.value
            }
        }


        //Check if the template has changed
        if(template.hashCode() != templatecfg.hash || args.loadAll) {
            if(template.hashCode() != templatecfg.hash)println("Template has changed")

            templatecfg.hash = template.hashCode()
            Main.saveFile(cfgfile, gson.toJson(templatecfg))
        }

        //Create an array for 5 items
        val mditems:Array<MainPage.MdItem> = Array(4, { MainPage.MdItem() })

        //Order the posts from the newest 5 posts
        for(f in Main.getFiles(config.markdown, "md")) {
            //parse the md file
            var cont: List<String> = Main.readFile(f).split("};")
            if (cont.size == 1) {
                cont = listOf("{\"title\": \"null\",\"date\":\"null\"", cont[0])
            }
            val mdcfg = gson.fromJson(cont[0] + "}", JsonMD::class.java)

            //Get the order in relation to the other ordered posts
            var i = mditems.size - 1
            var element = -1
            while (i >= 0) {
                if (mdcfg.unix > mditems[i].json.unix)
                    element = i
                i--
            }
            //If the post is in the top 5, put it in the array
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

        var tmp = ""

        //Compile the posts into the rss xml file
        mditems.forEach {
            if(it.file == "")return@forEach


            val dir = File(it.file)
            val file = File(dir.parentFile.path.replace(config.markdown, config.blog) + "/" + dir.nameWithoutExtension + "/").toString().replace(" ", "%20")
            //val text = it.content.substring(0, Math.min(300, it.content.length)) + "..."
            val date = SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z").format(Date(it.json.unix))

            tmp += "<item>\n\t<title>${it.json.title}</title>\n\t<link>http://www.ttocsneb.com/$file</link>\n\t<description>${it.json.description}</description>\n\t<pubDate>$date</pubDate>\n\t<guid>http://www.ttocsneb.com/$file</guid>\n</item>"

        }
        template = template.replace(content, tmp)
        Main.saveFile(location, template)
    }
}