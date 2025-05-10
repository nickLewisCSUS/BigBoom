package Client;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import Client.TurretAIController.TurretState;
import tage.*;
import tage.networking.client.GameConnectionClient;
import tage.shapes.AnimatedShape;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }
	
	@Override
	protected void processPacket(Object message)
	{	String strMessage = (String)message;
		System.out.println("message received -->" + strMessage);
		String[] messageTokens = strMessage.split(",");
		
		// Game specific protocol to handle the message
		if(messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if(messageTokens[0].compareTo("join") == 0)
			{	if(messageTokens[1].compareTo("first") == 0)
				{	System.out.println("Join success - You are the power-up authority!");
					game.setIsConnected(true);
					game.setPowerUpAuthority(true);
					sendCreateMessage(game.getPlayerPosition());
				}
				else if (messageTokens[1].compareTo("success") == 0) 
				{	System.out.println("Join success - You are a regular client.");
					game.setIsConnected(true);
					game.setPowerUpAuthority(false);
					sendCreateMessage(game.getPlayerPosition());
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{	System.out.println("join failure confirmed");
					game.setIsConnected(false);
			}	}
			
			// Handle BYE message
			// Format: (bye,remoteId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	// remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}
			
			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0))
			{	// create a new ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
				Float.parseFloat(messageTokens[2]),
				Float.parseFloat(messageTokens[3]),
				Float.parseFloat(messageTokens[4]));

				String avatarType = (messageTokens.length > 5) ? messageTokens[5] : "fast"; // fallback

				try
				{	ghostManager.createGhostAvatar(ghostID, ghostPosition, avatarType);
				}	catch (IOException e)
				{	System.out.println("error creating ghost avatar");
				}
			}
			

			if (messageTokens[0].compareTo("isnear") == 0) {
				System.out.println("Player is near NPC (turret)");
				((MyGame)game).setTurretShouldRotate(true);
			}
			
			if (messageTokens[0].compareTo("isfar") == 0) {
				System.out.println("Player is far from NPC (turret)");
				((MyGame)game).setTurretShouldRotate(false);
			}
			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
			}

			if (messageTokens[0].compareTo("headlight") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				boolean lightOn = messageTokens[2].equals("1");
				ghostManager.setGhostHeadlight(ghostID, lightOn);
			} 

			if (messageTokens[0].equals("scoreboard")) {
				Map<UUID, Integer> scores = new HashMap<>();
				for (int i = 1; i < messageTokens.length; i++) {
					String[] parts = messageTokens[i].split(":");
					UUID playerId = UUID.fromString(parts[0]);
					int kills = Integer.parseInt(parts[1]);
					scores.put(playerId, kills);
				}
				game.updateScoreboard(scores);
			}
			

			// Handle HEALTH message
			// Format: (health,remoteId,value)
			if (messageTokens[0].compareTo("health") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				float health = Float.parseFloat(messageTokens[2]);
				ghostManager.setGhostHealth(ghostID, health);
			}
			
			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].equals("move")) {
				UUID ghostID = UUID.fromString(messageTokens[1]);

				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4])
				);

				int index = 5;

				Matrix4f bodyRot = new Matrix4f();
				for (int i = 0; i < 4; i++)
					for (int j = 0; j < 4; j++)
						bodyRot.set(i, j, Float.parseFloat(messageTokens[index++]));

				Matrix4f turretRot = new Matrix4f();
				for (int i = 0; i < 4; i++)
					for (int j = 0; j < 4; j++)
						turretRot.set(i, j, Float.parseFloat(messageTokens[index++]));

				Matrix4f gunRot = new Matrix4f();
				for (int i = 0; i < 4; i++)
					for (int j = 0; j < 4; j++)
						gunRot.set(i, j, Float.parseFloat(messageTokens[index++]));

				GhostAvatar ghost = ghostManager.getGhostByID(ghostID);
				if (ghost != null) {
					ghost.setLocalLocation(ghostPosition);
					ghost.setLocalRotation(bodyRot);
					ghost.setTurretRotation(turretRot);
				}
			}

			if (messageTokens[0].equals("gunRot")) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Matrix4f gunRot = new Matrix4f();
				int index = 2;
				for (int i = 0; i < 4; i++)
					for (int j = 0; j < 4; j++)
						gunRot.set(i, j, Float.parseFloat(messageTokens[index++]));

				GhostAvatar ghost = ghostManager.getGhostByID(ghostID);
				if (ghost != null)
					ghost.setGunRotation(gunRot);
			}

			if (messageTokens[0].equals("turretRot")) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Matrix4f turretRot = new Matrix4f();
				int index = 2;
				for (int i = 0; i < 4; i++)
					for (int j = 0; j < 4; j++)
						turretRot.set(i, j, Float.parseFloat(messageTokens[index++]));

				GhostAvatar ghost = ghostManager.getGhostByID(ghostID);
				if (ghost != null) {
					ghost.setTurretRotation(turretRot);
				}
			}


			// Handle POWERUP message
			// Format: (powerup,remoteID,boostID,x,y,z)
			if (messageTokens[0].compareTo("powerup") == 0) {
				UUID remoteID = UUID.fromString(messageTokens[1]);
				int boostID = Integer.parseInt(messageTokens[2]);
				float x = Float.parseFloat(messageTokens[3]);
				float y = Float.parseFloat(messageTokens[4]);
				float z = Float.parseFloat(messageTokens[5]);

				for (PowerUp boost : game.getPowerUps()) {
					if (boost.getBoostID() == boostID) {
						boost.getBoostObject().setLocalLocation(new Vector3f(x,y,z));
						Matrix4f combined = new Matrix4f();
						combined.identity();
						combined.mul(boost.getBoostObject().getLocalRotation());
						combined.setTranslation(new Vector3f(x,y,z));
						double[] tempTransform = game.toDoubleArray(combined.get(new float[16]));
						boost.getBoostPhysics().setTransform(tempTransform);
						break;
					}
				}
			}

			if (messageTokens[0].compareTo("syncPowerUps") == 0)
			{
				UUID requestingClientID = UUID.fromString(messageTokens[1]);
				System.out.println("Got request to sync powerups for: " + requestingClientID);

				// I am the authority! Send my current powerup states to the new client
				for (PowerUp p : game.getPowerUps()) {
					Vector3f pos = p.getBoostObject().getWorldLocation();
					int boostID = p.getBoostID();
					boolean active = p.isActive();

					try
					{
						String syncMessage = "powerupSync," + requestingClientID.toString()
							+ "," + boostID
							+ "," + pos.x()
							+ "," + pos.y()
							+ "," + pos.z()
							+ "," + (active ? "1" : "0");
						System.out.println("Sending powerup sync: " + syncMessage);
						sendPacket(syncMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			 if (messageTokens[0].compareTo("powerupSync") == 0)
			{
				System.out.println("Received powerupSync message: " + strMessage);
				UUID targetClient = UUID.fromString(messageTokens[1]);
				System.out.println("Target client: " + targetClient + " matches? " + (!targetClient.equals(this.id)) + "This id: " + this.id);
				if (!targetClient.equals(this.id)) {
					// This update is not for me, ignore it.
					return;
				}
				
				int boostID = Integer.parseInt(messageTokens[2]);
				float x = Float.parseFloat(messageTokens[3]);
				float y = Float.parseFloat(messageTokens[4]);
				float z = Float.parseFloat(messageTokens[5]);
				boolean active = messageTokens[6].equals("1");

				// Update the local powerup
				for (PowerUp p : game.getPowerUps()) {
					if (p.getBoostID() == boostID) {
						p.getBoostObject().setLocalLocation(new Vector3f(x,y,z));
						if (active) {
							// Make sure it's visible
							p.getBoostObject().setLocalLocation(new Vector3f(x,y,z));
						} else {
							// Hide it below ground
							p.getBoostObject().setLocalLocation(new Vector3f(0, -999f ,0));
						}

						Matrix4f combined = new Matrix4f();
						combined.identity();
						combined.mul(p.getBoostObject().getLocalRotation());
						combined.setTranslation(new Vector3f(x,y,z));
						double[] tempTransform = game.toDoubleArray(combined.get(new float[16]));
						p.getBoostPhysics().setTransform(tempTransform);

						break;
					}
				}
			}

			if (messageTokens[0].equals("turretstate")) {
				String newState = messageTokens[1];
				System.out.println("Received turret state update: " + newState);
				game.getTurretAIController().setPreviousState(TurretState.valueOf(newState));
			}

			if (messageTokens[0].equals("turretRotate")) {
				Matrix4f rot = new Matrix4f();
				int index = 1;
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						rot.set(i, j, Float.parseFloat(messageTokens[index++]));
					}
				}
				game.getTurret().setLocalRotation(rot);
			}

			// --- Turret State Sync (Client Replay) ---
			if (messageTokens[0].equals("turretState")) {
				String state = messageTokens[1];
				AnimatedShape turretS = (AnimatedShape) game.getTurret().getShape();

				switch (state) {
					case "ACTIVATE":
						turretS.playAnimation("ACTIVATE", 3.0f, AnimatedShape.EndType.PAUSE, 0);
						break;
					case "SCAN":
						turretS.playAnimation("SCAN", 1.0f, AnimatedShape.EndType.LOOP, 0);
						break;
					case "DEACTIVATE":
						turretS.playAnimation("DEACTIVATE", 3.0f, AnimatedShape.EndType.PAUSE, 0);
				}
			}
	}	}
	
	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage()
	{	try 
		{	sendPacket(new String("join," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{	try 
		{	sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the client�s Avatar�s position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessage(Vector3f position)
	{	try 
		{	String avatarType = game.isUsingSlowTank() ? "slow" : "fast";
			String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + avatarType;
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position)
	{	try 
		{
			String avatarType = game.isUsingSlowTank() ? "slow" : "fast";
			String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + avatarType;
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the position.
	public void sendMoveMessage(Vector3f position, Matrix4f bodyRot, Matrix4f turretRot) {
		try {
			StringBuilder msg = new StringBuilder("move," + id.toString());
			msg.append(",").append(position.x());
			msg.append(",").append(position.y());
			msg.append(",").append(position.z());

			// Body rotation
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < 4; j++)
					msg.append(",").append(bodyRot.get(i, j));

			// Turret rotation
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < 4; j++)
					msg.append(",").append(turretRot.get(i, j));

			// 🔴 MISSING: Gun rotation
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < 4; j++)
					msg.append(",").append(game.getTankGun().getWorldRotation().get(i, j));

			sendPacket(msg.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendGunRotationMessage(Matrix4f gunRot) {
		try {
			StringBuilder msg = new StringBuilder("gunRot," + id.toString());
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < 4; j++)
					msg.append(",").append(gunRot.get(i, j));
			sendPacket(msg.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendHealthUpdate(float health) {
	try {
		String message = "health," + id.toString() + "," + health;
		sendPacket(message);
	} catch (IOException e) {
		e.printStackTrace();
	}
}

	public void sendPowerUpUpdate(int boostID, Vector3f position) {
		try {
			String message = "powerup," + id.toString();
			message += "," + boostID;
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();

			System.out.println("Sending POWERUP update: " + message);
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendHeadlightState(boolean isOn) {
		try {
			String message = "headlight," + id.toString() + "," + (isOn ? "1" : "0");
			System.out.println("[SEND] Sending headlight state: " + message);
			sendPacket(message);		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Message: (turretstate, state)
	// Example: (turretstate, NEAR)
	public void sendTurretStateUpdate(String state) {
		try {
			String msg = "turretstate, " + state;
			sendPacket(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendTurretRotateMessage(Matrix4f rot) {
		try {
			StringBuilder msg = new StringBuilder("turretRotate");
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					msg.append(", ").append(rot.get(i, j));
				}
			}
			sendPacket(msg.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendTurretStateMessage(String state) {
		try {
			sendPacket("turretState," + state);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendTankTurretRotationMessage(Matrix4f turretRot) {
    try {
        StringBuilder msg = new StringBuilder("turretRot," + id.toString());
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                msg.append(",").append(turretRot.get(i, j));
        sendPacket(msg.toString());
    } catch (IOException e) {
        e.printStackTrace();
    }
}

}
