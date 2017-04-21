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

    fun compile(args: Main.args, config: JsonConfig) {
        println("Compiling MainPage..")
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        //Load the template for the mainpage
        for(f in Main.getFiles(config.template, "json")) {
            val tmp = gson.fromJson(Main.readFile(f), JsonTemplate::class.java)
            if(tmp.template == "rss") {
                templatecfg = tmp
                cfgfile = f
            }
        }

        templatecfg.items.forEach {
            when(it.name) {
                "content" -> content = it.value
            }
        }

        template = Main.readFile(File(cfgfile).parentFile.path + "/" + templatecfg.file)

        if(template.hashCode() != templatecfg.hash || args.loadAll) {
            if(template.hashCode() != templatecfg.hash)println("Template has changed")

            templatecfg.hash = template.hashCode()
            Main.saveFile(cfgfile, gson.toJson(templatecfg))
        }

        val mditems:Array<MainPage.mditem> = Array(5, { MainPage.mditem() })

        for(f in Main.getFiles(config.markdown, "md")) {
            //parse the md file
            var cont: List<String> = Main.readFile(f).split("};")
            if (cont.size == 1) {
                cont = listOf("{\"title\": \"null\",\"date\":\"null\"", cont[0])
            }
            val mdcfg = gson.fromJson(cont[0] + "}", JsonMD::class.java)

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
                val md = MainPage.mditem()
                md.json = mdcfg
                md.file = f
                md.content = cont[1]
                mditems[element] = md
            }
        }

        var tmp = ""

        mditems.forEach {
            if(it.file == "")return@forEach


            val dir = File(it.file)
            val file = File(dir.parentFile.path.replace(config.markdown, config.blog) + "/" + dir.nameWithoutExtension + "/").toString().replace(" ", "%20")
            val text = it.content.substring(0, Math.min(300, it.content.length)) + "..."
            val date = SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z").format(Date(it.json.unix))

            //println(date)

            tmp += "<item>\n\t<title>${it.json.title}</title>\n\t<link>http://www.ttocsneb.com/$file</link>\n\t<description>${text}</description>\n\t<pubDate>$date</pubDate>\n\t<guid>http://www.ttocsneb.com/$file</guid>\n</item>"

        }
        template = template.replace(content, tmp)
        Main.saveFile(location, template)
        println(template)


    }
}