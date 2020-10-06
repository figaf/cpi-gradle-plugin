package com.figaf.plugin.response_parser;

import org.json.JSONObject;

/**
 * @author Arsenii Istlentev
 */
public class CpiCommonParser {

    public static String optString(JSONObject json, String key) {
        if (json.isNull(key)) {
            return null;
        } else {
            return json.optString(key, null);
        }
    }
}
