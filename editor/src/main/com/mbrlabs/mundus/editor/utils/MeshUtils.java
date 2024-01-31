package com.mbrlabs.mundus.editor.utils;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.BufferUtils;
import org.lwjgl.util.meshoptimizer.MeshOptimizer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * @author JamesTKhan
 * @version September 28, 2023
 */
public class MeshUtils {

    /**
     * Result of simplifying meshes, contains the new vertices and indices only
     * so we can run this on a background thread.
     * The first dimension of the vertices and indices arrays is the mesh index.
     */
    public static class SimplifyResult {
        // [Mesh][Vertices]
        private final float[][] vertices;

        // [Mesh][Indices]
        private final short[][] indices;

        public SimplifyResult(float[][] vertices, short[][] indices) {
            this.vertices = vertices;
            this.indices = indices;
        }

        public float[][] getVertices() {
            return vertices;
        }

        public short[][] getIndices() {
            return indices;
        }
    }

    /**
     * Uses MeshOptimizer Simplfy to simplify the given model. The resulting vertices and indices are returned.
     * in a SimplifyResult object.
     * @param model The model to simplify
     * @param simplificationFactor The multiplier to apply to the number of indices .5 == 50% target of original indices
     * @param targetError The target error to use for simplification 0.1f == 10% error
     * @return The SimplifyResult object containing the new vertices and indices
     */
    public static SimplifyResult simplify(Model model, float simplificationFactor, float targetError) {
        // validate simplification factor
        if (simplificationFactor <= 0.0 || simplificationFactor > 1) {
            throw new IllegalArgumentException("Simplification factor must be > 0 and <= 1");
        }

        float[][] vertices = new float[model.meshes.size][];
        short[][] indices = new short[model.meshes.size][];

        for (int i = 0; i < model.meshes.size; i++) {
            Mesh mesh = model.meshes.get(i);

            int vertexSize = mesh.getVertexSize() / Float.BYTES;
            short[] origIndices = new short[mesh.getNumIndices()];
            float[] origVertices = new float[mesh.getNumVertices() * vertexSize];

            mesh.getVertices(origVertices);
            mesh.getIndices(origIndices);

            // MeshOptimizer wants IntBuffers
            IntBuffer source = BufferUtils.newIntBuffer(origIndices.length);
            for (short value : origIndices) {
                source.put(value);
            }
            source.flip();

            // The resulting indices after simplification will be stored in this buffer
            IntBuffer destination = BufferUtils.newIntBuffer(mesh.getNumIndices());

            // Create ByteBuffer and derive FloatBuffer view
            ByteBuffer byteBuffer = BufferUtils.newByteBuffer(origVertices.length * Float.BYTES);
            FloatBuffer vertBuffer = byteBuffer.asFloatBuffer();

            vertBuffer.put(origVertices);
            vertBuffer.flip();

            float scale = MeshOptimizer.meshopt_simplifyScale(vertBuffer, mesh.getNumVertices(), mesh.getVertexSize());

            // Absolute error must be divided by the scaling factor before passing it to simplify as target_error.
            float scaledTargetError = targetError / scale;

            // Actual simplification
            int targetIndexCount = (int) (mesh.getNumIndices() * simplificationFactor);
            long newIndicesCount = MeshOptimizer.meshopt_simplify(destination, source, vertBuffer, mesh.getNumVertices(), mesh.getVertexSize(), targetIndexCount, scaledTargetError, MeshOptimizer.meshopt_SimplifyLockBorder, null);

            // Optimize the new index buffer for vertex cache efficiency
            ByteBuffer newVertexBuffer = BufferUtils.newByteBuffer(mesh.getNumVertices() * mesh.getVertexSize());
            long uniqueVertices = MeshOptimizer.meshopt_optimizeVertexFetch(newVertexBuffer, destination, byteBuffer, mesh.getNumVertices(), mesh.getVertexSize());

            float[] newVertices = new float[(int) uniqueVertices * vertexSize];
            newVertexBuffer.asFloatBuffer().get(newVertices, 0, newVertices.length);

            // Sadly, libGDX doesn't support int indices, so we have to convert them back to short
            short[] newInd = new short[(int) newIndicesCount];
            for (int j = 0; j < newIndicesCount; j++) {
                newInd[j] = (short) destination.get(j);
            }

//            Gdx.app.log("TEST", "Old indices: " + origIndices.length);
//            Gdx.app.log("TEST", "New indices: " + newInd.length);

            indices[i] = newInd;
            vertices[i] = newVertices;
        }

        return new SimplifyResult(vertices, indices);
    }
}
