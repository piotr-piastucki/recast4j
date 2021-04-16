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

import static org.recast4j.detour.DetourCommon.vCopy;

import java.util.Set;

import org.recast4j.recast.Heightfield;
import org.recast4j.recast.Span;

public class DynamicTileCheckpoint {

    final Heightfield heightfield;
    final Set<Long> colliders;

    public DynamicTileCheckpoint(Heightfield heightfield, Set<Long> colliders) {
        this.colliders = colliders;
        this.heightfield = clone(heightfield);
    }

    private Heightfield clone(Heightfield source) {
        Heightfield clone = new Heightfield(source.width, source.height, vCopy(source.bmin), vCopy(source.bmax), source.cs,
                source.ch, source.borderSize);
        for (int z = 0, pz = 0; z < source.height; z++, pz += source.width) {
            for (int x = 0; x < source.width; x++) {
                Span span = source.spans[pz + x];
                Span prevCopy = null;
                while (span != null) {
                    Span copy = new Span();
                    copy.smin = span.smin;
                    copy.smax = span.smax;
                    copy.area = span.area;
                    if (prevCopy == null) {
                        clone.spans[pz + x] = copy;
                    } else {
                        prevCopy.next = copy;
                    }
                    prevCopy = copy;
                    span = span.next;
                }
            }
        }
        return clone;
    }

}
