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

package com.mbrlabs.mundus.commons.scene3d;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.mbrlabs.mundus.commons.utils.Pools;

/**
 * Very simple implementation of a scene graph node.
 *
 * @author Marcus Brummer
 * @version 09-06-2016
 */
public class SimpleNode<T extends SimpleNode> extends BaseNode<T> {

    static boolean WORLD_SPACE_TRANSFORM = true;

    private static final Vector3 LOCAL_FORWARD = new Vector3(0, 0, 1);

    private final Vector3 localPosition;
    private final Quaternion localRotation;
    private final Vector3 localScale;

    // root * p0 * p1 * localMat = combined (absolute transfrom)
    private final Matrix4 combined;

    /** Flag to indicate that the transform is dirty and needs to be recalculated */
    protected boolean isTransformDirty;

    /** Observable that notifies listeners when the transform is marked dirty */
    protected DirtyObservable dirtyObservable;

    public SimpleNode(int id) {
        super(id);
        localPosition = new Vector3();
        localRotation = new Quaternion();
        localScale = new Vector3(1, 1, 1);
        combined = new Matrix4();
        dirtyObservable = new DirtyObservable();
        markDirty(); // Initialize the flag as true to ensure first calculation
    }

    /**
     * Copy construction
     * 
     * @param simpleNode
     * @param id
     */
    public SimpleNode(SimpleNode simpleNode, int id) {
        super(id);
        this.localPosition = new Vector3(simpleNode.localPosition);
        this.localRotation = new Quaternion(simpleNode.localRotation);
        this.localScale = new Vector3(simpleNode.localScale);
        this.combined = new Matrix4(simpleNode.combined);
        this.dirtyObservable = new DirtyObservable();
        this.markDirty();  // Initialize the flag as true to ensure first calculation
    }

    @Override
    public Vector3 getLocalPosition(Vector3 out) {
        return out.set(localPosition);
    }

    @Override
    public Quaternion getLocalRotation(Quaternion out) {
        return out.set(localRotation);
    }

    @Override
    public Vector3 getLocalScale(Vector3 out) {
        return out.set(localScale);
    }

    @Override
    public Vector3 getPosition(Vector3 out) {
        return getTransform().getTranslation(out);
    }

    @Override
    public Quaternion getRotation(Quaternion out) {
        return getTransform().getRotation(out, true);
    }

    @Override
    public Vector3 getScale(Vector3 out) {
        return getTransform().getScale(out);
    }

    @Override
    public Matrix4 getTransform() {
        if (isTransformDirty || parent != null && parent.isTransformDirty) {
            combined.set(localPosition, localRotation, localScale);
            if (parent != null) {
                combined.mulLeft(parent.getTransform());
            }
            isTransformDirty = false;
        }
        return combined;
    }

    @Override
    public Vector3 getForwardDirection(Vector3 out) {
        return out.set(LOCAL_FORWARD).rot(getTransform()).nor();
    }

    @Override
    public void translate(Vector3 v) {
        localPosition.add(v);
        markDirty();
    }

    @Override
    public void translate(float x, float y, float z) {
        localPosition.add(x, y, z);
        markDirty();
    }

    @Override
    public void rotate(Quaternion q) {
        localRotation.mulLeft(q);
        markDirty();
    }

    @Override
    public void rotate(float x, float y, float z, float w) {
        localRotation.mulLeft(x, y, z, w);
        markDirty();
    }

    @Override
    public void rotate(float yaw, float pitch, float roll) {
        Quaternion quaternion = getRotation(Pools.quaternionPool.obtain());
        quaternion.setEulerAngles(yaw, pitch, roll);
        rotate(quaternion);
        Pools.quaternionPool.free(quaternion);
        markDirty();
    }

    @Override
    public void scale(Vector3 v) {
        localScale.scl(v);
        markDirty();
    }

    @Override
    public void scale(float x, float y, float z) {
        localScale.scl(x, y, z);
        markDirty();
    }

    @Override
    public void setLocalPosition(float x, float y, float z) {
        localPosition.set(x, y, z);
        markDirty();
    }

    @Override
    public void setLocalRotation(final Quaternion q) {
        localRotation.set(q);
        markDirty();
    }

    @Override
    public void setLocalRotation(float x, float y, float z, float w) {
        localRotation.set(x, y, z, w);
        markDirty();
    }

    @Override
    public void setLocalRotation(float yaw, float pitch, float roll) {
        Quaternion quaternion = getRotation(Pools.quaternionPool.obtain());
        quaternion.setEulerAngles(yaw, pitch, roll);
        setLocalRotation(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
        Pools.quaternionPool.free(quaternion);
        markDirty();
    }

    @Override
    public void setLocalScale(float x, float y, float z) {
        localScale.set(x, y, z);
        markDirty();
    }

    @Override
    public void addChild(T child) {
        super.addChild(child);
        child.markDirty();
    }

    public void markDirty() {
        isTransformDirty = true;
        dirtyObservable.notifyListeners();

        if (children == null) return;
        for (T child : children) {
            child.markDirty();
        }
    }

    public boolean isDirty() {
        return isTransformDirty;
    }

    public void addDirtyListener(DirtyListener listener) {
        dirtyObservable.addListener(listener);
    }

    public void removeDirtyListener(DirtyListener listener) {
        dirtyObservable.removeListener(listener);
    }
}
