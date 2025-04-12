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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * Represents a ByteSize token, used to parse values like "10MB", "2GB".
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PublicEvolving
public class ByteSize implements Token {
    private final long bytes;

    private static final Pattern BYTE_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)(B|KB|MB|GB|TB)?");

    public ByteSize(String value) {
        this.bytes = parseByteSize(value);
    }

    private static long parseByteSize(String value) {
        Matcher matcher = BYTE_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid byte size format: " + value);
        }

        double size = Double.parseDouble(matcher.group(1));
        String unit = (matcher.group(2) != null) ? matcher.group(2).toUpperCase() : "B";

        switch (unit) {
            case "KB": return (long) (size * 1024);
            case "MB": return (long) (size * 1024 * 1024);
            case "GB": return (long) (size * 1024 * 1024 * 1024);
            case "TB": return (long) (size * 1024 * 1024 * 1024 * 1024);
            case "B":
            default: return (long) size;
        }
    }

    @Override
    public Long value() {
        return bytes;
    }

    public long getBytes() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.BYTE_SIZE.name());
        object.addProperty("value", bytes);
        return object;
    }
}
