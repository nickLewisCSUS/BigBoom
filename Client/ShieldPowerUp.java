package Client;

import tage.GameObject;
import tage.physics.PhysicsObject;

public class ShieldPowerUp extends PowerUp {

    public ShieldPowerUp(MyGame game, GameObject obj, PhysicsObject phys, int boostID, ProtocolClient client) {
        super(game, obj, phys, boostID, client);
    }

    @Override
    public void activate() {
        if (!active) return;
        deactivate();
        game.activateShield();
    }
}
