package com.ttocsneb.webcompiler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ttocsneb.webcompiler.json.JsonConfig
import com.ttocsneb.webcompiler.json.JsonProject
import com.ttocsneb.webcompiler.json.JsonTemplate
import java.io.File

/**
 * Compiler for the project Page
 */
class Projects {

    private var projects: String = ""
    private var carousel: String = ""
    private var carouselInd: String = ""
    private var file: String = ""


    /**
     * Compile the Project Page
     *
     * @param args Program Arguments
     * @param config Global Configuration
     */
    fun compile(args: Main.Args, config: JsonConfig) {
        println("Compileing Project Page")

        val gson: Gson = GsonBuilder().setPrettyPrinting().create()


        //Load the Project Page Template
        val templateFile: String = Main.getTemplate(config.template, "projects", gson)!!
        val templateCfg: JsonTemplate = gson.fromJson(Main.readFile(templateFile), JsonTemplate::class.java)
        var template: String = Main.readFile(File(templateFile).parentFile.path + "/" + templateCfg.file)


        //Load the items in the config
        templateCfg.items.forEach {
            when(it.name) {
                "project" -> projects = it.value
                "carousel" -> carousel = it.value
                "carouselInd" -> carouselInd = it.value
                "file" -> file = it.value
            }
        }


        //We will always recompile the Projects Page, so we don't worry about the changes made to it


        //Compile the sidebar
        template = Main.preCompile(config, templateCfg, gson, template)

        var projectHtml = ""

        var i = 0
        Main.getFiles(config.projects, "md").forEach {

            //Load the json
            var content: List<String> = Main.readFile(it).split("};")
            if(content.size == 1) {
                content = listOf("{\"title\": \"None\", \"icon\": \"None\", \"description\": \"None\", \"tags\": \"None\"", content[0])
            }
            val mdConfig = gson.fromJson(content[0] + "}", JsonProject::class.java)

            val file: String = File(it).parentFile.path

            if(i % 3 == 0) {
                projectHtml += "<div class=\"row\">\n"
            }
            projectHtml += "\t<div class=\"col-sm-4\">\n\t\t<div class=\"project-item\">\n\t\t\t<a href=\"/${file.replace("\\", "/")}/\">\n\t\t\t\t<img src=\"/${(file + "/" + mdConfig.icon).replace("\\", "/")}\">\n\t\t\t\t<p>${mdConfig.title}</p>\n\t\t\t</a>\n\t\t</div>\n\t</div>\n"

            if(i % 3 == 2) {
                projectHtml += "</div>\n"
            }
            i++
        }
        if(i % 3 != 2) {
            projectHtml += "</div>\n"
        }

        if(args.clean) {
            Main.delete(File(file))
        }

        template = template.replace(projects, projectHtml)

        Main.saveFile(file, template)
    }
}