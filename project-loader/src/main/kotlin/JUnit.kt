package de.itemis.mps.gradle.junit

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.*

data class Skipped(@JacksonXmlProperty(isAttribute = true)
                   val message: String,
                   @field:JacksonXmlText() val content: String)

data class Error(@field:JacksonXmlProperty(isAttribute = true)
                 val message: String,
                 @field:JacksonXmlProperty(isAttribute = true)
                 val type: String? = null,
                 @field:JacksonXmlText() val content: String)


data class Failure(@field:JacksonXmlProperty(isAttribute = true)
                   val message: String,
                   @field:JacksonXmlProperty(isAttribute = true)
                   val type: String? = null,
                   @field:JacksonXmlText() val content: String)

data class SystemOut(@field:JacksonXmlText()
                     val content: String)

data class SystemErr(@field:JacksonXmlText()
                     val content: String)


data class Testcase(
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String,
        @field:JacksonXmlProperty(isAttribute = true)
        val classname: String,
        @field:JacksonXmlProperty(isAttribute = true)
        val assertions: Int? = null,
        @field:JacksonXmlProperty(isAttribute = true)
        val status: String? = null,
        @field:JacksonXmlProperty(isAttribute = true)
        val time: String? = null,
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "skipped")
        @field:JsonInclude(JsonInclude.Include.NON_NULL)
        val skipped: Skipped? = null,
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "error")
        val errors: List<Error> = emptyList(),
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "failure")
        val failures: List<Failure> = emptyList(),
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "system-out")
        val systemOuts: List<SystemOut> = emptyList(),
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "system-err")
        val systemErrors: List<SystemErr> = emptyList()
        )


data class Testsuite(@field:JacksonXmlProperty(isAttribute = true)
                     val name: String,
                     @field:JacksonXmlProperty(isAttribute = true)
                     val tests: Int,
                     @field:JacksonXmlProperty(isAttribute = true)
                     val disabled: Int? = null,
                     @field:JacksonXmlProperty(isAttribute = true)
                     val errors: Int? = null,
                     @field:JacksonXmlProperty(isAttribute = true)
                     val failures: Int? = null,
                     @field:JacksonXmlProperty(isAttribute = true)
                     val id: Int,
                     @field:JacksonXmlProperty(localName = "package", isAttribute = true)
                     val pkg: String? = null,
                     @field:JacksonXmlProperty(isAttribute = true)
                     val skipped: Int? = null,
                     @field:JacksonXmlProperty(isAttribute = true)
                     val time: Int? = null,
                     @field:JacksonXmlProperty(isAttribute = true)
                     val timestamp: String? = null,
                     @field:JacksonXmlElementWrapper(useWrapping = false)
                     @field:JacksonXmlProperty(localName = "testcase")
                     val testcases: List<Testcase>
)

@JacksonXmlRootElement(localName = "testsuites")
data class Testsuites(@field:JacksonXmlProperty(isAttribute = true)
                      val disabled: Int = 0,
                      @field:JacksonXmlProperty(isAttribute = true)
                      val errors: Int = 0,
                      @field:JacksonXmlProperty(isAttribute = true)
                      val failures: Int = 0,
                      @field:JacksonXmlProperty(isAttribute = true)
                      val name: String,
                      @field:JacksonXmlProperty(isAttribute = true)
                      val tests: Int,
                      @field:JacksonXmlElementWrapper(useWrapping = false,localName = "testsuite")
                      @field:JacksonXmlProperty(localName = "testsuite")
                      val testsuites: List<Testsuite>
)

