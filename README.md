# sbt-github-actions

A plugin for assisting in building sbt projects using [GitHub Actions](https://github.com/features/actions), in the style of [sbt-travisci](https://github.com/dwijnand/sbt-travisci). Unlike sbt-travisci, though, this plugin also provides a mechanism for *generating* GitHub Actions workflows from the sbt build definition. Conceptually, sbt-travisci allows Travis and sbt to jointly represent the "source of truth", while sbt-github-actions idiomatically vests that power solely in sbt.

Note that the generative functionality is *optional* and doesn't need to be used if undesired.

An example of how this "source of truth" pattern differs between the two plugins can be seen with `crossScalaVersions`. With sbt-travisci, the `crossScalaVersions` and `scalaVersion` settings are populated from the `scala:` key in **.travis.yml**. However, with sbt-github-actions, the `scala:` entry in the job `matrix:` is populated from the `Global / crossScalaVersions` key in your **build.sbt**.

## Usage

Add the following to your `plugins.sbt`:

```sbt
addSbtPlugin("com.codecommit" % "sbt-github-actions" % <latest>)
```

To use the generative functionality, run `sbt githubWorkflowGenerate` and *and commit the results*. If your sbt build is ever changed such that the generated workflow is no longer in sync, the workflow run in GitHub Actions will begin failing and you will need to re-run this task (and commit the results).

## General Plugin

The `GitHubActionsPlugin` provides general functionality, giving builds the ability to introspect on their host workflow and whether or not they are running in GitHub Actions. This latter functionality, exposed by the `githubIsWorkflowBuild` global setting, is the most commonly used functionality of this plugin. If you need behavior within your build which is conditional on whether or not the build is running in CI, this is the setting you should branch on.

`githubWorkflowName` and `githubWorkflowDefinition` are designed to allow introspection on the *exact* definition of the workflow which is running the current build, if any. This kind of introspection is not common, but seems like it could be useful.

## Generative Plugin

As mentioned above, the `GenerativePlugin` is designed to make it easier to maintain GitHub Actions builds for sbt projects by generating **ci.yml** and **clean.yml** workflow definition files, and then forcibly failing the build if these files ever fall out of step with the build itself. The **ci.yml** workflow, by default, contains both `build` and `publish` jobs, though you will likely need to add extra steps to the `githubWorkflowPublishPreamble` and/or `githubWorkflowEnv` (e.g. decrypting and importing a GPG signing key) in order for publication to *actually* work.

If a `publish` job is not desired, simply set `githubWorkflowPublishBranchPatterns` to `Seq()`. By default, `publish` is restricted to run on `master`, and additional restrictions may be configured within the build.

Ivy, sbt, and Coursier caching are all handled by the generated **ci.yml** by default, as well as standard things like Git checkout, Scala setup (using Olafur's [excellent `setup-scala` action](https://github.com/olafurpg/setup-scala)), and more. The matrix for the `build` job will be generated from `crossScalaVersions` and has additional support for multiple JVMs and OSes. Additionally, compiled artifacts are properly uploaded so that jobs which are dependent on `build` can avoid redundant work (most notably, `publish`). Thus, publication is guaranteed to be based on binary files that were generated *and* tested by the `build` job, rather than re-generated by `publish`. (**NB**: due to what appear to be issues in Zinc, this isn't *quite* working yet; expect it to be fixed in a coming release of sbt-github-actions)

**clean.yml** is generated based on a static description because it *should* just be the default in all GitHub Actions projects. This is basically a hack to work around the fact that artifacts produced by GitHub Actions workflows count against personal and organization storage limits, but those artifacts also are retained *indefinitely* up until 2 GB. This is entirely unnecessary and egregious, since artifacts are transient and only useful for passing state between jobs within the same workflow. To make matters more complicated, artifacts from a given workflow are invisible to the GitHub API until that workflow is finished, which is why **clean.yml** has to be a *separate* workflow rather than part of **ci.yml**. It runs on every push to the repository.

This plugin is quite prescriptive in that it forcibly manages the contents of the **ci.yml** and **clean.yml** files. By default, **ci.yml** will contain a step which *verifies* that its contents (and the contents of **clean.yml**) correspond precisely to the most up-to-date generated version of themselves. If this is not the case, then the build is failed. However, there is no restriction in adding *other* workflows not named **ci.yml** or **clean.yml**. These will be ignored entirely by the plugin.

## Tasks

### Generative

- `githubWorkflowGenerate` – Generates (and overwrites if extant) **ci.yml** and **clean.yml** workflows according to configuration within sbt. The **clean.yml** workflow is something that GitHub Actions should just do by default: it removes old build artifacts to prevent them from running up your storage usage (it has no effect on currently running builds). This workflow is unconfigurable and is simply drawn from the static contents of the **clean.yml** resource file within this repository.
- `githubWorkflowCheck` – Checks to see if the **ci.yml** and **clean.yml** files are equivalent to what would be generated and errors if otherwise. This task is run from within the generated **ci.yml** to ensure that the build and the workflow are kept in sync. As a general rule, any time you change the workflow configuration within sbt, you should regenerate the **ci.yml** and commit the results, but inevitably people forget. This check fails the build if that happens. Note that if you *need* to manually fiddle with the **ci.yml** contents, for whatever reason, you will need to remove the call to this check from within the workflow, otherwise your build will simply fail.

## Settings

### General

- `githubIsWorkflowBuild` : `Boolean` – Indicates whether or not the build is currently running within a GitHub Actions Workflow
- `githubWorkflowName` : `String` – The name of the currently-running workflow. Will be undefined if not running in GitHub Actions.
- `githubWorkflowDefinition` : `Map[String, Any]` – The raw (parsed) contents of the workflow YAML definition. Will be undefined if not running in GitHub Actions, or if (for some reason) the workflow could not be identified. Workflows are located by taking the `githubWorkflowName` and finding the YAML definition which has the corresponding `name:` key/value pair.

### Generative

Any and all settings which affect the behavior of the generative plugin should be set in the `ThisBuild` scope (for example, `ThisBuild / crossScalaVersions :=` rather than just `crossScalaVersions := `). This is important because GitHub Actions workflows are global across the entire build, regardless of how individual projects are configured. A corollary of this is that it is not possible (yet) to have specific subprojects which build with different Scala versions, Java versions, or OSes. This is theoretically possible but it's very complicated. For now, I'm going to be lazy and wait for someone to say "pretty please" before implementing it.

#### General

- `githubWorkflowGeneratedCI` : `Seq[WorkflowJob]` — Contains a description of the **ci.yml** jobs that will drive the generation if used. This setting can be overridden to customize the jobs (e.g. by adding additional jobs to the workflow).
- `githubWorkflowGeneratedUploadSteps` : `Seq[WorkflowStep]` – Contains a list of steps which are used to upload generated intermediate artifacts from the `build` job. This is mostly for reference and introspection purposes; one would not be expected to *change* this setting.
- `githubWorkflowGeneratedDownloadSteps` : `Seq[WorkflowStep]` – Contains a list of steps which are used to download generated intermediate artifacts from the `build` job. This is mostly for reference and introspection purposes; one would not be expected to *change* this setting. This setting is particularly useful in conjunction with `githubWorkflowAddedJobs`: if you're adding a job which needs access to intermediate artifacts, you should make sure these steps are part of the process.
- `githubWorkflowGeneratedCacheSteps` : `Seq[WorkflowStep]` – Contains a list of steps which are used to set up caching for ivy, sbt, and coursier artifacts (respecting changes to files which contain versioning information). This is mostly for reference and introspection purposes; one would not be expected to *change* this setting. This setting is particularly useful in conjunction with `githubWorkflowAddedJobs`: if you're adding a job which needs to use sbt, you should probably ensure that these steps are part of the job.
- `githubWorkflowSbtCommand` : `String` – Any use of sbt within the generated workflow will compile to an invocation of this bash command. This defaults to `"sbt"`, but can be overridden to anything that is valid in bash syntax (e.g. `"csbt"`, or `"$SBT"`).
- `githubWorkflowDependencyPatterns` : `Seq[String]` – A list of file globs which dictate where dependency information is stored. This is conventionally just `**/*.sbt` and `project/build.properties`. If you store dependency information in some *other* file (for example, `project/Versions.scala`), then you should add a glob which matches that file in this setting. This is used for determining the appropriate cache keys for the Ivy and Coursier caches.
- `githubWorkflowTargetBranches` : `Seq[String]` – A list of globs which will match branches and tags for `push` and `pull-request` event types to trigger the **ci.yml** workflow. Defaults to `*`.
- `githubWorkflowEnv` : `Map[String, String]` – An environment which is global to the entire **ci.yml** workflow. Defaults to `Map("GITHUB_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}")` since it's so commonly needed.
- `githubWorkflowAddedJobs` : `Seq[WorkflowJob]` – A convenience mechanism for adding extra custom jobs to the **ci.yml** workflow (though you can also do this by modifying `githubWorkflowGeneratedCI`). Defaults to empty.

#### `build` Job

- `githubWorkflowBuildPreamble` : `Seq[WorkflowStep]` – Contains a list of steps which will be inserted into the `build` job in the **ci.yml** workflow *after* setup but *before* the `sbt test` invocation. Defaults to empty.
- `githubWorkflowBuild` : `WorkflowStep` – The step which invokes sbt to build and test your project. This defaults to just `sbt test`, but can be overridden to anything. For example, sbt plugin projects probably want to redefine this to be `WorkflowStep.Sbt(List("test", "scripted"))`, which would run the `test` and `scripted` sbt tasks, in order. Note that all uses of `WorkflowStep.Sbt` are compiled using the configured `githubWorkflowSbtCommand` invocation, and properly configured with respect to the build matrix-selected Scala version.
- `githubWorkflowJavaVersions` : `Seq[String]` – A list of Java versions, using [Jabba](https://github.com/shyiko/jabba) syntax, which will be used to `build` your project. Note that the `publish` job will always run under solely `adopt@1.8`. Defaults to `adopt@1.8`.
- `githubWorkflowScalaVersions` : `Seq[String]` – A list of Scala versions which will be used to `build` your project. Defaults to `crossScalaVersions` in `build`, and simply `scalaVersion` in `publish`.
- `githubWorkflowOSes` : `Seq[String]` – A list of operating systems, which will be ultimately passed to [the `runs-on:` directive](https://help.github.com/en/actions/reference/workflow-syntax-for-github-actions#jobsjob_idruns-on), on which to `build` your project. Defaults to `ubuntu-latest`. Note that, regardless of the value of this setting, only `ubuntu-latest` will be used for the `publish` job. This setting only affects `build`.

#### `publish` Job

- `githubWorkflowPublishPreamble` : `Seq[WorkflowStep]` – Similar to `githubWorkflowBuildPreamble`, this contains a series of steps which will be inserted into the `publish` job *after* setup but *before* the publication step. Defaults to empty.
- `githubWorkflowPublish` : `WorkflowStep` – The step which will be invoked to publish your project. This defaults to `sbt +publish`.
- `githubWorkflowPublishTargetBranches` : `Seq[RefPredicate]` – A list of branch predicates which will be applied to determine whether the `publish` job will run. Defaults to just `== master`. The supports all of the predicate types currently [allowed by GitHub Actions](https://help.github.com/en/actions/reference/context-and-expression-syntax-for-github-actions#functions). This exists because, while you usually want to run the `build` job on *every* branch, `publish` is obviously much more limited in applicability. If this list is empty, then the `publish` job will be omitted entirely from the workflow.
- `githubWorkflowPublishCond` : `Option[String]` – This is an optional added conditional check on the publish branch, which must be defined using [GitHub Actions expression syntax](https://help.github.com/en/actions/reference/context-and-expression-syntax-for-github-actions#about-contexts-and-expressions), which will be conjoined to determine the `if:` predicate on the `publish` job. Defaults to `None`.
