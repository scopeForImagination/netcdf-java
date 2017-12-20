package edu.ucar.build.ui

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.Diff
import spock.lang.Specification

/**
 * Tests ToolsUiJnlpExtensionTask.
 *
 * @author cwardgar
 * @since 2017-04-05
 */
class ToolsUiJnlpExtensionTaskSpec extends Specification {
    private static Project rootProject
    
    def setupSpec() {
        rootProject = new ProjectBuilder().withName('root').build()
        rootProject.with {
            version = '1.5'
            
            apply plugin: 'java'
            targetCompatibility = JavaVersion.VERSION_1_7
            
            repositories {
                jcenter()
            }
            dependencies {
                compile 'org.slf4j:slf4j-api:1.7.7'
                compile 'org.objenesis:objenesis:2.2'
                runtime 'org.hamcrest:hamcrest-core:1.3'
                testRuntime 'org.codehaus.groovy:groovy-all:2.4.5'
            }
        }
    }
    
    @Rule TemporaryFolder tempFolder
    
    def "just the writer"() {
        setup: "Identify control file for this test. It's located in src/test/resources/edu/ucar/build/ui/"
        String controlFileName = 'toolsUiJnlpExtension.jnlp'
    
        and: "Create a temp file that'll be deleted at the end."
        File tempFile = tempFolder.newFile()
    
        and: "create a writer with the specified properties"
        ToolsUiJnlpExtensionTask.Writer writer = new ToolsUiJnlpExtensionTask.Writer()
        writer.with {
            codebase = "https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/webstart"
            applicationVersion = rootProject.version
            applicationJarName = rootProject.jar.archiveName
            dependenciesConfig = rootProject.configurations.runtime
            outputFile = tempFile
        }
    
        and: "write JNLP to disk"
        writer.write()
    
        when: "compare expected XML (read from test resource) with just-written file, ignoring comments and whitespace"
        Diff diff = DiffBuilder.compare(Input.fromStream(getClass().getResourceAsStream(controlFileName)))
                               .withTest(Input.fromFile(tempFile))
                               .ignoreComments()
                               .normalizeWhitespace()
                               .build()

        then: "there will be no difference between the two"
        !diff.hasDifferences()
    }
    
    // This reads from a file generated by the java-gradle-plugin.
    // It is intended for use with GradleRunner.withPluginClasspath(), but that doesn't quite work for us because
    // we're not testing a plugin here; only the code in :buildSrc. So instead, we're going to feed those files
    // into the test build's buildscript classpath.
    List<File> buildSrcClasspath = PluginUnderTestMetadataReading.readImplementationClasspath()
    
    def "full Gradle build"() {
        setup: "variables"
        String taskName = 'toolsUiJnlpExtension'
        File outputFile = tempFolder.newFile()
        
        and: "declare initial content of build file"
        String buildFileContent = """
            buildscript {
                dependencies {
                    // Need this in order to resolve ToolsUiJnlpExtensionTask.
                    String buildSrcClasspathAsCsvString = '${buildSrcClasspath.join(',').replace('\\', '/')}'
                    classpath files(buildSrcClasspathAsCsvString.split(','))
                }
            }
            
            apply plugin: 'java'
            targetCompatibility = '1.7'
            
            repositories {
                jcenter()
            }
            dependencies {
                compile 'org.slf4j:slf4j-api:1.7.7'
                compile 'org.objenesis:objenesis:2.2'
                runtime 'org.hamcrest:hamcrest-core:1.3'
                testRuntime 'org.codehaus.groovy:groovy-all:2.4.5'
            }
            
            task $taskName(type: edu.ucar.build.ui.ToolsUiJnlpExtensionTask) {
                codebase = 'https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/webstart'
                outputFile = file('${outputFile.name}')
            }
        """
        
        and: "create a temporary build file containing that content"
        File buildFile = tempFolder.newFile('build.gradle')
        buildFile.text = buildFileContent
        
        and: "setup a GradleRunner that will execute '$taskName'"
        GradleRunner gradleRunner = GradleRunner.create().withProjectDir(tempFolder.root).withArguments(":$taskName")
        
        expect: "if we execute the task, it will succeed"
        gradleRunner.build().task(":$taskName")?.outcome == TaskOutcome.SUCCESS
        
        and: "if we re-execute the task without changing anything, it'll be UP-TO-DATE"
        gradleRunner.build().task(":$taskName")?.outcome == TaskOutcome.UP_TO_DATE
        
        when: "we update a dependency, which will change the runtime configuration Input that the task uses by default"
        buildFile.text = buildFileContent.replace("compile 'org.slf4j:slf4j-api:1.7.7'",
                                                  "compile 'org.slf4j:slf4j-api:1.7.8'")

        then: "test will not be UP-TO-DATE and will have to run again"
        gradleRunner.build().task(":$taskName")?.outcome == TaskOutcome.SUCCESS
    }
}
