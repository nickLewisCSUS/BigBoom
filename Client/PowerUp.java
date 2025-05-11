package Client;

import tage.GameObject;
import tage.physics.PhysicsObject;
import tage.networking.IGameConnection.ProtocolType;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import java.util.Random;
import java.util.UUID;

public abstract class PowerUp {
    protected MyGame game;
    protected ProtocolClient protClient;
    protected GameObject boostObject;
    protected PhysicsObject boostPhysics;
    protected boolean active;
    protected long cooldownEndTime;
    protected int boostID;
    protected final long COOLDOWN_DURATION = 5000; // 5 seconds respawn time

    public PowerUp(MyGame game, GameObject obj, PhysicsObject phys, int boostID, ProtocolClient client) {
        this.game = game;
        this.boostObject = obj;
        this.boostPhysics = phys;
        this.boostID = boostID;
        this.protClient = client;
        this.active = true;
        this.cooldownEndTime = 0;
    }

    public boolean isActive() { return active; }

    public int getBoostID() { return boostID; }

    public PhysicsObject getBoostPhysics() {
        return boostPhysics;
    }

    public GameObject getBoostObject() { return boostObject; }

    public void deactivate() {
        active = false;
        moveBelowGround();
        cooldownEndTime = System.currentTimeMillis() + COOLDOWN_DURATION;
        notifyPowerUpUpdate();
    }

    public void update() {
        if (game.isPowerUpAuthority()) { 
            if (!active && System.currentTimeMillis() >= cooldownEndTime) {
                reposition();
                active = true;
                notifyPowerUpUpdate();
            }
        }
    }

    protected void moveBelowGround() {
        Vector3f hiddenPos = new Vector3f(0, -999f, 0);
        boostObject.setLocalLocation(hiddenPos); // Hide it underground

        Matrix4f combined = new Matrix4f();
        combined.identity();
        combined.mul(boostObject.getLocalRotation());
        combined.setTranslation(hiddenPos);

        double[] tempTransform = game.toDoubleArray(combined.get(new float[16]));
        boostPhysics.setTransform(tempTransform);
    }

    public void reposition() {
        Random rand = new Random();
        float x, z, mazeHeight, distanceToPlayer, distanceToOtherBoost;
        Vector3f playerPos = game.getPlayerPosition();
        boolean tooClose;

        do {
            x = (rand.nextFloat() - 0.5f) * 50f;
            z = (rand.nextFloat() - 0.5f) * 50f;
            mazeHeight = game.getMaze().getHeight(x, z);

            distanceToPlayer= (float) Math.sqrt(
                Math.pow(playerPos.x() - x, 2) +
                Math.pow(playerPos.z() - z, 2)
            );

            tooClose = false;
            for (PowerUp otherBoost : game.getPowerUps()) {
                if (otherBoost != this && otherBoost.isActive()) {
                    Vector3f otherPos = otherBoost.getBoostObject().getWorldLocation();
                    distanceToOtherBoost = (float) Math.sqrt(
                        Math.pow(otherPos.x() - x, 2) +
                        Math.pow(otherPos.z() - z, 2)
                    );
                    if (distanceToOtherBoost < 10f) {
                        tooClose = true;
                        break;
                    }
                }
            }
            notifyPowerUpUpdate();
        } while (mazeHeight >= 1.0f || distanceToPlayer < 10f || tooClose);

        float terrainHeight = game.getTerrain().getHeight(x, z);
        Vector3f newPos = new Vector3f(x, terrainHeight - 9f, z);
        boostObject.setLocalLocation(newPos);

        Matrix4f combined = new Matrix4f();
        combined.identity();
        combined.mul(boostObject.getLocalRotation());
        combined.setTranslation(newPos);

        double[] tempTransform = game.toDoubleArray(combined.get(new float[16]));
        boostPhysics.setTransform(tempTransform);
    }

    protected void notifyPowerUpUpdate() {
        Vector3f pos = boostObject.getWorldLocation();
        protClient.sendPowerUpUpdate(boostID, pos);
    }

    public abstract void activate();
}
