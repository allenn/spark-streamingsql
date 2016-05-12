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

package org.apache.spark.sql.catalyst

import org.apache.spark.sql.catalyst.analysis._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.streaming.WindowedLogicalPlan
import org.apache.spark.sql.catalyst.util.DataTypeParser
import org.apache.spark.streaming.{Duration, Milliseconds, Minutes, Seconds}


import scala.language.implicitConversions

/**
  * Stream SQL parser to extend existed sql parser with time-based window support. Query can be
  * written as:
  * "SELECT * FROM table OVER (WINDOW '6' SECONDS, SLIDE '3' SECONDS)" GROUP BY ...
  *
  * Definition of time-based window semantics:
  *
  * OVER ( WINDOW 'stringLit' MILLISECONDS|SECONDS|MINUTES \
  * [, SLIDE 'stringLit' MILLISECONDS|SECONDS|MINUTES])
  *
  * The time-based window support is different from SQL standard row-based window function,
  * in which time-based window is a constraint of stream relation, so it must be followed by
  * " FROM streamized_table OVER (...), currently it has some limitations:
  * 1. WINDOW alias like SELECT ... FROM table OVER w ... WINDOW w (WINDOW "6" SECONDS,
  * ...) is not supported yet.
  * 2. for windowed join, two streamized table need to have same window constraint,
  * it is the constraint of Spark Streaming.
  * 3. Mix time-based window and row-based window is not supported yet.
  */

object StreamSqlParser extends AbstractSparkSQLParser with DataTypeParser with SqlParser {
  def apply(input: String, exceptionOnError: Boolean): Option[LogicalPlan] = {
    try {
      Some(parse(input))
    } catch {
      case _ if !exceptionOnError => None
      case x: Throwable => throw x
    }
  }

  protected def OVER = Keyword("OVER")
  protected def WINDOW = Keyword("WINDOW")
  protected def SLIDE = Keyword("SLIDE")

  protected def MILLISECONDS = Keyword("MILLISECONDS")
  protected def SECONDS = Keyword("SECONDS")
  protected def MINUTES = Keyword("MINUTES")

  protected lazy val durationType: Parser[Duration] =
    ( stringLit <~ MILLISECONDS ^^ { case s => Milliseconds(s.toInt) }
      | stringLit <~ SECONDS ^^ { case s => Seconds(s.toInt) }
      | stringLit <~ MINUTES ^^ { case s => Minutes(s.toInt) })

  protected lazy val windowOptions: Parser[(Duration, Option[Duration])] =
    OVER ~ "(" ~> ( WINDOW ~> durationType) ~
      ( "," ~ SLIDE ~> durationType).? <~ ")" ^^ {
      case w ~ s => (w, s)
    }

  protected override lazy val relationFactor: Parser[LogicalPlan] =
    ( tableIdentifier ~ windowOptions.? ~ (opt(AS) ~> opt(ident)) ^^ {
      case tableIdent ~ window ~ alias => window.map { w =>

        WindowedLogicalPlan(
          w._1,
          w._2,
          UnresolvedRelation(tableIdent, alias))
      }.getOrElse(UnresolvedRelation(tableIdent, alias))
    }
      | ("(" ~> start <~ ")") ~ windowOptions.? ~ (AS.? ~> ident) ^^ {
      case s ~ w ~ a => w.map { x =>
        WindowedLogicalPlan(
          x._1,
          x._2,
          Subquery(a, s))
      }.getOrElse(Subquery(a, s))
    })
}
