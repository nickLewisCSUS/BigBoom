package Client;

import tage.ai.behaviortrees.BTCondition;
import tage.GameObject;

public class PlayerFarCondition extends BTCondition {
    private MyGame game;
    private GameObject turret;

    public PlayerFarCondition(boolean toNegate, MyGame game, GameObject turret) {
        super(toNegate);
        this.game = game;
        this.turret = turret;
    }

    @Override
    protected boolean check() {
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return true;

        float dist = closest.getWorldLocation().distance(turret.getWorldLocation());
        System.out.println("DEBUG [PlayerFarCondition]: Closest distance = " + dist + ", In range: " + (dist > 30.0f));
        return dist > 30f;
    }
}
