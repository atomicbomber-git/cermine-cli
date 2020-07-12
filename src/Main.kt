import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.*
import org.apache.commons.io.FileUtils
import org.jdom.Element
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import pl.edu.icm.cermine.ContentExtractor
import java.io.File
import java.io.FileInputStream
import java.lang.Exception

class Main : CliktCommand() {
    private val inputPath: String by argument()
    private val outputDir: String? by option(help = "Output directory path.")
    private val maxConcurrent: Int by option(help = "Number of maximum parallel processes to run.").int().default(10)

    lateinit var outputDirFile: File
    private lateinit var inputPathFile: File

    override fun run() {
        setUpInputPath()
        setUpOutputDir()
        process()
    }

    private fun setUpInputPath() {
        this.inputPathFile = File(inputPath)

        if (!inputPathFile.exists()) {
            println("Input directory / file %s doesn't exist.".format(inputPathFile.absolutePath));
            throw Abort()
        }
    }

    private fun setUpOutputDir() {
        outputDir?.let {
            outputDirFile = File(it)

            if (outputDirFile.exists() && !outputDirFile.isDirectory) {
                echo("'%s' exists and it isn't a directory.".format(outputDirFile.absolutePath), err = true)
                throw Abort();
            }

            if (!outputDirFile.exists()) {
                if (!outputDirFile.mkdirs()) {
                    echo("Failed to create a directory: %s.".format(outputDirFile.absolutePath), err = true);
                    throw Abort();
                }

                echo("Successfully created a directory: %s.".format(outputDirFile.absolutePath))
            }
        }
    }

    private fun process() {
        runBlocking {
            echo("Starting everything now.")
            val time = System.currentTimeMillis()

            inputPathFile
                .walkTopDown()
                .toCollection(ArrayList())
                .filter { file -> file.extension == "pdf" }
                .chunked(maxConcurrent)
                .map { fileList ->
                    fileList.map { file ->
                        launch(Dispatchers.IO) {
                            processPdfFile(file)
                        }
                    }.joinAll()
                }

            echo("Finished everything. It took (${(System.currentTimeMillis() - time) / 1000.0}) seconds.")
            echo("The output directory is in ${outputDirFile.absolutePath}");
        }
    }

    private fun processPdfFile(file: File) {
        echo("Processing ${file.name}.")

        try {
            val outputData = ContentExtractor().run {
                setPDF(FileInputStream(file))
                this.labelledFullText
            }

            FileUtils.writeStringToFile(
                outputDirFile.resolve(file.nameWithoutExtension + ".zones"),
                (XMLOutputter(Format.getPrettyFormat())).outputString(outputData),
                "UTF-8"
            )
        } catch (ex: Exception) {
            echo("Failed to process ${file.name}.", err = true)
        } finally {
            echo("Finished processing ${file.name}.")
        }
    }

    private fun processFile(file: File): Element {
        val contentExtractor = ContentExtractor()
        contentExtractor.setPDF(FileInputStream(file))

        return contentExtractor.labelledFullText
    }
}

fun main(args: Array<String>) = Main().main(args)
