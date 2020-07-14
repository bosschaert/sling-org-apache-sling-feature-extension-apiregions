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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.builder.MergeHandler;
import org.apache.sling.feature.extension.apiregions.api.ApiExport;
import org.apache.sling.feature.extension.apiregions.api.ApiRegion;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;

/**
 * Merge to api region extensions
 */
public class APIRegionMergeHandler implements MergeHandler {

    @Override
    public boolean canMerge(Extension extension) {
        return ApiRegions.EXTENSION_NAME.equals(extension.getName());
    }

    @Override
    public void merge(HandlerContext context, Feature target, Feature source, Extension targetEx, Extension sourceEx) {
        if (!sourceEx.getName().equals(ApiRegions.EXTENSION_NAME))
            return;
        if (targetEx != null && !targetEx.getName().equals(ApiRegions.EXTENSION_NAME))
            return;

        try {
            final ApiRegions srcRegions = ApiRegions.parse((JsonArray) sourceEx.getJSONStructure());

            final ApiRegions targetRegions;
            if (targetEx != null) {
                targetRegions = ApiRegions.parse((JsonArray) targetEx.getJSONStructure());
            } else {
                targetEx = new Extension(sourceEx.getType(), sourceEx.getName(), sourceEx.getState());
                target.getExtensions().add(targetEx);

                targetRegions = new ApiRegions();
            }

            for (final ApiRegion targetRegion : targetRegions.listRegions()) {
                final ApiRegion sourceRegion = srcRegions.getRegionByName(targetRegion.getName());
                if (sourceRegion != null) {
                    for (final ApiExport srcExp : sourceRegion.listExports()) {
                        if (targetRegion.getExportByName(srcExp.getName()) == null) {
                            targetRegion.add(srcExp);
                        }
                    }
                    LinkedHashSet<ArtifactId> targetOrigins = new LinkedHashSet<>(Arrays.asList(targetRegion.getFeatureOrigins()));
                    LinkedHashSet<ArtifactId> sourceOrigins = new LinkedHashSet<>(Arrays.asList(sourceRegion.getFeatureOrigins()));
                    if (sourceOrigins.isEmpty()) {
                        sourceOrigins.add(source.getId());
                    }
                    targetOrigins.addAll(sourceOrigins);
                    targetRegion.setFeatureOrigins(targetOrigins.toArray(new ArtifactId[0]));
                }
            }

            // Build up a region map to identify the insertion order
            Map<String, Integer> regionPos = new HashMap<>();
            List<ApiRegion> tgtRegions = targetRegions.listRegions();
            for (int i=0; i<tgtRegions.size(); i++) {
                regionPos.put(tgtRegions.get(i).getName(), i);
            }

            // If there are any remaining regions in the src extension, process them now
            List<ApiRegion> sourceRegions = srcRegions.listRegions();
            int nextInsertPosition = tgtRegions.size();
            String nextFound = getNextFound(sourceRegions.size() - 1, regionPos, sourceRegions);
            int nextFoundPosition = regionPos.get(nextFound);

            for (int i=sourceRegions.size() - 1; i>=0; i--) {
                ApiRegion cur = sourceRegions.get(i);
                if (cur.getName().equals(nextFound)) {
                    nextFound = getNextFound(i - 1, regionPos, sourceRegions);
                    if (nextFound == null) {
                        nextInsertPosition = 0;
                    } else {
                        nextInsertPosition = regionPos.get(nextFound);
                    }
                } else {
                    LinkedHashSet<ArtifactId> origins = new LinkedHashSet<>(Arrays.asList(cur.getFeatureOrigins()));
                    if (origins.isEmpty())
                    {
                        origins.add(source.getId());
                        cur.setFeatureOrigins(origins.toArray(new ArtifactId[0]));
                    }
                    if (!targetRegions.add(nextInsertPosition, cur))
                    {
                        throw new IllegalStateException("Duplicate region " + cur.getName());
                    }
                }
            }

            /*
            for (final ApiRegion r : srcRegions.listRegions()) {
                if (targetRegions.getRegionByName(r.getName()) == null) {
                    LinkedHashSet<ArtifactId> origins = new LinkedHashSet<>(Arrays.asList(r.getFeatureOrigins()));
                    if (origins.isEmpty())
                    {
                        origins.add(source.getId());
                        r.setFeatureOrigins(origins.toArray(new ArtifactId[0]));
                    }
                    if (!targetRegions.add(r))
                    {
                        throw new IllegalStateException("Duplicate region " + r.getName());
                    }
                }
            }
            */

            targetEx.setJSONStructure(targetRegions.toJSONArray());

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getNextFound(int startPos, Map<String, Integer> regionPos, List<ApiRegion> sourceRegions) {
        for (int i=startPos; i>=0; i--) {
            String name = sourceRegions.get(i).getName();
            if (regionPos.get(name) != null) {
                return name;
            }
        }
        return null;
    }
}
