package Client;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;
import tage.audio.*;

import java.lang.Math;
import java.awt.*;

import java.awt.event.*;

import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;

import java.net.UnknownHostException;

import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.physics.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;

	private int counter=0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime, amt;


	private GameObject avatar, x, y, z, playerHealthBar, shield, terrain, maze, speedBoost, mine;
	private ObjShape ghostS, dolS, linxS, linyS, linzS, playerHealthBarS, shieldS, terrainS, mazeS, speedBoostS, mineS;
	private TextureImage doltx, ghostT, playerHealthBarT, shieldT, terrainHeightMap, terrainT, mazeHeightMap, mazeT, speedBoostT, mineT;
	private Light light;

	private IAudioManager audioMgr;
	private Sound mineSound;


	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	private int battleField;
	private PhysicsEngine physicsEngine;
	private Vector3f lastValidPosition;
	public enum MovementDirection { NONE, FORWARD, BACKWARD }
	private MovementDirection moveDirection = MovementDirection.NONE;
	private Vector3f nextPosition = null;
	private boolean terrainFollowMode = true;
    private float[] vals = new float[16];
	private boolean showHealthBar = true;
	private float currentHealth = 100.0f;
	private float maxHealth = 100f;


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
	{	ghostS = new Sphere();
		dolS = new ImportedModel("tigerTank.obj");
		shieldS = new ImportedModel("sheildmodel.obj");
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		terrainS = new TerrainPlane(1024);
		mazeS = new TerrainPlane(1024);
		speedBoostS = new ImportedModel("speedboost.obj");
		playerHealthBarS = new Cube();
		mineS = new ImportedModel("mine.obj");
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.png");
		shieldT = new TextureImage("sheild.jpg");
		ghostT = new TextureImage("redDolphin.jpg");
		terrainHeightMap = new TextureImage("terrain_height.png");
		terrainT = new TextureImage("terrain_texture1.png");
		mazeHeightMap = new TextureImage("maze.png");
		mazeT = new TextureImage("metal.jpg");
		speedBoostT = new TextureImage("speedBoostTx.png");
		playerHealthBarT = new TextureImage("red.png");
		mineT = new TextureImage("mineTexture.jpg");
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

		// build shield upgrade object
		shield = new GameObject(GameObject.root(), shieldS, shieldT);
		initialTranslation = (new Matrix4f()).translation(1f,0f,1f);
		shield.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(135.0f));
		shield.setLocalRotation(initialRotation);
		initialScale = (new Matrix4f()).scaling(0.1f, 0.1f, 0.1f);
		shield.setLocalScale(initialScale);

		// build mine object
		mine = new GameObject(GameObject.root(), mineS, mineT);
		initialTranslation = (new Matrix4f()).translation(2f,0f,2f);
		mine.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(0.03f, 0.1f, 0.03f);
		mine.setLocalScale(initialScale); 
		
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
	{	Light.setGlobalAmbient(.5f, .5f, .5f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);
	}

	public void loadSounds()
	{ 
		AudioResource resource1;
		audioMgr = engine.getAudioManager();
		resource1 = audioMgr.createAudioResource("mineBeeping.wav", AudioResourceType.AUDIO_SAMPLE);

		mineSound = new Sound(resource1, SoundType.SOUND_EFFECT, 100, true);
		mineSound.initialize(audioMgr);

		
		mineSound.setMaxDistance(50.0f);
		mineSound.setMinDistance(2.0f);
		mineSound.setRollOff(2.0f);
	}

	@Override
	public void initializeGame()
	{	prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		// ----------------- initialize camera ----------------
		positionCameraBehindAvatar();

		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();

		setupNetworking();
		setupInputActions(); 

		// initial sound settings
		mineSound.setLocation(mine.getWorldLocation());
		setEarParameters();
		mineSound.play();
		
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

		if (showHealthBar) {
			playerHealthBar.setLocalTranslation(new Matrix4f().translation(0f, 0.45f, 0f));
			float healthRatio = currentHealth / maxHealth;
			float baseLength = 0.25f;
			playerHealthBar.setLocalScale(new Matrix4f().scaling(baseLength * healthRatio, 0.0005f, 0.001f));
		} else {
			playerHealthBar.setLocalScale(new Matrix4f().scaling(0f)); // Hide it safely
		}
	
		im.update((float)elapsedTime);
		processNetworking((float)elapsedTime);;

		// Inputs and networking
		im.update((float)elapsedTime);
		processNetworking((float)elapsedTime);

		// update inputs and camera
		im.update((float)elapsedTime);
		positionCameraBehindAvatar();
        physicsEngine.update(0.016f); // 60Hz step
		if (terrainFollowMode) {
			updateAvatarHeight();
			updateAvatarPhysics();
		}

		// update sound
		setEarParameters();
		mineSound.setLocation(mine.getWorldLocation());
		

		Vector3f earLoc = engine.getAudioManager().getEar().getLocation();
		Vector3f mineLoc = mine.getWorldLocation();
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

		mineSound.setVolume(volume);

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

	}

	private void positionCameraBehindAvatar()
	{	Vector4f u = new Vector4f(-1f,0f,0f,1f);
		Vector4f v = new Vector4f(0f,1f,0f,1f);
		Vector4f n = new Vector4f(0f,0f,1f,1f);
		u.mul(avatar.getWorldRotation());
		v.mul(avatar.getWorldRotation());
		n.mul(avatar.getWorldRotation());
		Matrix4f w = avatar.getWorldTranslation();
		Vector3f position = new Vector3f(w.m30(), w.m31(), w.m32());
		position.add(-n.x()*2f, -n.y()*2f, -n.z()*2f);
		position.add(v.x()*.75f, v.y()*.75f, v.z()*.75f);
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		c.setLocation(position);
		c.setU(new Vector3f(u.x(),u.y(),u.z()));
		c.setV(new Vector3f(v.x(),v.y(),v.z()));
		c.setN(new Vector3f(n.x(),n.y(),n.z()));
	}


	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{
			case KeyEvent.VK_H:
			{
				showHealthBar = !showHealthBar;
				break;
			}
			case KeyEvent.VK_K: // test damage with 'K' key
			{	
				currentHealth -= 10;
				if (currentHealth < 0) currentHealth = 0;
				protClient.sendHealthUpdate(currentHealth);
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

		im.associateActionWithAllGamepads(Identifier.Button._1, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.X, turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
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
        avatar = new GameObject(GameObject.root(), dolS, doltx);
		avatar.setLocalLocation(new Vector3f(3,0,-3));
		avatar.lookAt(new Vector3f(0,0,0));
		
        double[] transform = toDoubleArray(avatar.getLocalTranslation().get(vals));
        PhysicsObject avatarPhys = physicsEngine.addCapsuleObject(physicsEngine.nextUID(), 1f, transform, 1f, 5f);
        avatar.setPhysicsObject(avatarPhys);
    }

	private void updateAvatarHeight() {
		Vector3f loc = avatar.getWorldLocation();
		float height = terrain.getHeight(loc.x(), loc.z());
		Vector3f corrected = new Vector3f(loc.x(), height - 10f, loc.z());
		avatar.setLocalLocation(corrected);
		
		// Update physics position
		Matrix4f translation = avatar.getLocalTranslation();
		double[] transform = toDoubleArray(translation.get(vals));
		avatar.getPhysicsObject().setTransform(transform);
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

}