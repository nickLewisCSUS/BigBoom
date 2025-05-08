package tage;

import tage.*;
import tage.nodeControllers.*;
import org.joml.*;
import java.lang.Math;
import Client.*;


import tage.input.*;
import tage.input.action.*;
import net.java.games.input.Component.Identifier;
import net.java.games.input.*;

/**
 * **Satellite** represents an interactive game object that reacts to the player's proximity.
 * <p>
 * This satellite can be in various states:
 * - **Neutral (Green)**: When the player is within the safe distance.
 * - **Warning (Yellow)**: When the player is too close.
 * - **Self-Destruct (Red)**: When the player is critically close.
 * - **Disarmed**: When successfully deactivated by the player.
 * - **Destroyed**: When self-destruction completes.
 * <p>
 * The satellite includes **flashing textures**, **proximity detection**, and
 * an integrated **light source** for visual indicators.
 *
 * @author Tyler Burguillos
 */
 
 public class Satellite extends GameObject {
	private TextureImage[] greenTextures, yellowTextures, redTextures, disarmTextures;
	private TextureImage normalTexture, destroyedTexture;
	private boolean isDestroyed = false, isDisarmed = false, isDisarming = false;
	private double selfDestructTimer = 0.0, disarmTimer = 0.0;
	private double disarmSpeed = 0.5;
	private int disarmTextureIndex = 0;
	private final float GOOD_RANGE, TOO_CLOSE_RANGE, SELF_DESTRUCT_RANGE;
	private final double SELF_DESTRUCT_TIME, DISARM_TIME;
	private Light satelliteLight;
	private NodeController satelliteBounceController;
	private MyGame game;
	public String name = "";
	private boolean disarmKeyHeld = false;
	
	public void setDisarmKeyHeld(boolean isHeld) { this.disarmKeyHeld = isHeld; }
	public void setIsDisarming(boolean isDisarming) { this.isDisarming = isDisarming; }
	
	/**
	 * **Satellite Constructor**
	 * <p>
	 * Initialize a satellite with textures, ranges, and timers for disarming & self-destruction.
	 *
	 * @param parent			Parent object in the scene graph.
	 * @param shape				The 3D shape of the satellite.
	 * @param normal			The normal texture.
	 * @param destroyed			The texture displayed when destroyed.
	 * @param green				Safe-state (green) flashing textures.
	 * @param yellow			Warning-state (yellow) flashing textures.
	 * @param red				Self-destruc state (red) flashing textures.
	 * @param disarm			Disarm sequence textures.
	 * @param goodRange			Distance where the satellite is safe.
	 * @param tooCloseRange		Distance where the satellite issues warnings.
	 * @param selfDestructRange	Distance where self-destruction begins.
	 * @param selfDestructTime	The time required for self-destruction
	 * @param disarmTime		Time required for disarming.
	 * @param game				Reference to the main game instance.
	 * @param name				Name identifier for the satellite.
	 */
	public Satellite(
		GameObject parent,
		ObjShape shape,
		TextureImage normal, 
		TextureImage destroyed, 
		TextureImage[] green, 
		TextureImage[] yellow, 
		TextureImage[] red, 
		TextureImage[] disarm, 
		float goodRange, 
		float tooCloseRange, 
		float selfDestructRange, 
		double selfDestructTime, 
		double disarmTime,
		MyGame game,
		String name
		) {
			super (parent, shape, normal);
			this.normalTexture = normal;
			this.destroyedTexture = destroyed;
			this.greenTextures = green;
			this.yellowTextures = yellow;
			this.redTextures = red;
			this.disarmTextures = disarm;
			this.GOOD_RANGE = goodRange;
			this.TOO_CLOSE_RANGE = tooCloseRange;
			this.SELF_DESTRUCT_RANGE = selfDestructRange;
			this.SELF_DESTRUCT_TIME = selfDestructTime;
			this.DISARM_TIME = disarmTime;
			this.game = game;
			this.name = name;
			this.satelliteLight = new Light();
			
			// Default light color (white)
			this.satelliteLight.setAmbient(0.3f, 0.3f, 0.3f);
			this.satelliteLight.setDiffuse(1.0f, 1.0f, 1.0f);
			this.satelliteLight.setSpecular(1.0f, 1.0f, 1.0f);
			
			Engine.getEngine().getSceneGraph().addLight(satelliteLight);
			
			satelliteBounceController = new BounceController();	
			(Engine.getEngine().getSceneGraph()).addNodeController(satelliteBounceController);
			satelliteBounceController.addTarget(this);
			satelliteBounceController.enable();

		}
		
		
	/**
     * **update() - Updates the satellite state based on proximity to the player. **
	 * <p>
	 * Handles:
	 * - **Texture changes** (flashing effects)
	 * - **Disarming sequence**
	 * - **Self-destruction countdown**
	 * - **Lighting adjustments**
	 *
	 * @param deltaTime		Time elapsed since the last frame.
	 * @param avatarPos		Position of the avatar.
	 * @param cameraPos		Position of the camera.
     */
    public void update(double deltaTime, Vector3f avatarPos) {
		Vector3f satPos = getWorldLocation();
		if (isDestroyed)  {	return; }
		
        float distance = satPos.distance(avatarPos);

        // If already disarmed, keep last disarmed texture
        if (isDisarmed) {
            setTextureImage(disarmTextures[9]);
            return;
        }
		
		// **Disarming Sequence**
        if (!isDisarming && disarmKeyHeld) {
			if (distance <= GOOD_RANGE && distance > TOO_CLOSE_RANGE) {
				isDisarming = true;
				disarmTimer = 0.0;
				disarmTextureIndex = 0;
			}
        } else if (isDisarming && distance > GOOD_RANGE || isDisarming && !disarmKeyHeld) {
			isDisarming = false;
            disarmTimer = 0.0;
		}
		
		if (isDisarming) {
			disarmTimer += deltaTime;
            if (disarmTimer >= 0.5) {
                disarmTimer = 0.0;
                disarmTextureIndex++;
                if (disarmTextureIndex >= 9) {
                    isDisarming = false;
                    disarmTextureIndex = 9;
					disarm();
                }
                setTextureImage(disarmTextures[disarmTextureIndex]);
            }
            return;
        }
		
		// **Self-Destruct Behavior**
		if (distance <= SELF_DESTRUCT_RANGE) {
			satelliteLight.setDiffuse(1.0f, 0.0f, 0.0f); // Ref Light
			satelliteLight.setSpecular(1.0f, 0.0f, 0.0f);
			selfDestructTimer += deltaTime;
			
			int index = (int) ((System.currentTimeMillis() / 50)  % redTextures.length);
			setTextureImage(redTextures[index]);
			
			if (selfDestructTimer >= SELF_DESTRUCT_TIME) {
				setTextureImage(destroyedTexture);
				destroy();
			}
			return;
		}
		
		// **Warning State (Too Close)**
		if (distance <= TOO_CLOSE_RANGE) {
			satelliteLight.setDiffuse(1.0f, 1.0f, 0.0f); // Yellow Light
			satelliteLight.setSpecular(1.0f, 1.0f, 0.0f);
			int index = (int) ((System.currentTimeMillis() / 80) % yellowTextures.length);
			setTextureImage(yellowTextures[index]);
			return;
		}
		
		// **Safe State (Green Flashing)**
		if (distance <= GOOD_RANGE && !isDisarming) {
			satelliteLight.setDiffuse(0.0f, 1.0f, 0.0f); // Yellow Light
			satelliteLight.setSpecular(0.0f, 1.0f, 0.0f);
			int index = (int) ((System.currentTimeMillis() / 100) % greenTextures.length);
			setTextureImage(greenTextures[index]);
			return;
		}

        // **Default State (Neutral)**
		satelliteLight.setDiffuse(0.0f, 0.0f, 1.0f); // Blue Light
		satelliteLight.setSpecular(0.0f, 0.0f, 1.0f);
        setTextureImage(normalTexture);
		satelliteLight.setLocation(satPos.add(0,1,0));
    }
	
	/**
     * **disarm() - Marks the satellite as disarmed and increments the game score.**
     */
    public void disarm() {
		if (!isDisarmed) {
			isDisarmed = true;
			game.incrementScore();

			// Create a mini satellite part and attach it to the avatar
			attachMiniSatellite();
		}
	}
	
	/**
     * **destroy() - Marks the satellite as destroyed and decrements the game score.**
     */
    public void destroy() {
		if (!isDestroyed) {
			isDestroyed = true;
			game.decrementScore();
		}
	}
	
	private void attachMiniSatellite() {
		// Create a small version of the disarmed satellite
		GameObject miniSatellite = new GameObject(game.getAvatar(), this.getShape(), disarmTextures[disarmTextures.length - 1]);

		// Scale down to make it look like a piece of the original
		miniSatellite.setLocalScale(new Matrix4f().scaling(0.05f));

		// Set initial position slightly above the dolphin
		miniSatellite.setLocalTranslation(new Matrix4f().translation(0, 2.5f, 0));

		// Attach it to the dolphin, so it moves with the dolphin
		game.getAvatar().addChild(miniSatellite);

		// Add the mini satellite to the rotating effect
		game.addMiniSatellite(miniSatellite);
	}


    /**
     * **reset() - Restores the satellite to its default state.**
     */
    public void reset() {
        isDestroyed = false;
        isDisarmed = false;
        isDisarming = false;
        disarmTextureIndex = 0;
        selfDestructTimer = 0.0;
        disarmTimer = 0.0;
        setTextureImage(normalTexture);
    }
	

	/** @return `true` if the satellite has self-destructed. */
	public boolean isDestroyed() { return isDestroyed; }
	
	/** @return `true` if the satellite has been successfully disarmed. */
	public boolean isDisarmed() { return isDisarmed; }
	
	/** @return The satellites */
	public String getName() { return (this.name + " Satellite"); }
	
	/** @return The safe range distance. */	
	public float getGoodRange() { return GOOD_RANGE; }
	
	/** The warning range distance. */
	public float getTooCloseRange() { return TOO_CLOSE_RANGE; }
	
	/** The self-destruct range distance. */
	public float getSelfDestructRange() { return SELF_DESTRUCT_RANGE; }
}