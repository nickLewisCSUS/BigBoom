package tage.nodeControllers;

import tage.*;
import org.joml.*;
import java.lang.Math;

public class BounceController extends NodeController {
    private float initialY;         // Stores the starting height
    private float bounceHeight = 1.5f; // Default bounce height
    private float bounceSpeed = 2.0f;  // Speed of bouncing
    private boolean initialized = false; // Ensures we set the initial height only once

    @Override
    public void apply(GameObject go) {
        // Set the initial height only once
        if (!initialized) {
            initialY = go.getWorldLocation().y;
            initialized = true;
        }

        // Get the object's scale to dynamically adjust bounce height
        Vector3f scale = new Vector3f();
        go.getWorldScale().getScale(scale);
        float objectHeight = scale.y; // Height of the object (assuming Y is up)

        // Adjust bounce height dynamically based on object scale
        float adjustedBounceHeight = objectHeight * 0.5f + bounceHeight;

        // Update elapsed time
        float elapsedTime = getElapsedTimeTotal() / 1000.0f; // Convert ms to seconds

        // Compute new Y position using a sine wave
        float newY = initialY + (float) Math.abs(Math.sin(elapsedTime * bounceSpeed)) * adjustedBounceHeight;

        // Ensure it never dips below its original height
        newY = Math.max(newY, initialY);

        // Preserve X and Z positions
        Vector3f currentPosition = go.getWorldLocation();
        float x = currentPosition.x;
        float z = currentPosition.z;

        // Apply the new transformation
        go.setLocalTranslation(new Matrix4f().translation(x, newY, z));
    }
}