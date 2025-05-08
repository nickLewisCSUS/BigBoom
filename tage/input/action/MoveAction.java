package tage.input.action;

import org.joml.*;
import tage.*;
import Client.*;
import net.java.games.input.Event;
import java.lang.Math;

/**
 * MoveAction handles movement (forward/backward) for either a GameObject (avatar) or Camera.
 * <p>
 * - If the player is **riding the avatar**, movement affects the avatar.
 * - If the player is **off the avatar**, movement affects the camera.
 * - Movement is **time-based** so it remains **consistent across different FPS rates**.
 * - **Gamepad and keyboard movements are now identical**.
 *
 * Example usage:
 * <pre>
 * inputManager.associateActionWithAllKeyboards(Identifier.Key.W, new MoveAction(game, true), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * inputManager.associateActionWithAllKeyboards(Identifier.Key.S, new MoveAction(game, false), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * inputManager.associateActionWithAllGamepads(Identifier.Axis.Y, new MoveAction(game), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 * </pre>
 *
 * @author Tyler Burguillos
 */

public class MoveAction extends AbstractInputAction {  
    private MyGame game;
    private GameObject avatar;
    private Camera camera;
    private boolean isForward;

    /** Base movement speed */
    private static final float BASE_MOVE_SPEED = 3.0f;

    /** Dead zone to prevent unintended movement from slight joystick deflections */
    private static final float DEAD_ZONE = 0.15f;

    /** Response curve to make joystick movement smoother */
    private static final float RESPONSE_CURVE = 1.5f;

    /**
     * Constructs a MoveAction.
     * <p>
     * This version defaults to **keyboard-based movement** (fixed speed).
     *
     * @param game Reference to the main game instance.
     */
    public MoveAction(MyGame game) { this.game = game; }

    /**
     * Constructs a MoveAction.
     * <p>
     * This version supports both **keyboard and gamepad-based movement**.
     *
     * @param game Reference to the main game instance.
     * @param isForward If `true`, moves forward; if `false`, moves backward.
     */
    public MoveAction(MyGame game, boolean isForward) {
        this.game = game;
        this.isForward = isForward;
    }

    /**
     * Performs the move action when triggered by keyboard or gamepad.
     * <p>
     * - **Keyboard Input:** Moves at a **fixed** speed (`BASE_MOVE_SPEED`).
     * - **Gamepad Input:** Uses a **response curve** for smoother movement.
     * - **Time-Based Movement:** Ensures movement remains **frame-rate independent**.
     *
     * @param time The elapsed time (delta time) since the last frame.
     * @param e The input event (key press or gamepad movement).
     */
    @Override
    public void performAction(float time, Event e) {
        if (e == null) {
            System.out.println("[MoveAction] Warning: Received null input event!");
            return;
        }

        if (game.getGameState()) return; // Stop movement if the game is over

        avatar = game.getAvatar();
        camera = game.getMainCam();

        float inputValue = e.getValue(); // Gamepad: -1 to 1, Keyboard: always 1

        // Ignore small stick movements below the dead zone threshold
        if (Math.abs(inputValue) < DEAD_ZONE) return;

        // Normalize gamepad input using a response curve for smoother movement
        float adjustedInput = (float) Math.signum(inputValue) * (float) Math.pow(Math.abs(inputValue), RESPONSE_CURVE);

        // Ensure movement speed is proportional but never exceeds BASE_MOVE_SPEED
        float moveSpeed = BASE_MOVE_SPEED * adjustedInput * time;

        // Reverse direction if moving backward
        if (!isForward) moveSpeed = -moveSpeed;

        // Apply movement to avatar
        avatar.move(moveSpeed);
    }
}