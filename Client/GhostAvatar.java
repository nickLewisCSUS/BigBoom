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
	UUID uuid;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p, Engine e) 
	{	super(GameObject.root(), s, t);
		uuid = id;
		setPosition(p);
		engine = e;
	}

	public void createHealthBar(ObjShape shape, TextureImage texture) {
		ghostHealthBar = new GameObject(this, shape, texture);
		updateHealthBar(); 
	}

	public void updateHealthBar() {
		float healthRatio = currentHealth / maxHealth;
		float baseLength = 0.25f;
		float ghostScaleFactor = 1f / 0.25f;
		ghostHealthBar.setLocalTranslation(new Matrix4f().translation(0f, 0.4f, 0f));
		ghostHealthBar.setLocalScale(new Matrix4f().scaling(baseLength * healthRatio * ghostScaleFactor, 0.001f, 0.001f));
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
		System.out.println("Toggling ghost light for " + uuid + " to " + on);
		if (on) {
			updateHeadlight();
		} else {
			ghostHeadlight.setLocation(new Vector3f(0, -1000, 0));
		}
	}
}
