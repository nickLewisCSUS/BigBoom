package Client;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.GameObject;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;
import tage.shapes.AnimatedShape;

public class TrackPlayerAction extends BTAction {
    private MyGame game;
    private AnimatedShape turretS;
    private boolean playerTrackingStarted = false;

    public TrackPlayerAction(MyGame g) {
        game = g;
        if (game.getUseAnimations()) {
            turretS = (AnimatedShape) g.getTurret().getShape();
        }
    }

    protected BTStatus update(float e) {
        GameObject turret = game.getTurret();
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return BTStatus.BH_FAILURE;

        Vector3f playerLoc = closest.getWorldLocation();
        Vector3f turretLoc = turret.getWorldLocation();

        if (!playerTrackingStarted) {
           //System.out.println("Stopping Animation!");
           if (game.getUseAnimations()) {
                turretS.stopAnimation();
            }
            playerTrackingStarted = true;
        }

        // Project the positions to XZ plane
        Vector3f flatPlayer = new Vector3f(playerLoc.x(), turretLoc.y(), playerLoc.z());
        Vector3f direction = flatPlayer.sub(turretLoc, new Vector3f()).normalize();

        // Calculate rotation around Y-axis
        float angle = (float) Math.atan2(direction.x, direction.z); // z forward
        Matrix4f rot = new Matrix4f().rotateY(angle);
        //System.out.println("[TrackPlayerAction] Rotating turret to face closest player: " + closest);
        turret.setLocalRotation(rot);

        if (game.isClosestToTurret()) {
            game.getProtocolClient().sendTurretRotateMessage(rot);
        }
        
        return BTStatus.BH_SUCCESS;
    }

    public boolean getPlayerTrackingStarted() {
        return playerTrackingStarted;
    }

    public void setPlayerTrackingStarted(boolean started) {
        this.playerTrackingStarted = started;
    }
}

