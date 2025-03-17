package tage.input.action;

import tage.*;
import org.joml.*;
import net.java.games.input.Event;
import net.java.games.input.Component.Identifier;
import tage.input.*;
import java.lang.Math;

/**
 * Manages a third-person orbit camera that follows a target GameObject.
 * <p>
 * This class handles:
 * <ul>
 *     <li>Yaw (orbit rotation around the target)</li>
 *     <li>Pitch (vertical movement of the camera)</li>
 *     <li>Zoom (distance from the target)</li>
 *     <li>Automatic synchronization with the target's rotation</li>
 * </ul>
 * It also handles input bindings for controlling the camera via both keyboard and gamepad.
 */
public class CameraOrbit3D {
    private Camera camera;
    private GameObject target;
    private InputManager inputManager;
    private float azimuth, elevation, radius;
	private boolean isUserAdjustingAzimuth;
	private float lastTargetYaw = 0.0f;

    // Camera movement constraints
    private static final float MIN_RADIUS = 4.5f;
    private static final float MAX_RADIUS = 15.0f;
    private static final float MIN_ELEVATION = 3.0f;
    private static final float MAX_ELEVATION = 60.0f;

    // Movement speeds
    private static final float YAW_SPEED = 2.0f;
    private static final float PITCH_SPEED = 1.5f;
    private static final float ZOOM_SPEED = 0.5f;

    // Dead zone for gamepad input
    private static final float DEAD_ZONE = 0.15f;

    /**
     * Constructs a CameraOrbit3D instance.
     *
     * @param cam    The camera being controlled.
     * @param target The GameObject the camera orbits around.
     * @param engine The game engine, used to access input handling.
     */
    public CameraOrbit3D(Camera cam, GameObject target, Engine engine) {
        this.camera = cam;
        this.target = target;
        this.inputManager = engine.getInputManager();
        this.azimuth = 180.0f;
        this.elevation = 20.0f;
        this.radius = 10.0f;
        setupInputActions();
        updateCameraPosition();
    }

    /**
     * Updates the camera's position based on azimuth, elevation, and radius.
     * The camera remains clamped to the target and rotates with it.
     */
    public void updateCameraPosition() {
        // Get the forward direction of the target to synchronize yaw
        Vector3f forward = target.getWorldForwardVector();
        float targetYaw = (float) Math.toDegrees(Math.atan2(forward.x, forward.z));

        // Only update azimuth if the player is NOT manually adjusting it 
		if (!isUserAdjustingAzimuth) {
			float deltaYaw = targetYaw - lastTargetYaw;
			azimuth += deltaYaw; // Adjust azimuth bt rhe amount the dolphin rotated
		} else {
			isUserAdjustingAzimuth = false;
		}

        // Convert azimuth and elevation into Cartesian coordinates
        float theta = (float) Math.toRadians(azimuth);
        float phi = (float) Math.toRadians(elevation);

        float x = radius * (float) Math.cos(phi) * (float) Math.sin(theta);
        float y = radius * (float) Math.sin(phi);
        float z = radius * (float) Math.cos(phi) * (float) Math.cos(theta);

        Vector3f targetPos = target.getWorldLocation();
        Vector3f newPos = new Vector3f(targetPos.x + x, targetPos.y + y, targetPos.z + z);

        // Prevent the camera from going below the minimum elevation
        if (newPos.y < MIN_ELEVATION) newPos.y = MIN_ELEVATION;

        camera.setLocation(newPos);
        camera.lookAt(target);
		
		// Update last known target yaw for next frame
		lastTargetYaw = targetYaw;
    }

    /**
     * Adjusts the azimuth (yaw) of the camera.
     *
     * @param value The amount to change the azimuth.
     */
    public void adjustAzimuth(float value) {
		if (value !=0) {
			isUserAdjustingAzimuth = true; // Mark manual control active
		} else {
			isUserAdjustingAzimuth = false; // If no input, allow auto-follow
		}
		
        azimuth += value;
        azimuth %= 360;
        updateCameraPosition();
    }

    /**
     * Adjusts the elevation (pitch) of the camera.
     *
     * @param value The amount to change the elevation.
     */
    public void adjustElevation(float value) {
        elevation += value;
        elevation = Math.max(MIN_ELEVATION, Math.min(MAX_ELEVATION, elevation));
        updateCameraPosition();
    }

    /**
     * Adjusts the zoom level of the camera.
     *
     * @param value The amount to adjust the zoom (positive values zoom in, negative values zoom out).
     */
    public void adjustZoom(float value) {
        radius += value;
        radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
    }

    /**
     * Configures input bindings for controlling the orbit camera.
     */
    private void setupInputActions() {
        OrbitAzimuthAction orbitYawLeft = new OrbitAzimuthAction(true);
        OrbitAzimuthAction orbitYawRight = new OrbitAzimuthAction(false);
        OrbitElevationAction orbitPitchUp = new OrbitElevationAction(true);
        OrbitElevationAction orbitPitchDown = new OrbitElevationAction(false);
        OrbitZoomAction orbitZoomIn = new OrbitZoomAction(1.0f);
        OrbitZoomAction orbitZoomOut = new OrbitZoomAction(-1.0f);

        // Keyboard Controls
        inputManager.associateActionWithAllKeyboards(Identifier.Key.LEFT, orbitYawLeft,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Identifier.Key.RIGHT, orbitYawRight,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Identifier.Key.UP, orbitPitchUp,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Identifier.Key.DOWN, orbitPitchDown,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Identifier.Key.PAGEUP, orbitZoomOut,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Identifier.Key.PAGEDOWN, orbitZoomIn,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // Gamepad Controls
        inputManager.associateActionWithAllGamepads(Identifier.Axis.Z, orbitYawLeft,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllGamepads(Identifier.Axis.RZ, orbitPitchUp,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllGamepads(Identifier.Axis.RX, orbitZoomOut,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllGamepads(Identifier.Axis.RY, orbitZoomIn,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    /**
     * Handles azimuth (yaw) rotation based on user input.
     */
    private class OrbitAzimuthAction extends AbstractInputAction {
        private boolean isKeyboard;
        private float direction;

        public OrbitAzimuthAction(boolean isLeft) {
            this.isKeyboard = true;
            this.direction = isLeft ? -1.0f : 1.0f;
        }

        @Override
        public void performAction(float time, Event e) {
            float value = e.getValue();
            if (!isKeyboard) {
                if (Math.abs(value) < DEAD_ZONE) return;
                adjustAzimuth(value * YAW_SPEED * time);
            } else {
                adjustAzimuth(direction * YAW_SPEED);
            }
        }
    }

    /**
     * Handles elevation (pitch) rotation based on user input.
     */
    private class OrbitElevationAction extends AbstractInputAction {
        private boolean isKeyboard;
        private float direction;

        public OrbitElevationAction(boolean isUp) {
            this.isKeyboard = true;
            this.direction = isUp ? 1.0f : -1.0f;
        }

        @Override
        public void performAction(float time, Event e) {
            float value = e.getValue();
            if (!isKeyboard) {
                if (Math.abs(value) < DEAD_ZONE) return;
                adjustElevation(value * PITCH_SPEED * time);
            } else {
                adjustElevation(direction * PITCH_SPEED);
            }
        }
    }

    /**
     * Handles zoom adjustments based on user input.
     */
    private class OrbitZoomAction extends AbstractInputAction {
        private float direction;

        public OrbitZoomAction(float direction) {
            this.direction = direction;
        }

        @Override
        public void performAction(float time, Event e) {
            float value = e.getValue();
            if (value < 0) return;
            adjustZoom(value * direction * ZOOM_SPEED);
        }
    }
}