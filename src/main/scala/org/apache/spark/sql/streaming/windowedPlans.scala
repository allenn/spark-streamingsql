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

package org.apache.spark.sql.streaming

import org.apache.spark.rdd.{EmptyRDD, RDD}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.plans.logical
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.streaming.{Duration, StreamComputation, Time}
import org.apache.spark.streaming.dstream.DStream

/**
 * Logical and physical plan of time-based window.
 */
case class WindowedLogicalPlan(
    windowDuration: Duration,
    slideDuration: Option[Duration],
    child: LogicalPlan)
  extends logical.UnaryNode {
  override def output = child.output
}

case class WindowedPhysicalPlan(
    windowDuration: Duration,
    slideDuration: Option[Duration],
    child: SparkPlan)
  extends execution.UnaryNode with StreamPlan {

  @transient private val wrappedStream =
    new DStream[InternalRow](streamSqlContext.streamingContext) {
    override def dependencies = parentStreams.toList
    override def slideDuration: Duration = parentStreams.head.slideDuration
    override def compute(validTime: Time): Option[RDD[InternalRow]] = Some(child.execute())

    private lazy val parentStreams = {
      def traverse(plan: SparkPlan): Seq[DStream[InternalRow]] = plan match {
          case x: StreamPlan => Seq(x.stream)
          case _ => plan.children.flatMap(traverse(_))
      }

      val streams = traverse(child)
      assert(!streams.isEmpty, s"Input query and related plan $child is not a stream plan")
      streams
    }
  }

  @transient val stream = slideDuration.map(wrappedStream.window(windowDuration, _))
    .getOrElse(wrappedStream.window(windowDuration))

  override def output = child.output
  import StreamComputation._
  import DStreamHelper._

  override def doExecute() = {

    Utils.invoke(classOf[DStream[InternalRow]], stream, "getOrCompute", (classOf[Time], validTime))
      .asInstanceOf[Option[RDD[InternalRow]]]
      .getOrElse(new EmptyRDD[InternalRow](sparkContext))
  }

}
