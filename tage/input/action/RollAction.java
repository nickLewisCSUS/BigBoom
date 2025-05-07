package tage.input.action;

import org.joml.*;
import tage.*;
import Client.*;
import net.java.games.input.Event;
import java.lang.Math;

/**
 * RollAction handles rolling (left/right tilting) for either a GameObject (avatar) or Camera.
 * <p>
 * - If the player is **riding the avatar**, rolling affects the avatar.
 * - If the player is **off the avatar**, rolling affects the camera.
 * - **Gamepad input is now time-based and smoothed** for consistency.
 *
 * Example usage:
 * <pre>
 * inputManager.associateActionWithAllKeyboards(Identifier.Key.LEFT, new RollAction(game, true), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * inputManager.associateActionWithAllKeyboards(Identifier.Key.RIGHT, new RollAction(game, false), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * inputManager.associateActionWithAllGamepads(Identifier.Axis.Z, new RollAction(game), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * </pre>
 *
 * @author Tyler Burguillos
 */

public class RollAction extends AbstractInputAction {
    private MyGame game;
    private GameObject avatar;
    private Camera camera;
    private boolean isLeft;

    /** Base roll speed (radians per second) */
    private static final float BASE_ROLL_SPEED = 1.5f;

    /** Dead zone to ignore unintended small joystick movements */
    private static final float DEAD_ZONE = 0.15f;

    /** Response curve for smooth joystick input */
    private static final float RESPONSE_CURVE = 1.5f;

    /**
     * Constructs a RollAction.
     * <p>
     * This version defaults to **keyboard-based rolling** (fixed speed).
     *
     * @param game Reference to the main game instance.
     */
    public RollAction(MyGame game) { this.game = game; }

    /**
     * Constructs a RollAction.
     * <p>
     * This version supports both **keyboard and gamepad-based rolling**.
     *
     * @param game Reference to the main game instance.
     * @param isLeft If `true`, rolls left; if `false`, rolls right.
     */
    public RollAction(MyGame game, boolean isLeft) {
        this.game = game;
        this.isLeft = isLeft;
    }

    /**
     * Performs the roll action when triggered by keyboard or gamepad.
     * <p>
     * - **Keyboard Input:** Rolls at a **fixed** speed (`BASE_ROLL_SPEED`).
     * - **Gamepad Input:** Uses a **response curve** for smoother rotation.
     * - **Time-Based Movement:** Ensures movement remains **frame-rate independent**.
     *
     * @param time The elapsed time (delta time) since the last frame.
     * @param e The input event (key press or gamepad movement).
     */
    @Override
    public void performAction(float time, Event e) {
        if (e == null) {
            System.out.println("[RollAction] Warning: Received null input event!");
            return;
        }

        if (game.getGameState()) return; // Stop roll if the game is over

        avatar = game.getAvatar();
        camera = game.getMainCam();

        float inputValue = e.getValue(); // Gamepad: -1 to 1, Keyboard: always 1

        // Ignore small stick movements below the dead zone threshold
        if (Math.abs(inputValue) < DEAD_ZONE) return;

        // Normalize gamepad input using a response curve for smoother movement
        float adjustedInput = (float) Math.signum(inputValue) * (float) Math.pow(Math.abs(inputValue), RESPONSE_CURVE);

        // Ensure rotation speed is proportional but never exceeds BASE_ROLL_SPEED
        float rollSpeed = BASE_ROLL_SPEED * adjustedInput * time;

        // Reverse direction if rolling left
        if (isLeft) rollSpeed = -rollSpeed;

        // Apply roll to avatar
        avatar.roll(rollSpeed);
    }
}