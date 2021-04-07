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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.recast4j.detour.MeshData;
import org.recast4j.detour.NavMeshBuilder;
import org.recast4j.detour.NavMeshDataCreateParams;
import org.recast4j.dynamic.collider.Collider;
import org.recast4j.dynamic.io.VoxelTile;
import org.recast4j.recast.Heightfield;
import org.recast4j.recast.PolyMesh;
import org.recast4j.recast.PolyMeshDetail;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilder.RecastBuilderResult;
import org.recast4j.recast.RecastConfig;
import org.recast4j.recast.Telemetry;

class DynamicTile {

    final VoxelTile voxelTile;
    DynamicTileCheckpoint checkpoint;
    RecastBuilderResult recastResult;
    MeshData meshData;
    private final Map<Long, Collider> colliders = new ConcurrentHashMap<>();
    private boolean dirty = true;

    DynamicTile(VoxelTile voxelTile) {
        this.voxelTile = voxelTile;
    }

    boolean build(RecastBuilder builder, DynamicNavMeshConfig config, Telemetry telemetry) {
        if (dirty) {
            VoxelTile vt = checkpoint != null ? checkpoint.voxelTile : voxelTile;
            Collection<Long> cpColliders = checkpoint != null ? checkpoint.colliders : Collections.emptySet();
            Heightfield heightfield = buildHeightfield(config, vt, cpColliders, telemetry);
            RecastBuilderResult r = buildRecast(builder, config, vt, heightfield, telemetry);
            NavMeshDataCreateParams params = navMeshCreateParams(vt.tileX, vt.tileZ, vt.cellSize, vt.cellHeight, config, r);
            meshData = NavMeshBuilder.createNavMeshData(params);
            return true;
        }
        return false;
    }

    private Heightfield buildHeightfield(DynamicNavMeshConfig config, VoxelTile vt, Collection<Long> rasterizedColliders,
            Telemetry telemetry) {
        Heightfield heightfield = vt.heightfield();
        colliders.forEach((id, c) -> {
            if (!rasterizedColliders.contains(id)) {
                c.rasterize(heightfield, telemetry);
            }
        });
        if (config.enableCheckpoints && !colliders.isEmpty()) {
            checkpoint = new DynamicTileCheckpoint(new VoxelTile(vt.tileX, vt.tileZ, heightfield),
                    new HashSet<>(colliders.keySet()));
        }
        return heightfield;
    }

    private RecastBuilderResult buildRecast(RecastBuilder builder, DynamicNavMeshConfig config, VoxelTile vt,
            Heightfield heightfield, Telemetry telemetry) {
        RecastConfig rcConfig = new RecastConfig(config.useTiles, config.tileSizeX, config.tileSizeZ, vt.borderSize,
                config.partitionType, vt.cellSize, vt.cellHeight, config.walkableSlopeAngle, true, true, true,
                config.walkableHeight, config.walkableRadius, config.walkableClimb, config.minRegionArea, config.regionMergeArea,
                config.maxEdgeLen, config.maxSimplificationError,
                Math.min(DynamicNavMesh.MAX_VERTS_PER_POLY, config.vertsPerPoly), true, config.detailSampleDistance,
                config.detailSampleMaxError, null);
        RecastBuilderResult r = builder.build(vt.tileX, vt.tileZ, null, rcConfig, heightfield, telemetry);
        if (config.keepIntermediateResults) {
            recastResult = r;
        }
        return r;
    }

    void addCollider(long cid, Collider collider) {
        colliders.put(cid, collider);
        dirty = true;
    }

    boolean containsCollider(long cid) {
        return colliders.containsKey(cid);
    }

    void removeCollider(long colliderId) {
        if (colliders.remove(colliderId) != null) {
            dirty = true;
            checkpoint = null;
        }
    }

    private NavMeshDataCreateParams navMeshCreateParams(int tilex, int tileZ, float cellSize, float cellHeight,
            DynamicNavMeshConfig config, RecastBuilderResult rcResult) {
        PolyMesh m_pmesh = rcResult.getMesh();
        PolyMeshDetail m_dmesh = rcResult.getMeshDetail();
        NavMeshDataCreateParams params = new NavMeshDataCreateParams();
        for (int i = 0; i < m_pmesh.npolys; ++i) {
            m_pmesh.flags[i] = 1;
        }
        params.tileX = tilex;
        params.tileZ = tileZ;
        params.verts = m_pmesh.verts;
        params.vertCount = m_pmesh.nverts;
        params.polys = m_pmesh.polys;
        params.polyAreas = m_pmesh.areas;
        params.polyFlags = m_pmesh.flags;
        params.polyCount = m_pmesh.npolys;
        params.nvp = m_pmesh.nvp;
        if (m_dmesh != null) {
            params.detailMeshes = m_dmesh.meshes;
            params.detailVerts = m_dmesh.verts;
            params.detailVertsCount = m_dmesh.nverts;
            params.detailTris = m_dmesh.tris;
            params.detailTriCount = m_dmesh.ntris;
        }
        params.walkableHeight = config.walkableHeight;
        params.walkableRadius = config.walkableRadius;
        params.walkableClimb = config.walkableClimb;
        params.bmin = m_pmesh.bmin;
        params.bmax = m_pmesh.bmax;
        params.cs = cellSize;
        params.ch = cellHeight;
        params.buildBvTree = true;

        params.offMeshConCount = 0;
        params.offMeshConVerts = new float[0];
        params.offMeshConRad = new float[0];
        params.offMeshConDir = new int[0];
        params.offMeshConAreas = new int[0];
        params.offMeshConFlags = new int[0];
        params.offMeshConUserID = new int[0];
        return params;
    }

}
