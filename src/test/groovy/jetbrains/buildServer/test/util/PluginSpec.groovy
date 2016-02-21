package jetbrains.buildServer.test.util

import spock.lang.Specification
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import static org.gradle.testkit.runner.TaskOutcome.*

class PluginSpec extends Specification {
    List<File> pluginClasspath
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }
        pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }

        buildFile = testProjectDir.newFile('build.gradle')
    }

    def "Report a service message"() {
        given:
        buildFile << """
            plugins {
                id 'jetbrains.buildServer.test.util.log-processor'
            }
            processLogfile {
                file '1.log'
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('processLogfile')
            .withPluginClasspath(pluginClasspath)
            .build()

        then:
        result.output.contains("##teamcity[buildProblem description='Error message in 1.log (line 1): 111' identity='1508414']")
        result.task(":processLogfile").outcome == SUCCESS
    }

    def "Show error message on missing file"() {
        given:
        buildFile << """
            plugins {
                id 'jetbrains.buildServer.test.util.log-processor'
            }
            processLogfile {
                file 'error.log'
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('processLogfile')
            .withPluginClasspath(pluginClasspath)
            .build()

        then:
        result.output.contains("File 'error.log' does not exist")
        result.task(":processLogfile").outcome == SUCCESS
    }
}