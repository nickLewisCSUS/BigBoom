package Client;

import tage.ai.behaviortrees.*;
import tage.*;
import org.joml.*;

public class TurretAIController {
    private BehaviorTree bt;
    private MyGame game;

    public TurretAIController(MyGame g) {
        this.game = g;
        setupBehaviorTree();
    }

    public void update(float elapsedTime) {
        game.setTurretShouldRotate(false);
        bt.update(elapsedTime);
    }

    private void setupBehaviorTree() {
        bt = new BehaviorTree(BTCompositeType.SELECTOR);

        // Behavior tree: if player is near, rotate; else stop rotating
        bt.insertAtRoot(new PlayerNearCondition(game, false));
        bt.insertAtRoot(new PlayerFarCondition(game, false));
    }
}