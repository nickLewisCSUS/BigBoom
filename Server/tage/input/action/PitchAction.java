package tage.input.action;

import org.joml.*;
import tage.*;
import a2.*;
import net.java.games.input.Event;
import java.lang.Math;

/**
 * PitchAction handles pitching (up/down tilting) for either a GameObject (avatar) or Camera.
 * <p>
 * - If the player is **riding the avatar**, pitching affects the avatar.
 * - If the player is **off the avatar**, pitching affects the camera.
 * - **Gamepad input is now time-based and smoothed** for consistency.
 *
 * Example usage:
 * <pre>
 * inputManager.associateActionWithAllKeyboards(Identifier.Key.UP, new PitchAction(game, true), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * inputManager.associateActionWithAllKeyboards(Identifier.Key.DOWN, new PitchAction(game, false), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * inputManager.associateActionWithAllGamepads(Identifier.Axis.RZ, new PitchAction(game), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * </pre>
 *
 * @author Tyler Burguillos
 */

public class PitchAction extends AbstractInputAction {
    private MyGame game;
    private GameObject avatar;
    private Camera camera;
    private boolean isUp;

    /** Base pitch speed (radians per second) */
    private static final float BASE_PITCH_SPEED = 1.5f;

    /** Dead zone to ignore unintended small joystick movements */
    private static final float DEAD_ZONE = 0.15f;

    /** Response curve for smooth joystick input */
    private static final float RESPONSE_CURVE = 1.5f;

    /**
     * Constructs a PitchAction.
     * <p>
     * This version defaults to **keyboard-based pitching** (fixed speed).
     *
     * @param game Reference to the main game instance.
     */
    public PitchAction(MyGame game) { this.game = game; }

    /**
     * Constructs a PitchAction.
     * <p>
     * This version supports both **keyboard and gamepad-based pitching**.
     *
     * @param game Reference to the main game instance.
     * @param isUp If `true`, pitches up; if `false`, pitches down.
     */
    public PitchAction(MyGame game, boolean isUp) {
        this.game = game;
        this.isUp = isUp;
    }

    /**
     * Performs the pitch action when triggered by keyboard or gamepad.
     * <p>
     * - **Keyboard Input:** Pitches at a **fixed** speed (`BASE_PITCH_SPEED`).
     * - **Gamepad Input:** Uses a **response curve** for smoother rotation.
     * - **Time-Based Movement:** Ensures movement remains **frame-rate independent**.
     *
     * @param time The elapsed time (delta time) since the last frame.
     * @param e The input event (key press or gamepad movement).
     */
    @Override
    public void performAction(float time, Event e) {
        if (e == null) {
            System.out.println("[PitchAction] Warning: Received null input event!");
            return;
        }

        if (game.getGameState()) return; // Stop pitch if the game is over

        avatar = game.getAvatar();
        camera = game.getMainCam();

        float inputValue = e.getValue(); // Gamepad: -1 to 1, Keyboard: always 1

        // Ignore small stick movements below the dead zone threshold
        if (Math.abs(inputValue) < DEAD_ZONE) return;

        // Normalize gamepad input using a response curve for smoother movement
        float adjustedInput = (float) Math.signum(inputValue) * (float) Math.pow(Math.abs(inputValue), RESPONSE_CURVE);

        // Ensure rotation speed is proportional but never exceeds BASE_PITCH_SPEED
        float pitchSpeed = BASE_PITCH_SPEED * adjustedInput * time;

        // Reverse direction if pitching down
        if (!isUp) pitchSpeed = -pitchSpeed;

        // Apply pitch to avatar
		avatar.pitch(pitchSpeed);
    }
}