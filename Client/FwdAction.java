package Client;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component;
import net.java.games.input.Event;
import org.joml.*;

public class FwdAction extends AbstractInputAction
{
	private MyGame game;
	private GameObject av;
	private Vector3f oldPosition, newPosition;
	private Vector4f fwdDirection;
	private ProtocolClient protClient;

	public FwdAction(MyGame g, ProtocolClient p)
	{	game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e)
	{	av = game.getAvatar();
		oldPosition = av.getWorldLocation();
		fwdDirection = new Vector4f(0f,0f,1f,1f);
		fwdDirection.mul(av.getWorldRotation());

		Component.Identifier id = e.getComponent().getIdentifier();
		float movementAmount;
		float moveSpeed;

		if (game.isBoosted()) {
			movementAmount = game.isUsingSlowTank() ? 0.018f : 0.03f;
			moveSpeed      = game.isUsingSlowTank() ? 0.035f : 0.06f;
		} else {
			movementAmount = game.isUsingSlowTank() ? 0.004f : 0.007f;
			moveSpeed      = game.isUsingSlowTank() ? 0.012f : 0.025f;
		}
		
		if (id == Component.Identifier.Key.W) {
			// consistent per update for digital input
		}
		else if (id == Component.Identifier.Key.S) {
			movementAmount = -movementAmount; // backward for digital input
		}
		else {
			// analog fallback (like gamepad triggers)
			float keyValue = e.getValue();
			if (keyValue > -0.2f && keyValue < 0.2f) return;
			movementAmount = keyValue * moveSpeed * time;
		}

		fwdDirection.mul(movementAmount* time);
		newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());

		// Terrain following and wall collision logic
		if (game.isTerrainFollowMode()) {
			float mazeHeight = game.getMaze().getHeight(newPosition.x(), newPosition.z());

			if (mazeHeight >= 1.0f) {
				// Wall Collision
				System.out.println("Blocked by wall at " + newPosition.x() + ", " + newPosition.z());
				return;
			}
			float terrainHeight = game.getTerrain().getHeight(newPosition.x(), newPosition.z());	
			newPosition.y = terrainHeight - 9f;
		}
		av.setLocalLocation(newPosition);

		protClient.sendMoveMessage(
			av.getWorldLocation(),
			av.getWorldRotation(),
			game.getTankTurret().getLocalRotation()
		);
	}
}