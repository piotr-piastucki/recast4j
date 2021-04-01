/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
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

import static org.recast4j.recast.RecastVectors.copy;

public class RecastBuilderConfig {

    public final RecastConfig cfg;

    public final int tileX;
    public final int tileZ;
    /** The width of the field along the x-axis. [Limit: >= 0] [Units: vx] **/
    public final int width;

    /** The height of the field along the z-axis. [Limit: >= 0] [Units: vx] **/
    public final int height;

    /** The minimum bounds of the field's AABB. [(x, y, z)] [Units: wu] **/
    public final float[] bmin = new float[3];

    /** The maximum bounds of the field's AABB. [(x, y, z)] [Units: wu] **/
    public final float[] bmax = new float[3];

    public RecastBuilderConfig(RecastConfig cfg, float[] bmin, float[] bmax) {
        this(cfg, bmin, bmax, 0, 0);
    }

    public RecastBuilderConfig(RecastConfig cfg, float[] bmin, float[] bmax, int tileX, int tileZ) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.cfg = cfg;
        copy(this.bmin, bmin);
        copy(this.bmax, bmax);
        if (cfg.useTiles) {
            float tsx = cfg.tileSizeX * cfg.cs;
            float tsz = cfg.tileSizeZ * cfg.cs;
            this.bmin[0] += tileX * tsx;
            this.bmin[2] += tileZ * tsz;
            this.bmax[0] = this.bmin[0] + tsx;
            this.bmax[2] = this.bmin[2] + tsz;
            // Expand the heighfield bounding box by border size to find the extents of geometry we need to build this
            // tile.
            //
            // This is done in order to make sure that the navmesh tiles connect correctly at the borders,
            // and the obstacles close to the border work correctly with the dilation process.
            // No polygons (or contours) will be created on the border area.
            //
            // IMPORTANT!
            //
            // :''''''''':
            // : +-----+ :
            // : | | :
            // : | |<--- tile to build
            // : | | :
            // : +-----+ :<-- geometry needed
            // :.........:
            //
            // You should use this bounding box to query your input geometry.
            //
            // For example if you build a navmesh for terrain, and want the navmesh tiles to match the terrain tile size
            // you will need to pass in data from neighbour terrain tiles too! In a simple case, just pass in all the 8
            // neighbours,
            // or use the bounding box below to only pass in a sliver of each of the 8 neighbours.
            this.bmin[0] -= cfg.borderSize * cfg.cs;
            this.bmin[2] -= cfg.borderSize * cfg.cs;
            this.bmax[0] += cfg.borderSize * cfg.cs;
            this.bmax[2] += cfg.borderSize * cfg.cs;
            width = cfg.tileSizeX + cfg.borderSize * 2;
            height = cfg.tileSizeZ + cfg.borderSize * 2;
        } else {
            int[] wh = Recast.calcGridSize(this.bmin, this.bmax, cfg.cs);
            width = wh[0];
            height = wh[1];
        }
    }

}
