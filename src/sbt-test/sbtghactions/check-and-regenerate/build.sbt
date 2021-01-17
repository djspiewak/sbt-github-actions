organization := "com.codecommit"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.1", "2.12.10")
ThisBuild / scalaVersion := crossScalaVersions.value.head

ThisBuild / githubWorkflowTargetTags += "v*"

ThisBuild / githubWorkflowJavaVersions += "graalvm@20.0.0"
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Tag("test"))

ThisBuild / githubWorkflowBuildMatrixAdditions += "test" -> List("this", "is")

ThisBuild / githubWorkflowBuildMatrixInclusions += MatrixInclude(
  Map("test" -> "this"),
  Map("extra" -> "sparta"))

ThisBuild / githubWorkflowBuildMatrixExclusions +=
  MatrixExclude(Map("scala" -> "2.12.10", "test" -> "is"))

ThisBuild / githubWorkflowBuild += WorkflowStep.Run(List("echo yo"))
ThisBuild / githubWorkflowPublish += WorkflowStep.Run(List("echo sup"))
ThisBuild / githubWorkflowAutoMerge := true
