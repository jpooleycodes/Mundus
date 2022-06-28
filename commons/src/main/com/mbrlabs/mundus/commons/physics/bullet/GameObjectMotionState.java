package com.mbrlabs.mundus.commons.physics.bullet;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.commons.scene3d.components.ModelComponent;
/**
 * @author James Pooley
 * @version June 15, 2022
 */
public class GameObjectMotionState extends btMotionState {
    private static final Vector3 tmp = new Vector3();
    private static final Vector3 tmp2 = new Vector3();
    private static final Quaternion tmpQuat = new Quaternion();
    private static final BoundingBox boundingBox = new BoundingBox();

    GameObject gameObject;
    float halfHeight = 0;

    public GameObjectMotionState(GameObject gameObject) {
        this.gameObject = gameObject;
        ModelComponent modelComponent = (ModelComponent) gameObject.findComponentByType(Component.Type.MODEL);
        if (modelComponent != null) {
            modelComponent.getModelInstance().calculateBoundingBox(boundingBox);
            halfHeight = boundingBox.getDimensions(tmp).y / 2f;
        }
    }

    @Override
    public void getWorldTransform (Matrix4 worldTrans) {
        worldTrans.set(gameObject.getPosition(tmp), gameObject.getRotation(tmpQuat), gameObject.getLocalScale(tmp2));
    }

    @Override
    public void setWorldTransform (Matrix4 worldTrans) {
        // GameObjects rely on vectors, so we update their vectors and not a matrix.
        worldTrans.getTranslation(tmp);
        worldTrans.getRotation(tmpQuat);
        gameObject.setLocalPosition(tmp.x, tmp.y - halfHeight, tmp.z);
        gameObject.setLocalRotation(tmpQuat.x, tmpQuat.y, tmpQuat.z, tmpQuat.w);
    }
}

