package Client;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}
	
	public void createGhostAvatar(UUID id, Vector3f position, String avatarType) throws IOException {
		System.out.println("adding ghost with ID --> " + id + " and avatarType --> " + avatarType);
	
		ObjShape shape;
		TextureImage texture;
	
		if (avatarType.equalsIgnoreCase("slow")) {
			shape = game.getSlowTankShape(); // Or use game.getSlowTankShape() if available
			texture = game.getSlowTankTexture();  // Use a different texture if you want
		} else {
			shape = game.getFastTankShape(); // default
			texture = game.getFastTankTexture();
		}
	
		GhostAvatar newAvatar = new GhostAvatar(id, shape, texture, position, game.getEngine());
		Matrix4f initialScale = new Matrix4f().scaling(avatarType.equalsIgnoreCase("slow") ? 1.5f : 1.0f);
		newAvatar.setLocalScale(initialScale);
		newAvatar.createHealthBar(game.getPlayerHealthBarShape(), game.getPlayerHealthBarTexture());
		ghostAvatars.add(newAvatar);
		newAvatar.initHeadlight();
	
		game.getEngine().getLightManager().loadLightArraySSBO();
	}
	
	public void removeGhostAvatar(UUID id)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{	System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id)
	{	GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while(it.hasNext())
		{	ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0)
			{	return ghostAvatar;
			}
		}		
		return null;
	}
	
	public void updateGhostAvatar(UUID id, Vector3f position, Matrix4f rotation)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		System.out.println("Updating ghost " + id + " to position " + position + " with new rotation.");
		if (ghostAvatar != null)
		{	ghostAvatar.setLocalLocation(position);       
			ghostAvatar.setLocalRotation(rotation);       
		}
		else
		{	System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
		if (ghostAvatar != null) {
			System.out.println("GhostAvatar FOUND, updating...");
		} else {
			System.out.println("Could NOT find GhostAvatar with ID: " + id);
		}
	}

	public void setGhostHealth(UUID id, float health) {
		GhostAvatar ghost = findAvatar(id);
		if (ghost != null) {
			ghost.setHealth(health);  // updates and rescales health bar
		}
	}

	public void setGhostHeadlight(UUID id, boolean on) {
		GhostAvatar ghost = findAvatar(id);
		if (ghost != null) {
			ghost.toggleGhostHeadlight(on);
		} else {
			System.out.println("[ERROR] Ghost not found for headlight update: " + id);
		}
	}

	public Vector<GhostAvatar> getGhosts() {
		return ghostAvatars;
	}
}
