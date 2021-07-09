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

final case class WorkflowJob(
    id: String,
    name: String,
    steps: List[WorkflowStep],
    cond: Option[String] = None,
    env: Map[String, String] = Map(),
    oses: List[String] = List("ubuntu-latest"),
    scalas: List[String] = List("2.13.4"),
    javas: List[String] = List("adopt@1.8"),
    needs: List[String] = List(),
    matrixFailFast: Option[Boolean] = None,
    matrixAdds: Map[String, List[String]] = Map(),
    matrixIncs: List[MatrixInclude] = List(),
    matrixExcs: List[MatrixExclude] = List(),
    runsOnExtraLabels: List[String] = List(),
    container: Option[JobContainer] = None,
    environment: Option[JobEnvironment] = None) {

  def needsJob(job: WorkflowJob): WorkflowJob =
    copy(needs = needs :+ job.id)
}
