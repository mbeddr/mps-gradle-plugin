package de.itemis.mps.gradle.junit

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.*

data class Skipped(@field:JacksonXmlText() val content: String)

/**
 * Indicates that the test errored.  An errored test is one that had an unanticipated problem. e.g., an unchecked throwable; or a problem with the implementation of the test. Contains as a text node relevant data for the error, e.g., a stack trace
 */
data class Error(
        /**
         * The error message. e.g., if a java exception is thrown, the return value of getMessage()
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val message: String,
        /**
         * The type of error that occured. e.g., if a java execption is thrown the full class name of the exception.
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val type: String,
        @field:JacksonXmlText() val content: String? = null)

/**
 * Indicates that the test failed. A failure is a test which the code has explicitly failed by using the mechanisms for that purpose. e.g., via an assertEquals. Contains as a text node relevant data for the failure, e.g., a stack trace
 */
data class Failure(
        /**
         * The message specified in the assert
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val message: String,
        /**
         * The type of the assert.
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val type: String,
        @field:JacksonXmlText() val content: String? = null)

data class SystemOut(@field:JacksonXmlText()
                     val content: String)

data class SystemErr(@field:JacksonXmlText()
                     val content: String)


data class Testcase(
        /**
         * Name of the test method
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String,
        /**
         * Full class name for the class the test method is in.
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val classname: String,
        /**
         * Time taken (in seconds) to execute the test
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val time: Int,
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "skipped")
        @field:JsonInclude(JsonInclude.Include.NON_NULL)
        val skipped: Skipped? = null,
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "error")
        @field:JsonInclude(JsonInclude.Include.NON_NULL)
        val error: Error? = null,
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "failure")
        @field:JsonInclude(JsonInclude.Include.NON_NULL)
        val failure: Failure? = null
)

@JacksonXmlRootElement(localName = "testsuite")
data class Testsuite(
        /**
         * Full class name of the test for non-aggregated testsuite documents. Class name without the package for aggregated testsuites documents
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String,
        /**
         * The total number of tests in the suite
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val tests: Int,
        /**
         * The total number of tests in the suite that errored. An errored test is one that had an unanticipated problem. e.g., an unchecked throwable; or a problem with the implementation of the test.
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val errors: Int = 0,
        /**
         * The total number of tests in the suite that failed. A failure is a test which the code has explicitly failed by using the mechanisms for that purpose. e.g., via an assertEquals
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val failures: Int = 0,
        /**
         * Only required if contained in a testsuites list
         * Starts at '0' for the first testsuite and is incremented by 1 for each following testsuite
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val id: Int? = null,
        /**
         * Derived from testsuite/@name in the non-aggregated documents
         */
        @field:JacksonXmlProperty(localName = "package", isAttribute = true)
        val pkg: String? = null,
        /**
         * The total number of ignored or skipped tests in the suite.
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val skipped: Int? = null,
        /**
         * Time taken (in seconds) to execute the tests in the suite
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val time: Int = 0,
        /**
         * when the test was executed. Timezone may not be specified.
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val timestamp: String,
        /**
         * Host on which the tests were executed. 'localhost' should be used if the hostname cannot be determined.
         */
        @field:JacksonXmlProperty(isAttribute = true)
        val hostname: String = "localhost",
        /**
         * Properties (e.g., environment settings) set during test execution
         */
        @field:JacksonXmlElementWrapper(useWrapping = true)
        @field:JacksonXmlProperty(localName = "properties")
        val properties: List<Property> = emptyList(),
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "testcase")
        val testcases: List<Testcase>,
        /**
         * Data that was written to standard out while the test was executed
         */
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "system-out")
        val systemOut: SystemOut = SystemOut(""),
        /**
         * Data that was written to standard error while the test was executed
         */
        @field:JacksonXmlElementWrapper(useWrapping = false)
        @field:JacksonXmlProperty(localName = "system-err")
        val systemError: SystemErr = SystemErr("")
)

data class Property(
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String,
        @field:JacksonXmlProperty(isAttribute = true)
        val value: String)

@JacksonXmlRootElement(localName = "testsuites")
data class Testsuites(@field:JacksonXmlElementWrapper(useWrapping = false, localName = "testsuite")
                      @field:JacksonXmlProperty(localName = "testsuite")
                      val testsuites: List<Testsuite>
)

