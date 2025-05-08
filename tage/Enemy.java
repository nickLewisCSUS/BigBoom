package tage;

import tage.*;
import tage.nodeControllers.*;
import org.joml.*;
import java.util.List;
import java.util.Random;
import java.lang.Math;
import Client.*;

/**
 * **Enemy** represents a dynamic NPC that interacts with the player based on their state.
 * <p>
 * The enemy's behavior includes:
 * - **Random movement** when the player is **riding the dolphin**
 * - **Avoiding satellites** to prevent self-destruction
 * - **Chasing the player** when the player is **off the dolphin**
 * - **Teleporting the player away** upon collision
 *
 * The enemy flashes **green** when idle and **red** when chasing.
 *
 * @author Tyler Burguillos
 */
public class Enemy extends GameObject {
    private TextureImage[] greenTextures, alertTextures;
    private Vector3f targetPosition;
    private double changeTargetCooldown = 0;
    private final double CHANGE_TARGET_INTERVAL = 2.0;
    private final float MOVE_SPEED = 1.0f;
    private final float CHASE_SPEED = 1.8f;
    private final float TOO_CLOSE_TO_SATELLITE = 8.0f;
	private MyGame game;
    private Random random = new Random();
	private NodeController enemyRotationController;

	/**
     * **Enemy Constructor**
     * <p>
     * Initializes the enemy's behavior and visual properties.
     *
     * @param parent        Parent object in the scene graph.
     * @param shape         The 3D shape of the enemy.
     * @param normalTexture Default texture of the enemy.
     * @param greenTextures Textures used for idle/flashing effect.
     * @param alertTextures Textures used when chasing the player.
     * @param game          Reference to the main game instance.
     */
    public Enemy(GameObject parent, ObjShape shape, TextureImage normalTexture, TextureImage[] greenTextures, TextureImage[] alertTextures, MyGame game) {
        super(parent, shape, normalTexture);
        this.greenTextures = greenTextures;
        this.alertTextures = alertTextures;
        this.targetPosition = getRandomPosition();
		this.game = game;
		enemyRotationController = new RotationController(Engine.getEngine(), new Vector3f(0,1,0), 0.001f);	
		(Engine.getEngine().getSceneGraph()).addNodeController(enemyRotationController);
		enemyRotationController.addTarget(this);
		enemyRotationController.enable();
    }

	/**
     * **update() - Handles the enemy's movement and interactions.**
     * <p>
     * - Moves randomly and avoids satellites if the player is **on the dolphin**.
     * - Chases the player if they are **off the dolphin**.
     * - Teleports the player upon contact.
     *
     * @param deltaTime   Time elapsed since the last frame.
     * @param playerPos   Position of the player avatar.
     * @param cameraPos   Position of the camera.
     * @param satellites  List of active satellites in the scene.
     * @param camera      The game’s camera.
     * @param dolphin     The player's avatar.
     */
    public void update(double deltaTime, Vector3f playerPos, Vector3f CameraPos, List<Satellite> satellites, Camera camera, GameObject dolphin) {
        Vector3f enemyPos = getWorldLocation();
        float distanceToPlayer = enemyPos.distance(playerPos);
		if (enemyRotationController.isEnabled()) {
			enemyRotationController.toggle();
		}

        // Chase the player
        if (distanceToPlayer > 1.0f) {
            moveTowards(playerPos, CHASE_SPEED, deltaTime);
            setTextureFlashing(alertTextures, 300);
        } else {
            // Teleport the player and dolphin when touched
            teleportPlayerAway(camera, dolphin, satellites);
			//Vector3f directionToOrigin = new Vector3f(0,0,0).sub(newPos).normalize();
			dolphin.lookAt(0,2,0);
			camera.lookAt(0,2,0);
        }
    }

    /**
     * **moveTowards() - Moves the enemy toward a target position.**
     * <p>
     * This method ensures movement is smooth and does not exceed the defined range.
     *
     * @param target    The target position to move towards.
     * @param speed     The movement speed.
     * @param deltaTime Time elapsed since the last frame.
     */
    private void moveTowards(Vector3f target, float speed, double deltaTime) {
        Vector3f enemyPos = getWorldLocation();
        Vector3f direction = target.sub(enemyPos, new Vector3f()).normalize();
        Vector3f movement = direction.mul(speed * (float) deltaTime);
        Vector3f newPos = enemyPos.add(movement);
		
		if (newPos.length() > 30) {
			newPos.normalize().mul(29.9f);
		}
		setLocalLocation(newPos);
    }

	/**
     * **getRandomPosition() - Generates a random position within the allowed range.**
     * <p>
     * Ensures the position remains within a **radius of 30**.
     *
     * @return A random valid position.
     */
    private Vector3f getRandomPosition() {
        Vector3f randomPos;
		do {
			randomPos = new Vector3f(
				(random.nextFloat() * 60) - 30,
				2,
				(random.nextFloat() * 60) - 30
			);
		} while (randomPos.length() > 30); // Ensure it stays inside a sphere of radius 20
		return randomPos;
    }

	/**
     * **teleportPlayerAway() - Relocates the player to a safe distance.**
     * <p>
     * Ensures the new position does not overlap with satellites.
     *
     * @param camera     The game camera.
     * @param dolphin    The player's avatar.
     * @param satellites List of active satellites.
     */
    private void teleportPlayerAway(Camera camera, GameObject dolphin, List<Satellite> satellites) {
        Vector3f newPos;
        boolean validLocation;
        do {
            validLocation = true;
            newPos = getRandomPosition();
            for (Satellite sat : satellites) {
                if (newPos.distance(sat.getWorldLocation()) < TOO_CLOSE_TO_SATELLITE) {
                    validLocation = false;
                    break;
                }
            }
        } while (!validLocation);

        camera.setLocation(newPos);
        dolphin.setLocalLocation(newPos);
    }

    /**
     * **setTextureFlashing() - Controls the enemy’s texture flashing effect.**
     * <p>
     * This method changes textures at a rate determined by the given modulo value.
     *
     * @param textures The texture set to use.
     * @param modulo   The speed of the texture change.
     */
    private void setTextureFlashing(TextureImage[] textures, int modulo) {
        int index = Math.abs((int) ((System.currentTimeMillis() / modulo) % textures.length));
        setTextureImage(textures[index]);
    }
}