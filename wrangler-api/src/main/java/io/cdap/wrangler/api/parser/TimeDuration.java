package io.cdap.wrangler.api.parser;


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

/**
 * Represents a TimeDuration token, used to parse values like "5s", "2m", "1h".
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PublicEvolving
public class TimeDuration implements Token {
    private final long milliseconds;

    private static final Pattern TIME_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)(ms|s|m|h|d)?");

    public TimeDuration(String value) {
        this.milliseconds = parseTimeDuration(value);
    }

    private static long parseTimeDuration(String value) {
        Matcher matcher = TIME_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time duration format: " + value);
        }

        double duration = Double.parseDouble(matcher.group(1));
        String unit = (matcher.group(2) != null) ? matcher.group(2).toLowerCase() : "ms";

        switch (unit) {
            case "s": return (long) (duration * 1000);
            case "m": return (long) (duration * 1000 * 60);
            case "h": return (long) (duration * 1000 * 60 * 60);
            case "d": return (long) (duration * 1000 * 60 * 60 * 24);
            case "ms":
            default: return (long) duration;
        }
    }

    @Override
    public Long value() {
        return milliseconds;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.TIME_DURATION.name());
        object.addProperty("value", milliseconds);
        return object;
    }
}
