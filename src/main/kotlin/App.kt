import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import mu.KotlinLogging
import step.*
import util.*
import java.nio.file.*
import util.CmdRunner
private val log = KotlinLogging.logger {}

fun main(args: Array<String>) = Cli().main(args)

class Cli : CliktCommand() {
    private val taFile: Path by option("-ta", help = "path for TAGALIGN file.")
        .path().required()
    private val ctaFile: Path? by option("-cta", help = "path for control TAGALIGN file.")
            .path()
    private val outputPrefix: String by option("-outputPrefix", help = "output file name prefix; defaults to 'output'").default("output")
    private val fraglen: Int by option("-fraglen", help = "Fragment Length").int().required()
    private val chrsz:Path by option("-chrsz",help = "2-col chromosome sizes file.").path().required()
    private val gensz:String? by option("-gensz",help = "Genome size (sum of entries in 2nd column of \\\n" +
            "chr. sizes file, or hs for human, ms for mouse).").default("")
    private val pvalThresh: Double by option("-pvalthresh", help = "pvalue threshold").double().default(0.01)
    private val cap_num_peak: Int by option("-cap-num-peak", help = "Capping number of peaks by taking top N peaks.").int().default(500000)
    private val makeSignal: Boolean by option("-make-signal", help = "Generate signal tracks for P-Value and fold enrichment.").flag()
    private val blacklistFile: Path by option("-blacklist", help = "Blacklist BED file.")
            .path().required()
    private val pairedEnd: Boolean by option("-pairedEnd", help = "Paired-end BAM.").flag()
    private val outDir by option("-outputDir", help = "path to output Directory")
            .path().required()

    override fun run() {
        val cmdRunner = DefaultCmdRunner()
        cmdRunner.runTask(taFile, ctaFile,blacklistFile,fraglen, chrsz, gensz, pvalThresh, cap_num_peak, makeSignal,pairedEnd, outDir, outputPrefix)
    }
}

/**
 * Runs pre-processing and bwa for raw input files
 * @param bwaInputs bwa Input
 * @param outDir Output Path
 */
fun CmdRunner.runTask(taFile:Path, ctaFile:Path?,blackListFile:Path,fraglen:Int, chrsz:Path, gensz:String?, pvalThresh:Double,  cap_num_peak:Int, makeSignal:Boolean, pairedEnd: Boolean, outDir:Path, outputPrefix:String) {

    log.info { "Calling peaks and generating signal tracks with MACS2..." }
    var mo:macs2Output =  macs2(taFile, ctaFile, chrsz, gensz, pvalThresh, cap_num_peak, makeSignal,pairedEnd, outDir, outputPrefix)

    log.info { "Blacklist-filtering peaks..." }
    var bfil =   blacklist_filter(mo.npeak, blackListFile, outDir)

    log.info { "Converting peak to bigbed..." }
    var bb = peak_to_bigbed(bfil, "narrowPeak", chrsz, outDir)

    log.info { "Shifted FRiP with fragment length..." }
    val frip_qc =  frip_shifted(taFile,bfil, fraglen, chrsz, outDir)
}