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

package com.mbrlabs.mundus.editor.tools.brushes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.mbrlabs.mundus.commons.assets.TerrainAsset;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent;
import com.mbrlabs.mundus.commons.terrain.SplatMap;
import com.mbrlabs.mundus.commons.terrain.SplatTexture;
import com.mbrlabs.mundus.commons.terrain.Terrain;
import com.mbrlabs.mundus.commons.utils.MathUtils;
import com.mbrlabs.mundus.commons.utils.Pools;
import com.mbrlabs.mundus.editor.Mundus;
import com.mbrlabs.mundus.editor.core.project.ProjectManager;
import com.mbrlabs.mundus.editor.events.GameObjectSelectedEvent;
import com.mbrlabs.mundus.editor.events.GlobalBrushSettingsChangedEvent;
import com.mbrlabs.mundus.editor.events.TerrainVerticesChangedEvent;
import com.mbrlabs.mundus.editor.history.CommandHistory;
import com.mbrlabs.mundus.editor.history.commands.TerrainsHeightCommand;
import com.mbrlabs.mundus.editor.history.commands.TerrainsPaintCommand;
import com.mbrlabs.mundus.editor.shader.EditorPBRTerrainShader;
import com.mbrlabs.mundus.editor.tools.Tool;
import com.mbrlabs.mundus.editor.tools.picker.GameObjectPicker;
import com.mbrlabs.mundus.editor.tools.terrain.FlattenTool;
import com.mbrlabs.mundus.editor.tools.terrain.RaiseLowerTool;
import com.mbrlabs.mundus.editor.tools.terrain.SmoothTool;
import com.mbrlabs.mundus.editor.tools.terrain.TerrainTool;
import com.mbrlabs.mundus.editor.ui.UI;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * A Terrain Brush can modify the terrainAsset in various ways (BrushMode).
 * <p>
 * This includes the height of every vertex in the terrainAsset grid & according
 * splatmap.
 *
 * @author Marcus Brummer
 * @version 30-01-2016
 */
public abstract class TerrainBrush extends Tool {

    /**
     * Defines the draw mode of a brush.
     */
    public enum BrushMode {
        /** Raises or lowers the terrainAsset height. */
        RAISE_LOWER,
        /** Sets all vertices of the selection to a specified height. */
        FLATTEN,
        /** Smooths terrain based on average height within radius */
        SMOOTH,
        /** Paints on the splatmap of the terrainAsset. */
        PAINT,
        /** Create a ramp between two points. */
        RAMP
    }

    /**
     * Defines two actions (and it's key codes) every brush and every mode can
     * have.
     * <p>
     * For instance the RAISE_LOWER mode has 'raise' has PRIMARY action and
     * 'lower' as secondary. Pressing the keycode of the secondary & the primary
     * key enables the secondary action.
     **/
    public enum BrushAction {
        PRIMARY(Input.Buttons.LEFT), SECONDARY(Input.Keys.SHIFT_LEFT);

        public final int code;

        BrushAction(int levelCode) {
            this.code = levelCode;
        }

    }

    /**
     * The range of the brush. This is the area in which the brush
     * has altered the terrain.
     */
    public static class BrushRange {
        public int minX;
        public int maxX;
        public int minZ;
        public int maxZ;
    }

    /**
     * An action that can modify the terrain in some way.
     */
    public interface TerrainModifyAction {
        void modify(TerrainBrush terrainBrush, TerrainComponent terrain, int x, int z, Vector3 localBrushPos, Vector3 vertexPos);
    }

    /**
     * A comparison that can be used to determine if a vertex should be modified
     */
    public interface TerrainModifyComparison {
        boolean compare(TerrainBrush terrainBrush, Vector3 vertexPos, Vector3 localBrushPos);
    }

    /**
     * Thrown if the brush is set to a mode, which it currently does not
     * support.
     */
    public static class ModeNotSupportedException extends Exception {
        public ModeNotSupportedException(String message) {
            super(message);
        }
    }

    // used for calculations
    protected static final Vector3 rampEndPoint = new Vector3();
    protected static final Vector2 c = new Vector2();
    protected static final Vector2 p = new Vector2();
    protected static final Vector2 v = new Vector2();
    protected static final Color c0 = new Color();
    protected static final Vector3 tVec0 = new Vector3();
    protected static final Vector3 tVec1 = new Vector3();
    private static final Matrix4 tmpMatrix = new Matrix4();

    // all brushes share the some common settings
    private static final GlobalBrushSettingsChangedEvent brushSettingsChangedEvent = new GlobalBrushSettingsChangedEvent();
    private static final GameObjectPicker.ComponentIgnoreFilter ignoreFilter = component -> !(component instanceof TerrainComponent);
    private static final TerrainTool raiseLowerTool = new RaiseLowerTool();
    private static final TerrainTool flattenTool = new FlattenTool();
    private static final TerrainTool smoothTool = new SmoothTool();
    private static boolean optimizeTerrainUpdates = false;
    private static float strength = 0.5f;
    private static float heightSample = 0f;
    private static SplatTexture.Channel paintChannel;

    // individual brush settings
    protected final Vector3 brushPos = new Vector3();
    protected float radius = 25f;
    protected TerrainComponent terrainComponent;
    protected BrushMode mode;
    private BrushAction action;
    private final BrushRange brushRange;

    private boolean mouseMoved = false;

    // the pixmap brush
    private final Pixmap brushPixmap;
    private final int pixmapCenter;

    // undo/redo system
    private TerrainsHeightCommand heightCommand = null;
    private TerrainsPaintCommand paintCommand = null;
    private boolean terrainHeightModified = false;
    private boolean splatmapModified = false;
    private boolean refreshConnectedTerrains = false;

    // Holds all terrains connected to the current terrain
    private final Set<TerrainComponent> connectedTerrains = new HashSet<>();

    // Holds all terrains that have been modified by the brush, for when optimized terrain updates is enabled
    private final Set<TerrainComponent> modifiedTerrains = new HashSet<>();

    private final GameObjectPicker goPicker;

    public TerrainBrush(ProjectManager projectManager, CommandHistory history,
            FileHandle pixmapBrush, GameObjectPicker goPicker) {
        super(projectManager, history);

        this.goPicker = goPicker;
        brushRange = new BrushRange();
        brushPixmap = new Pixmap(pixmapBrush);
        pixmapCenter = brushPixmap.getWidth() / 2;
    }

    @Override
    public void act() {
        if (action == null) return;
        if (terrainComponent == null) return;

        // sample height
        if (action == BrushAction.SECONDARY && (mode == BrushMode.FLATTEN)) {
            // brushPos is in world coords, convert to terrains local height by negating world height
            heightSample = brushPos.y - getTerrainPosition(tVec0).y;
            UI.INSTANCE.getToaster().success("Height Sampled: " + heightSample);
            action = null;
            return;
        }

        // Sample end point for ramp
        if (action == BrushAction.SECONDARY && (mode == BrushMode.RAMP)) {
            // brushPos is in world coords, convert to terrains local height by negating world height
            rampEndPoint.set(brushPos.x, brushPos.y - getTerrainPosition(tVec0).y, brushPos.z);
            UI.INSTANCE.getToaster().success("End Point Sampled: " + rampEndPoint);
            action = null;
            return;
        }

        // only act if mouse has been moved
        if (!mouseMoved) return;
        mouseMoved = false;

        if (mode == BrushMode.PAINT) {
            paint();
        } else if (mode == BrushMode.RAISE_LOWER) {
            raiseLowerTool.act(this);
        } else if (mode == BrushMode.FLATTEN) {
            flattenTool.act(this);
        } else if (mode == BrushMode.SMOOTH) {
            smoothTool.act(this);
        } else if (mode == BrushMode.RAMP) {
            createRamp();
        }

    }

    private void paint() {
        paint(terrainComponent, true);
    }

    private void paint(TerrainComponent terrainComponent, boolean updateNeighbors) {
        if (updateNeighbors) {
            Set<TerrainComponent> allNeighbors = getAllConnectedTerrains();

            for (TerrainComponent neighbor : allNeighbors) {
                if (neighbor == terrainComponent) continue;

                if (brushAffectsTerrain(brushPos, radius, neighbor)) {
                    paint(neighbor, false);
                }
            }
        }


        Terrain terrain = terrainComponent.getTerrainAsset().getTerrain();
        SplatMap sm = terrain.getTerrainTexture().getSplatmap();
        if (sm == null) return;

        // should convert world position to terrain local position
        tVec1.set(brushPos);
        getWorldToLocalPosition(terrainComponent, tVec1);

        final float splatX = (tVec1.x / (float) terrain.terrainWidth) * sm.getWidth();
        final float splatY = (tVec1.z / (float) terrain.terrainDepth) * sm.getHeight();
        final float splatRad = (radius / terrain.terrainWidth) * sm.getWidth();
        final Pixmap pixmap = sm.getPixmap();

        boolean modified = false;

        for (int smX = 0; smX < pixmap.getWidth(); smX++) {
            for (int smY = 0; smY < pixmap.getHeight(); smY++) {
                final float dst = Vector2.dst(splatX, splatY, smX, smY);
                if (dst <= splatRad) {
                    // If not already added, add the terrain to the list of modified terrains
                    if (modifiedTerrains.add(terrainComponent)) {
                        paintCommand.addTerrain(terrain);
                    }

                    final float opacity = getValueOfBrushPixmap(splatX, splatY, smX, smY, splatRad) * 0.5f * strength;
                    int newPixelColor = sm.additiveBlend(pixmap.getPixel(smX, smY), paintChannel, opacity);
                    pixmap.drawPixel(smX, smY, newPixelColor);
                    modified = true;
                }
            }
        }

        if (!modified) return;

        sm.updateTexture();
        splatmapModified = true;
        getProjectManager().current().assetManager.addModifiedAsset(terrainComponent.getTerrainAsset());
    }

    private void createRamp() {
        createRamp(terrainComponent, true);
    }

    private void createRamp(TerrainComponent terrainComponent, boolean updateNeighbors) {
        Terrain terrain = terrainComponent.getTerrainAsset().getTerrain();

        if (updateNeighbors) {
            Set<TerrainComponent> allNeighbors = getAllConnectedTerrains();

            for (TerrainComponent neighbor : allNeighbors) {
                if (neighbor == terrainComponent) continue;
                if (!rampIntersectsTerrain(neighbor, brushPos, rampEndPoint, radius)) continue;

                createRamp(neighbor, false);
            }
        }

        Vector3 localStartPoint = Pools.vector3Pool.obtain().set(brushPos);
        Vector3 localEndPoint = Pools.vector3Pool.obtain().set(rampEndPoint);
        getWorldToLocalPosition(terrainComponent, localStartPoint);
        getWorldToLocalPosition(terrainComponent, localEndPoint);

        // Calculate the direction and length of the ramp in local coordinates
        Vector3 rampDirection = new Vector3(localStartPoint).sub(localEndPoint).nor();
        float rampLength = localStartPoint.dst(localEndPoint);

        // Half width for distance checking
        float rampWidth = getScaledRadius(terrainComponent) * 2f;
        float halfWidth = rampWidth * 0.5f;

        Vector3 toVertex = Pools.vector3Pool.obtain();
        Vector2 nearestPoint = Pools.vector2Pool.obtain();
        Vector2 vertexPos2 = Pools.vector2Pool.obtain();
        Vector2 startPoint2 = Pools.vector2Pool.obtain().set(localStartPoint.x, localStartPoint.z);
        Vector2 rampEnd2 = Pools.vector2Pool.obtain().set(localEndPoint.x, localEndPoint.z);

        boolean modified = false;

        for (int x = 0; x < terrain.vertexResolution; x++) {
            for (int z = 0; z < terrain.vertexResolution; z++) {
                final Vector3 vertexPos = terrain.getVertexPosition(tVec0, x, z);
                toVertex.set(vertexPos).sub(localEndPoint);

                vertexPos2.set(vertexPos.x, vertexPos.z);
                MathUtils.findNearestPointOnLine(rampEnd2, startPoint2, vertexPos2, nearestPoint);

                float distanceToRampLine = vertexPos2.sub(nearestPoint).len();

                if (distanceToRampLine <= halfWidth) {
                    // If not already added, add the terrain to the list of modified terrains
                    if (modifiedTerrains.add(terrainComponent)) {
                        heightCommand.addTerrain(terrainComponent);
                    }

                    // Calculate the height from the ramp line
                    float projectedLength = rampDirection.dot(toVertex);
                    float slope = (localStartPoint.y - localEndPoint.y) / rampLength;
                    float rampHeight = localEndPoint.y + projectedLength * slope;

                    // Interpolate the height based on the distance from the center of the ramp
                    float interpolationFactor = 1.0f - (distanceToRampLine / halfWidth);
                    float interpolatedHeight = Interpolation.smooth2.apply(vertexPos.y, rampHeight, interpolationFactor * strength);

                    // Set the height of the vertex
                    final int index = z * terrain.vertexResolution + x;
                    terrain.heightData[index] = interpolatedHeight;
                    modified = true;
                }
            }
        }

        Pools.free(nearestPoint, vertexPos2, startPoint2, rampEnd2);
        Pools.free(toVertex, localStartPoint, localEndPoint);

        if (!modified) return;

        terrainComponent.getLodManager().disable();
        updateTerrain(terrain);
        terrainHeightModified = true;
        getProjectManager().current().assetManager.addModifiedAsset(terrainComponent.getTerrainAsset());
    }

    private static boolean rampIntersectsTerrain(TerrainComponent terrain, Vector3 rampStart, Vector3 rampEnd, float rampRadius) {
        Vector3 terrainMin = Pools.vector3Pool.obtain();
        Vector3 terrainMax = Pools.vector3Pool.obtain();
        Vector3 rampMin = Pools.vector3Pool.obtain();
        Vector3 rampMax = Pools.vector3Pool.obtain();
        Vector3 scale = Pools.vector3Pool.obtain();

        // Get the min and max coordinates of the TerrainComponent's AABB
        terrain.gameObject.getPosition(terrainMin);
        terrain.gameObject.getScale(scale);
        terrainMax.set(terrainMin).add(terrain.getTerrainAsset().getTerrain().terrainWidth * scale.x, 0, terrain.getTerrainAsset().getTerrain().terrainDepth * scale.z);

        // Get the min and max coordinates of the ramp and expand by the ramp's radius
        rampMin.set(Math.min(rampStart.x, rampEnd.x) - rampRadius, 0, Math.min(rampStart.z, rampEnd.z) - rampRadius);
        rampMax.set(Math.max(rampStart.x, rampEnd.x) + rampRadius, 0, Math.max(rampStart.z, rampEnd.z) + rampRadius);


        // Check if the bounding boxes intersect in the x and z coordinates
        boolean intersects = (terrainMin.x <= rampMax.x && terrainMax.x >= rampMin.x) &&
                (terrainMin.z <= rampMax.z && terrainMax.z >= rampMin.z);

        Pools.free(terrainMin, terrainMax, rampMin, rampMax, scale);
        return intersects;
    }

    /**
     * Returns all the connected terrains to the current terrain. This is done by performing a BFS
     * @return A set containing all the connected terrains
     */
    private Set<TerrainComponent> getAllConnectedTerrains() {
        // If we already have the connected terrains for the current terrain component, return them
        if (!refreshConnectedTerrains) return connectedTerrains;
        refreshConnectedTerrains = false;

        connectedTerrains.clear();

        // Limit how many terrains we can get to help with performance when iterating over many terrains
        int limit = 25;

        Queue<TerrainComponent> queue = new LinkedList<>();
        queue.add(terrainComponent);

        // Reuse the array to avoid creating a new one every time
        Array<TerrainComponent> neighbors = new Array<>();

        while (!queue.isEmpty() && connectedTerrains.size() < limit) {
            TerrainComponent currentTerrain = queue.poll();

            if (!connectedTerrains.contains(currentTerrain)) {
                connectedTerrains.add(currentTerrain);

               currentTerrain.getNeighbors(neighbors);
                for (TerrainComponent neighbor : neighbors) {
                    if (neighbor != null && !connectedTerrains.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
                neighbors.clear();
            }
        }

        return connectedTerrains;
    }

    public void modifyTerrain(TerrainModifyAction modifier, TerrainModifyComparison comparison, boolean updateNeighbors) {
        modifyTerrain(terrainComponent, modifier, comparison, updateNeighbors);
    }

    /**
     * Modifies the terrain using the given modifier and comparison
     * @param terrainComponent The terrain to modify
     * @param modifier The modifier to use
     * @param comparison The comparison to use
     * @param updateNeighbors Whether to update the neighbors of the terrain
     */
    public void modifyTerrain(TerrainComponent terrainComponent, TerrainModifyAction modifier, TerrainModifyComparison comparison, boolean updateNeighbors) {
        Vector3 localBrushPos = Pools.vector3Pool.obtain();

        if (updateNeighbors) {
            Set<TerrainComponent> allNeighbors = getAllConnectedTerrains();

            for (TerrainComponent neighbor : allNeighbors) {
                if (neighbor == terrainComponent) continue;

                float scaledRadius = getScaledRadius(neighbor);
                if (brushAffectsTerrain(brushPos, scaledRadius, neighbor)) {
                    modifyTerrain(neighbor, modifier, comparison, false);
                }
            }
        }

        getBrushLocalPosition(terrainComponent, localBrushPos);
        Terrain terrain = terrainComponent.getTerrainAsset().getTerrain();
        BrushRange range = calculateBrushRange(terrain, localBrushPos);

        boolean modified = false;
        // iterate over the affected vertices and modify them
        for (int x = range.minX; x < range.maxX; x++) {
            for (int z = range.minZ; z < range.maxZ; z++) {

                final Vector3 vertexPos = terrain.getVertexPosition(tVec0, x, z);
                localBrushPos.y = vertexPos.y;

                // Call the comparison function
                if (comparison.compare(this, vertexPos, localBrushPos)) {
                    // If not already added, add the terrain to the list of modified terrains
                    if (modifiedTerrains.add(terrainComponent)) {
                        heightCommand.addTerrain(terrainComponent);
                    }

                    // Call the modifier if the comparison function returns true
                    modifier.modify(this, terrainComponent, x, z, localBrushPos, vertexPos);
                    terrain.modifyVertex(x, z);
                    modified = true;
                }
            }
        }

        Pools.vector3Pool.free(localBrushPos);

        if (!modified) return;

        // Disable LoD temporarily while being modified
        terrainComponent.getLodManager().disable();

        updateTerrain(terrain);
        terrainHeightModified = true;
        getProjectManager().current().assetManager.addModifiedAsset(terrainComponent.getTerrainAsset());
    }

    private void updateTerrain(Terrain terrain) {
        if (optimizeTerrainUpdates) {
            terrain.getPlaneMesh().buildVertices();
            terrain.getPlaneMesh().updateMeshVertices();
        } else {
            terrain.update();
        }
    }

    public BrushRange calculateBrushRange(Terrain terrain, Vector3 localBrushPos) {
        // Calculate the size of each terrain cell in world units
        float terrainCellWidth = terrain.terrainWidth / (terrain.vertexResolution - 1f);
        float terrainCellDepth = terrain.terrainDepth / (terrain.vertexResolution - 1f);

        // Convert the brush position to terrain-local coordinates
        int brushX = Math.round((localBrushPos.x) / terrainCellWidth);
        int brushZ = Math.round((localBrushPos.z) / terrainCellDepth);

        // calculate the range of vertices affected by the brush
        brushRange.minX = Math.max(0, brushX - Math.round(radius / terrainCellWidth));
        brushRange.maxX = Math.min(terrain.vertexResolution, brushX + Math.round(radius / terrainCellWidth));
        brushRange.minZ = Math.max(0, brushZ - Math.round(radius / terrainCellDepth));
        brushRange.maxZ = Math.min(terrain.vertexResolution, brushZ + Math.round(radius / terrainCellDepth));

        return brushRange;
    }

    /**
     * Interpolates the brush texture in the range of centerX - radius to
     * centerX + radius and centerZ - radius to centerZ + radius. PointZ &
     * pointX lies between these ranges.
     * <p>
     * Interpolation is necessary, since the brush pixmap is fixed sized,
     * whereas the input values can scale. (Input points can be vertices or
     * splatmap texture coordinates)
     *
     * @return the interpolated r-channel value of brush pixmap at pointX,
     *         pointZ, which can be interpreted as terrainAsset height
     *         (raise/lower) or opacity (paint)
     */
    public float getValueOfBrushPixmap(float centerX, float centerZ, float pointX, float pointZ, float radius) {
        c.set(centerX, centerZ);
        p.set(pointX, pointZ);
        v.set(p.sub(c));

        final float progress = v.len() / radius;
        v.nor().scl(pixmapCenter * progress);

        final float mapX = pixmapCenter + (int) v.x;
        final float mapY = pixmapCenter + (int) v.y;
        c0.set(brushPixmap.getPixel((int) mapX, (int) mapY));

        return c0.r;
    }

    public void scale(float amount) {
        radius *= amount;
    }

    public float getRadius() {
        return radius;
    }

    public float getScaledRadius(TerrainComponent terrainComponent) {
        Vector3 scale = Pools.vector3Pool.obtain();
        float scaledRadius = radius / terrainComponent.gameObject.getScale(scale).x;
        Pools.free(scale);
        return scaledRadius;
    }

    public static float getStrength() {
        return strength;
    }

    public static void setStrength(float strength) {
        TerrainBrush.strength = strength;
        Mundus.INSTANCE.postEvent(brushSettingsChangedEvent);
    }

    public static float getHeightSample() {
        return heightSample;
    }

    public static void setPaintChannel(SplatTexture.Channel paintChannel) {
        TerrainBrush.paintChannel = paintChannel;
        Mundus.INSTANCE.postEvent(brushSettingsChangedEvent);
    }

    public static void setOptimizeTerrainUpdates(boolean optimizeTerrainUpdates) {
        TerrainBrush.optimizeTerrainUpdates = optimizeTerrainUpdates;
    }

    public BrushMode getMode() {
        return mode;
    }

    public void setMode(BrushMode mode) throws ModeNotSupportedException {
        if (!supportsMode(mode)) {
            throw new ModeNotSupportedException(getName() + " does not support " + mode);
        }
        this.mode = mode;
    }

    public TerrainAsset getTerrainAsset() {
        return terrainComponent.getTerrainAsset();
    }

    public void setTerrainComponent(TerrainComponent terrainComponent) {
        this.terrainComponent = terrainComponent;
        refreshConnectedTerrains = true;
    }

    public boolean supportsMode(BrushMode mode) {
        switch (mode) {
            case RAISE_LOWER:
            case FLATTEN:
            case PAINT:
            case SMOOTH:
            case RAMP:
                return true;
        }

        return false;
    }

    private Vector3 getTerrainPosition(Vector3 value) {
        terrainComponent.getModelInstance().transform.getTranslation(value);
        return value;
    }

    private void getBrushLocalPosition(TerrainComponent component, Vector3 value) {
        value.set(brushPos);
        getWorldToLocalPosition(component, value);
    }

    private void getWorldToLocalPosition(TerrainComponent component, Vector3 value) {
        value.mul(tmpMatrix.set(component.getModelInstance().transform).inv());
    }

    @Override
    public void render() {
        // rendering of the brush is done in the editor terrain shader
    }

    @Override
    public void dispose() {
        brushPixmap.dispose();
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (terrainHeightModified && heightCommand != null) {
            heightCommand.setHeightDataAfter();
            getHistory().add(heightCommand);

            if (optimizeTerrainUpdates) {
                for (TerrainComponent terrainComponent : modifiedTerrains) {
                    // We calculate normals after all terrain modifications are done
                    // as calculating normals is more expensive
                    Terrain terrain = terrainComponent.getTerrainAsset().getTerrain();

                    terrain.getPlaneMesh().calculateAverageNormals(Pools.vector3Pool);
                    terrain.getPlaneMesh().computeTangents();
                    terrain.getPlaneMesh().updateMeshVertices();
                }

            }

            for (TerrainComponent terrainComponent : modifiedTerrains) {
                Mundus.INSTANCE.postEvent(new TerrainVerticesChangedEvent(terrainComponent));
            }
        }

        if (splatmapModified && paintCommand != null) {
            paintCommand.setAfter();
            getHistory().add(paintCommand);
        }

        splatmapModified = false;
        terrainHeightModified = false;
        heightCommand = null;
        paintCommand = null;
        modifiedTerrains.clear();

        action = null;

        return false;
    }

    private BrushAction getAction() {
        final boolean primary = Gdx.input.isButtonPressed(BrushAction.PRIMARY.code);
        final boolean secondary = Gdx.input.isKeyPressed(BrushAction.SECONDARY.code);

        if (primary && secondary) {
            return BrushAction.SECONDARY;
        } else if (primary) {
            return BrushAction.PRIMARY;
        }

        return null;
    }

    public BrushAction getBrushAction() {
        return action;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // get action
        action = getAction();

        if (mode == BrushMode.FLATTEN || mode == BrushMode.RAISE_LOWER || mode == BrushMode.SMOOTH || mode == BrushMode.RAMP) {
            heightCommand = new TerrainsHeightCommand();
        } else if (mode == BrushMode.PAINT) {
            final SplatMap sm = terrainComponent.getTerrainAsset().getTerrain().getTerrainTexture().getSplatmap();
            if (sm != null) {
                paintCommand = new TerrainsPaintCommand();
            }
        }

        return false;
    }

    /**
     * Does the brush affect the given terrain at the given position?
     *
     * @param brushPos The position of the brush in world coordinates.
     * @param radius The radius of the brush.
     * @param terrainComponent The terrain to check.
     * @return True if the brush affects the terrain, false otherwise.
     */
    public boolean brushAffectsTerrain(Vector3 brushPos, float radius, TerrainComponent terrainComponent) {
        // Get the bounding box of the terrain in world coordinates.
        Vector3 dim = Pools.vector3Pool.obtain();
        Vector3 bPos = Pools.vector3Pool.obtain();
        Vector3 center = Pools.vector3Pool.obtain();
        Vector3 min = Pools.vector3Pool.obtain();
        Vector3 max = Pools.vector3Pool.obtain();
        Vector3 scale = Pools.vector3Pool.obtain();
        BoundingBox terrainBounds = Pools.boundingBoxPool.obtain();
        BoundingBox brushBounds = Pools.boundingBoxPool.obtain();

        bPos.set(brushPos).y = 0;
        terrainComponent.gameObject.getScale(scale);
        dim.set(this.terrainComponent.getDimensions()).scl(scale).scl(0.5f).y = 0;

        terrainComponent.gameObject.getPosition(tVec1);
        center.set(terrainComponent.getCenter()).add(tVec1).y = 0;

        min.set(center).sub(dim);
        max.set(center).add(dim);

        // Create the bounding box for the terrain
        terrainBounds.set(min, max);

        min.set(bPos).sub(radius, 0f, radius);
        max.set(bPos).add(radius, 0f, radius);

        // Create a bounding box for the brush
        brushBounds.set(min, max);

        // Check if the brush's bounding box intersects with the terrain's bounding box
        boolean intersects = brushBounds.intersects(terrainBounds);

        Pools.vector3Pool.free(dim);
        Pools.vector3Pool.free(bPos);
        Pools.vector3Pool.free(center);
        Pools.vector3Pool.free(min);
        Pools.vector3Pool.free(max);
        Pools.vector3Pool.free(scale);
        Pools.boundingBoxPool.free(terrainBounds);
        Pools.boundingBoxPool.free(brushBounds);

        return intersects;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        final boolean brushPosUpdated = updateBrushPosition(screenX, screenY);

        mouseMoved = true;

        EditorPBRTerrainShader.setPickerPosition(brushPos.x, brushPos.y, brushPos.z);

        // Show mouse position if it is on terrain
        if (brushPosUpdated) {
            UI.INSTANCE.getStatusBar().setMousePos(brushPos.x, brushPos.y, brushPos.z);
        } else {
            UI.INSTANCE.getStatusBar().clearMousePos();
        }

        return false;
    }

    /**
     * Updates the 'brushPos' variable if the mouse is on a terrain.
     * @param screenX The screen position X value.
     * @param screenY The screen position Y value.
     * @return True if 'brushPos' variable has updated otherwise false.
     */
    private boolean updateBrushPosition(int screenX, int screenY) {
        if (terrainComponent == null) return false;

        // Use picking to find current hovered terrain, filter picking to only pick terrains
        goPicker.setIgnoreFilter(ignoreFilter);
        GameObject go = goPicker.pick(getProjectManager().current().currScene, screenX, screenY);
        goPicker.clearIgnoreFilter();
        if (go == null) return false;

        TerrainComponent comp = (TerrainComponent) go.findComponentByType(Component.Type.TERRAIN);
        if (comp == null) return false;

        // If the hovered terrain is not the current terrain or connected to it, set it as the current terrain
        if (!getAllConnectedTerrains().contains(comp)) {
            setTerrainComponent(comp);
            getProjectManager().current().currScene.currentSelection = go;
            Mundus.INSTANCE.postEvent(new GameObjectSelectedEvent(go, false));
        }

        // Update the brush position
        Ray ray = getProjectManager().current().currScene.viewport.getPickRay(screenX, screenY);
        comp.getTerrainAsset().getTerrain().getRayIntersection(brushPos, ray, comp.getModelInstance().transform);

        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (amountY < 0) {
            scale(0.9f);
        } else {
            scale(1.1f);
        }
        EditorPBRTerrainShader.setPickerRadius(radius);

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return mouseMoved(screenX, screenY);
    }

    @Override
    public void onDisabled() {
        EditorPBRTerrainShader.activatePicker(false);
    }

    @Override
    public void onActivated() {
        EditorPBRTerrainShader.activatePicker(true);
        EditorPBRTerrainShader.setPickerPosition(brushPos.x, brushPos.y, brushPos.z);
        EditorPBRTerrainShader.setPickerRadius(radius);
    }

}
