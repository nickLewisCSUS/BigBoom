package tage.input.action;

import tage.*;
import org.joml.*;
import net.java.games.input.Event;
import net.java.games.input.Component;
import tage.input.*;
import java.lang.Math;

/**
 * Manages an independent overhead (top-down) camera.
 * <p>
 * - The camera starts centered on the avatar but moves independently.
 * - It can pan freely but is clamped to ±30.0f from the world origin.
 * - Uses **D-pad (Component.POV)** for panning and **shoulder buttons** for zooming.
 *
 * @author Tyler Burguillos
 */
public class OverheadCameraController {
    private Camera camera;
    private InputManager inputManager;

    private static final float PAN_SPEED = 2.0f;
    private static final float ZOOM_SPEED = 1.5f;
    private static final float MIN_ZOOM = 5.0f;
    private static final float MAX_ZOOM = 50.0f;
    private static final float WORLD_BOUND = 30.0f;
    private static final float DEAD_ZONE = 0.15f;
    private static final float RESPONSE_CURVE = 1.5f;

    /**
     * Constructs the Overhead Camera Controller.
     *
     * @param cam    The camera to control.
     * @param engine The game engine for input handling.
     * @param target The GameObject that the camera initially centers on.
     */
    public OverheadCameraController(Camera cam, GameObject target, Engine engine) {
        this.camera = cam;
        this.inputManager = engine.getInputManager();

        // Start centered on avatar
        Vector3f startPos = new Vector3f(target.getWorldLocation().x, 20.0f, target.getWorldLocation().z);
        camera.setLocation(startPos);

        setupInputActions();
    }

    /**
     * Moves the camera while ensuring it remains within world bounds.
     *
     * @param deltaX Change in X-axis position.
     * @param deltaZ Change in Z-axis position.
     */
    public void pan(float deltaX, float deltaZ) {
        Vector3f camPos = camera.getLocation();
        float newX = camPos.x + (deltaX * PAN_SPEED);
        float newZ = camPos.z + (deltaZ * PAN_SPEED);

        // Clamp within world boundaries
        newX = Math.max(-WORLD_BOUND, Math.min(WORLD_BOUND, newX));
        newZ = Math.max(-WORLD_BOUND, Math.min(WORLD_BOUND, newZ));

        camera.setLocation(new Vector3f(newX, camPos.y, newZ));
    }

    /**
     * Adjusts zoom level while keeping camera within min/max zoom range.
     *
     * @param delta Change in zoom level.
     */
    public void zoom(float delta) {
        Vector3f camPos = camera.getLocation();
        float newY = camPos.y + (delta * ZOOM_SPEED);

        // Clamp zoom range
        newY = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newY));

        camera.setLocation(new Vector3f(camPos.x, newY, camPos.z));
    }

    /** Configures input bindings for panning and zooming. */
    private void setupInputActions() {
        OverheadPOVPanAction povPan = new OverheadPOVPanAction();
        OverheadZoomAction zoomIn = new OverheadZoomAction(1);
        OverheadZoomAction zoomOut = new OverheadZoomAction(-1);

        // Keyboard bindings
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.J, new OverheadPanAction(-1, 0), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.L, new OverheadPanAction(1, 0), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.I, new OverheadPanAction(0, -1), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.K, new OverheadPanAction(0, 1), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.U, zoomIn, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.O, zoomOut, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // Gamepad bindings (D-pad for panning)
        inputManager.associateActionWithAllGamepads(Component.Identifier.Axis.POV, povPan, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllGamepads(Component.Identifier.Button._4, zoomIn, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllGamepads(Component.Identifier.Button._5, zoomOut, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    /** Handles D-pad (POV Hat) panning with scaled movement for gamepads. */
    private class OverheadPOVPanAction extends AbstractInputAction {
        @Override
        public void performAction(float time, Event e) {
            float povValue = e.getValue();
            float deltaX = 0, deltaZ = 0;

            // Check POV (D-pad) values
            if (povValue == Component.POV.UP) deltaZ = -1;
            else if (povValue == Component.POV.DOWN) deltaZ = 1;
            else if (povValue == Component.POV.LEFT) deltaX = -1;
            else if (povValue == Component.POV.RIGHT) deltaX = 1;
            else if (povValue == Component.POV.UP_LEFT) { deltaX = -1; deltaZ = -1; }
            else if (povValue == Component.POV.UP_RIGHT) { deltaX = 1; deltaZ = -1; }
            else if (povValue == Component.POV.DOWN_LEFT) { deltaX = -1; deltaZ = 1; }
            else if (povValue == Component.POV.DOWN_RIGHT) { deltaX = 1; deltaZ = 1; }

            // Apply scaled movement
            float adjustedX = deltaX * (float) Math.pow(Math.abs(deltaX), RESPONSE_CURVE);
            float adjustedZ = deltaZ * (float) Math.pow(Math.abs(deltaZ), RESPONSE_CURVE);

            pan(adjustedX, adjustedZ);
        }
    }

    /** Handles keyboard-based panning. */
    private class OverheadPanAction extends AbstractInputAction {
        private float deltaX, deltaZ;

        public OverheadPanAction(float deltaX, float deltaZ) {
            this.deltaX = deltaX;
            this.deltaZ = deltaZ;
        }

        @Override
        public void performAction(float time, Event e) {
            pan(deltaX, deltaZ);
        }
    }

    /** Handles zooming movement. */
    private class OverheadZoomAction extends AbstractInputAction {
        private float direction;

        public OverheadZoomAction(float direction) {
            this.direction = direction;
        }

        @Override
        public void performAction(float time, Event e) {
            zoom(direction);
        }
    }
}