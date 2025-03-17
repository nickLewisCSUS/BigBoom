package tage.input.action;

import net.java.games.input.Event;
import tage.*;

public class OrbitElevationAction extends AbstractInputAction {
	private CameraOrbit3D orbitCamera;
	private float direction; // 1.0 for rotate up, -1.0 for rotate down
	
	public OrbitElevationAction(CameraOrbit3D camera,  float direction) {
		this.orbitCamera = camera;
		this.direction = direction;
	}
	
	@Override
	public void performAction(float time, Event e) {
		float value = e.getValue();
		orbitCamera.adjustElevation(value * direction * 2.0f);
	}
}