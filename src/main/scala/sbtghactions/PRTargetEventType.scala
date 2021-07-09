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

sealed trait PRTargetEventType extends EventType

object PRTargetEventType {
  case object Assigned             extends PRTargetEventType
  case object Unassigned           extends PRTargetEventType
  case object Labeled              extends PRTargetEventType
  case object Unlabeled            extends PRTargetEventType
  case object Opened               extends PRTargetEventType
  case object Edited               extends PRTargetEventType
  case object Closed               extends PRTargetEventType
  case object Reopened             extends PRTargetEventType
  case object Synchronize          extends PRTargetEventType
  case object ReadyForReview       extends PRTargetEventType
  case object Locked               extends PRTargetEventType
  case object Unlocked             extends PRTargetEventType
  case object ReviewRequested      extends PRTargetEventType
  case object ReviewRequestRemoved extends PRTargetEventType
}
