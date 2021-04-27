/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/

package org.recast4j.dynamic;

import java.util.Optional;
import java.util.function.BiFunction;

import org.recast4j.recast.Heightfield;
import org.recast4j.recast.Span;

/**
 * Voxel raycast based on the algorithm described in
 *
 * "A Fast Voxel Traversal Algorithm for Ray Tracing" by John Amanatides and Andrew Woo
 */
public class VoxelQuery {

    private final float[] origin;
    private final float tileWidth;
    private final float tileDepth;
    private final BiFunction<Integer, Integer, Optional<Heightfield>> heightfieldProvider;

    public VoxelQuery(float[] origin, float tileWidth, float tileDepth,
            BiFunction<Integer, Integer, Optional<Heightfield>> heightfieldProvider) {
        this.origin = origin;
        this.tileWidth = tileWidth;
        this.tileDepth = tileDepth;
        this.heightfieldProvider = heightfieldProvider;
    }

    /**
     * Perform raycast using voxels heightfields.
     *
     * @return Optional with hit parameter (t) or empty if no hit found
     */
    public Optional<Float> raycast(float[] start, float[] end) {
        return traverseTiles(start, end);
    }

    private Optional<Float> traverseTiles(float[] start, float[] end) {
        float relStartX = start[0] - origin[0];
        float relStartZ = start[2] - origin[2];
        int sx = (int) Math.floor(relStartX / tileWidth);
        int sz = (int) Math.floor(relStartZ / tileDepth);
        int ex = (int) Math.floor((end[0] - origin[0]) / tileWidth);
        int ez = (int) Math.floor((end[2] - origin[2]) / tileDepth);
        int dx = ex - sx;
        int dz = ez - sz;
        int stepX = dx < 0 ? -1 : 1;
        int stepZ = dz < 0 ? -1 : 1;
        float xRem = (tileWidth + (relStartX % tileWidth)) % tileWidth;
        float zRem = (tileDepth + (relStartZ % tileDepth)) % tileDepth;
        float tx = end[0] - start[0];
        float tz = end[2] - start[2];
        float xOffest = Math.abs(tx < 0 ? xRem : tileWidth - xRem);
        float zOffest = Math.abs(tz < 0 ? zRem : tileDepth - zRem);
        tx = Math.abs(tx);
        tz = Math.abs(tz);
        float tMaxX = xOffest / tx;
        float tMaxZ = zOffest / tz;
        float tDeltaX = tileWidth / tx;
        float tDeltaZ = tileDepth / tz;
        float t = 0;
        while (true) {
            Optional<Float> hit = traversHeightfield(sx, sz, start, end, t, Math.min(1, Math.min(tMaxX, tMaxZ)));
            if (hit.isPresent()) {
                return hit;
            }
            if ((dx > 0 ? sx >= ex : sx <= ex) && (dz > 0 ? sz >= ez : sz <= ez)) {
                break;
            }
            if (tMaxX < tMaxZ) {
                t = tMaxX;
                tMaxX += tDeltaX;
                sx += stepX;
            } else {
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                sz += stepZ;
            }
        }
        return Optional.empty();
    }

    private Optional<Float> traversHeightfield(int x, int z, float[] start, float[] end, float tMin, float tMax) {
        Optional<Heightfield> ohf = heightfieldProvider.apply(x, z);
        if (ohf.isPresent()) {
            Heightfield hf = ohf.get();
            float tx = end[0] - start[0];
            float ty = end[1] - start[1];
            float tz = end[2] - start[2];
            float[] entry = { start[0] + tMin * tx, start[1] + tMin * ty, start[2] + tMin * tz };
            float[] exit = { start[0] + tMax * tx, start[1] + tMax * ty, start[2] + tMax * tz };
            float relStartX = entry[0] - hf.bmin[0];
            float relStartZ = entry[2] - hf.bmin[2];
            int sx = (int) Math.floor(relStartX / hf.cs);
            int sz = (int) Math.floor(relStartZ / hf.cs);
            int ex = (int) Math.floor((exit[0] - hf.bmin[0]) / hf.cs);
            int ez = (int) Math.floor((exit[2] - hf.bmin[2]) / hf.cs);
            int dx = ex - sx;
            int dz = ez - sz;
            int stepX = dx < 0 ? -1 : 1;
            int stepZ = dz < 0 ? -1 : 1;
            float xRem = (hf.cs + (relStartX % hf.cs)) % hf.cs;
            float zRem = (hf.cs + (relStartZ % hf.cs)) % hf.cs;
            float xOffest = Math.abs(tx < 0 ? xRem : hf.cs - xRem);
            float zOffest = Math.abs(tz < 0 ? zRem : hf.cs - zRem);
            tx = Math.abs(tx);
            tz = Math.abs(tz);
            float tMaxX = xOffest / tx;
            float tMaxZ = zOffest / tz;
            float tDeltaX = hf.cs / tx;
            float tDeltaZ = hf.cs / tz;
            float t = 0;
            while (true) {
                if (sx >= 0 && sx < hf.width && sz >= 0 && sz < hf.height) {
                    float y1 = start[1] + ty * (tMin + t) - hf.bmin[1];
                    float y2 = start[1] + ty * (tMin + Math.min(tMaxX, tMaxZ)) - hf.bmin[1];
                    float ymin = Math.min(y1, y2) / hf.ch;
                    float ymax = Math.max(y1, y2) / hf.ch;
                    Span span = hf.spans[sx + sz * hf.width];
                    while (span != null) {
                        if (span.smin <= ymin && span.smax >= ymax) {
                            return Optional.of(Math.min(1, tMin + t));
                        }
                        span = span.next;
                    }
                }
                if ((dx > 0 ? sx >= ex : sx <= ex) && (dz > 0 ? sz >= ez : sz <= ez)) {
                    break;
                }
                if (tMaxX < tMaxZ) {
                    t = tMaxX;
                    tMaxX += tDeltaX;
                    sx += stepX;
                } else {
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                    sz += stepZ;
                }
            }
        }
        return Optional.empty();
    }

}
