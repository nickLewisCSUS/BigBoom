package Client;

import tage.*;
import tage.CameraOrbit3D;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;
import tage.audio.*;

import java.lang.Math;
import java.awt.*;

import java.awt.event.*;


import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Random;
import java.net.InetAddress;

import java.net.UnknownHostException;

import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.*;
import net.java.games.input.Event;
import tage.networking.IGameConnection.ProtocolType;
import tage.nodeControllers.RotationController;
import tage.physics.*;
import tage.physics.JBullet.*;

import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;

	private int counter=0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime, amt;

	private GameObject avatar, x, y, z, playerHealthBar,terrain, maze, turret, headlightNode, tankTurret, tankGun, gunTip, turretTip;
	private AnimatedShape turretS_A;
	private ObjShape ghostS, tankS, slowTankS, linxS, linyS, linzS, playerHealthBarS, shieldS, terrainS, mazeS, speedBoostS, healthBoostS, tankBodyS, tankTurretS, tankGunS, bulletS, turretS;
	private TextureImage tankT, slowTankT, ghostT, playerHealthBarT, ghostHealthBarT, shieldT, terrainHeightMap, terrainT, mazeHeightMap, mazeT, speedBoostT, healthBoostT, turretT, bulletT;
	
	private boolean useSlowTank = false;
	private boolean useAnimations = false;

	private boolean physicsRenderEnabled = true;

	private Light light, headlight, healthSpotlight;
	private boolean headlightOn = true;
	private boolean darkMode = false;
	private ArrayList<PowerUpLight> powerUpLights = new ArrayList<>();

	private boolean showSpeedBoostHUD = false;
	private boolean showShieldHUD = false;
	private boolean showHealthBoostHUD = false;

	private Map<UUID, Integer> scoreboard = new HashMap<>();

	private boolean turretShouldRotate = false;
	private TurretAIController turretAI;

	private CameraOrbit3D orbitController;

	private IAudioManager audioMgr;
	private Sound turretSound;
	private Sound ambientSound;
	private long ambientFadeStart = 0;
	private boolean isFadingIn = true;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;
	private boolean running = true;

	private int battleField;
	private PhysicsEngine physicsEngine;
	private PhysicsObject avatarP, speedBoostP;

	public enum MovementDirection { NONE, FORWARD, BACKWARD }
	private MovementDirection moveDirection = MovementDirection.NONE;
	private Vector3f nextPosition = null;
	private boolean terrainFollowMode = true;
    private float[] vals = new float[16];
	private boolean showHealthBar = true;
	private float currentHealth = 100.0f;
	private float maxHealth = 100f;
	
	private ArrayList<PowerUp> powerUps = new ArrayList<>();
	private ArrayList<GameObject> avatars = new ArrayList<>();
	private boolean boosted = false;
	private long boostEndTime = 0;
	private boolean shieldActive = false;
	private long shieldEndTime = 0;
	private int nextBoostID = 0;

	private boolean initializedBoosts = false;
	private boolean isPowerUpAuthority = false;

	private float gunPitchAngle = 0f;
	private float turretYawAngle = 0f;
	private final float GUN_PITCH_MIN = (float)Math.toRadians(-10);
	private final float GUN_PITCH_MAX = (float)Math.toRadians(20);
	private float radius, height;

    private RotationController rotCtrl = new RotationController(engine, new Vector3f(0,1,0), 0.001f);
	private PhysicsObject avatarPhys;

	public float getSlowTankScale() { return 0.12f; }
	public float getFastTankScale() { return 0.10f; }
	private ArrayList<Bullet> activeBullets = new ArrayList<>();

	private long lastFireTime = 0;
	private final long fireCoolDownMillis = 250;

	public boolean isPowerUpAuthority() {
		return isPowerUpAuthority;
	}

	public boolean isClientConnected() {
		return isClientConnected;
	}

	public void setPowerUpAuthority(boolean value) {
		isPowerUpAuthority = value;
	}

	public boolean isBoosted() {
		return boosted;
	}

	public TurretAIController getTurretAIController() {
		return this.turretAI;
	}

	public ProtocolClient getProtocolClient() {
		return this.protClient;
	}

	public GameObject getTankTurret() {
		return tankTurret;
	}

	public GameObject getTankGun() {
		return tankGun;
	}

	public ObjShape getTankBodyShape() {
		return tankBodyS;
	}

	public ObjShape getTankTurretShape() {
		return tankTurretS;
	}

	public ObjShape getTankGunShape() {
		return tankGunS;
	}

	public MyGame(String serverAddress, int serverPort, String protocol)
	{	super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args)
	{	MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);

		// Ask user
		if (args.length > 3 && args[3].equalsIgnoreCase("slow")) {
			game.setUseSlowTank(true);
			System.out.println("Slow tank selected via args");
		} else {
			System.out.println("Fast tank selected (default)");
		}
			engine = new Engine(game);
			game.initializeSystem();
			game.game_loop();
		}

	@Override
	public void loadShapes()

	{	ghostS = new Sphere();
		slowTankS = new ImportedModel("tank3.obj");
		shieldS = new ImportedModel("sheildmodel.obj");
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		terrainS = new TerrainPlane(1024);
		mazeS = new TerrainPlane(1024);
		speedBoostS = new ImportedModel("speedboost.obj");
		healthBoostS = new ImportedModel("healthBoost.obj");
		playerHealthBarS = new Cube();
        tankBodyS = new ImportedModel("tankBody.obj");
        tankTurretS = new ImportedModel("tankTurret.obj");
        tankGunS = new ImportedModel("tankGun.obj");
		bulletS = new ImportedModel("tankRound.obj");


		if (useAnimations) {
			turretS_A = new AnimatedShape("turret.rkm", "turret.rks");
			turretS_A.loadAnimation("SCAN", "turretScan.rka");
			turretS_A.loadAnimation("ACTIVATE", "turretActivate.rka");
			turretS_A.loadAnimation("DEACTIVATE", "turretDeactivate.rka");
		} else {
			turretS = new ImportedModel("turret.obj");
		}


	}

	@Override
	public void loadTextures()
	{	tankT = new TextureImage("fastTank.png");
		slowTankT = new TextureImage("slowTank.png");
		shieldT = new TextureImage("sheild.jpg");
		ghostT = new TextureImage("fastTank.png");
		terrainHeightMap = new TextureImage("terrain_height.png");
		terrainT = new TextureImage("terrain_texture1.png");
		mazeHeightMap = new TextureImage("maze.png");
		mazeT = new TextureImage("metal.jpg");
		speedBoostT = new TextureImage("blank.png");
		playerHealthBarT = new TextureImage("blue.jpg");
		healthBoostT = new TextureImage("healthBoost.png");
		turretT = new TextureImage("red.png");
		bulletT = new TextureImage("bulletT.png");
		ghostHealthBarT = new TextureImage("red.png");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialRotation, initialScale;

		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
        physicsEngine.initSystem();

        buildTerrain();
        buildMaze();
        buildAvatar();

		// build turret object
		turret = new GameObject(GameObject.root(), (useAnimations ? turretS_A : turretS), turretT);
		initialTranslation = (new Matrix4f()).translation(-40f,0f,2f);
		turret.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(0.5f, 0.5f, 0.5f);
		turret.setLocalScale(initialScale);

		// build bullet spawn point
		turretTip = new GameObject(turret);
        turretTip.setLocalTranslation(new Matrix4f().translation(0.147f,1.1f,1.4f));
		turretTip.setLocalScale(new Matrix4f().scaling(1f));
        turretTip.propagateTranslation(true);
        turretTip.propagateRotation(true);
		turretTip.applyParentRotationToPosition(true);
		
		// add X,Y,-Z axes
		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f,0f,0f));
		(y.getRenderStates()).setColor(new Vector3f(0f,1f,0f));
		(z.getRenderStates()).setColor(new Vector3f(0f,0f,1f));

		playerHealthBar = new GameObject(avatar, playerHealthBarS, playerHealthBarT);

	}

	@Override
	public void initializeLights()
	{	
		Light.setGlobalAmbient(.5f, .5f, .5f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);

		// Head Light for Tank
		headlight = new Light();
		headlight.setType(Light.LightType.SPOTLIGHT);
		headlight.setAmbient(0.2f, 0.2f, 0.2f);
		headlight.setDiffuse(1.5f, 1.5f, 1.5f);
		headlight.setSpecular(1.0f, 1.0f, 1.0f);
		headlight.setCutoffAngle(15.0f); // Narrow beam
		headlight.setOffAxisExponent(20.0f); // Sharp focus
		headlight.setConstantAttenuation(1.0f);
		headlight.setLinearAttenuation(0.05f);
		headlight.setQuadraticAttenuation(0.01f);

		engine.getSceneGraph().addLight(headlight);

	}

	public void loadSounds()
	{ 
		AudioResource resource1;
		audioMgr = engine.getAudioManager();
		resource1 = audioMgr.createAudioResource("minebeeping.wav", AudioResourceType.AUDIO_SAMPLE);

		turretSound = new Sound(resource1, SoundType.SOUND_EFFECT, 100, true);
		turretSound.initialize(audioMgr);

		
		turretSound.setMaxDistance(50.0f);
		turretSound.setMinDistance(2.0f);
		turretSound.setRollOff(2.0f);

		AudioResource ambientRes = audioMgr.createAudioResource("backgroundAmbience.wav", AudioResourceType.AUDIO_SAMPLE);
		Sound ambientSound = new Sound(ambientRes, SoundType.SOUND_MUSIC, 10, true); // loop, volume 70%
		ambientSound.initialize(audioMgr);
		ambientSound.play();
		ambientFadeStart = System.currentTimeMillis();
	}

	@Override
	public void initializeGame()
	{	prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		elapsedTime = 0.0;

		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		// ----------------- initialize camera ----------------
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		orbitController = new CameraOrbit3D(c, avatar, gpName, "LEFT", engine);


		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();
		setupNetworking();
		setupInputActions();

		// Initialize physics
		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
        physicsEngine.initSystem();
		float[] gravity = {0f, -9.8f, 0f};
		physicsEngine.setGravity(gravity);

		// Setup avatar physics
		if (avatar != null) {
			avatar.getLocalTranslation().get(vals);
			double[] tempTransform = toDoubleArray(vals);
			float mass = 1.0f;
			if (useSlowTank) {

				radius = 1.12f;
				height = 2.9f;
				
			} else {
				radius = 1.1f;
			 	height = 2f;
			}
			avatarP = (engine.getSceneGraph().addPhysicsCapsuleX(mass, tempTransform, radius, height));
			avatarP.setBounciness(0.8f);
			avatar.setPhysicsObject(avatarP);
			} else {
			System.out.println("Warning: Avatar is null during physics setup!");
		}
		
		buildPowerUps();

		// Positon the camera
		//(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(3, 0, 3));
		// Enable rendering
		engine.enableGraphicsWorldRender();
		engine.enablePhysicsWorldRender();

		im = engine.getInputManager();

		// initial sound settings
		turretSound.setLocation(turret.getWorldLocation());
		setEarParameters();
		turretSound.play();

		turretAI = new TurretAIController(this);
	
	}

	public GameObject getAvatar() { return avatar; }
	public GameObject getTerrain() { return terrain; }
	public GameObject getMaze() { return maze; }
	public boolean isTerrainFollowMode() { return terrainFollowMode; }

	public void setEarParameters()
	{ Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		audioMgr.getEar().setLocation(avatar.getWorldLocation());
		audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
	}


	@Override
	public void update()
	{	elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		amt = elapsedTime * 0.03;
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();

		orbitController.updateCameraPosition();
		
		// Step physics world
		physicsEngine.update(0.016f); // 60Hz step

		StringBuilder scoreText = new StringBuilder("Scoreboard:\n");
		int i = 1;
		for (UUID playerId : scoreboard.keySet()) {
			scoreText.append("Player ").append(i++).append(": ")
					.append(scoreboard.get(playerId)).append(" kills\n");
		}
		Vector3f white = new Vector3f(1f, 1f, 1f);
		(engine.getHUDmanager()).setHUD2(scoreText.toString(), white, 20, 900);
		
		// build and set HUD
		int elapsTimeSec = Math.round((float)(System.currentTimeMillis()-startTime)/1000.0f);
		StringBuilder hudText = new StringBuilder("Powerups: ");
		if (showSpeedBoostHUD) hudText.append("Speed Boost  ");
		if (showShieldHUD)     hudText.append("Shield  ");
		if (showHealthBoostHUD) hudText.append("Healed  ");

		Vector3f hudColor = new Vector3f(0.9f, 0.6f, 0.1f); 
		(engine.getHUDmanager()).setHUD3(hudText.toString(), hudColor, 25, 40);

		if (!initializedBoosts && (isPowerUpAuthority || !isClientConnected)) {
			int counter = 0;
			for (PowerUp boost : powerUps) {
				counter++;
				System.out.println("# of powerups:" + counter);
				boost.reposition();
				protClient.sendPowerUpUpdate(boost.getBoostID(), boost.getBoostObject().getWorldLocation());
			} initializedBoosts = true;
		} 

		if (showHealthBar) {
			playerHealthBar.setLocalTranslation(new Matrix4f().translation(0f, 1.5f, 0f));
			float healthRatio = currentHealth / maxHealth;
			float baseLength = 7.0f;
			playerHealthBar.setLocalScale(new Matrix4f().scaling(baseLength * healthRatio, 0.1f, 0.1f));
		} else {
			playerHealthBar.setLocalScale(new Matrix4f().scaling(0f)); // Hide it safely
		}
	
		im.update((float)elapsedTime);
		processNetworking((float)elapsedTime);;

		// Update input and networking
		im.update((float)elapsedTime);
		processNetworking((float)elapsedTime);

		// Headlight of tank update logic
		if (headlightOn) {
			Vector3f pos = headlightNode.getWorldLocation();
			Matrix4f rot = headlightNode.getWorldRotation();
			Vector3f dir = new Vector3f(-rot.m20(), -rot.m21(), -rot.m22()).normalize();
		
			headlight.setLocation(pos);
			headlight.setDirection(dir);
		} else {
			headlight.setLocation(new Vector3f(0, -1000, 0)); 
		}

		for (GhostAvatar ghost : gm.getGhosts()) {
			ghost.updateHeadlight();
		}

		if (running && avatar.getPhysicsObject() != null) {
			checkForCollisions();
			physicsEngine.update((float)elapsTimeSec);

			Vector3f avatarPos = avatar.getWorldLocation();
			float terrainHeight = terrain.getHeight(avatarPos.x(), avatarPos.z());
			avatarPos.y = terrainHeight - 9f;
			avatar.setLocalLocation(avatarPos);
			
			// --- Manually build and push avatar transform into collider ---
			Matrix4f combined = new Matrix4f().identity();
			Matrix4f rotationCorrection = new Matrix4f().rotationY((float)Math.toRadians(90f));
			Matrix4f visualRotation = avatar.getLocalRotation();
			combined.mul(visualRotation);
			combined.mul(rotationCorrection);
			combined.setTranslation(avatar.getWorldLocation());
			double[] tempTransform = toDoubleArray(combined.get(vals));
			avatar.getPhysicsObject().setTransform(tempTransform);

		}
		
		if (useAnimations) {
			turretS_A.updateAnimation();
		}
		turretAI.update((float) elapsedTime);

		for (PowerUpLight pul : powerUpLights) {
			pul.spotlight.setLocation(pul.holder.getWorldLocation());
			pul.spotlight.setDirection(new Vector3f(0f, -1f, 0f));
		}

		// update inputs and camera
		im.update((float)elapsedTime);
        physicsEngine.update(0.016f); // 60Hz step
		if (terrainFollowMode) {
			updateAvatarHeight();
			updateTurretHeight();
		}

		// update sound
		setEarParameters();
		turretSound.setLocation(turret.getWorldLocation());

		if (turretShouldRotate) {
			rotateTurretTowardsPlayer();
		}

		if (isFadingIn && ambientSound != null) {
			long timeSinceStart = System.currentTimeMillis() - ambientFadeStart;
			int targetVolume = (int)(timeSinceStart / 100); // e.g., every 100ms increase 1 volume unit
			if (targetVolume >= 70) {
				ambientSound.setVolume(70); // Max volume
				isFadingIn = false;
			} else {
				ambientSound.setVolume(targetVolume);
			}
		}
		

		Vector3f earLoc = engine.getAudioManager().getEar().getLocation();
		Vector3f mineLoc = turret.getWorldLocation();
		float distance = mineLoc.distance(earLoc);

		// Manually fade volume
		float maxDistance = 15.0f;
		float minDistance = 2.0f;
		int volume = 100;

		if (distance > maxDistance) {
			volume = 0;
		} else if (distance < minDistance) {
			volume = 100;
		} else {
			float percent = (maxDistance - distance) / (maxDistance - minDistance);
    		volume = (int)(percent * 100);
		}

		turretSound.setVolume(volume);

		if (moveDirection != MovementDirection.NONE && nextPosition != null) {
			float terrainHeight = terrain.getHeight(nextPosition.x(), nextPosition.z());
			float mazeHeight = maze.getHeight(nextPosition.x(), nextPosition.z());

			if (mazeHeight >= 1.0f) {
				System.out.println("Blocked by wall at " + nextPosition.x() + ", " + nextPosition.z());
			} else {
				if (terrainFollowMode) {
					Vector3f adjustedPos = new Vector3f(nextPosition.x(), terrainHeight - 9f, nextPosition.z());
					avatar.setLocalLocation(adjustedPos);
				} else {
					avatar.setLocalLocation(nextPosition);
				}
				protClient.sendMoveMessage(
					avatar.getWorldLocation(),
					avatar.getWorldRotation(),
					tankTurret.getWorldRotation()
				);
			}
			moveDirection = MovementDirection.NONE;
			nextPosition = null;
		}

		if (boosted && System.currentTimeMillis() >= boostEndTime) {
			boosted = false;
		}

		for (PowerUp boost : powerUps) {
			boost.update();
		}


		updateBoostStatus();

		if (shieldActive && System.currentTimeMillis() >= shieldEndTime) {
			shieldActive = false;
			showSpeedBoostHUD = false;
			System.out.println("Shield expired.");
		}

		if (boosted && System.currentTimeMillis() >= boostEndTime) {
			boosted = false;
			showSpeedBoostHUD = false;
			System.out.println("Speed boost ended.");
		}
		updateBullets();

	}

	private void checkForCollisions()
	{	
		DynamicsWorld dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		Dispatcher dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		
		for (int i = 0; i < manifoldCount; i++)
		{
			PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
			RigidBody objA = (RigidBody) manifold.getBody0();
			RigidBody objB = (RigidBody) manifold.getBody1();
			
			JBulletPhysicsObject poA = JBulletPhysicsObject.getJBulletPhysicsObject(objA);
			JBulletPhysicsObject poB = JBulletPhysicsObject.getJBulletPhysicsObject(objB);
			
			for (int j = 0; j < manifold.getNumContacts(); j++)
			{
				ManifoldPoint contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f)
				{
					for (PowerUp boost : powerUps) {
						if ((poA == avatar.getPhysicsObject() && poB == boost.getBoostPhysics() ||
							(poB == avatar.getPhysicsObject() && poA == boost.getBoostPhysics()))) {
								if (boost.isActive()) {
									boost.activate();
								}
							}
					}
					System.out.println("Collision between " + poA + " and " + poB + " at " + contactPoint);
					break;
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{	case KeyEvent.VK_ESCAPE:
			{
				// Send the initial join message with a unique identifier for this client
				System.out.println("sending bye message to protocol host");
				protClient.sendByeMessage();
				System.exit(0);
		
			}
			case KeyEvent.VK_H:
			{
				showHealthBar = !showHealthBar;
				break;
			}
			case KeyEvent.VK_K: // test damage with 'K' key
			{	
				if (!isShieldActive()) {
					currentHealth -= 10;
					if (currentHealth < 0) currentHealth = 0;
					protClient.sendHealthUpdate(currentHealth);
					System.out.println("Taking Damage! Current Health: " + currentHealth);
				} else {
					System.out.println("Shield blocked damage!");
				}
				
				break;
			}
			case KeyEvent.VK_T:
			{	
				fireBullet();
				// terrainFollowMode = !terrainFollowMode;
				// if (!terrainFollowMode) {
				// 	avatar.setLocalLocation(new Vector3f(0, 50, 0));
				// 	System.out.println("Free flight mode enabled.");
				// } else {	
				// 	avatar.setLocalLocation(new Vector3f(3,0,-3));
				// 	avatar.lookAt(new Vector3f(0,0,0));
				// 	System.out.println("Terrain-following mode enabled.");
				// }
				break;
			}
			case KeyEvent.VK_F:
			{
				headlightOn = !headlightOn;
				protClient.sendHeadlightState(headlightOn);
				break;
			}
			case KeyEvent.VK_P:
			{
				physicsRenderEnabled = !physicsRenderEnabled;
				System.out.println("Physics render: " + (physicsRenderEnabled ? "ON" : "OFF"));

				if (physicsRenderEnabled)
					engine.enablePhysicsWorldRender();
				else
					engine.disablePhysicsWorldRender();
				break;
			}
			case KeyEvent.VK_E:
			{
				darkMode = !darkMode;
				if (darkMode) {
					Light.setGlobalAmbient(0.05f, 0.05f, 0.05f); // almost pitch dark
					System.out.println("Dark mode ON");
				} else {
					Light.setGlobalAmbient(0.5f, 0.5f, 0.5f); // normal lighting
					System.out.println("Dark mode OFF");
				}
				break;
			}
			case KeyEvent.VK_SPACE:
			{
				fireBullet();
				break;
			}
		}
		super.keyPressed(e);
	}

	// -------- Health Bar Section ---------
	private class ToggleHealthBarAction extends AbstractInputAction {
		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			showHealthBar = !showHealthBar;
			System.out.println("Health bar toggled: " + (showHealthBar ? "ON" : "OFF"));
		}
	}
	public ObjShape getPlayerHealthBarShape() {
		return playerHealthBarS;
	}
	
	public TextureImage getPlayerHealthBarTexture() {
		return playerHealthBarT;
	}

	public void setupInputActions() {
		FwdAction fwdAction = new FwdAction(this, protClient);
		TurnAction turnAction = new TurnAction(this, protClient);
		ToggleHealthBarAction toggleHealthBar = new ToggleHealthBarAction();

		// Arrow key actions
		AbstractInputAction pitchUp = new AbstractInputAction() {
			public void performAction(float time, Event evt) {
				updateGunPitch((float)Math.toRadians(-0.7));
			}
		};

		AbstractInputAction pitchDown = new AbstractInputAction() {
			public void performAction(float time, Event evt) {
				updateGunPitch((float)Math.toRadians(0.7));
			}
		};

		AbstractInputAction yawLeft = new AbstractInputAction() {
			public void performAction(float time, Event evt) {
				updateTurretYaw((float)Math.toRadians(0.7));
			}
		};

		AbstractInputAction yawRight = new AbstractInputAction() {
			public void performAction(float time, Event evt) {
				updateTurretYaw((float)Math.toRadians(-0.7));
			}
		};

		im.associateActionWithAllKeyboards(Key.W, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.S, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.A, turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.D, turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// Bind to arrow keys
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.UP, pitchUp, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.DOWN, pitchDown, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.LEFT, yawLeft, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.RIGHT, yawRight, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// im.associateActionWithAllGamepads(Identifier.Button._1, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		// im.associateActionWithAllGamepads(Identifier.Axis.X, turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Button._2, toggleHealthBar, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public ObjShape getBulletShape() { return bulletS; }
	public TextureImage getBulletTexture() { return bulletT; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	
	private void setupNetworking()
	{	isClientConnected = false;	
		try 
		{	protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	catch (UnknownHostException e) 
		{	e.printStackTrace();
		}	catch (IOException e) 
		{	e.printStackTrace();
		}
		if (protClient == null)
		{	System.out.println("missing protocol host");
		}
		else
		{	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public void setUseSlowTank(boolean val) {
		this.useSlowTank = val;
	}

	public void setUseAnimations(boolean val) {
		this.useAnimations = val;
	}

	public boolean getUseAnimations() {
		return this.useAnimations;
	}

	public void setTurretShouldRotate(boolean shouldRotate) {
		turretShouldRotate = shouldRotate;
	}

	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	protClient.sendByeMessage();
			}
		}
	}

	@Override
	public void loadSkyBoxes() {
		battleField = engine.getSceneGraph().loadCubeMap("battleField");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(battleField);
		engine.getSceneGraph().setSkyBoxEnabled(true);
	}

	private void buildTerrain() {
        float[] up = {0, 1, 0};
        terrain = new GameObject(GameObject.root(), terrainS, terrainT);
        terrain.setLocalLocation(new Vector3f(0, -10, 0));
        terrain.setLocalScale(new Matrix4f().scaling(300f, 20f, 300f));
        terrain.setHeightMap(terrainHeightMap);
        terrain.getRenderStates().setTiling(1);

        Matrix4f trans = new Matrix4f(terrain.getLocalTranslation());
        double[] transform = toDoubleArray(trans.get(vals));
        PhysicsObject plane = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(), transform, up, 0f);
        terrain.setPhysicsObject(plane);
    }

    private void buildMaze() {
        float[] up = {0, 1, 0};
        maze = new GameObject(GameObject.root(), mazeS, mazeT);
        maze.setLocalLocation(new Vector3f(0, -11, 0));
        maze.setLocalScale(new Matrix4f().scaling(300f, 20f, 300f));
        maze.setHeightMap(mazeHeightMap);
        maze.getRenderStates().setTiling(1);
		maze.getRenderStates().setTileFactor(1024);

        Matrix4f trans = new Matrix4f(maze.getLocalTranslation());
        double[] transform = toDoubleArray(trans.get(vals));
        PhysicsObject plane = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(), transform, up, 0f);
        maze.setPhysicsObject(plane);
    }

    private void buildAvatar() {
		float scale = useSlowTank ? getSlowTankScale() : getFastTankScale();
		TextureImage texture = useSlowTank ? slowTankT : tankT;

		// Tank Body (parent)
		avatar = new GameObject(GameObject.root(), tankBodyS, texture);
		avatar.setLocalTranslation(new Matrix4f().translation(3, 0, -3));
		avatar.setLocalScale(new Matrix4f().scaling(scale));

		// Scaled offsets (just like GhostAvatar)
		float turretOffsetY = 0.25f;
		float turretOffsetZ = 0.4f * scale;
		float gunOffsetY = 0.25f;
		float gunOffsetZ = 0.7f * scale;

		// Turret base
		tankTurret = new GameObject(avatar, tankTurretS, texture);
		tankTurret.setLocalTranslation(new Matrix4f().translation(0f, turretOffsetY, turretOffsetZ));
		tankTurret.propagateTranslation(true);
		tankTurret.propagateRotation(true);
		tankTurret.applyParentRotationToPosition(true);

        // Gun (child of turret base)
        tankGun = new GameObject(tankTurret, tankGunS, playerHealthBarT);
        tankGun.setLocalTranslation(new Matrix4f().translation(0,gunOffsetY, gunOffsetZ));
        tankGun.propagateTranslation(true);
        tankGun.propagateRotation(true);
		tankGun.applyParentRotationToPosition(true);
		
		// build bullet spawn point
		gunTip = new GameObject(tankGun);
        gunTip.setLocalTranslation(new Matrix4f().translation(0,0,2.1f));
		gunTip.setLocalScale(new Matrix4f().scaling(5f));
        gunTip.propagateTranslation(true);
        gunTip.propagateRotation(true);
		gunTip.applyParentRotationToPosition(true);
		avatar.lookAt(new Vector3f(0,0,0));
		
		// Optional transform correction
		Matrix4f correctedTransform = new Matrix4f().identity()
			.mul(new Matrix4f().rotationZ((float) Math.toRadians(90.0f)))
			.setTranslation(avatar.getWorldLocation());

		// Headlight node
		headlightNode = new GameObject(avatar, linxS, playerHealthBarT);
		headlightNode.setLocalScale(new Matrix4f().scaling(0f)); // invisible
		headlightNode.setLocalTranslation(new Matrix4f().translation(0f, 0.3f, 0f));

		avatars.add(avatar);
	}

	

	public ObjShape getSlowTankShape() {
		return slowTankS;
	}
	
	public ObjShape getFastTankShape() {
		return tankS;
	}
	
	public TextureImage getFastTankTexture() {
		return tankT;
	}
	public TextureImage getSlowTankTexture() {
		return slowTankT;
	}

	private void updateAvatarHeight() {
		// Vector3f loc = avatar.getWorldLocation();
		// float height = terrain.getHeight(loc.x(), loc.z());
		// Vector3f corrected = new Vector3f(loc.x(), height - 9f, loc.z());
		// avatar.setLocalLocation(corrected);
	}

	private void updateTurretHeight() {
		Vector3f loc = turret.getWorldLocation();
		float height = terrain.getHeight(loc.x(), loc.z());
		Vector3f corrected = new Vector3f(loc.x(), height - 10f, loc.z());
		turret.setLocalLocation(corrected);
	}
	
	public GameObject getTurret() {
		return turret;
	}

    public double[] toDoubleArray(float[] arr) {
		double[] result = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			result[i] = (double) arr[i];
		}
		return result;
    }

    public float[] toFloatArray(double[] arr) {
        float[] result = new float[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = (float) arr[i];
        return result;
    }

	public void buildPowerUps() {

		int numEachType = 10;
		
		for (int i = 0; i < numEachType; i++) {
			
			// --- Speed Boost ---
			GameObject speedObj = new GameObject(GameObject.root(), speedBoostS, speedBoostT);
			speedObj.setLocalScale(new Matrix4f().scaling(0.25f));
			PhysicsObject speedPhys = physicsEngine.addSphereObject(
				physicsEngine.nextUID(), 
				0f, 
				toDoubleArray(speedObj.getLocalTranslation().get(new float[16])), 
				0.7f
			);
			speedObj.setPhysicsObject(speedPhys);
            rotCtrl.addTarget(speedObj);
			speedPhys = (engine.getSceneGraph().addPhysicsSphere(0, toDoubleArray(speedObj.getLocalTranslation().get(new float[16])), 0.7f));	
			speedObj.setPhysicsObject(speedPhys);

			SpeedBoost speedBoost = new SpeedBoost(this, speedObj, speedPhys, nextBoostID++, protClient);
			powerUps.add(speedBoost);

			// --- Health Boost ---
			GameObject healthObj = new GameObject(GameObject.root(), healthBoostS, healthBoostT);
			healthObj.setLocalScale(new Matrix4f().scaling(0.20f));
			PhysicsObject healthPhys = physicsEngine.addSphereObject(
				physicsEngine.nextUID(), 
				0f, 
				toDoubleArray(healthObj.getLocalTranslation().get(new float[16])), 
				0.7f
			);
			healthObj.setPhysicsObject(healthPhys);
            rotCtrl.addTarget(healthObj);
			healthPhys = (engine.getSceneGraph().addPhysicsSphere(0, toDoubleArray(healthObj.getLocalTranslation().get(new float[16])), 0.7f));	
			healthObj.setPhysicsObject(healthPhys);
	
			HealthBoost healthBoost = new HealthBoost(this, healthObj, healthPhys, nextBoostID++, protClient);
			powerUps.add(healthBoost);

			// --- Sheild Powerup ---
			GameObject sheildObj = new GameObject(GameObject.root(), shieldS, shieldT);
			sheildObj.setLocalScale(new Matrix4f().scaling(0.25f));
			PhysicsObject sheildPhys = physicsEngine.addSphereObject(
				physicsEngine.nextUID(), 
				0f, 
				toDoubleArray(sheildObj.getLocalTranslation().get(new float[16])), 
				0.7f
			);
			sheildObj.setPhysicsObject(sheildPhys);
            rotCtrl.addTarget(sheildObj);
			sheildPhys = (engine.getSceneGraph().addPhysicsSphere(0, toDoubleArray(sheildObj.getLocalTranslation().get(new float[16])), 0.7f));	
			sheildObj.setPhysicsObject(sheildPhys);

			ShieldPowerUp shieldPowerUp = new ShieldPowerUp(this, sheildObj, sheildPhys, nextBoostID++, protClient);
			powerUps.add(shieldPowerUp);

			addSpotlightAbove(speedObj, new Vector3f(0f, 1f, 0f));     // green for speed
			addSpotlightAbove(healthObj, new Vector3f(1f, 0f, 0f));    // red for health
			addSpotlightAbove(sheildObj, new Vector3f(1f, 1f, 0f));    // yellow for shield

            engine.getSceneGraph().addNodeController(rotCtrl);
            rotCtrl.enable();
		}
		initializedBoosts = false;
	}

	private void updateBoostStatus() {
		if (boosted && System.currentTimeMillis() >= boostEndTime) {
			boosted = false;
		}
	}

	private void addSpotlightAbove(GameObject obj, Vector3f color) {
		// Create a light object
		Light spotlight = new Light();
		spotlight.setType(Light.LightType.SPOTLIGHT);
		spotlight.setAmbient(0.1f, 0.1f, 0.1f);
		spotlight.setDiffuse(color.x(), color.y(), color.z());
		spotlight.setSpecular(color.x(), color.y(), color.z());
		spotlight.setCutoffAngle(10.0f); // smaller cone
		spotlight.setOffAxisExponent(20.0f);
		spotlight.setConstantAttenuation(1.0f);
		spotlight.setLinearAttenuation(0.05f);
		spotlight.setQuadraticAttenuation(0.01f);
	
		// Create a child GameObject that holds the light above the powerup
		GameObject lightHolder = new GameObject(obj);  // parented to power-up
		lightHolder.setLocalTranslation(new Matrix4f().translation(0f, 2f, 0f)); // hover above
	
		spotlight.setLocation(lightHolder.getWorldLocation());
		spotlight.setDirection(new Vector3f(0f, -1f, 0f)); // shine down
	
		engine.getSceneGraph().addLight(spotlight);

		powerUpLights.add(new PowerUpLight(lightHolder, spotlight));
	}

	public void updateScoreboard(Map<UUID, Integer> newBoard) {
		scoreboard = newBoard;
	}

	public boolean isUsingSlowTank() {
		return useSlowTank;
	}
	
	public ArrayList<PowerUp> getPowerUps() {
		return powerUps;
	}

	public TextureImage getGhostHealthBarTexture() {
		return ghostHealthBarT;
	}

	public void increasePlayerHealth(float amount) {
		currentHealth += amount;
		if (currentHealth > maxHealth) {
			currentHealth = maxHealth;
		}
		showHealthBoostHUD = true;
		if (protClient != null) {
			protClient.sendHealthUpdate(currentHealth);
		}
		System.out.println("Health increased! Current health: " + currentHealth);
	}

	public void activateShield() {
		shieldActive = true;
		shieldEndTime = System.currentTimeMillis() + 5000;
		showShieldHUD = true;
		System.out.println("Sheild activated!");
	}

	public boolean isShieldActive() {
		return shieldActive;
	}

	public void activateSpeedBoost() {
		boosted = true;
		boostEndTime = System.currentTimeMillis() + 5000;
		showSpeedBoostHUD = true;
		System.out.println("Speed boost activated");
	}
	private void rotateTurretTowardsPlayer() {
		Vector3f turretPos = turret.getWorldLocation();
		Vector3f avatarPos = avatar.getWorldLocation();
	
		Vector3f direction = new Vector3f(
			avatarPos.x() - turretPos.x(),
			0,
			avatarPos.z() - turretPos.z()
		);
	
		direction.normalize();
	
		float angle = (float) Math.atan2(direction.x(), direction.z());
	
		// Keep the turret's current translation and scale
		Matrix4f currentTranslation = turret.getLocalTranslation();
		Matrix4f currentScale = turret.getLocalScale();
	
		// Set only the rotation
		Matrix4f rotation = new Matrix4f().rotationY(angle);
	
		turret.setLocalRotation(rotation);
		turret.setLocalTranslation(currentTranslation);
		turret.setLocalScale(currentScale);
	}

	public List<GameObject> getAllAvatars() {
		return avatars;
	}

	public GameObject getClosestAvatar(GameObject from) {
		float minDist = Float.MAX_VALUE;
		GameObject closest = null;
		for (GameObject avatar : getAllAvatars()) {
			float dist = avatar.getWorldLocation().distance(from.getWorldLocation());
			if (dist < minDist) {
				minDist = dist;
				closest = avatar;
			}
		}
		return closest;
	}

	public boolean isClosestToTurret() {
		GameObject closest = getClosestAvatar(getTurret());
		return closest == getAvatar();
	}	

	private void updateGunPitch(float deltaAngle) {
		gunPitchAngle += deltaAngle;

		// Clamp pitch between -45 and +20 degrees
		gunPitchAngle = Math.max((float)Math.toRadians(-45), Math.min((float)Math.toRadians(0), gunPitchAngle));

		// Reapply rotation
		Matrix4f pitch = new Matrix4f().rotationX(gunPitchAngle);
		tankGun.setLocalRotation(pitch);
		if (protClient != null) protClient.sendGunRotationMessage(pitch);

	}

	private void updateTurretYaw(float deltaAngle) {
		turretYawAngle += deltaAngle;

		// Clamp pitch between -45 and +20 degrees
		turretYawAngle = Math.max((float)Math.toRadians(-180), Math.min((float)Math.toRadians(180), turretYawAngle));

		// Reapply rotation
		Matrix4f yaw = new Matrix4f().rotationY(turretYawAngle);
		tankTurret.setLocalRotation(yaw);
		orbitController.setTurretYaw(turretYawAngle);

		if (protClient != null) {
			protClient.sendTankTurretRotationMessage(tankTurret.getLocalRotation());
		}
	}

	public float getTurretYawAngle() {
		return turretYawAngle;
	}

	public CameraOrbit3D getOrbitController() {
		return orbitController;
	}
	
	public void fireBullet() { 
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastFireTime < fireCoolDownMillis) {
			// Too soon to fire
			return;
		}

		lastFireTime = currentTime;

		Vector3f position = gunTip.getWorldLocation();
		Matrix4f rotation = gunTip.getWorldRotation();
		Vector3f direction = tankGun.getWorldForwardVector();
		System.out.println("ATTEMPTING TO FIRE BULLET");
		System.out.println("Bullet Spawn Y: " + position.y());
		System.out.println("Applying Bullet Velocity: " + direction.mul(30f));
		Bullet bullet = new Bullet(engine, physicsEngine, bulletS, bulletT, position, rotation, direction, this, gunTip);
		activeBullets.add(bullet);

		if (protClient != null) {
			protClient.sendBulletMessage(position, direction);
		}
	}

	public void updateBullets() {
		Iterator<Bullet> iterator = activeBullets.iterator();
		while (iterator.hasNext()) {
			Bullet b = iterator.next();
			GameObject obj = b.getBulletObject();

			// -- Sync visual transform with physics transform ---
			float[] floatTransform = toFloatArray(obj.getPhysicsObject().getTransform());
			Matrix4f physicsMatrix = new Matrix4f().set(floatTransform);
			Matrix4f translationOnly = new Matrix4f().identity().setTranslation(physicsMatrix.m30(), physicsMatrix.m31(), physicsMatrix.m32());
			obj.setLocalTranslation(translationOnly);

			// --- Check terrain height to remove bullet if it falls too low ---
			Vector3f loc = obj.getWorldLocation();
			System.out.println("Bullet Height: " + loc.y());

			//float mazeHeight = terrain.getHeight(loc.x(), loc.z());
			if (loc.y() < 0 - 10f) { // too low = delete
				//System.out.println("Terrain Height: " + terrainHeight);
				b.deactivate(engine, physicsEngine);
				iterator.remove();
			}

			for (PowerUp boost : powerUps) {
				if (obj.getWorldLocation().distance(boost.getBoostObject().getWorldLocation()) < 1.0f) {
					System.out.println("Bullet hit power-up!");
					// b.deactivate(engine, physicsEngine);
					// iterator.remove();
					break;
				}
			}
		}
	}

	public PhysicsEngine getPhysicsEngine() {
		return physicsEngine;
	}

	public ArrayList<Bullet> getActiveBullets() {
		return activeBullets;
	}
}