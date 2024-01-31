package com.mbrlabs.mundus.commons.terrain;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.utils.Disposable;

/**
 * Level of Detail (LoD) Holds the meshes for each level of detail of a model which can
 * be swapped out as needed.
 *
 * @author JamesTKhan
 * @version September 28, 2023
 */
public class LodLevel implements Disposable {
    private final Mesh[] lodMeshes;

    public LodLevel(Mesh[] meshes) {
        lodMeshes = meshes;
    }

    public Mesh[] getLodMesh() {
        return lodMeshes;
    }

    @Override
    public void dispose() {
        // Dispose all but the base mesh lod0, that is managed by the model
        for (int i = 1 ; i < lodMeshes.length ; i++) {
            lodMeshes[i].dispose();
        }
    }
}
