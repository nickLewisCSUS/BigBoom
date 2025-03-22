package tage.shapes;
import tage.*;

/**
 * **ManualOctahedron** defines a manually created **octahedron-shaped object**.
 * <p>
 * This class extends `ManualObject` and provides:
 * - **Custom vertices** defining an octahedral shape.
 * - **Texture coordinates** for proper texturing.
 * - **Normals** for lighting calculations.
 * - **Automatic initialization** upon instantiation.
 * </p>
 *
 * The octahedron consists of:
 * - **A top pyramid** (pointing up) with a shared apex.
 * - **A bottom pyramid** (pointing down) with a shared base.
 *
 * @author Tyler Burguillos
 */
public class ManualOctahedron extends ManualObject {
    private float[] vertices = new float[]{
        // Top Pyramid (pointing up)
        0.0f,  1.0f,  0.0f,   // Top vertex (A)
       -1.0f,  0.0f, -1.0f,   // Front-left (B)
        1.0f,  0.0f, -1.0f,   // Front-right (C)

        0.0f,  1.0f,  0.0f,   // Top vertex (A)
        1.0f,  0.0f, -1.0f,   // Front-right (C)
        1.0f,  0.0f,  1.0f,   // Back-right (D)

        0.0f,  1.0f,  0.0f,   // Top vertex (A)
        1.0f,  0.0f,  1.0f,   // Back-right (D)
       -1.0f,  0.0f,  1.0f,   // Back-left (E)

        0.0f,  1.0f,  0.0f,   // Top vertex (A)
       -1.0f,  0.0f,  1.0f,   // Back-left (E)
       -1.0f,  0.0f, -1.0f,   // Front-left (B)

        // Bottom Pyramid (pointing down)
        0.0f, -1.0f,  0.0f,   // Bottom vertex (F)
       -1.0f,  0.0f, -1.0f,   // Front-left (B)
        1.0f,  0.0f, -1.0f,   // Front-right (C)

        0.0f, -1.0f,  0.0f,   // Bottom vertex (F)
        1.0f,  0.0f, -1.0f,   // Front-right (C)
        1.0f,  0.0f,  1.0f,   // Back-right (D)

        0.0f, -1.0f,  0.0f,   // Bottom vertex (F)
        1.0f,  0.0f,  1.0f,   // Back-right (D)
       -1.0f,  0.0f,  1.0f,   // Back-left (E)

        0.0f, -1.0f,  0.0f,   // Bottom vertex (F)
       -1.0f,  0.0f,  1.0f,   // Back-left (E)
       -1.0f,  0.0f, -1.0f    // Front-left (B)
    };

    // Texture coordinates
    private float[] texCoords = new float[]{
        0.25f, 0.25f,   0.0f, 0.25f,   0.125f, 0.5f,   // Top pyramid
        0.25f, 0.25f,   0.125f, 0.5f,   0.375f, 0.5f,
        0.25f, 0.25f,   0.375f, 0.5f,   0.5f, 0.25f,
        0.25f, 0.25f,   0.5f, 0.25f,   0.375f, 0.0f,

        0.625f, 0.5f,   0.875f, 0.5f,   0.75f, 0.25f,   // Bottom pyramid
        0.625f, 0.5f,   0.75f, 0.25f,   0.5f, 0.25f,
        0.625f, 0.5f,   0.5f, 0.25f,   0.375f, 0.5f,
        0.625f, 0.5f,   0.375f, 0.5f,   0.5f, 0.75f
    };

    // Normals (approximate)
    private float[] normals = new float[]{
        0.0f,  1.0f,  0.0f,  // Top vertex normal
       -1.0f,  0.0f, -1.0f,  // Front-left normal
        1.0f,  0.0f, -1.0f,  // Front-right normal

        0.0f,  1.0f,  0.0f,
        1.0f,  0.0f, -1.0f,
        1.0f,  0.0f,  1.0f,

        0.0f,  1.0f,  0.0f,
        1.0f,  0.0f,  1.0f,
       -1.0f,  0.0f,  1.0f,

        0.0f,  1.0f,  0.0f,
       -1.0f,  0.0f,  1.0f,
       -1.0f,  0.0f, -1.0f,

        0.0f, -1.0f,  0.0f,  // Bottom vertex normal
       -1.0f,  0.0f, -1.0f,
        1.0f,  0.0f, -1.0f,

        0.0f, -1.0f,  0.0f,
        1.0f,  0.0f, -1.0f,
        1.0f,  0.0f,  1.0f,

        0.0f, -1.0f,  0.0f,
        1.0f,  0.0f,  1.0f,
       -1.0f,  0.0f,  1.0f,

        0.0f, -1.0f,  0.0f,
       -1.0f,  0.0f,  1.0f,
       -1.0f,  0.0f, -1.0f
    };

    /**
     * **Constructs a ManualOctahedron.**
     * <p>
     * This constructor initializes the object by setting:
     * - **Vertices** for shape definition.
     * - **Texture coordinates** for UV mapping.
     * - **Normals** for lighting effects.
     * </p>
     */
    public ManualOctahedron() {
        super();
        setNumVertices(vertices.length / 3);
        setVertices(vertices);
        setTexCoords(texCoords);
        setNormals(normals);
    }
}