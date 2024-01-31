package com.mbrlabs.mundus.editor.utils;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;

/**
 * @author JamesTKhan
 * @version October 05, 2023
 */
public class ThreadLocalPools {
    /**
     * ThreadLocal pool for Vector3 objects when accessing from other threads
     */
    public static ThreadLocal<Pool<Vector3>> vector3ThreadPool = ThreadLocal.withInitial(() -> new Pool<Vector3>() {
        @Override
        protected Vector3 newObject () {
            return new Vector3();
        }

        @Override
        protected void reset(Vector3 object) {
            object.set(0,0,0);
        }
    });
}
