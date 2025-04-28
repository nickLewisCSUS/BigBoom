package Client;

import tage.ai.behaviortrees.BTCondition;
import tage.ai.behaviortrees.BTStatus;

public class PlayerNearCondition extends BTCondition {
    private MyGame game;

    public PlayerNearCondition(MyGame g, boolean toNegate) {
        super(toNegate);
        this.game = g;
    }

    protected boolean check() {
        float distance = game.getAvatar().getWorldLocation().distance(game.getTurret().getWorldLocation());
        return distance < 10.0f; // player is "near" if within 10 units
    }

    @Override
    public BTStatus update(float elapsedTime) {
        if (check()) {
            game.setTurretShouldRotate(true);
            return BTStatus.BH_SUCCESS;
        } else {
            return BTStatus.BT_FAILURE;
        }
    }
}