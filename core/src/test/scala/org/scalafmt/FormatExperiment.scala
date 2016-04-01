package org.scalafmt

import org.scalafmt.util.ExperimentResult
import org.scalafmt.util.ExperimentResult.Skipped
import org.scalafmt.util.ExperimentResult.Success
import org.scalafmt.util.ExperimentResult.Timeout
import org.scalafmt.util.FormatAssertions
import org.scalafmt.util.ScalaFile
import org.scalafmt.util.ScalaProjectsExperiment
import org.scalafmt.util.ScalacParser
import org.scalatest.FunSuite
import scala.collection.JavaConversions._
import scala.meta._

trait FormatExperiment extends ScalaProjectsExperiment with FormatAssertions {
  override val verbose = false

  val okRepos = Set(
      "goose",
      "scala-js",
      "fastparse",
      "scalding",
      "I wan't trailing commas!!!"
  )
  val badRepos = Set(
      "kafka"
  )

  def okScalaFile(scalaFile: ScalaFile): Boolean = {
    okRepos(scalaFile.repo) && !badFile(scalaFile.filename)
  }

  def badFile(filename: String): Boolean =
    Seq(
        // Auto generated files
        "scalding-core/src/main/scala/com/twitter/scalding/macros/impl/TypeDescriptorProviderImpl.scala",
        "scalding/serialization/macros/impl/ordered_serialization/providers/ProductOrderedBuf.scala",
        "scalding-core/src/main/scala/com/twitter/scalding/typed/GeneratedFlattenGroup.scala",
        "emitter/JSDesugaring.scala",
        "js/ThisFunction.scala",
        "js/Any.scala"
    ).exists(filename.contains)

  override def runOn(scalaFile: ScalaFile): ExperimentResult = {
    val code = scalaFile.read
    if (!ScalacParser.checkParseFails(code)) {
      val startTime = System.nanoTime()
      val formatted =
        ScalaFmt.format_![Source](code, ScalaStyle.NoIndentStripMargin)
      assertFormatPreservesAst[Source](code, formatted)
      print("+")
      Success(scalaFile, System.nanoTime() - startTime)
    } else {
      Skipped(scalaFile)
    }
  }

  def scalaFiles = ScalaFile.getAll.filter(okScalaFile)
}

// TODO(olafur) integration test?

class FormatExperimentTest extends FunSuite with FormatExperiment {

  def validate(result: ExperimentResult): Unit = result match {
    case _: Success | _: Timeout | _: Skipped =>
    case failure => fail(s"""Unexpected failure:
                            |$failure""".stripMargin)
  }

  test(s"scalafmt formats a bunch of OSS projects") {
    runExperiment(scalaFiles)
    results.toIterable.foreach(validate)
    printResults()
  }
}

object FormatExperimentApp extends FormatExperiment with App {
  runExperiment(scalaFiles)
  printResults()
}