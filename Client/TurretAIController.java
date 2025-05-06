package Client;

import tage.ai.behaviortrees.*;
import tage.*;
import tage.input.*;
import tage.shapes.*;
import org.joml.*;

public class TurretAIController {
    private BehaviorTree bt;
    private MyGame game;
    public enum TurretState { VERY_NEAR, NEAR, FAR, NONE }
    private TurretState previousState = TurretState.NONE;
    private ActivateAction activateAction;
    private DeactivateAction deactivateAction;

    public TurretAIController(MyGame g) {
        game = g;
        setupBehaviorTree();
    }

    public void update(float elapsedTime) {
        bt.update(elapsedTime);
    }

    private void setupBehaviorTree() {
        bt = new BehaviorTree(BTCompositeType.SELECTOR);
        activateAction = new ActivateAction(game);
        deactivateAction = new DeactivateAction(game);

        // Close range tracking
        BTSequence trackSequence = new BTSequence(1);
        trackSequence.addChild(new TrackPlayerAction(game));
    
        // Mid range scanning (after activating)
        BTSequence scanSequence = new BTSequence(2);
        scanSequence.addChild(activateAction);
    
        // Long range deactivation
        BTSequence farSequence = new BTSequence(3);
        farSequence.addChild(deactivateAction);
    
        // Insert conditions into behavior tree
        bt.insertAtRoot(new PlayerVeryNearCondition(false, game, game.getTurret(), this, activateAction) {
            public BTStatus update(float e) {
                if (check()) return trackSequence.tick(e);
                return BTStatus.BH_FAILURE; 
            }
        });
    
        bt.insertAtRoot(new PlayerNearCondition(false, game, game.getTurret(), this, activateAction, deactivateAction) {
            public BTStatus update(float e) {
                if (check()) return scanSequence.tick(e);
                return BTStatus.BH_FAILURE;
            }
        });
    
        bt.insertAtRoot(new PlayerFarCondition(false, game, game.getTurret(), this, activateAction) {
            public BTStatus update(float e) {
                if (check()) return farSequence.tick(e);
                return BTStatus.BH_FAILURE;
            }
        });
    }

    public TurretState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(TurretState previousState) {
        this.previousState = previousState;
    }
}
