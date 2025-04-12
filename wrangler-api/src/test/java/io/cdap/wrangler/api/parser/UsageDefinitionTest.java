/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.wrangler.api.parser;

import io.cdap.wrangler.api.Optional;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link UsageDefinition}
 */
public class UsageDefinitionTest {

  @Test
  public void testUsageDefinitionCreation() {
    UsageDefinition.Builder builder = UsageDefinition.builder("test");
    builder.define("fname", TokenType.COLUMN_NAME);
    builder.define("lname", TokenType.COLUMN_NAME, Optional.TRUE);
    builder.define("condition", TokenType.EXPRESSION, Optional.TRUE);
    UsageDefinition definition = builder.build();
    Assert.assertEquals("test", definition.getDirectiveName());
  }

  @Test
  public void testUsageStringCreation() {
    List<String> usages = new ArrayList<>();

    UsageDefinition.Builder builder = UsageDefinition.builder("rename");
    builder.define("source", TokenType.COLUMN_NAME);
    builder.define("destination", TokenType.COLUMN_NAME);
    String usage = builder.build().toString();
    Assert.assertEquals("rename :source :destination", usage);
    usages.add(usage);

    builder = UsageDefinition.builder("parse-as-csv");
    builder.define("col", TokenType.COLUMN_NAME);
    builder.define("delimiter", TokenType.TEXT, Optional.TRUE);
    builder.define("header", TokenType.BOOLEAN, Optional.TRUE);
    usage = builder.build().toString();
    Assert.assertEquals("parse-as-csv :col  ['delimiter'] [header (true/false)]", usage);
    usages.add(usage);

    builder = UsageDefinition.builder("send-to-error");
    builder.define("expr", TokenType.EXPRESSION);
    builder.define("metric", TokenType.TEXT, Optional.TRUE);
    usage = builder.build().toString();
    Assert.assertEquals("send-to-error exp:{<expr>}  ['metric']", usage);
    usages.add(usage);

    builder = UsageDefinition.builder("set-columns");
    builder.define("cols", TokenType.COLUMN_NAME_LIST);
    usage = builder.build().toString();
    Assert.assertEquals("set-columns :cols [,:cols  ]*", usage);
    usages.add(usage);

    Assert.assertTrue(true);
  }
  
//  @Test
//  public void testByteSizeParsing() {
//    ByteSize size1 = new ByteSize("10KB");
//    ByteSize size2 = new ByteSize("2MB");
//    ByteSize size3 = new ByteSize("1GB");
//
//    Assert.assertEquals(10 * 1024, size1.getBytes());
//    Assert.assertEquals(2 * 1024 * 1024, size2.getBytes());
//    Assert.assertEquals(1L * 1024 * 1024 * 1024, size3.getBytes());
//  }
//
//  @Test
//  public void testTimeDurationParsing() {
//    TimeDuration time1 = new TimeDuration("500ms");
//    TimeDuration time2 = new TimeDuration("2s");
//    TimeDuration time3 = new TimeDuration("3m");
//
//    Assert.assertEquals(500, time1.getMilliseconds());
//    Assert.assertEquals(2000, time2.getMilliseconds());
//    Assert.assertEquals(180000, time3.getMilliseconds());
//  }
//
//  @Test
//  public void testInvalidByteSize() {
//    Exception exception = Assert.assertThrows(IllegalArgumentException.class, () -> new ByteSize("10XYZ"));
//    Assert.assertTrue(exception.getMessage().contains("Invalid byte size format"));
//  }
//
//  @Test
//  public void testInvalidTimeDuration() {
//    Exception exception = Assert.assertThrows(IllegalArgumentException.class, () -> new TimeDuration("100XYZ"));
//    Assert.assertTrue(exception.getMessage().contains("Invalid time duration format"));
//  }
  
}
