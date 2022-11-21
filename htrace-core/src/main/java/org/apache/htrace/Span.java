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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Base interface for gathering and reporting statistics about a block of
 * execution.
 * <p>
 * Spans form a tree structure with the parent relationship. The first span in a
 * trace has no parent span.
 */
@JsonSerialize(using = Span.SpanSerializer.class)
public interface Span {
  public static final long ROOT_SPAN_ID = 0x74ace;

  /**
   * The block has completed, stop the clock
   */
  void stop();

  /**
   * Get the start time, in milliseconds
   */
  long getStartTimeMillis();

  /**
   * Get the stop time, in milliseconds
   */
  long getStopTimeMillis();

  /**
   * Return the total amount of time elapsed since start was called, if running,
   * or difference between stop and start
   */
  long getAccumulatedMillis();

  /**
   * Has the span been started and not yet stopped?
   */
  boolean isRunning();

  /**
   * Return a textual description of this span
   */
  String getDescription();

  /**
   * A pseudo-unique (random) number assigned to this span instance
   */
  long getSpanId();

  /**
   * A pseudo-unique (random) number assigned to the trace associated with this
   * span
   */
  long getTraceId();

  /**
   * Create a child span of this span with the given description
   */
  Span child(String description);

  @Override
  String toString();

  /**
   * Return the pseudo-unique (random) number of the first parent span, returns
   * ROOT_SPAN_ID if there are no parents.
   */
  long getParentId();

  /**
   * Add a data annotation associated with this span
   */
  void addKVAnnotation(byte[] key, byte[] value);

  /**
   * Add a timeline annotation associated with this span
   */
  void addTimelineAnnotation(String msg);

  /**
   * Get data associated with this span (read only)
   */
  Map<byte[], byte[]> getKVAnnotations();

  /**
   * Get any timeline annotations (read only)
   */
  List<TimelineAnnotation> getTimelineAnnotations();

  /**
   * Return a unique id for the node or process from which this Span originated.
   * IP address is a reasonable choice.
   *
   * @return
   */
  String getProcessId();

  /**
   * Serialize to Json
   */
  String toJson();

  public static class SpanSerializer extends JsonSerializer<Span> {
    @Override
    public void serialize(Span span, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeStartObject();
      jgen.writeStringField("i", String.format("%016x", span.getTraceId()));
      jgen.writeStringField("s", String.format("%016x", span.getSpanId()));
      jgen.writeNumberField("b", span.getStartTimeMillis());
      jgen.writeNumberField("e", span.getStopTimeMillis());
      jgen.writeStringField("d", span.getDescription());
      jgen.writeStringField("r", span.getProcessId());
      jgen.writeArrayFieldStart("p");
      if (span.getParentId() != ROOT_SPAN_ID) {
        jgen.writeString(String.format("%016x", span.getParentId()));
      }
      jgen.writeEndArray();
      Map<byte[], byte[]> traceInfoMap = span.getKVAnnotations();
      if (!traceInfoMap.isEmpty()) {
        jgen.writeObjectFieldStart("n");
        for (Map.Entry<byte[], byte[]> e : traceInfoMap.entrySet()) {
          jgen.writeStringField(new String(e.getKey(), "UTF-8"),
              new String(e.getValue(), "UTF-8"));
        }
        jgen.writeEndObject();
      }
      List<TimelineAnnotation> timelineAnnotations =
          span.getTimelineAnnotations();
      if (!timelineAnnotations.isEmpty()) {
        jgen.writeArrayFieldStart("t");
        for (TimelineAnnotation tl : timelineAnnotations) {
          jgen.writeStartObject();
          jgen.writeNumberField("t", tl.getTime());
          jgen.writeStringField("m", tl.getMessage());
          jgen.writeEndObject();
        }
        jgen.writeEndArray();
      }
      jgen.writeEndObject();
    }
  }
}
