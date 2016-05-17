/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.typesafe.sbt.SbtScalariform._
import org.scalastyle.sbt.ScalastylePlugin
import sbt.Keys._
import sbt._

import scalariform.formatter.preferences._

object Properties {
  val SPARK_VERSION = "1.6.0"
  val provided = false
}


object StreamSQLBuild extends Build {

  import Dependencies._
  import Properties._

  lazy val root = Project(id = "spark", base = file("."),
    settings = commonSettings ++ Seq(
      name := s"spark-streaming-sql_${SPARK_VERSION}",
      description := "Spark stream sql extension",
      libraryDependencies ++= sparkDeps ++ testDeps,
      parallelExecution in Test := false,
      publishMavenStyle := true,
      publishTo := Some(
        Resolver.file(
          "local-repo", new File(Path.userHome.getAbsolutePath + "/.m2/repository"
          )
        )
      )
    )
  )

  lazy val runScalaStyle = taskKey[Unit]("testScalaStyle")

  // rat task need to be added later.
  lazy val runRat = taskKey[Unit]("run-rat-task")
  lazy val runRatTask = runRat := {
    "bin/run-rat.sh" !
  }


  lazy val commonSettings = Seq(

    crossPaths := false,
    scalaVersion := "2.10.4",
    scalaBinaryVersion := "2.10",
    retrieveManaged := true,
    retrievePattern := "[type]s/[artifact](-[revision])(-[classifier]).[ext]",

    organization := "org.apache.spark",
    name := s"spark-streamingSql_${scalaBinaryVersion.value}",
    version := s"${SPARK_VERSION}_0.1.0",

    runScalaStyle := {
      org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
    },

    (compile in Compile) <<= (compile in Compile) dependsOn runScalaStyle,

    scalacOptions := Seq("-deprecation",
      "-feature",
      "-language:implicitConversions",
      "-language:postfixOps"),
    resolvers ++= Dependencies.repos,
    parallelExecution in Test := false

  ) ++ scalariformPrefs ++ ScalastylePlugin.Settings

  lazy val scalariformPrefs = defaultScalariformSettings ++ Seq(
    ScalariformKeys.preferences := FormattingPreferences()
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, false)
  )

}
