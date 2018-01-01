package com.ttocsneb.webcompiler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ttocsneb.webcompiler.json.JsonConfig
import com.ttocsneb.webcompiler.json.JsonProject
import com.ttocsneb.webcompiler.json.JsonTemplate
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.ParserEmulationProfile
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.File

class ProjectPages {

    private var blog: String = ""
    private var title: String = ""
    private var description: String = ""
    private var keywords: String = ""
    private var author: String = ""

    private var template: String = ""
    private var templateCfg: JsonTemplate = JsonTemplate()

    private var parser: Parser? = null
    private var renderer: HtmlRenderer? = null



    /**
     * Compile all of the Projects
     *
     * @param args Program Arguments
     * @param config Global Configuration
     */
    fun compile(args: Main.Args, config: JsonConfig) {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        //Load the Project Template
        val templateFile: String = Main.getTemplate(config.template, "blog", gson)!!
        templateCfg = gson.fromJson(Main.readFile(templateFile), JsonTemplate::class.java)
        template = Main.readFile(File(templateFile).parentFile.path + "/" + templateCfg.file)
        template = Main.preCompile(config, templateCfg, gson, template)


        //Load the items in the config
        templateCfg.items.forEach {
            when(it.name) {
                "blog" -> blog = it.value
                "title" -> title = it.value
                "desc" -> description = it.value
                "keywords" -> keywords = it.value
                "author" -> author = it.value

            }
        }

        template = template.replace(author, "Benjamin Jacobs")


        val options: MutableDataHolder = MutableDataSet()
        options.setFrom(ParserEmulationProfile.MARKDOWN)
        parser = Parser.builder(options).build()
        renderer = HtmlRenderer.builder(options).build()


        //Go through each project and compile it

        Main.getFiles(config.projects, "md").forEach {
            compileProject(args, gson, it)
        }

    }

    private fun compileProject(args: Main.Args, gson: Gson, file: String) {
        var content: List<String> = Main.readFile(file).split("};")
        if(content.size == 1) {
            content = listOf("{\"title\": \"None\", \"icon\": \"None\", \"description\": \"None\", \"tags\": \"None\"", content[0])
        }
        val mdConfig = gson.fromJson(content[0] + "}", JsonProject::class.java)
        val hash = mdConfig.hash
        mdConfig.hash = 0
        content = listOf(gson.toJson(mdConfig), content[1])

        val folder = File(file).parentFile.path

        //Check if the hash has changed
        when {
            (hash != content.hashCode() || args.loadAll || args.clean) -> {
                mdConfig.hash = content.hashCode()
                Main.saveFile(file, gson.toJson(mdConfig) + ";" + content[1])
            }
            mdConfig.title == "None" -> {
                println("\tError: invalid json at: $file\n\t\tCreating JSON..\n\t\tSkipping..")
                return
            }
            else -> return
        }

        println("\tCompiling $file")

        val outputFile = File("$folder/index.html")

        val node = parser!!.parse(content[1])
        val post = "<h1>${mdConfig.title}</h1>\n${renderer!!.render(node)}"

        val text = template
                .replace(blog, post)
                .replace(title, mdConfig.title)
                .replace(description, mdConfig.description)
                .replace(keywords, mdConfig.tags)

        if(args.clean && outputFile.exists()) {
            Main.delete(outputFile)
        }

        Main.saveFile(outputFile.path, text)

    }

}