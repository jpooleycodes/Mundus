package com.mbrlabs.mundus.commons.terrain;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import net.mgsx.gltf.loaders.shared.geometry.MeshTangentSpaceGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Used for building a subdivideable plane mesh for things like terrain.
 * @author JamesTKhan
 * @version June 06, 2023
 */
public class PlaneMesh implements Disposable {
    static class MeshInfo {
        public int vertexResolution;
        public float[] heightData;
        public int width;
        public int depth;
        public Vector2 uvScale;
        public VertexAttributes attribs;
    }

    private static final MeshPartBuilder.VertexInfo tempVertexInfo = new MeshPartBuilder.VertexInfo();

    private final int vertexResolution;

    private final MeshInfo terrainMeshInfo;
    private VertexAttributes attribs;

    private float[] vertices;
    private short[] indices;

    private final int stride;
    private final int posPos;
    private final int norPos;
    private final int uvPos;

    // Tracks the modified vertices bounds
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    private Map<Integer, Array<Integer>> vertexToTriangleMap;

    private Mesh mesh;

    public PlaneMesh(MeshInfo terrainMeshInfo) {
        this.terrainMeshInfo = terrainMeshInfo;
        this.vertexResolution = terrainMeshInfo.vertexResolution;

        this.attribs = terrainMeshInfo.attribs;

        this.posPos = attribs.getOffset(VertexAttributes.Usage.Position, -1);
        this.norPos = attribs.getOffset(VertexAttributes.Usage.Normal, -1);
        this.uvPos = attribs.getOffset(VertexAttributes.Usage.TextureCoordinates, -1);
        this.stride = attribs.vertexSize / 4;
    }

    public Mesh buildMesh() {
        final int numVertices = terrainMeshInfo.vertexResolution * terrainMeshInfo.vertexResolution;
        final int numIndices = (terrainMeshInfo.vertexResolution - 1) * (terrainMeshInfo.vertexResolution - 1) * 6;

        indices = buildIndices();
        buildVertexToTriangleMap();

        if (vertices == null) {
            vertices = new float[numVertices * stride];
        }

        buildVertices();

        Mesh mesh = new Mesh(true, numVertices, numIndices, attribs);
        mesh.setIndices(indices);
        mesh.setVertices(vertices);

        this.mesh = mesh;
        return mesh;
    }

    private short[] buildIndices() {
        final int vertexResolution = terrainMeshInfo.vertexResolution;
        final int w = vertexResolution - 1;
        final int h = vertexResolution - 1;
        short[] indices = new short[w * h * 6];
        int i = -1;
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                final int c00 = y * vertexResolution + x;
                final int c10 = c00 + 1;
                final int c01 = c00 + vertexResolution;
                final int c11 = c10 + vertexResolution;
                indices[++i] = (short) c11;
                indices[++i] = (short) c10;
                indices[++i] = (short) c00;
                indices[++i] = (short) c00;
                indices[++i] = (short) c01;
                indices[++i] = (short) c11;
            }
        }
        return indices;
    }

    /**
     * This method builds a map that associates each vertex index with a list of indices
     * of triangles that the vertex is part of. This map is used for efficient lookup
     * of adjacent triangles when calculating vertex normals.
     *
     * The map is stored in the instance variable vertexToTriangleMap, where the key is
     * the vertex index and the value is a list of triangle indices.
     *
     * Note: This method is to be called during the mesh building process, after the
     * indices array has been populated.
     */
    private void buildVertexToTriangleMap() {
        vertexToTriangleMap = new HashMap<>();
        for (int i = 0; i < indices.length; i += 3) {
            int triangleIndex = i / 3;
            for (int j = 0; j < 3; j++) {
                int vertexIndex = (indices[i + j] & 0xFFFF);
                Array<Integer> triangleIndices = vertexToTriangleMap.get(vertexIndex);
                if (triangleIndices == null) {
                    triangleIndices = new Array<>();
                    vertexToTriangleMap.put(vertexIndex, triangleIndices);
                }
                triangleIndices.add(triangleIndex);
            }
        }
    }

    public void buildVertices() {
        int vertexResolution = terrainMeshInfo.vertexResolution;
        if (minX <= maxX && minZ <= maxZ) {
            // If we have a bounding box, only update that region.
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    calculateVertexAt(tempVertexInfo, x, z);
                    setVertex(z * vertexResolution + x, tempVertexInfo);
                }
            }
        } else {
            // If the bounding box is empty, then we need to update all vertices.
            for (int x = 0; x < vertexResolution; x++) {
                for (int z = 0; z < vertexResolution; z++) {
                    calculateVertexAt(tempVertexInfo, x, z);
                    setVertex(z * vertexResolution + x, tempVertexInfo);
                }
            }
        }
    }

    private void setVertex(int index, MeshPartBuilder.VertexInfo info) {
        index *= stride;
        if (posPos >= 0) {
            vertices[index + posPos] = info.position.x;
            vertices[index + posPos + 1] = info.position.y;
            vertices[index + posPos + 2] = info.position.z;
        }
        if (uvPos >= 0) {
            vertices[index + uvPos] = info.uv.x;
            vertices[index + uvPos + 1] = info.uv.y;
        }
        if (norPos >= 0) {
            // The final normal is calculated after vertices are built in calculateAverageNormals
            vertices[index + norPos] = 0f;
            vertices[index + norPos + 1] = 1f;
            vertices[index + norPos + 2] = 0f;
        }
    }

    private MeshPartBuilder.VertexInfo calculateVertexAt(MeshPartBuilder.VertexInfo out, int x, int z) {
        int vertexResolution = terrainMeshInfo.vertexResolution;

        final float dx = (float) x / (float) (vertexResolution - 1);
        final float dz = (float) z / (float) (vertexResolution - 1);
        final float height = terrainMeshInfo.heightData[z * vertexResolution + x];

        out.position.set(dx * terrainMeshInfo.width, height, dz * terrainMeshInfo.depth);
        out.uv.set(dx, dz).scl(terrainMeshInfo.uvScale);

        return out;
    }

    /**
     * This method calculates and sets the average normal for each vertex in the terrain mesh.
     * It first calculates the normal of each face (triangle) in the mesh, then for each vertex,
     * it calculates the average normal from the normals of all faces that include this vertex.
     *
     * Note: This method should be called after the vertices and indices of the mesh have been defined and set.
     * It directly modifies the vertices array to set the normal for each vertex.
     * @param pool A pool of Vector3 objects to be used for temporary calculations. If on
     *             a background thread, this pool should be thread-safe.
     */
    public void calculateAverageNormals(Pool<Vector3> pool) {
        final int numIndices = (this.vertexResolution - 1) * (vertexResolution - 1) * 6;

        Vector3 v1 = pool.obtain();
        Vector3 v2 = pool.obtain();
        Vector3 v3 = pool.obtain();

        // Calculate face normals for each triangle and store them in an array
        Vector3[] faceNormals = new Vector3[numIndices / 3];
        for (int i = 0; i < numIndices; i += 3) {
            getVertexPos(v1, indices[i] & 0xFFFF);
            getVertexPos(v2, indices[i + 1] & 0xFFFF);
            getVertexPos(v3, indices[i + 2] & 0xFFFF);
            Vector3 normal = calculateFaceNormal(pool.obtain(), v1, v2, v3);
            faceNormals[i / 3] = normal;
        }

        int minXIndex, maxXIndex, minZIndex, maxZIndex;
        if (minX <= maxX && minZ <= maxZ) {
            // Only calculate normals for vertices within the modified region
            minXIndex = Math.max(0, minX - 1);
            maxXIndex = Math.min(vertexResolution - 1, maxX + 1);
            minZIndex = Math.max(0, minZ - 1);
            maxZIndex = Math.min(vertexResolution - 1, maxZ + 1);
        } else {
            // Calculate normals for all vertices
            minXIndex = 0;
            maxXIndex = vertexResolution - 1;
            minZIndex = 0;
            maxZIndex = vertexResolution - 1;
        }

        // Calculate and set vertex normals
        for (int z = minZIndex; z <= maxZIndex; z++) {
            for (int x = minXIndex; x <= maxXIndex; x++) {
                int i = z * vertexResolution + x;
                calculateVertexNormal(v1, i, faceNormals);
                setVertexNormal(i, v1);
            }
        }

        pool.free(v1);
        pool.free(v2);
        pool.free(v3);
        for (Vector3 normal : faceNormals) {
            pool.free(normal);
        }
    }


    /**
     * Retrieve the vertex x,y,z position from the vertices array for the given vertex index.
     */
    private void getVertexPos(Vector3 out, int index) {
        int start = index * stride;
        out.set(vertices[start + posPos], vertices[start + posPos + 1], vertices[start + posPos + 2]);
    }

    /**
     * Set the vertex x,y,z normal in the vertices array for the given vertex index.
     */
    private void setVertexNormal(int vertexIndex, Vector3 normal) {
        int start = vertexIndex * stride;
        vertices[start + norPos] = normal.x;
        vertices[start + norPos + 1] = normal.y;
        vertices[start + norPos + 2] = normal.z;
    }

    /**
     * This method calculates the normal of a face in 3D space given its three vertices.
     * The face is assumed to be a triangle.
     *
     * @param out The Vector3 to store the result in.
     * @param vertex1 The first vertex of the triangle.
     * @param vertex2 The second vertex of the triangle.
     * @param vertex3 The third vertex of the triangle.
     *
     * @return A normalized Vector3 representing the normal of the face.
     */
    private Vector3 calculateFaceNormal(Vector3 out, Vector3 vertex1, Vector3 vertex2, Vector3 vertex3) {
        Vector3 edge1 = vertex2.sub(vertex1); // Vector from vertex1 to vertex2
        Vector3 edge2 = vertex3.sub(vertex1); // Vector from vertex1 to vertex3
        out.set(edge1).crs(edge2); // Cross product of edge1 and edge2
        return out.nor(); // Return the normalized normal vector
    }

    /**
     * This method calculates the average normal of a vertex by averaging the normals
     * of all the faces that the vertex is part of.
     *
     * @param vertexIndex The index of the vertex for which the normal is to be calculated.
     * @param faceNormals An array containing the normals of all faces in the mesh.
     *
     * @return A normalized Vector3 representing the average normal of the vertex.
     */
    private Vector3 calculateVertexNormal(Vector3 out, int vertexIndex, Vector3[] faceNormals) {
        Vector3 vertexNormal = out.set(0,0,0);
        Array<Integer> triangleIndices = vertexToTriangleMap.get(vertexIndex);
        if (triangleIndices != null) {
            for (int triangleIndex : triangleIndices) {
                Vector3 faceNormal = faceNormals[triangleIndex];
                if (faceNormal != null) {
                    vertexNormal.add(faceNormal);
                }
            }
        }
        return vertexNormal.nor();
    }

    @Override
    public void dispose() {
        mesh.dispose();
    }

    /**
     * Get Vertex Normal at x,z point of terrain
     *
     * @param out
     *            Output vector
     * @param x
     *            the x coord on terrain
     * @param z
     *            the z coord on terrain
     * @return the normal at the point of terrain
     */
    public Vector3 getNormalAt(Vector3 out, int x, int z) {
        int vertexIndex = z * vertexResolution + x;
        int start = vertexIndex * stride;
        return out.set(vertices[start + norPos], vertices[start + norPos + 1], vertices[start + norPos + 2]);
    }


    public float[] getVertices() {
        return vertices;
    }

    public void computeTangents() {
        VertexAttribute normalMapUVs = null;
        for(VertexAttribute a : attribs){
            if(a.usage == VertexAttributes.Usage.TextureCoordinates){
                normalMapUVs = a;
            }
        }
        MeshTangentSpaceGenerator.computeTangentSpace(vertices, indices, attribs, false, true, normalMapUVs);
    }

    public void updateMeshVertices() {
        mesh.setVertices(vertices);
        resetBoundingBox();
    }

    void resetBoundingBox() {
        // reset bounding box
        minX = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        minZ = Integer.MAX_VALUE;
        maxZ = Integer.MIN_VALUE;
    }

    /**
     * When only a subsection of the terrain is modified, this method can be used to track
     * and expand the bounding box of the modified region. This allows the terrain to only update the
     * vertices that are affected by the modification.
     *
     * @param x the x coordinate of the modified vertex
     * @param z the z coordinate of the modified vertex
     */
    public void modifyVertex(int x, int z) {
        // Expand the bounding box to include this vertex.
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
    }
}
