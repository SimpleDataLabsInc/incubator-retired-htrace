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

package org.apache.htrace.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.htrace.Span;
import org.apache.htrace.SpanReceiver;
import org.apache.htrace.TimelineAnnotation;
import org.apache.htrace.TraceCreator;
import org.apache.htrace.TraceTree.SpansByParent;
import org.apache.htrace.TraceTree;
import org.apache.htrace.protobuf.generated.SpanProtos;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Multimap;


public class TestHBaseSpanReceiver {
  private static final Log LOG = LogFactory.getLog(TestHBaseSpanReceiver.class);
  private static final HBaseTestingUtility UTIL = new HBaseTestingUtility();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UTIL.startMiniCluster(1);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    UTIL.shutdownMiniCluster();
  }

  // Reenable after fix circular dependency
  @Ignore @Test
  public void testHBaseSpanReceiver() {
    Table htable = HBaseTestUtil.createTable(UTIL);
    SpanReceiver receiver = HBaseTestUtil.startReceiver(UTIL);
    TraceCreator tc = new TraceCreator(receiver);
    tc.createThreadedTrace();
    tc.createSimpleTrace();
    tc.createSampleRpcTrace();
    HBaseTestUtil.stopReceiver(receiver);
    Scan scan = new Scan();
    scan.addFamily(Bytes.toBytes(HBaseSpanReceiver.DEFAULT_COLUMNFAMILY));
    scan.setMaxVersions(1);
    ArrayList<Span> spans = new ArrayList<Span>();
    try {
      ResultScanner scanner = htable.getScanner(scan);
      Result result = null;
      while ((result = scanner.next()) != null) {
        for (Cell cell : result.listCells()) {
          InputStream in = new ByteArrayInputStream(cell.getQualifierArray(),
                                                    cell.getQualifierOffset(),
                                                    cell.getQualifierLength());
          spans.add(new TestSpan(SpanProtos.Span.parseFrom(in)));
        }
      }
    } catch (IOException e) {
      Assert.fail("failed to get spans from HBase. " + e.getMessage());
    }

    TraceTree traceTree = new TraceTree(spans);
    Collection<Span> roots =
        traceTree.getSpansByParent().find(Span.ROOT_SPAN_ID);
    Assert.assertTrue("Trace tree must have roots", !roots.isEmpty());
    Assert.assertEquals(3, roots.size());

    Map<String, Span> descs = new HashMap<String, Span>();
    for (Span root : roots) {
      descs.put(root.getDescription(), root);
    }
    Assert.assertTrue(descs.keySet().contains(TraceCreator.RPC_TRACE_ROOT));
    Assert.assertTrue(descs.keySet().contains(TraceCreator.SIMPLE_TRACE_ROOT));
    Assert.assertTrue(descs.keySet().contains(TraceCreator.THREADED_TRACE_ROOT));

    SpansByParent spansByParentId = traceTree.getSpansByParent();
    Span rpcRoot = descs.get(TraceCreator.RPC_TRACE_ROOT);
    Assert.assertEquals(1, spansByParentId.find(rpcRoot.getSpanId()).size());
    Span rpcChild1 = spansByParentId.find(rpcRoot.getSpanId()).iterator().next();
    Assert.assertEquals(1, spansByParentId.find(rpcChild1.getSpanId()).size());
    Span rpcChild2 = spansByParentId.find(rpcChild1.getSpanId()).iterator().next();
    Assert.assertEquals(1, spansByParentId.find(rpcChild2.getSpanId()).size());
    Span rpcChild3 = spansByParentId.find(rpcChild2.getSpanId()).iterator().next();
    Assert.assertEquals(0, spansByParentId.find(rpcChild3.getSpanId()).size());

    Scan iscan = new Scan();
    iscan.addColumn(Bytes.toBytes(HBaseSpanReceiver.DEFAULT_INDEXFAMILY),
                    HBaseSpanReceiver.INDEX_SPAN_QUAL);
    try {
      ResultScanner scanner = htable.getScanner(iscan);
      Result result = null;
      while ((result = scanner.next()) != null) {
        for (Cell cell : result.listCells()) {
          InputStream in = new ByteArrayInputStream(cell.getValueArray(),
                                                    cell.getValueOffset(),
                                                    cell.getValueLength());
          Assert.assertEquals(SpanProtos.Span.parseFrom(in).getParentId(),
                              Span.ROOT_SPAN_ID);
        }
      }
    } catch (IOException e) {
      Assert.fail("failed to get spans from index family. " + e.getMessage());
    }
  }

  private class TestSpan implements Span {
    SpanProtos.Span span;

    public TestSpan(SpanProtos.Span span) {
      this.span = span;
    }

    @Override
    public long getTraceId() {
      return span.getTraceId();
    }

    @Override
    public long getParentId() {
      return span.getParentId();
    }

    @Override
    public long getStartTimeMillis() {
      return span.getStart();
    }

    @Override
    public long getStopTimeMillis() {
      return span.getStop();
    }

    @Override
    public long getSpanId() {
      return span.getSpanId();
    }

    @Override
    public String getProcessId() {
      return span.getProcessId();
    }

    @Override
    public String getDescription() {
      return span.getDescription();
    }

    @Override
    public String toString() {
      return String.format("Span{Id:0x%16x,parentId:0x%16x,pid:%s,desc:%s}",
                           getSpanId(), getParentId(),
                           getProcessId(), getDescription());
    }

    @Override
    public Map<byte[], byte[]> getKVAnnotations() {
      return Collections.emptyMap();
    }

    @Override
    public List<TimelineAnnotation> getTimelineAnnotations() {
      return Collections.emptyList();
    }

    @Override
    public void addKVAnnotation(byte[] key, byte[] value) {}

    @Override
    public void addTimelineAnnotation(String msg) {}

    @Override
    public synchronized void stop() {}

    @Override
    public synchronized boolean isRunning() {
      return false;
    }

    @Override
    public synchronized long getAccumulatedMillis() {
      return span.getStop() - span.getStart();
    }

    @Override
    public Span child(String description) {
      return null;
    }

    @Override
    public String toJson() { return null; }
  }
}
