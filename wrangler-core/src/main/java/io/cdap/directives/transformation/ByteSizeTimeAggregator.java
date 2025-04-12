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

package io.cdap.directives.transformation;

import java.util.List;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.ReportErrorAndProceed;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.lineage.Lineage;
import io.cdap.wrangler.api.lineage.Mutation;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;

//package io.cdap.wrangler.directive;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveContext;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
//import io.cdap.wrangler.api.Lineage;
//import io.cdap.wrangler.api.Mutation;
//import io.cdap.wrangler.api.Row;
//import io.cdap.wrangler.api.UsageDefinition;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.annotations.Categories;
//import io.cdap.wrangler.api.annotations.Description;
//import io.cdap.wrangler.api.annotations.Name;
//import io.cdap.wrangler.api.annotations.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates byte size and time duration columns across rows,
 * returning a single row with final totals (or averages) in specified units.
 */
@Plugin(type = Directive.TYPE)
@Name(ByteSizeTimeAggregator.NAME)
@Categories(categories = { "transform" })
@Description("Aggregates a byte size column and a time duration column across all rows and outputs a single row with the totals or averages.")
public class ByteSizeTimeAggregator implements Directive, Lineage {

  public static final String NAME = "byte-size-time-aggregator";

  // Required arguments
  private String sourceByteColumn;     // e.g. "data_transfer_size"
  private String sourceTimeColumn;     // e.g. "response_time"
  private String targetSizeColumn;     // e.g. "total_size_mb"
  private String targetTimeColumn;     // e.g. "total_time_sec"

  // Optional arguments
  private String sizeUnit;             // e.g. "MB", "GB"
  private String timeUnit;             // e.g. "seconds", "minutes"
  private String aggregationType;      // "total" or "average"

  // Accumulators
  private long totalBytes = 0L;
  private long totalNanos = 0L;
  private int rowCount = 0;

  private boolean finalized = false; // Ensure we only finalize once

  @Override
  public UsageDefinition define() {
    // Build usage definition with 4 required + 3 optional arguments
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);

    // 1. source column with byte sizes
    builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
    // 2. source column with time durations
    builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
    // 3. target column name for total size
    builder.define("targetSizeColumn", TokenType.COLUMN_NAME);
    // 4. target column name for total or average time
    builder.define("targetTimeColumn", TokenType.COLUMN_NAME);

    // Optional: output unit for bytes (MB, GB), for time (seconds, minutes)
    builder.define("sizeUnit", TokenType.TEXT, true);
    builder.define("timeUnit", TokenType.TEXT, true);
    // Optional: aggregation type (total, average)
    builder.define("aggregationType", TokenType.TEXT, true);

    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    // Parse required columns
    this.sourceByteColumn = ((ColumnName) args.value("byteSizeColumn")).value();
    this.sourceTimeColumn = ((ColumnName) args.value("timeDurationColumn")).value();
    this.targetSizeColumn = ((ColumnName) args.value("targetSizeColumn")).value();
    this.targetTimeColumn = ((ColumnName) args.value("targetTimeColumn")).value();

    // Optional arguments
    this.sizeUnit = parseOptionalText(args, "sizeUnit");       // default: no unit conversion
    this.timeUnit = parseOptionalText(args, "timeUnit");       // default: no unit conversion
    this.aggregationType = parseOptionalText(args, "aggregationType"); // default: "total"

    // Validate aggregation type
    if (aggregationType == null || aggregationType.isEmpty()) {
      aggregationType = "total";
    }
    else {
      aggregationType = aggregationType.toLowerCase();
      if (!aggregationType.equals("total") && !aggregationType.equals("average")) {
        throw new DirectiveParseException(NAME, "aggregationType must be 'total' or 'average' if specified.");
      }
    }
  }

  /**
   * Parse an optional argument of type TEXT, returning null if not specified.
   */
  private String parseOptionalText(Arguments args, String fieldName) {
    Object val = args.value(fieldName);
    if (val == null) {
      return null; // not provided
    }
    return ((Text) val).value();
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    // For each row, read byte/time columns, convert to canonical units, accumulate
    for (Row row : rows) {
      // Find indices
      int byteIdx = row.find(sourceByteColumn);
      int timeIdx = row.find(sourceTimeColumn);

      if (byteIdx == -1 || timeIdx == -1) {
        // skip row if columns not found
        continue;
      }

      Object byteVal = row.getValue(byteIdx);
      Object timeVal = row.getValue(timeIdx);

      if (byteVal == null || timeVal == null) {
        // skip row if null
        continue;
      }

      // Convert to bytes
      long bytes = parseBytes(byteVal.toString());
      // Convert to nanoseconds
      long nanos = parseDurationToNano(timeVal.toString());

      totalBytes += bytes;
      totalNanos += nanos;
      rowCount++;
    }

    // Return empty list during "execute" because the final single row is produced in finalize
    return rows;
  }

//  @Override
  public List<Row> finalize(ExecutorContext context) throws DirectiveExecutionException {
    if (finalized) {
      // Ensure finalize not called multiple times
      return new ArrayList<>();
    }
    finalized = true;

    // Prepare the final aggregated values
    long finalBytes = totalBytes;
    long finalNanos = totalNanos;

    if (aggregationType.equals("average") && rowCount > 0) {
      finalBytes = finalBytes / rowCount;
      finalNanos = finalNanos / rowCount;
    }

    // Convert final bytes to requested sizeUnit
    double convertedSize = convertBytes(finalBytes, sizeUnit);
    // Convert final nanos to requested timeUnit
    double convertedTime = convertNanos(finalNanos, timeUnit);

    // Return a single new row
    Row result = new Row();
    result.add(targetSizeColumn, convertedSize);
    result.add(targetTimeColumn, convertedTime);

    List<Row> output = new ArrayList<>();
    output.add(result);
    return output;
  }

  @Override
  public void destroy() {
    // no-op
  }

  // --------------- Helper Methods ---------------

  private long parseBytes(String val) {
    val = val.trim().toUpperCase();
    if (val.endsWith("KB")) {
      return (long) (Double.parseDouble(val.replace("KB", "")) * 1024);
    } else if (val.endsWith("MB")) {
      return (long) (Double.parseDouble(val.replace("MB", "")) * 1024 * 1024);
    } else if (val.endsWith("GB")) {
      return (long) (Double.parseDouble(val.replace("GB", "")) * 1024 * 1024 * 1024);
    } else if (val.endsWith("B")) {
      return (long) Double.parseDouble(val.replace("B", ""));
    }
    // Assume raw bytes if no suffix
    return (long) Double.parseDouble(val);
  }

  private long parseDurationToNano(String val) {
    val = val.trim().toLowerCase();
    if (val.endsWith("ms")) {
      double ms = Double.parseDouble(val.replace("ms", "").trim());
      return (long) (ms * 1_000_000);
    } else if (val.endsWith("s")) {
      double seconds = Double.parseDouble(val.replace("s", "").trim());
      return (long) (seconds * 1_000_000_000);
    } else if (val.endsWith("m")) {
      double minutes = Double.parseDouble(val.replace("m", "").trim());
      return (long) (minutes * 60 * 1_000_000_000);
    } else if (val.endsWith("h")) {
      double hours = Double.parseDouble(val.replace("h", "").trim());
      return (long) (hours * 3600 * 1_000_000_000);
    }
    // Assume raw nanoseconds if no suffix
    return (long) Double.parseDouble(val);
  }

  private double convertBytes(long bytes, String unit) {
    if (unit == null || unit.isEmpty()) {
      // no conversion
      return bytes;
    }
    switch (unit.toLowerCase()) {
      case "mb":
        return bytes / (1024.0 * 1024.0);
      case "gb":
        return bytes / (1024.0 * 1024.0 * 1024.0);
      default:
        return bytes; // no recognized unit
    }
  }

  private double convertNanos(long nanos, String unit) {
    if (unit == null || unit.isEmpty()) {
      // no conversion
      return nanos;
    }
    switch (unit.toLowerCase()) {
      case "seconds":
        return nanos / 1_000_000_000.0;
      case "minutes":
        return nanos / (60.0 * 1_000_000_000.0);
      default:
        return nanos; // no recognized unit
    }
  }

  // --------------- Lineage ---------------

  @Override
  public Mutation lineage() {
    return Mutation.builder()
      .readable("Aggregated '%s' and '%s' into '%s' and '%s' with type=%s, sizeUnit=%s, timeUnit=%s",
        sourceByteColumn, sourceTimeColumn, targetSizeColumn, targetTimeColumn,
        aggregationType, sizeUnit, timeUnit)
      .relation(sourceByteColumn, targetSizeColumn)
      .relation(sourceTimeColumn, targetTimeColumn)
      .build();
  }
}
