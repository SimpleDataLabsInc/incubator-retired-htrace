/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.htrace;

import org.apache.htrace.impl.AlwaysSampler;
import org.apache.htrace.impl.NeverSampler;

/**
 * Extremely simple callback to determine the frequency that an action should be
 * performed.
 * <p>
 * 'T' is the object type you require to create a more advanced sampling
 * function. For example if there is some RPC information in a 'Call' object,
 * you might implement Sampler Call generic. Then when the RPC is received you can call
 * one of the Trace.java functions that takes the extra 'info' parameter, which
 * will be passed into the next function you implemented.
 * <p>
 * For the example above, the next(T info) function may look like this
 * <p>
 * <pre>
 * <code>public boolean next(T info) {
 *   if (info == null) {
 *     return false;
 *   } else if (info.getName().equals("get")) {
 *     return Math.random() greaterthan 0.5;
 *   } else if (info.getName().equals("put")) {
 *     return Math.random() greaterthan 0.25;
 *   } else {
 *     return false;
 *   }
 * }
 * </code>
 * </pre>
 * This would trace 50% of all gets, 75% of all puts and would not trace any other requests.
 */
public interface Sampler<T> {

  public static final Sampler<?> ALWAYS = AlwaysSampler.INSTANCE;
  public static final Sampler<?> NEVER = NeverSampler.INSTANCE;

  public boolean next(T info);

}
