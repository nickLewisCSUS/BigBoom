package Client;

import tage.ai.behaviortrees.BTCondition;
import tage.ai.behaviortrees.BTStatus;

public class PlayerFarCondition extends BTCondition {
    private MyGame game;

    public PlayerFarCondition(MyGame g, boolean toNegate) {
        super(toNegate);
        this.game = g;
    }

    protected boolean check() {
        float distance = game.getAvatar().getWorldLocation().distance(game.getTurret().getWorldLocation());
        return distance >= 10.0f; // player is "far" if 10 units or more
    }

    @Override
    public BTStatus update(float elapsedTime) {
        if (check()) {
            game.setTurretShouldRotate(false);
            return BTStatus.BH_SUCCESS;
        } else {
            return BTStatus.BT_FAILURE;
        }
    }
}
