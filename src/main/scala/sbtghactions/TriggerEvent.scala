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

import sbtghactions.RenderFunctions._

sealed trait TriggerEvent extends Product with Serializable {
  val name: String = SnakeCase(productPrefix)
  def render: String
}

final case class Schedule(cron: String) extends TriggerEvent {

  override def render: String =
    s"""|$name:
        |  - cron: '$cron'""".stripMargin
}

sealed trait ManualEvent extends TriggerEvent

object ManualEvent {

  final case class Input(
      name: String,
      description: String,
      default: Option[String],
      required: Boolean) {

    //TODO Should we check if the name is a safe string? What if not?
    def render: String =
      s"""|$name:
          |  description: ${wrap(description)}
          |  required: $required
          |""".stripMargin + default.map(wrap).map("default: " + _).map(indentOnce).getOrElse("")
  }

  final case class WorkflowDispatch(inputs: Seq[Input]) extends ManualEvent {

    override def render: String =
      s"$name:${renderInputs(inputs)}"
  }

  private def renderInputs(inputs: Seq[Input]) =
    if (inputs.isEmpty) ""
    else indentOnce { "\ninputs:\n" + inputs.map(_.render).map(indentOnce).mkString("\n") }

  final case class RepositoryDispatch(types: Seq[String]) extends ManualEvent {

    override def render: String =
      s"$name:${indentOnce { renderParamWithList("types", types) }}"
  }
}

sealed trait WebhookEvent extends TriggerEvent

sealed trait PlainNameEvent extends WebhookEvent {
  final override def render: String = name
}

sealed trait TypedEvent extends WebhookEvent {

  def types: Seq[EventType]

  final override def render: String =
    s"$name:${renderTypes(types)}"
}

object WebhookEvent {

  final case class CheckRun(types: Seq[CheckRunEventType]) extends TypedEvent

  final case class CheckSuite(types: Seq[CheckSuiteEventType]) extends TypedEvent

  case object Create extends PlainNameEvent

  case object Delete extends PlainNameEvent

  case object Deployment extends PlainNameEvent

  case object DeploymentStatus extends PlainNameEvent

  case object Fork extends PlainNameEvent

  case object Gollum extends PlainNameEvent

  final case class IssueComment(types: Seq[IssueCommentEventType]) extends TypedEvent

  final case class Issues(types: Seq[IssuesEventType]) extends TypedEvent

  final case class Label(types: Seq[LabelEventType]) extends TypedEvent

  final case class Milestone(types: Seq[MilestoneEventType]) extends TypedEvent

  case object PageBuild extends PlainNameEvent

  final case class Project(types: Seq[ProjectEventType]) extends TypedEvent

  final case class ProjectCard(types: Seq[ProjectCardEventType]) extends TypedEvent

  final case class ProjectColumn(types: Seq[ProjectColumnEventType]) extends TypedEvent

  case object Public extends PlainNameEvent

  final case class PullRequest(
      branches: Seq[String],
      tags: Seq[String],
      paths: Seq[String],
      types: Seq[PREventType])
      extends WebhookEvent {

    override def render: String =
      s"$name:" +
        indentOnce { renderBranches(branches) + renderTags + renderPaths + renderTypes }

    private def renderTags          = renderParamWithList("tags", tags)
    private def renderPaths: String = if (paths.isEmpty) "" else renderParamWithList("paths", paths)

    private def renderTypes =
      if (types == PREventType.Defaults) ""
      else "" + renderParamWithList("types", types.map(_.render))

  }

  final case class PullRequestReview(types: Seq[PRReviewEventType]) extends TypedEvent

  final case class PullRequestReviewComment(types: Seq[PRReviewCommentEventType]) extends TypedEvent

  final case class PullRequestTarget(types: Seq[PRTargetEventType]) extends TypedEvent

  final case class Push(branches: Seq[String], tags: Seq[String], paths: Seq[String])
      extends WebhookEvent {

    override def render: String =
      s"$name:" +
        indentOnce { renderBranches(branches) + renderTags + renderPaths }

    def renderTags: String  = if (tags.isEmpty) "" else renderParamWithList("tags", tags)
    def renderPaths: String = if (paths.isEmpty) "" else renderParamWithList("paths", paths)

  }

  final case class RegistryPackage(types: Seq[RegistryPackageEventType]) extends TypedEvent

  final case class Release(types: Seq[ReleaseEventType]) extends TypedEvent

  case object Status extends PlainNameEvent

  final case class Watch(types: Seq[WatchEventType]) extends TypedEvent

  final case class WorkflowRun(workflows: Seq[String], types: Seq[WorkflowRunEventType])
      extends WebhookEvent {

    override def render: String =
      s"$name:${indentOnce(renderParamWithList("workflows", workflows))}${renderTypes(types)}"
  }
}
