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

package io.cdap.wrangler.parser;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;

import io.cdap.wrangler.api.CompileException;
import io.cdap.wrangler.api.CompileStatus;
import io.cdap.wrangler.api.Compiler;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Tests {@link RecipeCompiler}
 */
public class RecipeCompilerTest {

  private static final Compiler compiler = new RecipeCompiler();

  @Test
  public void testSuccessCompilation() throws Exception {
    try {
      Compiler compiler = new RecipeCompiler();
      CompileStatus status = compiler.compile(
          "parse-as-csv :body ' ' true;\n"
        + "set-column :abc, :edf;\n"
        + "send-to-error exp:{ window < 10 } ;\n"
        + "parse-as-simple-date :col 'yyyy-mm-dd' :col 'test' :col2,:col4,:col9 10 exp:{test < 10};\n"
      );

      Assert.assertNotNull(status.getSymbols());
      Assert.assertEquals(4, status.getSymbols().size());
    } catch (CompileException e) {
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testMacroSkippingDuringParsing() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv :body ',' true;",
      "${macro1}",
      "${macro${number}}",
      "parse-as-csv :body '${delimiter}' true;"
    };

    CompileStatus status = TestingRig.compile(recipe);
    Assert.assertEquals(true, status.isSuccess());
  }

  @Test
  public void testSingleMacroLikeWranglerPlugin() throws Exception {
    String[] recipe = new String[] {
      "${directives}"
    };

    CompileStatus status = TestingRig.compile(recipe);
    Assert.assertEquals(true, status.isSuccess());
  }

  @Test
  public void testSparedPragmaLoadDirectives() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "${directives}",
      "#pragma load-directives root1,root2,root3;"
    };
    TestingRig.compileSuccess(recipe);
  }

  @Test
  public void testNestedMacros() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "${directives_${number}}"
    };
    TestingRig.compileSuccess(recipe);
  }

  @Test
  public void testSemiColonMissing() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5",
      "${directives_${number}}"
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testMissingOpenBraceOnMacro() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "$directives}"
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testMissingCloseBraceOnMacro() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "${directives"
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testMissingBothBraceOnMacro() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "${directives"
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testMissingPragmaHash() throws Exception {
    String[] recipe = new String[] {
      "pragma load-directives test1,test2,test3,test4,test5;",
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testTypograhicalErrorPragmaLoadDirectives() throws Exception {
    String[] recipe = new String[] {
      "pragma test1,test2,test3,test4,test5;",
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testWithIfStatement() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2;",
      "${macro_1}",
      "if ((test > 10) && (window < 20)) {  parse-as-csv :body ',' true; if (window > 10) " +
        "{ send-to-error exp:{test > 10}; } }"
    };
    TestingRig.compileSuccess(recipe);
  }

  @Test
  public void testComplexExpression() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv body , true",
      "drop body",
      "merge body_1 body_2 Full_Name ' '",
      "drop body_1,body_2",
      "find-and-replace body_4 s/Washington//g",
      "send-to-error empty(body_4)",
      "send-to-error body_5 =~ \"DC.*\"",
      "filter-rows-on regex-match body_5 *as*"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Assert.assertTrue(true);
  }

  @Test
  public void test() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv body , true",
      "drop body",
      "merge body_1 body_2 Full_Name ' '",
      "drop body_1,body_2",
      "find-and-replace body_4 s/Washington//g",
      "send-to-error empty(body_4)",
      "send-to-error body_5 =~ \"DC.*\"",
      "filter-rows-on regex-match body_5 *as*"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Assert.assertTrue(true);
  }

  @Test
  public void testSingleLineDirectives() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv :body '\t' true; drop :body;"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Assert.assertTrue(true);
  }

  @Test
  public void testError() throws Exception {
    String[] recipe = new String[] {
      "parse-as-abababa-csv :body '\t' true; drop :body;"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Assert.assertTrue(true);
  }

  @Test
  public void testRecipePragmaWithCompiler() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4;",
      "${directives}"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Set<String> loadableDirectives = compile.getSymbols().getLoadableDirectives();
    Assert.assertEquals(4, loadableDirectives.size());
  }
  
  @Test
  public void testAggregateByteSizeTimeDirective() throws Exception {
    // Prepare sample rows with the following columns:
    // "data_transfer_size" with values like "10KB", "1MB", "512B"
    // "response_time" with values like "500ms", "2s", "150ms"
    List<Row> rows = new ArrayList<>();

    Row row1 = new Row();
    row1.add("data_transfer_size", "10KB");  // 10KB = 10 * 1024 = 10240 bytes
    row1.add("response_time", "500ms");      // 500ms = 500 milliseconds
    rows.add(row1);

    Row row2 = new Row();
    row2.add("data_transfer_size", "1MB");     // 1MB = 1048576 bytes
    row2.add("response_time", "2s");           // 2s = 2000 milliseconds
    rows.add(row2);

    Row row3 = new Row();
    row3.add("data_transfer_size", "512B");    // 512B = 512 bytes
    row3.add("response_time", "150ms");        // 150ms = 150 milliseconds
    rows.add(row3);

    // Total bytes = 10240 + 1048576 + 512 = 1051328 bytes 
    // Total time in ms = 500 + 2000 + 150 = 2650 ms
    // Converted total size in MB = 1051328 / (1024*1024) ≈ 1.002 MB
    // Converted total time in seconds = 2650 / 1000 = 2.65 seconds

    // Define a directive recipe for ByteSizeTimeAggregator.
    // For example: 
    // "byte-size-time-aggregator :data_transfer_size :response_time total_size_mb total_time_sec MB seconds total"
    String[] recipe = new String[] {
      "byte-size-time-aggregator :data_transfer_size :response_time total_size_mb total_time_sec MB seconds total"
    };

    // Execute the recipe using TestingRig (which should parse and execute the directive)
    List<Row> results = TestingRig.execute(recipe, rows);

    // We expect a single aggregated row as output.
    Assert.assertEquals(1, results.size());
    Row aggregated = results.get(0);

    // Retrieve aggregated values from the result row.
    // They should be stored under the target column names defined in the recipe.
    Object sizeObj = aggregated.getValue("total_size_mb");
    Object timeObj = aggregated.getValue("total_time_sec");

    // Assuming the aggregator outputs results as Doubles:
    double totalSizeMB = (sizeObj instanceof Number) ? ((Number) sizeObj).doubleValue() : Double.parseDouble(sizeObj.toString());
    double totalTimeSec = (timeObj instanceof Number) ? ((Number) timeObj).doubleValue() : Double.parseDouble(timeObj.toString());

    // Verify the results with appropriate tolerances.
    Assert.assertEquals(1.002, totalSizeMB, 0.01);
    Assert.assertEquals(2.65, totalTimeSec, 0.01);
  }
  
  // ... any further test methods ...
}
