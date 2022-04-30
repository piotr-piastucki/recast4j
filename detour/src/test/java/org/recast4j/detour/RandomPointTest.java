/*
recast4j Copyright (c) 2015-2021 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.detour;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.recast4j.detour.DetourCommon.vDist2D;

import org.junit.jupiter.api.Test;
import org.recast4j.detour.NavMeshQuery.FRand;

public class RandomPointTest extends AbstractDetourTest {

    @Test
    public void testRandom() {
        FRand f = new FRand(1);
        QueryFilter filter = new DefaultQueryFilter();
        for (int i = 0; i < 1000; i++) {
            Result<FindRandomPointResult> point = query.findRandomPoint(filter, f);
            assertThat(point.succeeded()).isTrue();
            Tupple2<MeshTile, Poly> tileAndPoly = navmesh.getTileAndPolyByRef(point.result.getRandomRef()).result;
            float[] bmin = new float[2];
            float[] bmax = new float[2];
            for (int j = 0; j < tileAndPoly.second.vertCount; j++) {
                int v = tileAndPoly.second.verts[j] * 3;
                bmin[0] = j == 0 ? tileAndPoly.first.data.verts[v] : Math.min(bmin[0], tileAndPoly.first.data.verts[v]);
                bmax[0] = j == 0 ? tileAndPoly.first.data.verts[v] : Math.max(bmax[0], tileAndPoly.first.data.verts[v]);
                bmin[1] = j == 0 ? tileAndPoly.first.data.verts[v + 2] : Math.min(bmin[1], tileAndPoly.first.data.verts[v + 2]);
                bmax[1] = j == 0 ? tileAndPoly.first.data.verts[v + 2] : Math.max(bmax[1], tileAndPoly.first.data.verts[v + 2]);
            }
            assertThat(point.result.getRandomPt()[0] >= bmin[0]);
            assertThat(point.result.getRandomPt()[0] <= bmax[0]);
            assertThat(point.result.getRandomPt()[2] >= bmin[1]);
            assertThat(point.result.getRandomPt()[2] <= bmax[1]);
        }
    }

    @Test
    public void testRandomAroundCircle() {
        FRand f = new FRand(1);
        QueryFilter filter = new DefaultQueryFilter();
        FindRandomPointResult point = query.findRandomPoint(filter, f).result;
        for (int i = 0; i < 1000; i++) {
            Result<FindRandomPointResult> result = query.findRandomPointAroundCircle(point.getRandomRef(), point.getRandomPt(),
                    5f, filter, f);
            assertThat(result.failed()).isFalse();
            point = result.result;
            Tupple2<MeshTile, Poly> tileAndPoly = navmesh.getTileAndPolyByRef(point.getRandomRef()).result;
            float[] bmin = new float[2];
            float[] bmax = new float[2];
            for (int j = 0; j < tileAndPoly.second.vertCount; j++) {
                int v = tileAndPoly.second.verts[j] * 3;
                bmin[0] = j == 0 ? tileAndPoly.first.data.verts[v] : Math.min(bmin[0], tileAndPoly.first.data.verts[v]);
                bmax[0] = j == 0 ? tileAndPoly.first.data.verts[v] : Math.max(bmax[0], tileAndPoly.first.data.verts[v]);
                bmin[1] = j == 0 ? tileAndPoly.first.data.verts[v + 2] : Math.min(bmin[1], tileAndPoly.first.data.verts[v + 2]);
                bmax[1] = j == 0 ? tileAndPoly.first.data.verts[v + 2] : Math.max(bmax[1], tileAndPoly.first.data.verts[v + 2]);
            }
            assertThat(point.getRandomPt()[0] >= bmin[0]);
            assertThat(point.getRandomPt()[0] <= bmax[0]);
            assertThat(point.getRandomPt()[2] >= bmin[1]);
            assertThat(point.getRandomPt()[2] <= bmax[1]);
        }
    }

    @Test
    public void testRandomWithinCircle() {
        FRand f = new FRand(1);
        QueryFilter filter = new DefaultQueryFilter();
        FindRandomPointResult point = query.findRandomPoint(filter, f).result;
        float radius = 5f;
        for (int i = 0; i < 1000; i++) {
            Result<FindRandomPointResult> result = query.findRandomPointWithinCircle(point.getRandomRef(), point.getRandomPt(),
                    radius, filter, f);
            assertThat(result.failed()).isFalse();
            float distance = vDist2D(point.getRandomPt(), result.result.getRandomPt());
            assertTrue(distance <= radius);
            point = result.result;
        }
    }

    @Test
    public void testPerformance() {
        FRand f = new FRand(1);
        QueryFilter filter = new DefaultQueryFilter();
        FindRandomPointResult point = query.findRandomPoint(filter, f).result;
        float radius = 5f;
        // jvm warmup
        for (int i = 0; i < 1000; i++) {
            query.findRandomPointAroundCircle(point.getRandomRef(), point.getRandomPt(), radius, filter, f);
        }
        for (int i = 0; i < 1000; i++) {
            query.findRandomPointWithinCircle(point.getRandomRef(), point.getRandomPt(), radius, filter, f);
        }
        long t1 = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            query.findRandomPointAroundCircle(point.getRandomRef(), point.getRandomPt(), radius, filter, f);
        }
        long t2 = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            query.findRandomPointWithinCircle(point.getRandomRef(), point.getRandomPt(), radius, filter, f);
        }
        long t3 = System.nanoTime();
        System.out.println("Random point around circle: " + (t2 - t1) / 1000000 + "ms");
        System.out.println("Random point within circle: " + (t3 - t2) / 1000000 + "ms");
    }

}
