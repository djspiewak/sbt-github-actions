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

import org.specs2.mutable.Specification
import org.specs2.specification.AllExpectations
import sbtghactions.ManualEvent.Input

class TriggerEventSpec extends Specification with AllExpectations {
  "schedule" should {
    "render cron expression" in {
      Schedule("* * * * *").render mustEqual "schedule:\n  - cron: '* * * * *'"
    }
  }

  "workflow dispatch" should {
    "render without inputs" in {
      ManualEvent.WorkflowDispatch(Nil).render mustEqual "workflow_dispatch:"
    }

    "render inputs" in {

      val expected =
        """workflow_dispatch:
          |  inputs:
          |    ref:
          |      description: The branch, tag or SHA to build
          |      required: true
          |      default: master""".stripMargin

      ManualEvent
        .WorkflowDispatch(
          List(
            Input("ref", "The branch, tag or SHA to build", Some("master"), required = true)
          )
        )
        .render mustEqual expected
    }
  }

  "repository dispatch" should {
    "render without types" in {
      ManualEvent.RepositoryDispatch(Nil).render mustEqual "repository_dispatch:"
    }

    "render types" in {

      val expected =
        """repository_dispatch:
          |  types: [event1, event2]""".stripMargin

      ManualEvent.RepositoryDispatch(List("event1", "event2")).render mustEqual expected
    }
  }

  "pull request" should {
    "render without branches, tags, paths or types" in {
      val expected = "pull_request:"
      WebhookEvent.PullRequest(Nil, Nil, Nil, Nil).render mustEqual expected
    }

    "render without branches, tags, paths, but with types" in {
      val expected =
        """|pull_request:
           |  types: [edited, ready_for_review]""".stripMargin
      WebhookEvent
        .PullRequest(Nil, Nil, Nil, List(PREventType.Edited, PREventType.ReadyForReview))
        .render mustEqual expected
    }

    "render without branches, but with tags, paths and types" in {
      val expected =
        """|pull_request:
           |  tags: [v1*, v2*]
           |  paths: [src/main/**]
           |  types: [edited, ready_for_review]""".stripMargin
      WebhookEvent
        .PullRequest(
          Nil,
          List("v1*", "v2*"),
          List("src/main/**"),
          List(PREventType.Edited, PREventType.ReadyForReview)
        )
        .render mustEqual expected
    }

    "render with branches, tags and types" in {
      val expected =
        """|pull_request:
           |  branches: [master]
           |  tags: [v1*, v2*]
           |  paths: [src/main/**]
           |  types: [edited, ready_for_review]""".stripMargin
      WebhookEvent
        .PullRequest(
          List("master"),
          List("v1*", "v2*"),
          List("src/main/**"),
          List(PREventType.Edited, PREventType.ReadyForReview)
        )
        .render mustEqual expected
    }

    "render without types, but with branches, tags and paths" in {
      val expected =
        """|pull_request:
           |  branches: [master]
           |  tags: [v1*, v2*]
           |  paths: [src/main/**]""".stripMargin
      WebhookEvent
        .PullRequest(List("master"), List("v1*", "v2*"), List("src/main/**"), Nil)
        .render mustEqual expected
    }

    "render without tags, but with branches and types" in {
      val expected =
        """|pull_request:
           |  branches: [master]
           |  types: [edited, ready_for_review]""".stripMargin
      WebhookEvent
        .PullRequest(List("master"), Nil, Nil, List(PREventType.Edited, PREventType.ReadyForReview))
        .render mustEqual expected
    }
    "render only with paths" in {
      val expected =
        """|push:
           |  paths: [src/main/**]""".stripMargin
      WebhookEvent.PullRequest(Nil, Nil, List("src/main/**"), Nil).render mustEqual expected
    }
  }

  "push" should {
    "render without branches, tags or paths" in {
      val expected = "push:"
      WebhookEvent.Push(Nil, Nil, Nil).render mustEqual expected
    }

    "render without branches, but with tags and paths" in {
      val expected =
        """|push:
           |  tags: [v1*, v2*]
           |  paths: [src/main/**]""".stripMargin
      WebhookEvent.Push(Nil, List("v1*", "v2*"), List("src/main/**")).render mustEqual expected
    }

    "render only with paths" in {
      val expected =
        """|push:
           |  paths: [src/main/**]""".stripMargin
      WebhookEvent.Push(Nil, Nil, List("src/main/**")).render mustEqual expected
    }

    "render with branches and tags" in {
      val expected =
        """|push:
           |  branches: [master]
           |  tags: [v1*, v2*]""".stripMargin
      WebhookEvent.Push(List("master"), List("v1*", "v2*"), Nil).render mustEqual expected
    }

    "render without tags, but with branches" in {
      val expected =
        """|push:
           |  branches: [master]""".stripMargin
      WebhookEvent.Push(List("master"), Nil, Nil).render mustEqual expected
    }
  }

  "workflow run" should {
    "render without workflows and types" in {
      val expected = "workflow_run:"
      WebhookEvent.WorkflowRun(Nil, Nil).render mustEqual expected
    }
  }

  "plain name events" should {
    "render their name only" in {

      val nameEvents = List(
        (WebhookEvent.Create, "create"),
        (WebhookEvent.Delete, "delete"),
        (WebhookEvent.Deployment, "deployment"),
        (WebhookEvent.DeploymentStatus, "deployment_status"),
        (WebhookEvent.Fork, "fork"),
        (WebhookEvent.Gollum, "gollum"),
        (WebhookEvent.PageBuild, "page_build"),
        (WebhookEvent.Public, "public"),
        (WebhookEvent.Status, "status"),
      )

      forall(nameEvents) { case (event, name) =>
        event.render mustEqual s"$name"
      }

    }
  }

  "typed events" should {
    "render their name only, if no types are given" in {

      val events = List(
        (WebhookEvent.CheckRun(Nil), "check_run:"),
        (WebhookEvent.CheckSuite(Nil), "check_suite:"),
        (WebhookEvent.IssueComment(Nil), "issue_comment:"),
        (WebhookEvent.Issues(Nil), "issues:"),
        (WebhookEvent.Label(Nil), "label:"),
        (WebhookEvent.Milestone(Nil), "milestone:"),
        (WebhookEvent.Project(Nil), "project:"),
        (WebhookEvent.ProjectCard(Nil), "project_card:"),
        (WebhookEvent.ProjectColumn(Nil), "project_column:"),
        (WebhookEvent.PullRequestReview(Nil), "pull_request_review:"),
        (WebhookEvent.PullRequestReviewComment(Nil), "pull_request_review_comment:"),
        (WebhookEvent.PullRequestTarget(Nil), "pull_request_target:"),
        (WebhookEvent.RegistryPackage(Nil), "registry_package:"),
        (WebhookEvent.Release(Nil), "release:"),
        (WebhookEvent.Watch(Nil), "watch:"),
      )

      forall(events) { case (event, rendered) =>
        event.render mustEqual s"$rendered"
      }

    }

    "render their name only, if no types are given" in {

      val events = List(
        (
          WebhookEvent.CheckRun(Seq(CheckRunEventType.Created, CheckRunEventType.Completed)),
          "check_run:\n  types: [created, completed]"
        ),
        (
          WebhookEvent.CheckSuite(
            Seq(CheckSuiteEventType.Requested, CheckSuiteEventType.Completed)
          ),
          "check_suite:\n  types: [requested, completed]"
        ),
        (
          WebhookEvent.IssueComment(
            Seq(IssueCommentEventType.Created, IssueCommentEventType.Edited)
          ),
          "issue_comment:\n  types: [created, edited]"
        ),
        (
          WebhookEvent.Issues(Seq(IssuesEventType.Opened, IssuesEventType.Edited)),
          "issues:\n  types: [opened, edited]"
        ),
        (
          WebhookEvent.Label(Seq(LabelEventType.Created, LabelEventType.Edited)),
          "label:\n  types: [created, edited]"
        ),
        (
          WebhookEvent.Milestone(Seq(MilestoneEventType.Opened, MilestoneEventType.Closed)),
          "milestone:\n  types: [opened, closed]"
        ),
        (
          WebhookEvent.Project(Seq(ProjectEventType.Created, ProjectEventType.Closed)),
          "project:\n  types: [created, closed]"
        ),
        (
          WebhookEvent.ProjectCard(
            Seq(ProjectCardEventType.ConvertedToAnIssue, ProjectCardEventType.Moved)
          ),
          "project_card:\n  types: [converted_to_an_issue, moved]"
        ),
        (
          WebhookEvent.ProjectColumn(
            Seq(ProjectColumnEventType.Moved, ProjectColumnEventType.Created)
          ),
          "project_column:\n  types: [moved, created]"
        ),
        (
          WebhookEvent.PullRequestReview(
            Seq(PRReviewEventType.Edited, PRReviewEventType.Dismissed)
          ),
          "pull_request_review:\n  types: [edited, dismissed]"
        ),
        (
          WebhookEvent.PullRequestReviewComment(
            Seq(PRReviewCommentEventType.Created, PRReviewCommentEventType.Edited)
          ),
          "pull_request_review_comment:\n  types: [created, edited]"
        ),
        (
          WebhookEvent.PullRequestTarget(
            Seq(PRTargetEventType.Edited, PRTargetEventType.ReadyForReview)
          ),
          "pull_request_target:\n  types: [edited, ready_for_review]"
        ),
        (
          WebhookEvent.RegistryPackage(
            Seq(RegistryPackageEventType.Updated, RegistryPackageEventType.Published)
          ),
          "registry_package:\n  types: [updated, published]"
        ),
        (
          WebhookEvent.Release(Seq(ReleaseEventType.Edited, ReleaseEventType.Unpublished)),
          "release:\n  types: [edited, unpublished]"
        ),
        (WebhookEvent.Watch(Seq(WatchEventType.Started)), "watch:\n  types: [started]"),
      )

      forall(events) { case (event, rendered) =>
        event.render mustEqual s"$rendered"
      }

    }
  }
}
