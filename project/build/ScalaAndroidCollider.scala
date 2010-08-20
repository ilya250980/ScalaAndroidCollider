import sbt._

trait Defaults {
   def androidPlatformName = "android-7"
}

class ScalaAndroidCollider( info: ProjectInfo ) extends ParentProject( info ) {
   override def shouldCheckOutputDirectories = false
   override def updateAction = task { None }

   lazy val main  = project( ".", "ScalaAndroidCollider", new MainProject( _ ))
// lazy val tests = project("tests",  "tests", new TestProject( _ ), main)

   class MainProject( info: ProjectInfo ) extends AndroidProject( info ) with Defaults with MarketPublish {
      val keyalias  = "change-me"
//    val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test"
//      val scalaosc      = "de.sciss" %% "scalaosc" % "0.20"
      val scalacollider = "de.sciss" %% "scalacollider" % "0.20"
   }

// class TestProject(info: ProjectInfo) extends AndroidTestProject(info) with Defaults
}