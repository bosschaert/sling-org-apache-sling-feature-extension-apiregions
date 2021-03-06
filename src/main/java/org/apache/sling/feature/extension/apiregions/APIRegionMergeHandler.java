/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.extension.apiregions;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.builder.MergeHandler;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import static org.apache.sling.feature.extension.apiregions.AbstractHandler.API_REGIONS_NAME;
import static org.apache.sling.feature.extension.apiregions.AbstractHandler.EXPORTS_KEY;
import static org.apache.sling.feature.extension.apiregions.AbstractHandler.NAME_KEY;

public class APIRegionMergeHandler implements MergeHandler {
    @Override
    public boolean canMerge(Extension extension) {
        return API_REGIONS_NAME.equals(extension.getName());
    }

    @Override
    public void merge(HandlerContext context, Feature target, Feature source, Extension targetEx, Extension sourceEx) {
        if (!sourceEx.getName().equals(API_REGIONS_NAME))
            return;
        if (targetEx != null && !targetEx.getName().equals(API_REGIONS_NAME))
            return;

        JsonReader srcJR = Json.createReader(new StringReader(sourceEx.getJSON()));
        JsonArray srcJA = srcJR.readArray();

        Map<String, Map<String, Object>> srcRegions = new LinkedHashMap<>();
        for (int i=0; i < srcJA.size(); i++) {
            String regionName = null;
            Map<String, Object> region = new LinkedHashMap<>();
            JsonObject jo = srcJA.getJsonObject(i);
            for (Map.Entry<String, JsonValue> entry : jo.entrySet()) {
                Object val;
                switch (entry.getKey()) {
                case EXPORTS_KEY:
                    val = readJsonArray((JsonArray) entry.getValue());
                    break;
                default:
                    val = ((JsonString) entry.getValue()).getString();
                    if (NAME_KEY.equals(entry.getKey())) {
                        regionName = val.toString();
                    }
                    break;
                }
                region.put(entry.getKey(), val);
            }
            if (regionName == null) {
                throw new IllegalStateException("No region name specified: " + sourceEx.getJSON());
            }
            srcRegions.put(regionName, region);
        }

        JsonArray tgtJA;
        if (targetEx != null) {
            JsonReader tgtJR = Json.createReader(new StringReader(targetEx.getJSON()));
            tgtJA = tgtJR.readArray();
        } else {
            targetEx = new Extension(sourceEx.getType(), sourceEx.getName(), sourceEx.isRequired());
            target.getExtensions().add(targetEx);

            tgtJA = Json.createArrayBuilder().build();
        }

        StringWriter sw = new StringWriter();
        JsonGenerator gen = Json.createGenerator(sw);
        gen.writeStartArray();
        for (JsonValue jv : tgtJA) {
            if (jv instanceof JsonObject) {
                JsonObject jo = (JsonObject) jv;
                Map<String, Object> srcRegion = srcRegions.remove(jo.getString(NAME_KEY));
                if (srcRegion != null) {
                    gen.writeStartObject();
                    for (Map.Entry<String, JsonValue> entry : jo.entrySet()) {
                        Object sp = srcRegion.get(entry.getKey());
                        if (EXPORTS_KEY.equals(entry.getKey()) && sp instanceof List) {
                            List<String> tgtPkgs = readJsonArray((JsonArray) entry.getValue());
                            @SuppressWarnings("unchecked")
                            List<String> srcPkgs = (List<String>) sp;
                            for (String srcPkg : srcPkgs) {
                                if (!tgtPkgs.contains(srcPkg)) {
                                    tgtPkgs.add(srcPkg);
                                }
                            }
                            gen.writeStartArray(entry.getKey());
                            for (String p : tgtPkgs) {
                                gen.write(p);
                            }
                            gen.writeEnd();
                        } else {
                            gen.write(entry.getKey(), entry.getValue());
                        }
                    }
                    gen.writeEnd();
                } else {
                    gen.write(jv);
                }
            }
        }

        // If there are any remaining regions in the src extension, process them now
        for (Map<String, Object> region : srcRegions.values()) {
            gen.writeStartObject();
            for (Map.Entry<String, Object> entry : region.entrySet()) {
                if (entry.getValue() instanceof Collection) {
                    gen.writeStartArray(entry.getKey());
                    for (Object o : (Collection<?>) entry.getValue()) {
                        gen.write(o.toString());
                    }
                    gen.writeEnd();
                } else {
                    gen.write(entry.getKey(), entry.getValue().toString());
                }
            }
            gen.writeEnd();
        }

        gen.writeEnd();
        gen.close();

        targetEx.setJSON(sw.toString());
    }

    private static List<String> readJsonArray(JsonArray jsonArray) {
        List<String> l = new ArrayList<>();
        for (JsonValue jv : jsonArray) {
            if (jv instanceof JsonString) {
                String pkg = ((JsonString) jv).getString();
                if (!pkg.startsWith("#")) { // ignore comment lines
                    l.add(pkg);
                }
            }
        }
        return l;
    }
}
