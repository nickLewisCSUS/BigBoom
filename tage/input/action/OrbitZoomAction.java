package tage.input.action;

import net.java.games.input.Event;
import tage.*;

public class OrbitZoomAction extends AbstractInputAction {
	private CameraOrbit3D orbitCamera;
	private float direction; // 1.0 for zoom in, -1.0 for zoom out
	
	public OrbitZoomAction(CameraOrbit3D camera, float direction) {
		this.orbitCamera = camera;
		this.direction = direction;
	}
	
	@Override
	public void performAction(float time, Event e) {
		float value = e.getValue();
		
		// Filter out negative drift (sometimes triggers return negatives when released)
		if (value < 0) value = 0;
		
		orbitCamera.adjustZoom(value * direction * 2.0f);
	}
}