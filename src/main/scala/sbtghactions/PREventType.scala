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

sealed trait PREventType extends EventType

object PREventType {
  val Defaults = List(Opened, Reopened, Synchronize)

  case object Assigned             extends PREventType
  case object Unassigned           extends PREventType
  case object Labeled              extends PREventType
  case object Unlabeled            extends PREventType
  case object Opened               extends PREventType
  case object Edited               extends PREventType
  case object Closed               extends PREventType
  case object Reopened             extends PREventType
  case object Synchronize          extends PREventType
  case object ReadyForReview       extends PREventType
  case object Locked               extends PREventType
  case object Unlocked             extends PREventType
  case object ReviewRequested      extends PREventType
  case object ReviewRequestRemoved extends PREventType
}
