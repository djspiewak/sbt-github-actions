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

sealed trait IssuesEventType extends EventType

object IssuesEventType {
  case object Opened       extends IssuesEventType
  case object Edited       extends IssuesEventType
  case object Deleted      extends IssuesEventType
  case object Transferred  extends IssuesEventType
  case object Pinned       extends IssuesEventType
  case object Unpinned     extends IssuesEventType
  case object Closed       extends IssuesEventType
  case object Reopened     extends IssuesEventType
  case object Assigned     extends IssuesEventType
  case object Unassigned   extends IssuesEventType
  case object Labeled      extends IssuesEventType
  case object Unlabeled    extends IssuesEventType
  case object Locked       extends IssuesEventType
  case object Unlocked     extends IssuesEventType
  case object Milestoned   extends IssuesEventType
  case object Demilestoned extends IssuesEventType
}
