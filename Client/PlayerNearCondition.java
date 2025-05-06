package Client;

import tage.ai.behaviortrees.BTCondition;
import Client.TurretAIController.TurretState;
import tage.GameObject;

public class PlayerNearCondition extends BTCondition {
    private MyGame game;
    private GameObject turret;
    private TurretAIController controller;
    private ActivateAction activateAction;
    private DeactivateAction deactivateAction;

    public PlayerNearCondition(boolean toNegate, MyGame game, GameObject turret, TurretAIController controller, ActivateAction activateAction, DeactivateAction deactivateAction) {
        super(toNegate);
        this.game = game;
        this.turret = turret;
        this.controller = controller;
        this.activateAction = activateAction;
        this.deactivateAction = deactivateAction;
    }

    @Override
    protected boolean check() {
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return false;

        float dist = closest.getWorldLocation().distance(turret.getWorldLocation());
        boolean inRange = (dist >= 10f && dist <= 30f);
        System.out.println("Previous State" + controller.getPreviousState());
        if (inRange && controller.getPreviousState() != TurretAIController.TurretState.NEAR) {
            System.out.println("hfjkhfkladhflkdahfjklfhjlkhajfkhfjkhakj");
            controller.setPreviousState(TurretState.NEAR);
            activateAction.setActivateAnimationStarted(false);
            activateAction.setScanActivationStarted(false);
            deactivateAction.setDeactivateAnimationStarted(false);
        }
        System.out.println("DEBUG [PlayerNearCondition]: Closest distance = " + dist + ", In range: " + (dist >= 10f && dist <= 30f));
        return inRange;
    }
}
