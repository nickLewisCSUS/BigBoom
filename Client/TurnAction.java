package Client;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component;
import net.java.games.input.Event;
import org.joml.*;
import org.joml.Math;

public class TurnAction extends AbstractInputAction
{
	private MyGame game;
	private GameObject av;
	private Vector4f oldUp;
	private Matrix4f rotAroundAvatarUp, oldRotation, newRotation;
	private ProtocolClient protClient;

	public TurnAction(MyGame g, ProtocolClient p)
	{	game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e)
	{	
		av = game.getAvatar();
		oldRotation = new Matrix4f(av.getWorldRotation());
		oldUp = new Vector4f(0f,1f,0f,1f).mul(oldRotation);
		Vector3f upVec = new Vector3f(oldUp.x(), oldUp.y(), oldUp.z());

		float angle = 0f;
        float baseTurnSpeed = 2.5f;
		
		Component.Identifier id = e.getComponent().getIdentifier();
		if (id == Component.Identifier.Key.A)
			angle = (float) Math.toRadians(baseTurnSpeed); // turn left
		else if (id == Component.Identifier.Key.D)
			angle = (float) Math.toRadians(-baseTurnSpeed ); // turn right
		else {
			// analog stick? fall back to analog logic
			float keyValue = e.getValue();
			if (keyValue > -.2 && keyValue < .2) return;
			float turnSpeed = 0.5f; // keep it low
			angle = keyValue * turnSpeed * time;
		}


		//smooth turn using quadternions
		Quaternionf oldQuat = new Quaternionf().setFromUnnormalized(oldRotation);
		Quaternionf turnQuat = new Quaternionf().rotateAxis(angle, upVec.x(), upVec.y(), upVec.z());
		Quaternionf targetQuat = new Quaternionf(oldQuat).mul(turnQuat);
		Quaternionf smoothQuat = new Quaternionf(oldQuat).slerp(targetQuat, 0.1f);
		Matrix4f smoothRot = new Matrix4f().rotation(smoothQuat);
		av.setLocalRotation(smoothRot);

		// Avoid crash in single-player mode
		protClient.sendMoveMessage(
			av.getWorldLocation(),
			av.getWorldRotation(),
			game.getTankTurret().getLocalRotation()
		);
	}
}


