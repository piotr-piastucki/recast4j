/*
recast4j Copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.recast;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.recast4j.recast.RecastConstants.PartitionType;
import org.recast4j.recast.geom.InputGeomProvider;

public class RecastLayersTest {
    private final float m_cellSize = 0.3f;
    private final float m_cellHeight = 0.2f;
    private final float m_agentHeight = 2.0f;
    private final float m_agentRadius = 0.6f;
    private final float m_agentMaxClimb = 0.9f;
    private final float m_agentMaxSlope = 45.0f;
    private final int m_regionMinSize = 8;
    private final int m_regionMergeSize = 20;
    private final float m_regionMinArea = m_regionMinSize * m_regionMinSize * m_cellSize * m_cellSize;
    private final float m_regionMergeArea = m_regionMergeSize * m_regionMergeSize * m_cellSize * m_cellSize;
    private final float m_edgeMaxLen = 12.0f;
    private final float m_edgeMaxError = 1.3f;
    private final int m_vertsPerPoly = 6;
    private final float m_detailSampleDist = 6.0f;
    private final float m_detailSampleMaxError = 1.0f;
    private final PartitionType m_partitionType = PartitionType.WATERSHED;
    private final int m_tileSize = 48;

    @Test
    public void testDungeon() {
        HeightfieldLayerSet lset = build("dungeon.obj", 3, 2);
        assertThat(lset.layers).hasSize(1);
        assertThat(lset.layers[0].width).isEqualTo(48);
        assertThat(lset.layers[0].hmin).isEqualTo(51);
        assertThat(lset.layers[0].hmax).isEqualTo(67);
        assertThat(lset.layers[0].heights[7]).isEqualTo(17);
        assertThat(lset.layers[0].heights[107]).isEqualTo(15);
        assertThat(lset.layers[0].heights[257]).isEqualTo(13);
        assertThat(lset.layers[0].heights[1814]).isEqualTo(255);
        assertThat(lset.layers[0].cons[12]).isEqualTo(135);
        assertThat(lset.layers[0].cons[109]).isEqualTo(15);
        assertThat(lset.layers[0].cons[530]).isEqualTo(15);
        assertThat(lset.layers[0].cons[1600]).isEqualTo(0);
    }

    @Test
    public void test() {
        HeightfieldLayerSet lset = build("nav_test.obj", 3, 2);
        assertThat(lset.layers).hasSize(3);
        assertThat(lset.layers[0].width).isEqualTo(48);
        assertThat(lset.layers[0].hmin).isEqualTo(13);
        assertThat(lset.layers[0].hmax).isEqualTo(30);
        assertThat(lset.layers[0].heights[7]).isEqualTo(0);
        assertThat(lset.layers[0].heights[107]).isEqualTo(255);
        assertThat(lset.layers[0].heights[257]).isEqualTo(0);
        assertThat(lset.layers[0].heights[1814]).isEqualTo(255);
        assertThat(lset.layers[0].cons[12]).isEqualTo(133);
        assertThat(lset.layers[0].cons[109]).isEqualTo(0);
        assertThat(lset.layers[0].cons[530]).isEqualTo(0);
        assertThat(lset.layers[0].cons[1600]).isEqualTo(15);

        assertThat(lset.layers[1].width).isEqualTo(48);
        assertThat(lset.layers[1].hmin).isEqualTo(13);
        assertThat(lset.layers[1].hmax).isEqualTo(13);
        assertThat(lset.layers[1].heights[7]).isEqualTo(255);
        assertThat(lset.layers[1].heights[107]).isEqualTo(255);
        assertThat(lset.layers[1].heights[257]).isEqualTo(255);
        assertThat(lset.layers[1].heights[1814]).isEqualTo(255);
        assertThat(lset.layers[1].cons[12]).isEqualTo(0);
        assertThat(lset.layers[1].cons[109]).isEqualTo(0);
        assertThat(lset.layers[1].cons[530]).isEqualTo(0);
        assertThat(lset.layers[1].cons[1600]).isEqualTo(0);

        assertThat(lset.layers[2].width).isEqualTo(48);
        assertThat(lset.layers[2].hmin).isEqualTo(76);
        assertThat(lset.layers[2].hmax).isEqualTo(76);
        assertThat(lset.layers[2].heights[7]).isEqualTo(255);
        assertThat(lset.layers[2].heights[107]).isEqualTo(255);
        assertThat(lset.layers[2].heights[257]).isEqualTo(255);
        assertThat(lset.layers[2].heights[1814]).isEqualTo(255);
        assertThat(lset.layers[2].cons[12]).isEqualTo(0);
        assertThat(lset.layers[2].cons[109]).isEqualTo(0);
        assertThat(lset.layers[2].cons[530]).isEqualTo(0);
        assertThat(lset.layers[2].cons[1600]).isEqualTo(0);
    }

    @Test
    public void test2() {
        HeightfieldLayerSet lset = build("nav_test.obj", 2, 4);
        assertThat(lset.layers).hasSize(2);
        assertThat(lset.layers[0].width).isEqualTo(48);
        assertThat(lset.layers[0].hmin).isEqualTo(13);
        assertThat(lset.layers[0].hmax).isEqualTo(13);
        assertThat(lset.layers[0].heights[7]).isEqualTo(0);
        assertThat(lset.layers[0].heights[107]).isEqualTo(0);
        assertThat(lset.layers[0].heights[257]).isEqualTo(0);
        assertThat(lset.layers[0].heights[1814]).isEqualTo(0);
        assertThat(lset.layers[0].cons[12]).isEqualTo(135);
        assertThat(lset.layers[0].cons[109]).isEqualTo(15);
        assertThat(lset.layers[0].cons[530]).isEqualTo(0);
        assertThat(lset.layers[0].cons[1600]).isEqualTo(15);

        assertThat(lset.layers[1].width).isEqualTo(48);
        assertThat(lset.layers[1].hmin).isEqualTo(68);
        assertThat(lset.layers[1].hmax).isEqualTo(101);
        assertThat(lset.layers[1].heights[7]).isEqualTo(33);
        assertThat(lset.layers[1].heights[107]).isEqualTo(255);
        assertThat(lset.layers[1].heights[257]).isEqualTo(255);
        assertThat(lset.layers[1].heights[1814]).isEqualTo(3);
        assertThat(lset.layers[1].cons[12]).isEqualTo(0);
        assertThat(lset.layers[1].cons[109]).isEqualTo(0);
        assertThat(lset.layers[1].cons[530]).isEqualTo(15);
        assertThat(lset.layers[1].cons[1600]).isEqualTo(0);

    }

    private HeightfieldLayerSet build(String filename, int x, int y) {
        ObjImporter importer = new ObjImporter();
        InputGeomProvider geom = importer.load(getClass().getResourceAsStream(filename));
        RecastBuilder builder = new RecastBuilder();
        RecastConfig cfg = new RecastConfig(true, m_tileSize, m_tileSize, RecastConfig.calcBorder(m_agentRadius, m_cellSize),
                m_partitionType, m_cellSize, m_cellHeight, m_agentMaxSlope, true, true, true, m_agentHeight, m_agentRadius,
                m_agentMaxClimb, m_regionMinArea, m_regionMergeArea, m_edgeMaxLen, m_edgeMaxError, m_vertsPerPoly, true,
                m_detailSampleDist, m_detailSampleMaxError, SampleAreaModifications.SAMPLE_AREAMOD_GROUND);
        RecastBuilderConfig bcfg = new RecastBuilderConfig(cfg, geom.getMeshBoundsMin(), geom.getMeshBoundsMax(), x, y);
        HeightfieldLayerSet lset = builder.buildLayers(geom, bcfg);
        return lset;
    }
}
