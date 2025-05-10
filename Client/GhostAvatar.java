package Client;

import java.util.UUID;

import tage.*;
import org.joml.*;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject
{
	private GameObject ghostHealthBar;
	private float currentHealth = 100f;
	private float maxHealth = 100f;
	private Light ghostHeadlight;
	private GameObject headlightNode;
	private Engine engine;
	private boolean isHeadlightOn = false;
	private GameObject ghostTurret;
	private GameObject ghostGun;
	UUID uuid;

	public GhostAvatar(UUID id, ObjShape bodyShape, ObjShape turretShape, ObjShape gunShape, TextureImage texture, Vector3f pos, Engine e, float scale) {
	super(GameObject.root(), bodyShape, texture);
	uuid = id;
	engine = e;

	setLocalLocation(pos);
	setLocalScale(new Matrix4f().scaling(scale)); // scale whole ghost

	// Match player tank turret/gun positioning exactly (do NOT scale Y)
	float turretOffsetY = 0.25f;
	float turretOffsetZ = 0.4f * scale;
	float gunOffsetY = 0.25f;
	float gunOffsetZ = 0.7f * scale;

	// Create turret
	ghostTurret = new GameObject(this, turretShape, texture);
	ghostTurret.setLocalTranslation(new Matrix4f().translation(0f, turretOffsetY, turretOffsetZ));
	ghostTurret.propagateTranslation(true);
	ghostTurret.propagateRotation(true);
	ghostTurret.applyParentRotationToPosition(true);

	// Create gun
	ghostGun = new GameObject(ghostTurret, gunShape, texture);
	ghostGun.setLocalTranslation(new Matrix4f().translation(0f, gunOffsetY, gunOffsetZ));
	ghostGun.propagateTranslation(true);
	ghostGun.propagateRotation(true);
	ghostGun.applyParentRotationToPosition(true);
}

	public void createHealthBar(ObjShape shape, TextureImage texture) {
		MyGame myGame = (MyGame)(engine.getGame()); 
		TextureImage ghostHealthTex = myGame.getGhostHealthBarTexture();
		ghostHealthBar = new GameObject(this, shape, ghostHealthTex);
		updateHealthBar(); 
	}

	public void updateHealthBar() {
		float healthRatio = currentHealth / maxHealth;
		float baseLength = 2.0f;
		float ghostScaleFactor = 1f / 0.25f;
		ghostHealthBar.setLocalTranslation(new Matrix4f().translation(0f, 1.5f, 0f));
		ghostHealthBar.setLocalScale(new Matrix4f().scaling(baseLength * healthRatio * ghostScaleFactor, 0.1f, 0.1f));
	}

	public void setHealth(float health) {
		this.currentHealth = health;
		updateHealthBar();
	}
	
	
	public UUID getID() { return uuid; }
	public void setPosition(Vector3f m) { setLocalLocation(m); }
	public void setRotation(Matrix4f r) { setLocalRotation(r); }
	public Vector3f getPosition() { return getWorldLocation(); }

	public void initHeadlight() {
		headlightNode = new GameObject(this); // child of ghost
		headlightNode.setLocalTranslation(new Matrix4f().translation(0f, 0.3f, 0f));
	
		ghostHeadlight = new Light();
		ghostHeadlight.setType(Light.LightType.SPOTLIGHT);
		ghostHeadlight.setAmbient(0.2f, 0.2f, 0.2f);
		ghostHeadlight.setDiffuse(1.5f, 1.5f, 1.5f);
		ghostHeadlight.setSpecular(1.0f, 1.0f, 1.0f);
		ghostHeadlight.setCutoffAngle(15.0f);
		ghostHeadlight.setOffAxisExponent(20.0f);
		ghostHeadlight.setConstantAttenuation(1.0f);
		ghostHeadlight.setLinearAttenuation(0.05f);
		ghostHeadlight.setQuadraticAttenuation(0.01f);
	
		ghostHeadlight.setLocation(new Vector3f(0, -1000, 0)); // start off
		engine.getSceneGraph().addLight(ghostHeadlight);
	}
	
	public void updateHeadlight() {
		if (!isHeadlightOn) return; 
		Vector3f pos = headlightNode.getWorldLocation();
		Matrix4f rot = headlightNode.getWorldRotation();
		Vector3f dir = new Vector3f(-rot.m20(), -rot.m21(), -rot.m22()).normalize();
		ghostHeadlight.setLocation(pos);
		ghostHeadlight.setDirection(dir);
	}
	
	public void toggleGhostHeadlight(boolean on) {
		isHeadlightOn = on;
		if (on) {
			updateHeadlight();
		} else {
			ghostHeadlight.setLocation(new Vector3f(0, -1000, 0));
		}
	}

	public void updateTurretRotation(Matrix4f rot) {
	if (ghostTurret != null) {
		ghostTurret.setLocalRotation(rot);
	}
	}

	public void updateGunRotation(Matrix4f rot) {
		if (ghostGun != null) {
			ghostGun.setLocalRotation(rot);
		}
	}

	public void setTurretRotation(Matrix4f rot) {
		 if (ghostTurret != null) {
			ghostTurret.setLocalRotation(rot);
		} else {
			System.out.println("[GhostAvatar] Warning: Tried to rotate null ghostTurret!");
		}
	}

	public void setGunRotation(Matrix4f rot) {
		ghostGun.setLocalRotation(rot);
	}

	public void buildParts(ObjShape turretShape, ObjShape gunShape, TextureImage texture) {
		ghostTurret = new GameObject(this, turretShape, texture);
		ghostTurret.setLocalTranslation(new Matrix4f().translation(0f, 0.25f, 0.4f));
		ghostTurret.propagateTranslation(true);
		ghostTurret.propagateRotation(true);
		ghostTurret.applyParentRotationToPosition(true);

		ghostGun = new GameObject(ghostTurret, gunShape, texture);
		ghostGun.setLocalTranslation(new Matrix4f().translation(0f, 0.25f, 0.7f));
		ghostGun.propagateTranslation(true);
		ghostGun.propagateRotation(true);
		ghostGun.applyParentRotationToPosition(true);
	}
}
