package Client;

import tage.GameObject;
import tage.Light;

public class PowerUpLight {
	GameObject holder;
	Light spotlight;

	public PowerUpLight(GameObject holder, Light spotlight) {
		this.holder = holder;
		this.spotlight = spotlight;
	}
}
