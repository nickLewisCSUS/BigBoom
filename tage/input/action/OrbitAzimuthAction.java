package tage.input.action;

import net.java.games.input.Event;
import tage.*;

public class OrbitAzimuthAction extends AbstractInputAction {
	private CameraOrbit3D orbitCamera;
	private float direction; // 1.0 for rotate left, -1.0 for rotate right
	
	public OrbitAzimuthAction(CameraOrbit3D camera, float direction) {
		this.orbitCamera = camera;
		this.direction = direction;
	}
	
	@Override
	public void performAction(float time, Event e) {
		float value = e.getValue();
		orbitCamera.adjustAzimuth(value * 2.0f); // Adjust yaw sensitivity
	}
}