package huancun.utils

import firrtl.annotations.{Annotation, ModuleName, Named, SingleTargetAnnotation}
import chisel3._
import chisel3.experimental.annotate

case class SRAMClkDivBy2Annotation(mod: ModuleName) extends SingleTargetAnnotation[ModuleName] {
  override val target: ModuleName = mod

  override def duplicate(n: ModuleName): Annotation = this.copy(n)
}

case class SRAMSpecialDepthAnnotation(mod: ModuleName) extends SingleTargetAnnotation[ModuleName] {
  override val target: ModuleName = mod

  override def duplicate(n: ModuleName): Annotation = this.copy(n)
}

object CustomAnnotations {
  def annotateClkDivBy2(mod: Module) = {
    annotate()(Seq(SRAMClkDivBy2Annotation(mod.toNamed)))
  }
  def annotateSpecialDepth(mod: Module) = {
    annotate()(Seq(SRAMSpecialDepthAnnotation(mod.toNamed)))
  }
}
