/*
 * Copyright 2021 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbtghactions

import sbt._
import sbt.Keys._


import java.io.{BufferedWriter, FileWriter}
import java.nio.file.FileSystems
import scala.io.Source

object GenerativePlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends GenerativeKeys {
    type WorkflowJob = sbtghactions.WorkflowJob
    val WorkflowJob = sbtghactions.WorkflowJob

    type JobContainer = sbtghactions.JobContainer
    val JobContainer = sbtghactions.JobContainer

    type WorkflowStep = sbtghactions.WorkflowStep
    val WorkflowStep = sbtghactions.WorkflowStep

    type RefPredicate = sbtghactions.RefPredicate
    val RefPredicate = sbtghactions.RefPredicate

    type Ref = sbtghactions.Ref
    val Ref = sbtghactions.Ref

    type UseRef = sbtghactions.UseRef
    val UseRef = sbtghactions.UseRef

    type PREventType = sbtghactions.PREventType
    val PREventType = sbtghactions.PREventType

    type MatrixInclude = sbtghactions.MatrixInclude
    val MatrixInclude = sbtghactions.MatrixInclude

    type MatrixExclude = sbtghactions.MatrixExclude
    val MatrixExclude = sbtghactions.MatrixExclude
  }

  import autoImport._

  private def indent(output: String, level: Int): String = {
    val space = (0 until level * 2).map(_ => ' ').mkString
    (space + output.replace("\n", s"\n$space")).replaceAll("""\n[ ]+\n""", "\n\n")
  }

  private def isSafeString(str: String): Boolean =
    !(str.indexOf(':') >= 0 ||    // pretend colon is illegal everywhere for simplicity
      str.indexOf('#') >= 0 ||    // same for comment
      str.indexOf('!') == 0 ||
      str.indexOf('*') == 0 ||
      str.indexOf('-') == 0 ||
      str.indexOf('?') == 0 ||
      str.indexOf('{') == 0 ||
      str.indexOf('}') == 0 ||
      str.indexOf('[') == 0 ||
      str.indexOf(']') == 0 ||
      str.indexOf(',') == 0 ||
      str.indexOf('|') == 0 ||
      str.indexOf('>') == 0 ||
      str.indexOf('@') == 0 ||
      str.indexOf('`') == 0 ||
      str.indexOf('"') == 0 ||
      str.indexOf('\'') == 0 ||
      str.indexOf('&') == 0)

  private def wrap(str: String): String =
    if (str.indexOf('\n') >= 0)
      "|\n" + indent(str, 1)
    else if (isSafeString(str))
      str
    else
      s"'${str.replace("'", "''")}'"

  def compileList(items: List[String], level: Int): String = {
    val rendered = items.map(wrap)
    if (rendered.map(_.length).sum < 40)   // just arbitrarily...
      rendered.mkString(" [", ", ", "]")
    else
      "\n" + indent(rendered.map("- " + _).mkString("\n"), level)
  }

  def compileListOfSimpleDicts(items: List[Map[String, String]]): String =
    items map { dict =>
      val rendered = dict map {
        case (key, value) => s"$key: $value"
      } mkString "\n"

      "-" + indent(rendered, 1).substring(1)
    } mkString "\n"

  def compilePREventType(tpe: PREventType): String = {
    import PREventType._

    tpe match {
      case Assigned => "assigned"
      case Unassigned => "unassigned"
      case Labeled => "labeled"
      case Unlabeled => "unlabeled"
      case Opened => "opened"
      case Edited => "edited"
      case Closed => "closed"
      case Reopened => "reopened"
      case Synchronize => "synchronize"
      case ReadyForReview => "ready_for_review"
      case Locked => "locked"
      case Unlocked => "unlocked"
      case ReviewRequested => "review_requested"
      case ReviewRequestRemoved => "review_request_removed"
    }
  }

  def compileRef(ref: Ref): String = ref match {
    case Ref.Branch(name) => s"refs/heads/$name"
    case Ref.Tag(name) => s"refs/tags/$name"
  }

  def compileBranchPredicate(target: String, pred: RefPredicate): String = pred match {
    case RefPredicate.Equals(ref) =>
      s"$target == '${compileRef(ref)}'"

    case RefPredicate.Contains(Ref.Tag(name)) =>
      s"(startsWith($target, 'refs/tags/') && contains($target, '$name'))"

    case RefPredicate.Contains(Ref.Branch(name)) =>
      s"(startsWith($target, 'refs/heads/') && contains($target, '$name'))"

    case RefPredicate.StartsWith(ref) =>
      s"startsWith($target, '${compileRef(ref)}')"

    case RefPredicate.EndsWith(Ref.Tag(name)) =>
      s"(startsWith($target, 'refs/tags/') && endsWith($target, '$name'))"

    case RefPredicate.EndsWith(Ref.Branch(name)) =>
      s"(startsWith($target, 'refs/heads/') && endsWith($target, '$name'))"
  }

  def compileEnvironment(environment: JobEnvironment): String =
    environment.url match {
      case Some(url) =>
        val fields = s"""name: ${wrap(environment.name)}
           |url: ${wrap(url.toString)}""".stripMargin
        s"""environment:
           |${indent(fields, 1)}""".stripMargin
      case None =>
        s"environment: ${wrap(environment.name)}"
    }

  def compileEnv(env: Map[String, String], prefix: String = "env"): String =
    if (env.isEmpty) {
      ""
    } else {
      val rendered = env map {
        case (key, value) =>
          if (!isSafeString(key) || key.indexOf(' ') >= 0)
            sys.error(s"'$key' is not a valid environment variable name")

          s"""$key: ${wrap(value)}"""
      }
s"""$prefix:
${indent(rendered.mkString("\n"), 1)}"""
    }

  def compileStep(step: WorkflowStep, sbt: String, declareShell: Boolean = false): String = {
    import WorkflowStep._

    val renderedName = step.name.map(wrap).map("name: " + _ + "\n").getOrElse("")
    val renderedId = step.id.map(wrap).map("id: " + _ + "\n").getOrElse("")
    val renderedCond = step.cond.map(wrap).map("if: " + _ + "\n").getOrElse("")
    val renderedShell = if (declareShell) "shell: bash\n" else ""

    val renderedEnvPre = compileEnv(step.env)
    val renderedEnv = if (renderedEnvPre.isEmpty)
      ""
    else
      renderedEnvPre + "\n"

    val preamblePre = renderedName + renderedId + renderedCond + renderedEnv

    val preamble = if (preamblePre.isEmpty)
      ""
    else
      preamblePre

    val body = step match {
      case Run(commands, _, _, _, _) =>
        renderedShell + "run: " + wrap(commands.mkString("\n"))

      case Sbt(commands, _, _, _, _) =>
        val version = "++${{ matrix.scala }}"
        val sbtClientMode = sbt.matches("""sbt.* --client($| .*)""")
        val safeCommands = if (sbtClientMode)
          s"'${(version :: commands).mkString("; ")}'"
        else commands map { c =>
          if (c.indexOf(' ') >= 0)
            s"'$c'"
          else
            c
        } mkString(version + " ", " ", "")

        renderedShell + "run: " + wrap(s"$sbt $safeCommands")

      case Use(ref, params, _, _, _, _) =>
        val renderedParamsPre = compileEnv(params, prefix = "with")
        val renderedParams = if (renderedParamsPre.isEmpty)
          ""
        else
          "\n" + renderedParamsPre

        val decl = ref match {
          case UseRef.Public(owner, repo, ref) =>
            s"uses: $owner/$repo@$ref"

          case UseRef.Local(path) =>
            val cleaned = if (path.startsWith("./"))
              path
            else
              "./" + path

            s"uses: $cleaned"

          case UseRef.Docker(image, tag, Some(host)) =>
            s"uses: docker://$host/$image:$tag"

          case UseRef.Docker(image, tag, None) =>
            s"uses: docker://$image:$tag"
        }

        decl + renderedParams
    }

    indent(preamble + body, 1).updated(0, '-')
  }

  def compileJob(job: WorkflowJob, sbt: String): String = {
    val renderedNeeds = if (job.needs.isEmpty)
      ""
    else
      s"\nneeds: [${job.needs.mkString(", ")}]"

    val renderedEnvironment = job.environment.map(compileEnvironment).map("\n" + _).getOrElse("")

    val renderedCond = job.cond.map(wrap).map("\nif: " + _).getOrElse("")

    val renderedContainer = job.container match {
      case Some(JobContainer(image, credentials, env, volumes, ports, options)) =>
        if (credentials.isEmpty && env.isEmpty && volumes.isEmpty && ports.isEmpty && options.isEmpty) {
          "\n" + s"container: ${wrap(image)}"
        } else {
          val renderedImage = s"image: ${wrap(image)}"

          val renderedCredentials = credentials match {
            case Some((username, password)) =>
              s"\ncredentials:\n${indent(s"username: ${wrap(username)}\npassword: ${wrap(password)}", 1)}"

            case None =>
              ""
          }

          val renderedEnv = if (!env.isEmpty)
            "\n" + compileEnv(env)
          else
            ""

          val renderedVolumes = if (!volumes.isEmpty)
            s"\nvolumes:${compileList(volumes.toList map { case (l, r) => s"$l:$r" }, 1)}"
          else
            ""

          val renderedPorts = if (!ports.isEmpty)
            s"\nports:${compileList(ports.map(_.toString), 1)}"
          else
            ""

          val renderedOptions = if (!options.isEmpty)
            s"\noptions: ${wrap(options.mkString(" "))}"
          else
            ""

          s"\ncontainer:\n${indent(renderedImage + renderedCredentials + renderedEnv + renderedVolumes + renderedPorts + renderedOptions, 1)}"
        }

      case None =>
        ""
    }

    val renderedEnvPre = compileEnv(job.env)
    val renderedEnv = if (renderedEnvPre.isEmpty)
      ""
    else
      "\n" + renderedEnvPre

    List("include", "exclude") foreach { key =>
      if (job.matrixAdds.contains(key)) {
        sys.error(s"key `$key` is reserved and cannot be used in an Actions matrix definition")
      }
    }

    val renderedMatricesPre = job.matrixAdds map {
      case (key, values) => s"$key: ${values.map(wrap).mkString("[", ", ", "]")}"
    } mkString "\n"

    // TODO refactor all of this stuff to use whitelist instead
    val whitelist = Map("os" -> job.oses, "scala" -> job.scalas, "java" -> job.javas) ++ job.matrixAdds

    def checkMatching(matching: Map[String, String]): Unit = {
      matching foreach {
        case (key, value) =>
          if (!whitelist.contains(key)) {
            sys.error(s"inclusion key `$key` was not found in matrix")
          }

          if (!whitelist(key).contains(value)) {
            sys.error(s"inclusion key `$key` was present in matrix, but value `$value` was not in ${whitelist(key)}")
          }
      }
    }

    val renderedIncludesPre = if (job.matrixIncs.isEmpty) {
      renderedMatricesPre
    } else {
      job.matrixIncs.foreach(inc => checkMatching(inc.matching))

      val rendered = compileListOfSimpleDicts(job.matrixIncs.map(i => i.matching ++ i.additions))

      val renderedMatrices = if (renderedMatricesPre.isEmpty)
        ""
      else
        renderedMatricesPre + "\n"

      s"${renderedMatrices}include:\n${indent(rendered, 1)}"
    }

    val renderedExcludesPre = if (job.matrixExcs.isEmpty) {
      renderedIncludesPre
    } else {
      job.matrixExcs.foreach(exc => checkMatching(exc.matching))

      val rendered = compileListOfSimpleDicts(job.matrixExcs.map(_.matching))

      val renderedIncludes = if (renderedIncludesPre.isEmpty)
        ""
      else
        renderedIncludesPre + "\n"

      s"${renderedIncludes}exclude:\n${indent(rendered, 1)}"
    }

    val renderedMatrices = if (renderedExcludesPre.isEmpty)
      ""
    else
      "\n" + indent(renderedExcludesPre, 2)

    val declareShell = job.oses.exists(_.contains("windows"))

    val runsOn = if (job.runsOnExtraLabels.isEmpty)
      s"$${{ matrix.os }}"
    else
      job.runsOnExtraLabels.mkString(s"""[ "$${{ matrix.os }}", """, ", ", " ]" )

    val renderedFailFast = job.matrixFailFast.fold("")("\n  fail-fast: " + _)

    val body = s"""name: ${wrap(job.name)}${renderedNeeds}${renderedCond}
strategy:${renderedFailFast}
  matrix:
    os:${compileList(job.oses, 3)}
    scala:${compileList(job.scalas, 3)}
    java:${compileList(job.javas, 3)}${renderedMatrices}
runs-on: ${runsOn}${renderedEnvironment}${renderedContainer}${renderedEnv}
steps:
${indent(job.steps.map(compileStep(_, sbt, declareShell = declareShell)).mkString("\n\n"), 1)}"""

    s"${job.id}:\n${indent(body, 1)}"
  }

  def compileWorkflow(
      name: String,
      branches: List[String],
      tags: List[String],
      prEventTypes: List[PREventType],
      env: Map[String, String],
      jobs: List[WorkflowJob],
      sbt: String)
      : String = {

    val renderedEnvPre = compileEnv(env)
    val renderedEnv = if (renderedEnvPre.isEmpty)
      ""
    else
      renderedEnvPre + "\n\n"

    val renderedTypesPre = prEventTypes.map(compilePREventType).mkString("[", ", ", "]")
    val renderedTypes = if (prEventTypes.sortBy(_.toString) == PREventType.Defaults)
      ""
    else
      "\n" + indent("types: " + renderedTypesPre, 2)

    val renderedTags = if (tags.isEmpty)
      ""
    else
s"""
    tags: [${tags.map(wrap).mkString(", ")}]"""

    s"""# This file was automatically generated by sbt-github-actions using the
# githubWorkflowGenerate task. You should add and commit this file to
# your git repository. It goes without saying that you shouldn't edit
# this file by hand! Instead, if you wish to make changes, you should
# change your sbt build configuration to revise the workflow description
# to meet your needs, then regenerate this file.

name: ${wrap(name)}

on:
  pull_request:
    branches: [${branches.map(wrap).mkString(", ")}]$renderedTypes
  push:
    branches: [${branches.map(wrap).mkString(", ")}]$renderedTags

${renderedEnv}jobs:
${indent(jobs.map(compileJob(_, sbt)).mkString("\n\n"), 1)}"""
}

  val settingDefaults = Seq(
    githubWorkflowSbtCommand := "sbt",
    // This is currently set to false because of https://github.com/sbt/sbt/issues/6468. When a new SBT version is
    // released that fixes this issue then check for that SBT version (or higher) and set to true.
    githubWorkflowUseSbtThinClient := false,

    githubWorkflowBuildMatrixFailFast := None,
    githubWorkflowBuildMatrixAdditions := Map(),
    githubWorkflowBuildMatrixInclusions := Seq(),
    githubWorkflowBuildMatrixExclusions := Seq(),
    githubWorkflowBuildRunsOnExtraLabels := Seq(),

    githubWorkflowBuildPreamble := Seq(),
    githubWorkflowBuildPostamble := Seq(),
    githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test"), name = Some("Build project"))),

    githubWorkflowPublishPreamble := Seq(),
    githubWorkflowPublishPostamble := Seq(),
    githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("+publish"), name = Some("Publish project"))),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.Equals(Ref.Branch("main"))),
    githubWorkflowPublishCond := None,

    githubWorkflowJavaVersions := Seq("adopt@1.8"),
    githubWorkflowScalaVersions := crossScalaVersions.value,
    githubWorkflowOSes := Seq("ubuntu-latest"),
    githubWorkflowDependencyPatterns := Seq("**/*.sbt", "project/build.properties"),
    githubWorkflowTargetBranches := Seq("**"),
    githubWorkflowTargetTags := Seq(),

    githubWorkflowEnv := Map("GITHUB_TOKEN" -> s"$${{ secrets.GITHUB_TOKEN }}"),
    githubWorkflowAddedJobs := Seq())

  private lazy val internalTargetAggregation = settingKey[Seq[File]]("Aggregates target directories from all subprojects")

  private val windowsGuard = Some("contains(runner.os, 'windows')")

  private val PlatformSep = FileSystems.getDefault.getSeparator
  private def normalizeSeparators(pathStr: String): String = {
    pathStr.replace(PlatformSep, "/")   // *force* unix separators
  }

  private val pathStrs = Def setting {
    val base = (ThisBuild / baseDirectory).value.toPath

    internalTargetAggregation.value map { file =>
      val path = file.toPath

      if (path.isAbsolute)
        normalizeSeparators(base.relativize(path).toString)
      else
        normalizeSeparators(path.toString)
    }
  }

  override def globalSettings = Seq(
    internalTargetAggregation := Seq(),
    githubWorkflowArtifactUpload := true)

  override def buildSettings = settingDefaults ++ Seq(
    githubWorkflowPREventTypes := PREventType.Defaults,

    githubWorkflowGeneratedUploadSteps := {
      if (githubWorkflowArtifactUpload.value) {
        val sanitized = pathStrs.value map { str =>
          if (str.indexOf(' ') >= 0)    // TODO be less naive
            s"'$str'"
          else
            str
        }

        val tar = WorkflowStep.Run(
          List(s"tar cf targets.tar ${sanitized.mkString(" ")} project/target"),
          name = Some("Compress target directories"))

        val upload = WorkflowStep.Use(
          UseRef.Public(
            "actions",
            "upload-artifact",
            "v2"),
          name = Some(s"Upload target directories"),
          params = Map(
            "name" -> s"target-$${{ matrix.os }}-$${{ matrix.scala }}-$${{ matrix.java }}",
            "path" -> "targets.tar"))

        Seq(tar, upload)
      } else {
        Seq()
      }
    },

    githubWorkflowGeneratedDownloadSteps := {
      val scalas = githubWorkflowScalaVersions.value

      if (githubWorkflowArtifactUpload.value) {
        scalas flatMap { v =>
          val download = WorkflowStep.Use(
            UseRef.Public(
              "actions",
              "download-artifact",
              "v2"),
            name = Some(s"Download target directories ($v)"),
            params = Map(
              "name" -> s"target-$${{ matrix.os }}-$v-$${{ matrix.java }}"))

          val untar = WorkflowStep.Run(
            List(
              "tar xf targets.tar",
              "rm targets.tar"),
            name = Some(s"Inflate target directories ($v)"))

          Seq(download, untar)
        }
      } else {
        Seq()
      }
    },

    githubWorkflowGeneratedCacheSteps := {
      val hashes = githubWorkflowDependencyPatterns.value map { glob =>
        s"$${{ hashFiles('$glob') }}"
      }

      Seq(
        WorkflowStep.Use(
          UseRef.Public(
            "actions",
            "cache",
            "v2"),
          name = Some("Cache sbt"),
          params = Map(
            "path" -> Seq(
              "~/.sbt",
              "~/.ivy2/cache",
              "~/.coursier/cache/v1",
              "~/.cache/coursier/v1",
              "~/AppData/Local/Coursier/Cache/v1",
              "~/Library/Caches/Coursier/v1"
            ).mkString("\n"),
            "key" -> s"$${{ runner.os }}-sbt-cache-v2-${hashes.mkString("-")}"
          )
        )
      )
    },

    githubWorkflowJobSetup := {
      val autoCrlfOpt = if (githubWorkflowOSes.value.exists(_.contains("windows"))) {
        List(
          WorkflowStep.Run(
            List("git config --global core.autocrlf false"),
            name = Some("Ignore line ending differences in git"),
            cond = windowsGuard))
      } else {
        Nil
      }

      autoCrlfOpt ::: List(
        WorkflowStep.CheckoutFull,
        WorkflowStep.SetupScala) :::
        githubWorkflowGeneratedCacheSteps.value.toList
    },

    githubWorkflowGeneratedCI := {
      val publicationCondPre =
        githubWorkflowPublishTargetBranches.value.map(compileBranchPredicate("github.ref", _)).mkString("(", " || ", ")")

      val publicationCond = githubWorkflowPublishCond.value match {
        case Some(cond) => publicationCondPre + " && (" + cond + ")"
        case None => publicationCondPre
      }

      val uploadStepsOpt = if (githubWorkflowPublishTargetBranches.value.isEmpty && githubWorkflowAddedJobs.value.isEmpty)
        Nil
      else
        githubWorkflowGeneratedUploadSteps.value.toList

      val publishJobOpt = Seq(
        WorkflowJob(
          "publish",
          "Publish Artifacts",
          githubWorkflowJobSetup.value.toList :::
            githubWorkflowGeneratedDownloadSteps.value.toList :::
            githubWorkflowPublishPreamble.value.toList :::
            githubWorkflowPublish.value.toList :::
            githubWorkflowPublishPostamble.value.toList,
          cond = Some(s"github.event_name != 'pull_request' && $publicationCond"),
          scalas = List(scalaVersion.value),
          javas = List(githubWorkflowJavaVersions.value.head),
          needs = List("build"))).filter(_ => !githubWorkflowPublishTargetBranches.value.isEmpty)

      Seq(
        WorkflowJob(
          "build",
          "Build and Test",
          githubWorkflowJobSetup.value.toList :::
            githubWorkflowBuildPreamble.value.toList :::
            WorkflowStep.Sbt(
              List("githubWorkflowCheck"),
              name = Some("Check that workflows are up to date")) ::
            githubWorkflowBuild.value.toList :::
            githubWorkflowBuildPostamble.value.toList :::
            uploadStepsOpt,
          oses = githubWorkflowOSes.value.toList,
          scalas = githubWorkflowScalaVersions.value.toList,
          javas = githubWorkflowJavaVersions.value.toList,
          matrixFailFast = githubWorkflowBuildMatrixFailFast.value,
          matrixAdds = githubWorkflowBuildMatrixAdditions.value,
          matrixIncs = githubWorkflowBuildMatrixInclusions.value.toList,
          matrixExcs = githubWorkflowBuildMatrixExclusions.value.toList,
          runsOnExtraLabels = githubWorkflowBuildRunsOnExtraLabels.value.toList )) ++ publishJobOpt ++ githubWorkflowAddedJobs.value
    })

  private val generateCiContents = Def task {
    val sbt = if (githubWorkflowUseSbtThinClient.value) {
      githubWorkflowSbtCommand.value + " --client"
    } else {
      githubWorkflowSbtCommand.value
    }
    compileWorkflow(
      "Continuous Integration",
      githubWorkflowTargetBranches.value.toList,
      githubWorkflowTargetTags.value.toList,
      githubWorkflowPREventTypes.value.toList,
      githubWorkflowEnv.value,
      githubWorkflowGeneratedCI.value.toList,
      sbt)
  }

  private val readCleanContents = Def task {
    val src = Source.fromURL(getClass.getResource("/clean.yml"))
    try {
      src.getLines().mkString("\n")
    } finally {
      src.close()
    }
  }

  private val workflowsDirTask = Def task {
    val githubDir = baseDirectory.value / ".github"
    val workflowsDir = githubDir / "workflows"

    if (!githubDir.exists()) {
      githubDir.mkdir()
    }

    if (!workflowsDir.exists()) {
      workflowsDir.mkdir()
    }

    workflowsDir
  }

  private val ciYmlFile = Def task {
    workflowsDirTask.value / "ci.yml"
  }

  private val cleanYmlFile = Def task {
    workflowsDirTask.value / "clean.yml"
  }

  override def projectSettings = Seq(
    Global / internalTargetAggregation ++= {
      if (githubWorkflowArtifactUpload.value)
        Seq(target.value)
      else
        Seq()
    },

    githubWorkflowGenerate / aggregate := false,
    githubWorkflowCheck / aggregate := false,

    githubWorkflowGenerate := {
      val ciContents = generateCiContents.value
      val cleanContents = readCleanContents.value

      val ciYml = ciYmlFile.value
      val cleanYml = cleanYmlFile.value

      val ciWriter = new BufferedWriter(new FileWriter(ciYml))
      try {
        ciWriter.write(ciContents)
        ciWriter.newLine()
      } finally {
        ciWriter.close()
      }

      val cleanWriter = new BufferedWriter(new FileWriter(cleanYml))
      try {
        cleanWriter.write(cleanContents)
        cleanWriter.newLine()
      } finally {
        cleanWriter.close()
      }
    },

    githubWorkflowCheck := {
      val expectedCiContents = generateCiContents.value
      val expectedCleanContents = readCleanContents.value

      val ciYml = ciYmlFile.value
      val cleanYml = cleanYmlFile.value

      val log = state.value.log

      def loadFile(file: File): String = {
        val ciSource = Source.fromFile(file)
        try {
          ciSource.getLines().mkString("\n")
        } finally {
          ciSource.close()
        }
      }

      def reportMismatch(file: File, expected: String, actual: String): Unit = {
        log.error(s"Expected:\n$expected")
        log.error(s"Actual:\n${diff(expected, actual)}")
        sys.error(s"${file.getName} does not contain contents that would have been generated by sbt-github-actions; try running githubWorkflowGenerate")
      }

      def compare(file: File, expected: String): Unit = {
        val actual = loadFile(file)
        if (expected != actual) {
          reportMismatch(file, expected, actual)
        }
      }

      compare(ciYml, expectedCiContents)
      compare(cleanYml, expectedCleanContents)
    })

  private[sbtghactions] def diff(expected: String, actual: String): String = {
    val expectedLines = expected.split("\n", -1)
    val actualLines = actual.split("\n", -1)
    val (lines, _) = expectedLines.zipAll(actualLines, "", "").foldLeft((Vector.empty[String], false)) {
      case ((acc, foundDifference), (expectedLine, actualLine)) if expectedLine == actualLine =>
        (acc :+ actualLine, foundDifference)
      case ((acc, false), ("", actualLine)) =>
        val previousLineLength = acc.lastOption.map(_.length).getOrElse(0)
        val padding = " " * previousLineLength
        val highlight = s"$padding^ (additional lines)"
        (acc :+ highlight :+ actualLine, true)
      case ((acc, false), (_, "")) =>
        val previousLineLength = acc.lastOption.map(_.length).getOrElse(0)
        val padding = " " * previousLineLength
        val highlight = s"$padding^ (missing lines)"
        (acc :+ highlight, true)
      case ((acc, false), (expectedLine, actualLine)) =>
        val sameCount = expectedLine.zip(actualLine).takeWhile({ case (a, b) => a == b }).length
        val padding = " " * sameCount
        val highlight = s"$padding^ (different character)"
        (acc :+ actualLine :+ highlight, true)
      case ((acc, true), (_, "")) =>
        (acc, true)
      case ((acc, true), (_, actualLine)) =>
        (acc :+ actualLine, true)
    }
    lines.mkString("\n")
  }
}
