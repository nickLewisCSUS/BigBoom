package Client;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.Engine;
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.networking.client.GameConnectionClient;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;

public class Bullet {
    private GameObject bulletObj;
    private PhysicsObject bulletPhys;
    private boolean active = true;
    private boolean ownedByLocalPlayer;

    public Bullet(Engine engine, PhysicsEngine physics, ObjShape shape, TextureImage texture, Vector3f worldPosition, Vector3f direction, MyGame game, GameObject gunTip, boolean ownedByLocalPlayer) {
        // Create visual bullet object
        bulletObj = new GameObject(GameObject.root(), shape, texture);
        bulletObj.setLocalTranslation(new Matrix4f().translation(worldPosition));
        bulletObj.setLocalScale(new Matrix4f().scaling(0.5f));
        System.out.println(" Bullet Spawn Y: " + bulletObj.getWorldLocation().y());
        
        // Create physics objects using same world position
        Matrix4f transform = new Matrix4f().translation(worldPosition);
        float[] transformVals = transform.get(new float[16]);
        double[] transformDoubles = game.toDoubleArray(transformVals);
        
        // Add physics body (capsule shape)
        bulletPhys = engine.getSceneGraph().addPhysicsCapsuleX(1f, transformDoubles, 0.2f, 0.5f);
        bulletPhys.setBounciness(0.2f);
        bulletPhys.setDamping(0.1f, 0.1f);

        // Apply force (impulse-style velocity)
        Vector3f velocity = new Vector3f(direction.normalize().mul(30f));
        System.out.println("Applying bullet velocity: " + velocity);
        float[] velocityArr = new float[] { velocity.x(), velocity.y(), velocity.z() };
        bulletPhys.setLinearVelocity(velocityArr);
        bulletObj.setPhysicsObject(bulletPhys);

        this.ownedByLocalPlayer = ownedByLocalPlayer;
    }

    public GameObject getBulletObject() {
        return bulletObj;
    }

    public void deactivate(Engine engine, PhysicsEngine physics) {
        // if (!active) return;
        // active = false;
        engine.getSceneGraph().removeGameObject(bulletObj);
        engine.getSceneGraph().removePhysicsObject(bulletPhys);
    }

    public boolean isOwnedByLocalPlayer() {
        return ownedByLocalPlayer;
    }
}
