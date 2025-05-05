package Client;

import tage.ai.behaviortrees.BTCondition;
import tage.GameObject;
import org.joml.Vector3f;

public class PlayerVeryNearCondition extends BTCondition {
    private MyGame game;

    public PlayerVeryNearCondition(MyGame g) {
        super(false); // not negated
        game = g;
    }

    protected boolean check() {
        GameObject turret = game.getTurret();
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return false;

        float dist = closest.getWorldLocation().distance(turret.getWorldLocation());
        System.out.println("DEBUG [PlayerVeryNearCondition]: Closest distance = " + dist + ", In range: " + (dist <= 10.0f));
        return dist <= 10.0f;
    }
}
