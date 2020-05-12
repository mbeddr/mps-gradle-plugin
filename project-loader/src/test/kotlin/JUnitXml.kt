import com.fasterxml.jackson.dataformat.xml.XmlMapper
import de.itemis.mps.gradle.junit.*
import org.junit.Assert
import org.junit.Test
import org.xmlunit.validation.Languages
import org.xmlunit.validation.Validator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.transform.stream.StreamSource


class JUnitXmlTest {

    private fun String.loadResource(): URL? {
        return this@JUnitXmlTest::class.java.getResource(this)
    }

    fun getCurrentTimeStamp(): String {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        return df.format(Date())
    }

    @Test
    fun `minimum test suite is valid`() {
        validateWithJunitSchema(Testsuite(
                name = "my tests",
                testcases = emptyList(),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut("")
        ))
    }

    @Test
    fun `everything test suite is valid`() {
        validateWithJunitSchema(Testsuite(
                name = "my tests",
                testcases = emptyList(),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
        ))
    }

    @Test
    fun `test suite with one simple test is valid`() {

        val test = Testcase(name = "my fancy test", classname = "my class name", time = 0)
        validateWithJunitSchema(Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
        ))
    }

    @Test
    fun `test suite with one complex test with error is valid`() {

        val test = Testcase(
                name = "my fancy test",
                classname = "my class name",
                time = 0,
                error = de.itemis.mps.gradle.junit.Error(message = "my message", type = "")
        )
        validateWithJunitSchema(Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
        ))
    }

    @Test
    fun `test suite with one complex test with error and content is valid`() {

        val test = Testcase(
                name = "my fancy test",
                classname = "my class name",
                time = 0,
                error = de.itemis.mps.gradle.junit.Error(message = "my message", type = "", content = "something")
        )
        validateWithJunitSchema(Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
        ))
    }

    @Test
    fun `test suite with one complex test with failure is valid`() {

        val test = Testcase(
                name = "my fancy test",
                classname = "my class name",
                time = 0,
                failure = de.itemis.mps.gradle.junit.Failure(message = "my message", type = "")
        )
        validateWithJunitSchema(Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
        ))
    }

    @Test
    fun `test suite with one complex test with failure and content is valid`() {

        val test = Testcase(
                name = "my fancy test",
                classname = "my class name",
                time = 0,
                failure = de.itemis.mps.gradle.junit.Failure(message = "my message", type = "", content = "something")
        )
        validateWithJunitSchema(Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
        ))
    }

    @Test
    fun `test suite with one complex test with skipped is valid`() {

        val test = Testcase(
                name = "my fancy test",
                classname = "my class name",
                time = 0,
                skipped = de.itemis.mps.gradle.junit.Skipped(content = "something")
        )
        validateWithJunitSchema(Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
        ))
    }

    @Test
    fun `test suits with one test suite is valid`() {
        val testsuite = Testsuite(
                name = "my tests",
                testcases = emptyList(),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                id = 0,
                pkg = "something"
        )
        validateWithJunitSchema(Testsuites(listOf(testsuite)))
    }

    @Test
    fun `test suits with test suites is valid`() {
        val testsuite = Testsuite(
                name = "my tests",
                testcases = emptyList(),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                id = 0,
                pkg = "something"
        )
        val testsuite2 = Testsuite(
                name = "my tests 2",
                testcases = emptyList(),
                tests = 34,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                id = 1,
                pkg = "something"
        )
        validateWithJunitSchema(Testsuites(listOf(testsuite, testsuite2)))
    }

    private fun validateWithJunitSchema(it: Any) {
        val v = Validator.forLanguage(Languages.W3C_XML_SCHEMA_NS_URI)
        v.setSchemaSource(StreamSource("junit.xsd".loadResource()!!.openStream(), "JUni.xsd"))
        val xmlMapper = XmlMapper()
        val outputStream = ByteArrayOutputStream()
        xmlMapper.writeValue(outputStream, it)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val result = v.validateInstance(StreamSource(inputStream))
        Assert.assertTrue(result.problems.joinToString { it.message }, result.isValid)
    }


}