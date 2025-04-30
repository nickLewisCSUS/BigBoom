package Client;

import org.joml.Vector3f;
import tage.GameObject;
import tage.physics.PhysicsObject;
import tage.networking.IGameConnection.ProtocolType;

public class SpeedBoost extends PowerUp {
    
    public SpeedBoost(MyGame game, GameObject obj, PhysicsObject phys, int boostID, ProtocolClient client) {
        super(game, obj, phys, boostID, client);
    }

    @Override
    public void activate() {
        if (!active) return;
        deactivate();
        game.activateSpeedBoost();
    }
}