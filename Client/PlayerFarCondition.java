package Client;

import tage.ai.behaviortrees.BTCondition;
import tage.GameObject;

public class PlayerFarCondition extends BTCondition {
    private MyGame game;
    private GameObject turret;
    private ActivateAction activateAction;
    private TurretAIController controller;

    public PlayerFarCondition(boolean toNegate, MyGame game, GameObject turret,TurretAIController controller, ActivateAction activateAction) {
        super(toNegate);
        this.game = game;
        this.turret = turret; 
        this.controller = controller;
        this.activateAction = activateAction;
    }

    @Override
    protected boolean check() {
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return true;

        float dist = closest.getWorldLocation().distance(turret.getWorldLocation());
        boolean inRange = (dist > 30f);
        if (inRange && controller.getPreviousState() != TurretAIController.TurretState.FAR) {
            activateAction.setActivateAnimationStarted(false);
            activateAction.setScanActivationStarted(false);
            System.out.println("DEBUG [PlayerFarCondition]: Closest distance = " + dist + ", In range: " + (dist > 30.0f));
        }
        return inRange;
    }
}
