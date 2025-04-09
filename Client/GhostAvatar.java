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
	UUID uuid;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) 
	{	super(GameObject.root(), s, t);
		uuid = id;
		setPosition(p);
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
}
