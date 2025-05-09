package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> 
{

	private Map<UUID, Integer> killCounts = new HashMap<>();
	private Map<UUID, String> clientAvatarTypes = new HashMap<>();

	public GameServerUDP(int localPort) throws IOException 
	{	super(localPort, ProtocolType.UDP);
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort)
	{
		String message = (String)o;
		String[] messageTokens = message.split(",");
		
		if(messageTokens.length > 0)
		{	// JOIN -- Case where client just joined the server
			// Received Message Format: (join,localId)
			if(messageTokens[0].compareTo("join") == 0)
			{	try 
				{	IClientInfo ci;					
					ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(messageTokens[1]);
					addClient(ci, clientID);
					System.out.println("Join request received from - " + clientID.toString());
					boolean isFirst = getClients().size() == 1;
					System.out.println("Client size:" + (getClients().size() == 1));
					sendJoinedMessage(clientID, isFirst);

					// Tell the power-up authority to share power-up positions with this new client
					if (getClients().size() > 1) {
						UUID firstClient = getClients().keySet().iterator().next(); // assume first client is authority
						try
						{
							sendPacket("syncPowerUps," + clientID.toString(), firstClient);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
			
			// BYE -- Case where clients leaves the server
			// Received Message Format: (bye,localId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				System.out.println("Exit request received from - " + clientID.toString());
				sendByeMessages(clientID);
				removeClient(clientID);
			}
			
			// CREATE -- Case where server receives a create message (to specify avatar location)
			// Received Message Format: (create,localId,x,y,z)
			if(messageTokens[0].compareTo("create") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				String avatarType = messageTokens[5];

				clientAvatarTypes.put(clientID, avatarType);

				sendCreateMessages(clientID, pos, avatarType);
				sendWantsDetailsMessages(clientID);
			}
			
			// DETAILS-FOR --- Case where server receives a details for message
			// Received Message Format: (dsfr,remoteId,localId,x,y,z)
			if(messageTokens[0].compareTo("dsfr") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				String[] pos = {messageTokens[3], messageTokens[4], messageTokens[5]};
				sendDetailsForMessage(clientID, remoteID, pos);
			}

			// HEALTH --- Server received health update
			if (messageTokens[0].compareTo("health") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String healthMessage = String.join(",", messageTokens); // Forward as-is
				try {
					forwardPacketToAll(healthMessage, clientID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (messageTokens[0].equals("headlight")) {
				UUID clientID = UUID.fromString(messageTokens[1]);
			
				StringBuilder headlightMessage = new StringBuilder("headlight," + clientID.toString());
				for (int i = 2; i < messageTokens.length; i++) {
					headlightMessage.append(",").append(messageTokens[i]);
				}
			
			
				try {
					forwardPacketToAll(headlightMessage.toString(), clientID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (messageTokens[0].compareTo("move") == 0)
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				float x = Float.parseFloat(messageTokens[2]);
				float y = Float.parseFloat(messageTokens[3]);
				float z = Float.parseFloat(messageTokens[4]);

				Vector3f playerPos = new Vector3f(x, y, z);
				Vector3f turretPos = new Vector3f(-10f, 0f, 2f); // turret static position

				if (playerPos.distance(turretPos) < 10.0f) {
					System.out.println("Server: Player is near the turret!");
					sendIsNearMessage(clientID);
				}
				else {
					System.out.println("Server: Player is far from the turret!");
					sendIsFarMessage(clientID); // <-- OPTIONAL if you want turret to stop
				}

				// Rebuild full move message to forward to other clients
				StringBuilder moveMessage = new StringBuilder("move," + clientID.toString());
				for (int i = 2; i < messageTokens.length; i++) {
					moveMessage.append(",").append(messageTokens[i]);
				}

				System.out.println("Forwarding MOVE from: " + clientID + " → " + moveMessage.toString());

				try {
					forwardPacketToAll(moveMessage.toString(), clientID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// POWERUP --- Case where server receives a powerup message
			if (messageTokens[0].compareTo("powerup") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				// Rebuild full powerup message to forward to other clients
				StringBuilder powerupMessage = new StringBuilder("powerup," + clientID.toString());
				for (int i = 2; i < messageTokens.length; i++) {
					powerupMessage.append(",").append(messageTokens[i]);
				}

				System.out.println("Forwarding POWERUP from: " + clientID + " → " + powerupMessage.toString());
				
				try {
					forwardPacketToAll(powerupMessage.toString(), clientID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// POWERUP --- Case where server receives a powerup message
			if (messageTokens[0].compareTo("powerup") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				// Rebuild full powerup message to forward to other clients
				StringBuilder powerupMessage = new StringBuilder("powerup," + clientID.toString());
				for (int i = 2; i < messageTokens.length; i++) {
					powerupMessage.append(",").append(messageTokens[i]);
				}

				System.out.println("Forwarding POWERUP from: " + clientID + " → " + powerupMessage.toString());
				
				try {
					forwardPacketToAll(powerupMessage.toString(), clientID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (messageTokens[0].compareTo("powerupSync") == 0) {
				try {
					UUID targetClient = UUID.fromString(messageTokens[1]);
					StringBuilder msg = new StringBuilder(messageTokens[0]);
					for (int i = 1; i < messageTokens.length; i++) {
						msg.append(",").append(messageTokens[i]);
					}
					sendPacket(msg.toString(), targetClient);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (messageTokens[0].equals("turretRotate")) {
				try { 
					UUID senderID = UUID.fromString(messageTokens[1]);
					StringBuilder rotateMsg = new StringBuilder("turretRotate");
					for (int i = 1; i < messageTokens.length; i++) {
						rotateMsg.append(", ").append(messageTokens[i]);
					}
					forwardPacketToAll(rotateMsg, senderID);
				} catch (IOException e) {
					System.err.println("[Server] Error forwarding turretRotate: " + e.getMessage());
					e.printStackTrace();
				}
			}

			if (messageTokens[0].equals("turretState")) {
				try {
					UUID senderID = UUID.fromString(messageTokens[1]);
					String msg = String.join(",", messageTokens);
					forwardPacketToAll(msg, senderID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Informs the client who just requested to join the server if their if their 
	// request was able to be granted. 
	// Message Format: (join,success) or (join,failure)
	
	public void sendJoinedMessage(UUID clientID, boolean isFirst)
	{	try 
		{	System.out.println("Confirming join to client " + clientID);
			String message = "join," + (isFirst ? "first" : "success");
			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that the avatar with the identifier remoteId has left the server. 
	// This message is meant to be sent to all client currently connected to the server 
	// when a client leaves the server.
	// Message Format: (bye,remoteId)
	
	public void sendByeMessages(UUID clientID)
	{	try 
		{	String message = new String("bye," + clientID.toString());
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a new avatar has joined the server with the unique identifier 
	// remoteId. This message is intended to be send to all clients currently connected to 
	// the server when a new client has joined the server and sent a create message to the 
	// server. This message also triggers WANTS_DETAILS messages to be sent to all client 
	// connected to the server. 
	// Message Format: (create,remoteId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessages(UUID clientID, String[] position, String avatarType)
	{	try 
		{	String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + avatarType;	
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client of the details for a remote client�s avatar. This message is in response 
	// to the server receiving a DETAILS_FOR message from a remote client. That remote client�s 
	// message�s localId becomes the remoteId for this message, and the remote client�s message�s 
	// remoteId is used to send this message to the proper client. 
	// Message Format: (dsfr,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String[] position)
	{	try 
		{	String avatarType = clientAvatarTypes.getOrDefault(remoteId, "fast");
			String message = new String("dsfr," + remoteId.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + avatarType;

			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a local client that a remote client wants the local client�s avatar�s information. 
	// This message is meant to be sent to all clients connected to the server when a new client 
	// joins the server. 
	// Message Format: (wsds,remoteId)
	
	public void sendWantsDetailsMessages(UUID clientID)
	{	try 
		{	String message = new String("wsds," + clientID.toString());	
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a remote client�s avatar has changed position. x, y, and z represent 
	// the new position of the remote avatar. This message is meant to be forwarded to all clients
	// connected to the server when it receives a MOVE message from the remote client.   
	// Message Format: (move,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessages(UUID clientID, String[] position)
	{	try 
		{	String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// Informs clients that a powerup has moved or changed position.
	// Message Format: (powerup,remoteID,powerupID,x,y,z)
	public void sendPowerUpMessages(UUID clientID, int powerupID, String[] position)
	{ 	try 
		{	String message = new String("powerup," + clientID.toString());
			message += "," + powerupID;
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
	}
	catch (IOException e) 
	{	e.printStackTrace();
}	}

	public void sendIsNearMessage(UUID clientID) {
		try {
			String message = "isnear," + clientID.toString();
			sendPacket(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendIsFarMessage(UUID clientID) {
		try {
			String message = "isfar," + clientID.toString();
			sendPacket(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Call this when a player gets a kill (you decide how to trigger it)
	public void incrementKill(UUID killerID) {
		killCounts.put(killerID, killCounts.getOrDefault(killerID, 0) + 1);
		broadcastScoreboard();
	}

	private void broadcastScoreboard() {
		StringBuilder sb = new StringBuilder("scoreboard");
		for (Map.Entry<UUID, Integer> entry : killCounts.entrySet()) {
			sb.append(",").append(entry.getKey()).append(":").append(entry.getValue());
		}
		try {
			forwardPacketToAll(sb.toString(), null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
