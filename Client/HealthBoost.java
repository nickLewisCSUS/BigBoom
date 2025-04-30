package Client;

import tage.GameObject;
import tage.physics.PhysicsObject;

public class HealthBoost extends PowerUp {

    public HealthBoost(MyGame game, GameObject obj, PhysicsObject phys, int boostID, ProtocolClient client) {
        super(game, obj, phys, boostID, client);
    }

    @Override
    public void activate() {
        if (!active) return;
        deactivate();
        game.increasePlayerHealth(20.0f);
    }
}
