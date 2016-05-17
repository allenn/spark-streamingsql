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
package org.apache.spark.streaming

import org.apache.spark.SparkContext
import org.apache.spark.rdd.EmptyRDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.streaming.dstream.DStream

/**
  * Created by francisco.aranda@gft.com on 12/5/16.
  */
object StreamComputation {

  private[spark] object DStreamHelper {
    var validTime: Time = null

    def setValidTime(time: Time): Unit = {
      validTime = time
    }
  }

  import DStreamHelper._

  implicit class GetOrCompute(stream: DStream[InternalRow]) {

    def execute(sparkContext: SparkContext) = {
      assert(validTime != null)

      stream.getOrCompute(validTime)
        .getOrElse(new EmptyRDD[InternalRow](sparkContext))
    }

  }


}
