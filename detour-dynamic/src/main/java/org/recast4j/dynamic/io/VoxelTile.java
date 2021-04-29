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

package org.recast4j.dynamic.io;

import static org.recast4j.dynamic.io.ByteUtils.getIntBE;
import static org.recast4j.dynamic.io.ByteUtils.getIntLE;
import static org.recast4j.dynamic.io.ByteUtils.getShortBE;
import static org.recast4j.dynamic.io.ByteUtils.getShortLE;
import static org.recast4j.dynamic.io.ByteUtils.putInt;
import static org.recast4j.dynamic.io.ByteUtils.putShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.recast4j.recast.Heightfield;
import org.recast4j.recast.Span;

public class VoxelTile {

    private static final int SERIALIZED_SPAN_COUNT_BYTES = 2;
    private static final int SERIALIZED_SPAN_BYTES = 12;
    public final int tileX;
    public final int tileZ;
    public final int borderSize;
    public int width;
    public int depth;
    public final float[] boundsMin;
    public float[] boundsMax;
    public float cellSize;
    public float cellHeight;
    final byte[] spanData;

    public VoxelTile(int tileX, int tileZ, int width, int depth, float[] boundsMin, float[] boundsMax, float cellSize,
            float cellHeight, int borderSize, ByteBuffer buffer) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.width = width;
        this.depth = depth;
        this.boundsMin = boundsMin;
        this.boundsMax = boundsMax;
        this.cellSize = cellSize;
        this.cellHeight = cellHeight;
        this.borderSize = borderSize;
        spanData = toByteArray(buffer, width, depth, VoxelFile.PREFERRED_BYTE_ORDER);
    }

    public VoxelTile(int tileX, int tileZ, Heightfield heightfield) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        width = heightfield.width;
        depth = heightfield.height;
        boundsMin = heightfield.bmin;
        boundsMax = heightfield.bmax;
        cellSize = heightfield.cs;
        cellHeight = heightfield.ch;
        borderSize = heightfield.borderSize;
        spanData = serializeSpans(heightfield, VoxelFile.PREFERRED_BYTE_ORDER);
    }

    public Heightfield heightfield() {
        return VoxelFile.PREFERRED_BYTE_ORDER == ByteOrder.BIG_ENDIAN ? heightfieldBE() : heightfieldLE();
    }

    private Heightfield heightfieldBE() {
        Heightfield hf = new Heightfield(width, depth, boundsMin, boundsMax, cellSize, cellHeight, borderSize);
        int position = 0;
        for (int z = 0, pz = 0; z < depth; z++, pz += width) {
            for (int x = 0; x < width; x++) {
                Span prev = null;
                int spanCount = getShortBE(spanData, position);
                position += 2;
                for (int s = 0; s < spanCount; s++) {
                    Span span = new Span();
                    span.smin = getIntBE(spanData, position);
                    position += 4;
                    span.smax = getIntBE(spanData, position);
                    position += 4;
                    span.area = getIntBE(spanData, position);
                    position += 4;
                    if (prev == null) {
                        hf.spans[pz + x] = span;
                    } else {
                        prev.next = span;
                    }
                    prev = span;
                }
            }
        }
        return hf;
    }

    private Heightfield heightfieldLE() {
        Heightfield hf = new Heightfield(width, depth, boundsMin, boundsMax, cellSize, cellHeight, borderSize);
        int position = 0;
        for (int z = 0, pz = 0; z < depth; z++, pz += width) {
            for (int x = 0; x < width; x++) {
                Span prev = null;
                int spanCount = getShortLE(spanData, position);
                position += 2;
                for (int s = 0; s < spanCount; s++) {
                    Span span = new Span();
                    span.smin = getIntLE(spanData, position);
                    position += 4;
                    span.smax = getIntLE(spanData, position);
                    position += 4;
                    span.area = getIntLE(spanData, position);
                    position += 4;
                    if (prev == null) {
                        hf.spans[pz + x] = span;
                    } else {
                        prev.next = span;
                    }
                    prev = span;
                }
            }
        }
        return hf;
    }

    private byte[] serializeSpans(Heightfield heightfield, ByteOrder order) {
        int[] counts = new int[heightfield.width * heightfield.height];
        int totalCount = 0;
        for (int z = 0, pz = 0; z < heightfield.height; z++, pz += heightfield.width) {
            for (int x = 0; x < heightfield.width; x++) {
                Span span = heightfield.spans[pz + x];
                while (span != null) {
                    counts[pz + x]++;
                    totalCount++;
                    span = span.next;
                }
            }
        }
        byte[] data = new byte[totalCount * SERIALIZED_SPAN_BYTES + counts.length * SERIALIZED_SPAN_COUNT_BYTES];
        int position = 0;
        for (int z = 0, pz = 0; z < heightfield.height; z++, pz += heightfield.width) {
            for (int x = 0; x < heightfield.width; x++) {
                position = putShort(counts[pz + x], data, position, order);
                Span span = heightfield.spans[pz + x];
                while (span != null) {
                    position = putInt(span.smin, data, position, order);
                    position = putInt(span.smax, data, position, order);
                    position = putInt(span.area, data, position, order);
                    span = span.next;
                }
            }
        }
        return data;
    }

    private byte[] toByteArray(ByteBuffer buf, int width, int height, ByteOrder order) {
        byte[] data = new byte[buf.limit()];
        if (buf.order() == order) {
            buf.get(data);
        } else {
            int l = width * height;
            int position = 0;
            for (int i = 0; i < l; i++) {
                int count = buf.getShort();
                putShort(count, data, position, order);
                position += 2;
                for (int j = 0; j < count; j++) {
                    putInt(buf.getInt(), data, position, order);
                    position += 4;
                    putInt(buf.getInt(), data, position, order);
                    position += 4;
                    putInt(buf.getInt(), data, position, order);
                    position += 4;
                }
            }
        }
        return data;
    }
}
