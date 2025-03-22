package tage.input.action;

import org.joml.*;
import tage.*;
import a2.*;
import net.java.games.input.Event;
import java.lang.Math;

/**
 * YawAction handles yawing (left/right turning) for either a GameObject (avatar) or Camera.
 * <p>
 * - If the player is **riding the avatar**, yawing affects the avatar.
 * - If the player is **off the avatar**, yawing affects the camera.
 * - Yawing is **smoothed for gamepads** to ensure consistent rotation speeds.
 * - Movement is **time-based** so it remains **consistent across different FPS rates**.
 *
 * Example usage:
 * <pre>
 * inputManager.associateActionWithAllKeyboards(Identifier.Key.A, new YawAction(game, true), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * inputManager.associateActionWithAllKeyboards(Identifier.Key.D, new YawAction(game, false), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * inputManager.associateActionWithAllGamepads(Identifier.Axis.X, new YawAction(game), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * </pre>
 *
 * @author Tyler Burguillos
 */

public class YawAction extends AbstractInputAction {
    private MyGame game;
    private GameObject avatar;
    private Camera camera;
    private boolean isLeft;

    /** Base yaw speed (ensures even turning for both keyboard and gamepad) */
    private static final float BASE_YAW_SPEED = 1.5f;

    /** Dead zone to prevent unintended movement from slight joystick deflections */
    private static final float DEAD_ZONE = 0.15f;

    /** Response curve to make joystick turning smoother */
    private static final float RESPONSE_CURVE = 1.5f;

    /**
     * Constructs a YawAction.
     * <p>
     * This version defaults to **keyboard-based yawing** (fixed speed).
     *
     * @param game Reference to the main game instance.
     */
    public YawAction(MyGame game) { this.game = game; }

    /**
     * Constructs a YawAction.
     * <p>
     * This version supports both **keyboard and gamepad-based yawing**.
     *
     * @param game Reference to the main game instance.
     * @param isLeft If `true`, yaws left; if `false`, yaws right.
     */
    public YawAction(MyGame game, boolean isLeft) {  
        this.game = game;
        this.isLeft = isLeft;
    }

    /**
     * Performs the yaw action when triggered by keyboard or gamepad.
     * <p>
     * - **Keyboard Input:** Yaws at a **fixed** speed (`BASE_YAW_SPEED`).
     * - **Gamepad Input:** Uses a **response curve** for smoother turning.
     * - **Time-Based Movement:** Ensures yawing remains **frame-rate independent**.
     *
     * @param time The elapsed time (delta time) since the last frame.
     * @param e The input event (key press or gamepad movement).
     */
    @Override
    public void performAction(float time, Event e) {
        if (e == null) {
            System.out.println("[YawAction] Warning: Received null input event!");
            return;
        }

        if (game.getGameState()) return; // Stop yawing if the game is over

        avatar = game.getAvatar();
        camera = game.getMainCam();

        float inputValue = e.getValue(); // Gamepad: -1 to 1, Keyboard: always 1

        // Ignore small stick movements below the dead zone threshold
        if (Math.abs(inputValue) < DEAD_ZONE) return;

        // Normalize gamepad input using a response curve for smoother yawing
        float adjustedInput = (float) Math.signum(inputValue) * (float) Math.pow(Math.abs(inputValue), RESPONSE_CURVE);

        // Ensure yaw speed is proportional but never exceeds BASE_YAW_SPEED
        float yawSpeed = BASE_YAW_SPEED * adjustedInput * time;

        // Reverse direction if yawing right
        if (!isLeft) yawSpeed = -yawSpeed;

        // Apply yaw to avatar
        avatar.yaw(yawSpeed);
 
    }
}