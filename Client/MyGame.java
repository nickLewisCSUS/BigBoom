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
import java.util.Random;
import java.net.InetAddress;

import java.net.UnknownHostException;

import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;
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

	private GameObject avatar, x, y, z, playerHealthBar, shield, terrain, maze, speedBoost, turret, headlightNode;
	private ObjShape ghostS, tankS, linxS, linyS, linzS, playerHealthBarS, shieldS, terrainS, mazeS, speedBoostS, healthBoostS, turretS;
	private TextureImage tankT, ghostT, playerHealthBarT, shieldT, terrainHeightMap, terrainT, mazeHeightMap, mazeT, speedBoostT, healthBoostT, turretT;

	private Light light;
	private Light headlight;
	private boolean headlightOn = true;

	private boolean turretShouldRotate = false;
	private TurretAIController turretAI;

	private CameraOrbit3D orbitController;

	private IAudioManager audioMgr;
	private Sound turretSound;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;
	private boolean running = false;

	private int battleField;
	private PhysicsEngine physicsEngine;
	private PhysicsObject avatarP, terrainP, speedBoostP;

	private Vector3f lastValidPosition;
	public enum MovementDirection { NONE, FORWARD, BACKWARD }
	private MovementDirection moveDirection = MovementDirection.NONE;
	private Vector3f nextPosition = null;
	private boolean terrainFollowMode = true;
    private float[] vals = new float[16];
	private boolean showHealthBar = true;
	private float currentHealth = 100.0f;
	private float maxHealth = 100f;
	
	private ArrayList<PowerUp> powerUps = new ArrayList<>();
	private boolean boosted = false;
	private long boostEndTime = 0;
	private boolean shieldActive = false;
	private long shieldEndTime = 0;
	private int nextBoostID = 0;

	private boolean initializedBoosts = false;
	private boolean isPowerUpAuthority = false;

	public boolean isPowerUpAuthority() {
		return isPowerUpAuthority;
	}

	public void setPowerUpAuthority(boolean value) {
		isPowerUpAuthority = value;
	}

	public boolean isBoosted() {
		return boosted;
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
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	turretS = new ImportedModel("turret1.obj");
		//turretS.loadAnimation("scanning", "turret.rka");
		ghostS = new Sphere();
		tankS = new ImportedModel("tiger2.obj");
		shieldS = new ImportedModel("sheildmodel.obj");
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		terrainS = new TerrainPlane(1024);
		mazeS = new TerrainPlane(1024);
		speedBoostS = new ImportedModel("speedboost.obj");
		healthBoostS = new ImportedModel("healthBoost.obj");
		playerHealthBarS = new Cube();
	}

	@Override
	public void loadTextures()
	{	tankT = new TextureImage("tanktext.png");
		shieldT = new TextureImage("sheild.jpg");
		ghostT = new TextureImage("redDolphin.jpg");
		terrainHeightMap = new TextureImage("terrain_height.png");
		terrainT = new TextureImage("terrain_texture1.png");
		mazeHeightMap = new TextureImage("maze.png");
		mazeT = new TextureImage("metal.jpg");
		speedBoostT = new TextureImage("blank.png");//"speedBoostTx.png");
		playerHealthBarT = new TextureImage("red.png");
		healthBoostT = new TextureImage("healthBoost.png");
		turretT = new TextureImage("red.png");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialRotation, initialScale;

		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
        physicsEngine.initSystem();

        buildTerrain();
        buildMaze();
        buildAvatar();

		lastValidPosition = avatar.getWorldLocation();

		// build speed powerup
		speedBoost = new GameObject(GameObject.root(), speedBoostS, speedBoostT);
		initialTranslation = (new Matrix4f()).translation(0f,0f,-1f);
		speedBoost.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(135.0f));
		speedBoost.setLocalRotation(initialRotation);
		initialScale = (new Matrix4f()).scaling(0.25f);
		speedBoost.setLocalScale(initialScale);
		double[] transform = toDoubleArray(speedBoost.getLocalTranslation().get(vals));
		speedBoostP = physicsEngine.addSphereObject(physicsEngine.nextUID(), 0f, transform, 0.70f);
		speedBoost.setPhysicsObject(speedBoostP);

		// build shield upgrade object
		shield = new GameObject(GameObject.root(), shieldS, shieldT);
		initialTranslation = (new Matrix4f()).translation(1f,0f,1f);
		shield.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(135.0f));
		shield.setLocalRotation(initialRotation);
		initialScale = (new Matrix4f()).scaling(0.1f, 0.1f, 0.1f);
		shield.setLocalScale(initialScale);

		// build turret object
		turret = new GameObject(GameObject.root(), turretS, turretT);
		initialTranslation = (new Matrix4f()).translation(-10f,0f,2f);
		turret.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(0.5f, 0.5f, 0.5f);
		turret.setLocalScale(initialScale); 
		
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
			//float radius = 0.38f;
			//float height = 1.2f;
			float radius = 1.1f;
			float height = 1.5f;
			avatarP = (engine.getSceneGraph().addPhysicsCapsuleX(mass, tempTransform, radius, height));
			avatarP.setBounciness(0.8f);
			avatar.setPhysicsObject(avatarP);
		} else {
			System.out.println("Warning: Avatar is null during physics setup!");
		}

		// Setup speedboost physics
		if (speedBoost != null) {
			speedBoost.getLocalTranslation().get(vals);
			double[] tempTransform = toDoubleArray(vals);
			float mass = 0.0f;
			float radius = 0.70f; 
			speedBoostP = (engine.getSceneGraph().addPhysicsSphere(mass, tempTransform, radius));
			speedBoostP.setBounciness(0.8f);
			speedBoost.setPhysicsObject(speedBoostP);
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
	{	Matrix4f  currentTranslation, currentRotation;
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		amt = elapsedTime * 0.03;
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();

		orbitController.updateCameraPosition();
		
		// build and set HUD
		int elapsTimeSec = Math.round((float)(System.currentTimeMillis()-startTime)/1000.0f);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String counterStr = Integer.toString(counter);
		String dispStr1 = "Time = " + elapsTimeStr;
		String dispStr2 = "camera position = "
			+ (c.getLocation()).x()
			+ ", " + (c.getLocation()).y()
			+ ", " + (c.getLocation()).z();
		Vector3f hud1Color = new Vector3f(1,0,0);
		Vector3f hud2Color = new Vector3f(1,1,1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

		if (!initializedBoosts && isPowerUpAuthority) {
			int counter = 0;
			for (PowerUp boost : powerUps) {
				counter++;
				System.out.println("# of powerups:" + counter);
				boost.reposition();
				protClient.sendPowerUpUpdate(boost.getBoostID(), boost.getBoostObject().getWorldLocation());
			} initializedBoosts = true;
		} 

		if (showHealthBar) {
			playerHealthBar.setLocalTranslation(new Matrix4f().translation(0f, 2.5f, 0f));
			float healthRatio = currentHealth / maxHealth;
			float baseLength = 3.0f;
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

		// Step physics world
        physicsEngine.update(0.016f); // 60Hz step

		if (running && avatar.getPhysicsObject() != null) {
			checkForCollisions();
			physicsEngine.update((float)elapsTimeSec);

			Vector3f avatarPos = avatar.getWorldLocation();
			float terrainHeight = terrain.getHeight(avatarPos.x(), avatarPos.z());
			avatarPos.y = terrainHeight - 9f;
			avatar.setLocalLocation(avatarPos);
			
			// --- Manually build and push avatar transform into collider ---
			Matrix4f combined = new Matrix4f();
			combined.identity();
			Matrix4f rotationCorrection = new Matrix4f().rotationY((float)Math.toRadians(90f));
			combined.mul(avatar.getLocalRotation());
			combined.mul(rotationCorrection);
			combined.setTranslation(avatar.getWorldLocation());
			double[] tempTransform = toDoubleArray(combined.get(vals));
			avatar.getPhysicsObject().setTransform(tempTransform);

			// --- Manually build and push avatar transform into collider ---
			combined = new Matrix4f();
			combined.identity();
			rotationCorrection = new Matrix4f().rotationY((float)Math.toRadians(90f));
			combined.mul(speedBoost.getLocalRotation());
			combined.mul(rotationCorrection);
			combined.setTranslation(speedBoost.getWorldLocation());
			tempTransform = toDoubleArray(combined.get(vals));
			speedBoost.getPhysicsObject().setTransform(tempTransform);
		}
		turretAI.update((float) elapsedTime);

		// update inputs and camera
		im.update((float)elapsedTime);
        physicsEngine.update(0.016f); // 60Hz step
		if (terrainFollowMode) {
			updateAvatarHeight();
			updateAvatarPhysics();
			updateTurretHeight();
		}

		// update sound
		setEarParameters();
		turretSound.setLocation(turret.getWorldLocation());

		if (turretShouldRotate) {
			rotateTurretTowardsPlayer();
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
				protClient.sendMoveMessage(avatar.getWorldLocation(), avatar.getWorldRotation());
			}
			moveDirection = MovementDirection.NONE;
			nextPosition = null;
		}

		// Update health bar position and scale
		playerHealthBar.setLocalTranslation(new Matrix4f().translation(0f, 0.4f, 0f));
		float healthRatio = currentHealth / maxHealth;
		float baseLength = 0.25f;
		playerHealthBar.setLocalScale(new Matrix4f().scaling(baseLength * healthRatio, 0.001f, 0.001f));
		playerHealthBar.getRenderStates().setColor(new Vector3f(1f, 0f, 0f));

		if (boosted && System.currentTimeMillis() >= boostEndTime) {
			boosted = false;
		}

		for (PowerUp boost : powerUps) {
			boost.update();
		}


		updateBoostStatus();

		if (shieldActive && System.currentTimeMillis() >= shieldEndTime) {
			shieldActive = false;
			System.out.println("Shield expired.");
		}

		if (boosted && System.currentTimeMillis() >= boostEndTime) {
			boosted = false;
			System.out.println("Speed boost ended.");
		}
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
		{	case KeyEvent.VK_H:
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
			case KeyEvent.VK_F:
			{	
				terrainFollowMode = !terrainFollowMode;
				if (!terrainFollowMode) {
					avatar.setLocalLocation(new Vector3f(0, 50, 0));
					System.out.println("Free flight mode enabled.");
				} else {	
					avatar.setLocalLocation(new Vector3f(3,0,-3));
					avatar.lookAt(new Vector3f(0,0,0));
					System.out.println("Terrain-following mode enabled.");
				}
				break;
			}
			case KeyEvent.VK_UP:
			{
				Vector3f right = avatar.getWorldRightVector();
				Matrix4f rotation = new Matrix4f().rotate(.01f, right.x(), right.y(), right.z());
				avatar.setLocalRotation(rotation.mul(avatar.getLocalRotation()));
				break;
			}
			case KeyEvent.VK_DOWN:
			{
				Vector3f right = avatar.getWorldRightVector();
				Matrix4f rotation = new Matrix4f().rotate(-.01f, right.x(), right.y(), right.z());
				avatar.setLocalRotation(rotation.mul(avatar.getLocalRotation()));
				break;
			}
			case KeyEvent.VK_SPACE:
			{
				System.out.println("Starting physics...");
				running = true;
				break;
			}
			case KeyEvent.VK_L:
			{
				headlightOn = !headlightOn;
				protClient.sendHeadlightState(headlightOn);
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

		im.associateActionWithAllKeyboards(Key.W, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.S, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.A, turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.D, turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// im.associateActionWithAllGamepads(Identifier.Button._1, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		// im.associateActionWithAllGamepads(Identifier.Axis.X, turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Button._2, toggleHealthBar, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostT; }
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
        avatar = new GameObject(GameObject.root(), tankS, tankT);
		avatar.setLocalLocation(new Vector3f(3,0,-3));
		avatar.setLocalScale(new Matrix4f().scaling(0.5f, 0.5f, 0.5f)); // Scale used for tiger2.obj

		avatar.lookAt(new Vector3f(0,0,0));

		Matrix4f correctedTransform = new Matrix4f();
		correctedTransform.identity();
		correctedTransform.mul(new Matrix4f().rotationZ((float)Math.toRadians(90.0f)));
		correctedTransform.setTranslation(avatar.getWorldLocation());
		
        double[] transform = toDoubleArray(avatar.getLocalTranslation().get(vals));
        PhysicsObject avatarPhys = physicsEngine.addCapsuleObject(physicsEngine.nextUID(), 0f, transform, 1f, 2f);
        avatar.setPhysicsObject(avatarPhys);

		headlightNode = new GameObject(avatar);
		headlightNode.setLocalTranslation(new Matrix4f().translation(0f, 0.3f, 0f));
    }

	private void updateAvatarHeight() {
		Vector3f loc = avatar.getWorldLocation();
		float height = terrain.getHeight(loc.x(), loc.z());
		Vector3f corrected = new Vector3f(loc.x(), height - 9f, loc.z());
		avatar.setLocalLocation(corrected);
		
		// Update physics position
		Matrix4f translation = avatar.getLocalTranslation();
		double[] transform = toDoubleArray(translation.get(vals));
		avatar.getPhysicsObject().setTransform(transform);
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

	
    private void updateAvatarPhysics() {
        Matrix4f translation = new Matrix4f().set(toFloatArray(avatar.getPhysicsObject().getTransform()));
        avatar.setLocalTranslation(translation);
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

		int numEachType = 5;
		
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
			sheildPhys = (engine.getSceneGraph().addPhysicsSphere(0, toDoubleArray(sheildObj.getLocalTranslation().get(new float[16])), 0.7f));	
			sheildObj.setPhysicsObject(sheildPhys);

			ShieldPowerUp shieldPowerUp = new ShieldPowerUp(this, sheildObj, sheildPhys, nextBoostID++, protClient);
			powerUps.add(shieldPowerUp);

		}
		initializedBoosts = false;
	}

	private void updateBoostStatus() {
		if (boosted && System.currentTimeMillis() >= boostEndTime) {
			boosted = false;
		}
	}

	public ArrayList<PowerUp> getPowerUps() {
		return powerUps;
	}

	public void increasePlayerHealth(float amount) {
		currentHealth += amount;
		if (currentHealth > maxHealth) {
			currentHealth = maxHealth;
		}
		if (protClient != null) {
			protClient.sendHealthUpdate(currentHealth);
		}
		System.out.println("Health increased! Current health: " + currentHealth);
	}

	public void activateShield() {
		shieldActive = true;
		shieldEndTime = System.currentTimeMillis() + 5000;
		System.out.println("Sheild activated!");
	}

	public boolean isShieldActive() {
		return shieldActive;
	}

	public void activateSpeedBoost() {
		boosted = true;
		boostEndTime = System.currentTimeMillis() + 5000;
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

}