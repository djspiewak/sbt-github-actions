package sbtghactions

sealed trait GenerationTarget extends Product with Serializable

object GenerationTarget {
  val all: Set[GenerationTarget] = Set(GenerationTarget.CI, GenerationTarget.Clean, GenerationTarget.Custom)

  case object CI     extends GenerationTarget
  case object Clean  extends GenerationTarget
  case object Custom extends GenerationTarget
}
