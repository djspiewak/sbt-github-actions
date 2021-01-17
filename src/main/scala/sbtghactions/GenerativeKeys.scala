/*
 * Copyright 2020 Daniel Spiewak
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

trait GenerativeKeys {

  lazy val githubWorkflowGenerate = taskKey[Unit]("Generates (and overwrites if extant) a ci.yml and clean.yml actions description according to configuration")
  lazy val githubWorkflowCheck = taskKey[Unit]("Checks to see if the ci.yml and clean.yml files are equivalent to what would be generated and errors if otherwise")

  lazy val githubWorkflowGeneratedCI = settingKey[Seq[WorkflowJob]]("The sequence of jobs which will make up the generated ci workflow (ci.yml)")
  lazy val githubWorkflowGeneratedUploadSteps = settingKey[Seq[WorkflowStep]]("The sequence of steps used to upload intermediate build artifacts for an adjacent job")
  lazy val githubWorkflowGeneratedDownloadSteps = settingKey[Seq[WorkflowStep]]("The sequence of steps used to download intermediate build artifacts published by an adjacent job")
  lazy val githubWorkflowGeneratedCacheSteps = settingKey[Seq[WorkflowStep]]("The sequence of steps used to configure caching for ivy, sbt, and coursier")

  lazy val githubWorkflowSbtCommand = settingKey[String]("The command which invokes sbt (default: sbt")

  lazy val githubWorkflowBuildMatrixFailFast = settingKey[Option[Boolean]]("Whether or not to enable the fail-fast strategy (default: None/Enabled)")
  lazy val githubWorkflowBuildMatrixAdditions = settingKey[Map[String, List[String]]]("A map of additional matrix dimensions for the build job. Each list should be non-empty. (default: {})")
  lazy val githubWorkflowBuildMatrixInclusions = settingKey[Seq[MatrixInclude]]("A list of matrix inclusions (default: [])")
  lazy val githubWorkflowBuildMatrixExclusions = settingKey[Seq[MatrixExclude]]("A list of matrix exclusions (default: [])")
  lazy val githubWorkflowBuildRunsOnExtraLabels = settingKey[Seq[String]]("A list of additional labels to append to each run of the matrix executions")

  lazy val githubWorkflowBuildPreamble = settingKey[Seq[WorkflowStep]]("A list of steps to insert after base setup but before compiling and testing (default: [])")
  lazy val githubWorkflowBuildPostamble = settingKey[Seq[WorkflowStep]]("A list of steps to insert after comping and testing but before the end of the build job (default: [])")
  lazy val githubWorkflowBuild = settingKey[Seq[WorkflowStep]]("A sequence of workflow steps which compile and test the project (default: [Sbt(List(\"test\"))])")

  lazy val githubWorkflowPublishPreamble = settingKey[Seq[WorkflowStep]]("A list of steps to insert after base setup but before publishing (default: [])")
  lazy val githubWorkflowPublishPostamble = settingKey[Seq[WorkflowStep]]("A list of steps to insert after publication but before the end of the publish job (default: [])")
  lazy val githubWorkflowPublish = settingKey[Seq[WorkflowStep]]("A sequence workflow steps which publish the project (default: [Sbt(List(\"+publish\"))])")
  lazy val githubWorkflowPublishTargetBranches = settingKey[Seq[RefPredicate]]("A set of branch predicates which will be applied to determine whether the current branch gets a publication stage; if empty, publish will be skipped entirely (default: [== master])")
  lazy val githubWorkflowPublishCond = settingKey[Option[String]]("A set of conditionals to apply to the publish job to further restrict its run (default: [])")

  lazy val githubWorkflowJavaVersions = settingKey[Seq[String]]("A list of Java versions to be used for the build job. The publish job will use the *first* of these versions. (default: [adopt@1.8])")
  lazy val githubWorkflowScalaVersions = settingKey[Seq[String]]("A list of Scala versions on which to build the project (default: crossScalaVersions.value)")
  lazy val githubWorkflowOSes = settingKey[Seq[String]]("A list of OS names (default: [ubuntu-latest])")

  lazy val githubWorkflowDependencyPatterns = settingKey[Seq[String]]("A list of file globes within the project which affect dependency information (default: [**/*.sbt, project/build.properties])")
  lazy val githubWorkflowTargetBranches = settingKey[Seq[String]]("A list of branch patterns on which to trigger push and PR builds (default: [*])")
  lazy val githubWorkflowTargetTags = settingKey[Seq[String]]("A list of tag patterns on which to trigger push builds (default: [])")
  lazy val githubWorkflowPREventTypes = settingKey[Seq[PREventType]]("A list of pull request event types which will be used to trigger builds (default: [opened, synchronize, reopened])")

  lazy val githubWorkflowArtifactUpload = settingKey[Boolean]("Controls whether or not to upload target directories in the event that multiple jobs are running sequentially. Can be set on a per-project basis (default: true)")
  lazy val githubWorkflowJobSetup = settingKey[Seq[WorkflowStep]]("The automatically-generated checkout, setup, and cache steps which are common to all jobs which touch the build (default: autogenerated)")

  lazy val githubWorkflowEnv = settingKey[Map[String, String]]("A map of static environment variable assignments global to the workflow (default: { GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }} })")
  lazy val githubWorkflowAddedJobs = settingKey[Seq[WorkflowJob]]("A list of additional jobs to add to the CI workflow (default: [])")

  lazy val githubWorkflowAutoMerge = settingKey[Boolean]("Whether or not to enable auto merging of bot PRs, e.g. by scala-steward (default: false)")
  lazy val githubWorkflowAutoMergeAuthor = settingKey[String]("The user name of a bot whose PRs will be merged automatically (default: scala-steward)")
  lazy val githubWorkflowAutoMergeMethod = settingKey[MergeMethod]("The merge method to use for auto merging bot PRs (default: Squash)")
}

object GenerativeKeys extends GenerativeKeys
