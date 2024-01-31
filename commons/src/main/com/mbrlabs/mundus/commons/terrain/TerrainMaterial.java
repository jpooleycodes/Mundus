/*
 * Copyright (c) 2016. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus.commons.terrain;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.mbrlabs.mundus.commons.terrain.attributes.TerrainAttribute;
import com.mbrlabs.mundus.commons.terrain.attributes.TerrainAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * This essentially behaves like a terrain material
 *
 * @author Marcus Brummer
 * @version 28-01-2016
 */
public class TerrainMaterial extends TerrainAttributes {
    private final Map<SplatTexture.Channel, SplatTexture> textures;
    private final Map<SplatTexture.Channel, SplatTexture> normalTextures;
    private SplatMap splatmap;
    private Terrain terrain;

    public TerrainMaterial() {
        textures = new HashMap<>(5, 1);
        normalTextures = new HashMap<>(5, 1);
    }

    public SplatTexture getTexture(SplatTexture.Channel channel) {
        return textures.get(channel);
    }

    public SplatTexture getNormalTexture(SplatTexture.Channel channel) {
        return normalTextures.get(channel);
    }

    public void removeTexture(SplatTexture.Channel channel) {
        if (splatmap == null) return;
        if (!textures.containsKey(channel)) return;

        textures.remove(channel);
        splatmap.clearChannel(channel);
        splatmap.updateTexture();

        if (has(getTerrainAttribute(channel, false))) {
            remove(getTerrainAttribute(channel, false));
        }
    }

    public void removeNormalTexture(SplatTexture.Channel channel) {
            normalTextures.remove(channel);
            if (has(getTerrainAttribute(channel, true))) {
                remove(getTerrainAttribute(channel, true));
            }
    }

    public void setSplatTexture(SplatTexture tex) {
        textures.put(tex.channel, tex);
        TerrainAttribute attr = (TerrainAttribute) get(getTerrainAttribute(tex.channel, false));
        if (attr == null) {
            set(new TerrainAttribute(getTerrainAttribute(tex.channel, false)));
        }
    }

    public void setSplatNormalTexture(SplatTexture tex) {
        normalTextures.put(tex.channel, tex);
        set(new TerrainAttribute(getTerrainAttribute(tex.channel, true)));
    }

    public boolean isTriplanar() {
        return has(TerrainAttribute.Triplanar);
    }

    public void setTriplanar(boolean triplanar) {
        if (triplanar) {
            set(new TerrainAttribute(TerrainAttribute.Triplanar));
        } else {
            remove(TerrainAttribute.Triplanar);
        }
    }

    private long getTerrainAttribute(SplatTexture.Channel channel, boolean isNormal) {
        switch (channel) {
            case BASE:
                if (isNormal)
                    return TerrainAttribute.NormalMapBase;
                else
                    return TerrainAttribute.DiffuseBase;
            case R:
                if (isNormal)
                    return TerrainAttribute.NormalMapR;
                else
                    return TerrainAttribute.DiffuseR;
            case G:
                if (isNormal)
                    return TerrainAttribute.NormalMapG;
                else
                    return TerrainAttribute.DiffuseG;
            case B:
                if (isNormal)
                    return TerrainAttribute.NormalMapB;
                else
                    return TerrainAttribute.DiffuseB;
            case A:
                if (isNormal)
                    return TerrainAttribute.NormalMapA;
                else
                    return TerrainAttribute.DiffuseA;
            default:
                throw new GdxRuntimeException("Invalid channel");
        }
    }

    public SplatTexture.Channel getNextFreeChannel() {
        // base
        SplatTexture st = textures.get(SplatTexture.Channel.BASE);
        if (st == null || st.texture.getID() == null) return SplatTexture.Channel.BASE;
        // r
        st = textures.get(SplatTexture.Channel.R);
        if (st == null) return SplatTexture.Channel.R;
        // g
        st = textures.get(SplatTexture.Channel.G);
        if (st == null) return SplatTexture.Channel.G;
        // b
        st = textures.get(SplatTexture.Channel.B);
        if (st == null) return SplatTexture.Channel.B;
        // a
        st = textures.get(SplatTexture.Channel.A);
        if (st == null) return SplatTexture.Channel.A;

        return null;
    }

    public boolean hasTextureChannel(SplatTexture.Channel channel) {
        return textures.containsKey(channel);
    }

    public boolean hasNormalChannel(SplatTexture.Channel channel) {
        return normalTextures.containsKey(channel);
    }

    public int countTextures() {
        return textures.size();
    }

    public Map<SplatTexture.Channel, SplatTexture> getTextures() {
        return textures;
    }

    public Map<SplatTexture.Channel, SplatTexture> getNormalTextures() {
        return normalTextures;
    }

    public SplatMap getSplatmap() {
        return splatmap;
    }

    public void setSplatmap(SplatMap splatmap) {
        this.splatmap = splatmap;
        if (splatmap == null) {
            remove(TerrainAttribute.SplatMap);
        } else {
            set(new TerrainAttribute(TerrainAttribute.SplatMap));
        }
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }

    public boolean hasNormalTextures() {
        return normalTextures.size() > 0;
    }

}
