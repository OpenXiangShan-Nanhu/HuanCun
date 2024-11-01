import mill._
import scalalib._
import scalafmt._
import $file.common
import $file.`rocket-chip`.common
import $file.`rocket-chip`.cde.common
import $file.`rocket-chip`.hardfloat.common

val defaultVersions = Map(
  "chisel" -> "6.1.0",
  "chisel-plugin" -> "6.1.0",
  "chiseltest" -> "5.0.0",
  "scala" -> "2.13.10",
)

def getVersion(dep: String, org: String = "org.chipsalliance", cross: Boolean = false) = {
  val version = sys.env.getOrElse(dep + "Version", defaultVersions(dep))
  if (cross)
    ivy"$org:::$dep:$version"
  else
    ivy"$org::$dep:$version"
}

trait HasChisel extends ScalaModule {
  def chiselModule: Option[ScalaModule] = None

  def chiselPluginJar: T[Option[PathRef]] = None

  def chiselIvy: Option[Dep] = Some(getVersion("chisel"))

  def chiselPluginIvy: Option[Dep] = Some(getVersion("chisel-plugin", cross=true))

  override def scalaVersion = defaultVersions("scala")

  override def scalacOptions = super.scalacOptions() ++
    Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")

  override def ivyDeps = super.ivyDeps() ++ Agg(chiselIvy.get)

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)
}

object rocketchip extends `rocket-chip`.common.RocketChipModule with HasChisel {

  val rcPath = os.pwd / "rocket-chip"
  override def millSourcePath = rcPath

  def mainargsIvy = ivy"com.lihaoyi::mainargs:0.5.0"

  def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.5"

  object macros extends `rocket-chip`.common.MacrosModule with HasChisel {
    def scalaReflectIvy = ivy"org.scala-lang:scala-reflect:${scalaVersion}"
  }

  object cde extends `rocket-chip`.cde.common.CDEModule with HasChisel {
    override def millSourcePath = rcPath / "cde" / "cde"
  }

  object hardfloat extends `rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {
    override def millSourcePath = rcPath / "hardfloat" / "hardfloat"
  }

  def macrosModule = macros

  def hardfloatModule = hardfloat

  def cdeModule = cde

}

object xsutils extends SbtModule with HasChisel {
  override def millSourcePath = os.pwd / "xs-utils"

  override def moduleDeps = super.moduleDeps ++ Seq(rocketchip)
}

object utility extends SbtModule with HasChisel {
  override def millSourcePath = os.pwd / "Utility"

  override def moduleDeps = super.moduleDeps ++ Seq(rocketchip)
}


object HuanCun extends SbtModule with HasChisel with millbuild.common.HuanCunModule {

  override def millSourcePath = millOuterCtx.millSourcePath

  def rocketModule: ScalaModule = rocketchip

  def utilityModule: ScalaModule = utility

  def xsutilsModule: ScalaModule = xsutils

  object test extends SbtModuleTests with TestModule.ScalaTest {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      getVersion("chiseltest", "edu.berkeley.cs"),
    )
  }

}
