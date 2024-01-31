
package com.mbrlabs.mundus.editor.terrain.noise.modifiers;

import com.mbrlabs.mundus.editor.terrain.noise.TerrainNoiseData;
import com.mbrlabs.mundus.editor.utils.FastNoiseLite;

/**
 * Base Noise Modifier for modifying terrain noise
 * @author JamesTKhan
 * @version November 02, 2022
 */
public abstract class NoiseModifier implements TerrainModifier {
    protected static final FastNoiseLite.Vector2 fVector2 = new FastNoiseLite.Vector2(0,0);
    protected FastNoiseLite noise = new FastNoiseLite();
    protected float domainWarpFrequency = 0.0f;
    protected FastNoiseLite.NoiseType type;
    protected FastNoiseLite.FractalType fractalType;
    protected FastNoiseLite.DomainWarpType domainType;
    protected float domainWarpAmps = 0.0f;
    protected float frequency = 0.0f;
    protected boolean additive = false;

    public FastNoiseLite getNoiseGenerator() {
        return noise;
    }

    @Override
    public void modify(TerrainNoiseData terrainNoiseData, float x, float y) {
        fVector2.x = x;
        fVector2.y = y;

        // Apply domain warp
        noise.SetFrequency(domainWarpFrequency);
        noise.DomainWarp(fVector2);

        // Reset regular frequency
        noise.SetFrequency(frequency);
    }

    public FastNoiseLite.NoiseType getType() {
        return type;
    }

    public void setType(final FastNoiseLite.NoiseType type) {
        this.type = type;
        noise.SetNoiseType(type);
    }

    public FastNoiseLite.FractalType getFractalType() {
        return fractalType;
    }

    public void setFractalType(final FastNoiseLite.FractalType fractalType) {
        this.fractalType = fractalType;
        noise.SetFractalType(fractalType);
    }

    public FastNoiseLite.DomainWarpType getDomainType() {
        return domainType;
    }

    public void setDomainType(final FastNoiseLite.DomainWarpType domainType) {
        this.domainType = domainType;
        noise.SetDomainWarpType(domainType);
    }

    public float getDomainWarpAmps() {
        return domainWarpAmps;
    }

    public void setDomainWarpAmps(final float domainWarpAmps) {
        this.domainWarpAmps = domainWarpAmps;
        noise.SetDomainWarpAmp(domainWarpAmps);
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    public float getDomainWarpFrequency() {
        return domainWarpFrequency;
    }

    public void setDomainWarpFrequency(float domainWarpFrequency) {
        this.domainWarpFrequency = domainWarpFrequency;
    }

    public void setFractalGain(float gain) {
        this.noise.SetFractalGain(gain);
    }

    public float getFractalGain() {
        return noise.GetFractalGain();
    }

    public void setFractalLacunarity(float lacunariy) {
        this.noise.SetFractalLacunarity(lacunariy);
    }

    public float getFractalLacunarity() {
        return noise.GetFractalLacunarity();
    }

    public boolean getNoiseAdditive() {return this.additive;}

    public void setNoiseAdditive(boolean additive) {this.additive = additive;}
}
