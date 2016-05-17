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

import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution.datasources.LogicalRelation
import org.apache.spark.sql.execution.{SparkPlan, SparkPlanner}
import org.apache.spark.sql.{SQLContext, Strategy}
import org.apache.spark.streaming.Time

/** Stream related strategies to map stream specific logical plan to physical plan. */
class StreamStrategies(sqlContext: SQLContext) extends SparkPlanner(sqlContext) {

  override def strategies: Seq[Strategy] = Seq(StreamStrategy) ++ super.strategies

  object StreamStrategy extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case LogicalDStream(output, stream) => PhysicalDStream(output, stream) :: Nil
      case x@WindowedLogicalPlan(w, s, child) =>
        WindowedPhysicalPlan(w, s, planLater(child)) :: Nil
      case l@LogicalRelation(t: StreamPlan, _) =>
        PhysicalDStream(l.output, t.stream) :: Nil
      case _ => Nil
    }
  }

}

