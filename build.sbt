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

name := "sbt-github-actions"

ThisBuild / baseVersion := "0.2"

ThisBuild / organization := "com.codecommit"
ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

Global / scalaVersion := "2.12.10"

Global / githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest", "windows-latest")
Global / githubWorkflowBuild := WorkflowStep.Sbt(List("test", "scripted"))

Global / githubWorkflowPublishTargetBranches := Seq()

sbtPlugin := true
sbtVersion := "1.3.8"

libraryDependencies += "org.specs2" %% "specs2-core" % "4.8.3" % Test

disablePlugins(TravisCiPlugin)
enablePlugins(SbtPlugin)

scriptedLaunchOpts ++= Seq("-Dplugin.version=" + version.value)
scriptedBufferLog := true
