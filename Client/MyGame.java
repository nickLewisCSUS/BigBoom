package Client;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

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
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.physics.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;

	private int counter=0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime, amt;

	private GameObject tor, avatar, x, y, z, terrain, maze, speedBoost;
	private ObjShape torS, ghostS, dolS, linxS, linyS, linzS, terrainShape, mazeShape, speedBoostShape;
	private TextureImage doltx, ghostT, terrainHeightMap, terrainTexture, mazeHeightMap, mazeTexture, speedBoostTexture;
	private Light light;

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
	{	torS = new Torus(0.5f, 0.2f, 48);
		ghostS = new Sphere();
		dolS = new ImportedModel("dolphinHighPoly.obj");
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		terrainShape = new TerrainPlane(1024);
		mazeShape = new TerrainPlane(1024);
		speedBoostShape = new ImportedModel("speedboost.obj");
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.png");
		ghostT = new TextureImage("redDolphin.jpg");
		terrainHeightMap = new TextureImage("terrain_height.png");
		terrainTexture = new TextureImage("terrain_texture1.png");
		mazeHeightMap = new TextureImage("maze.png");
		mazeTexture = new TextureImage("metal.jpg");
		speedBoostTexture = new TextureImage("speedBoostTx.png");
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
		speedBoost = new GameObject(GameObject.root(), speedBoostShape, speedBoostTexture);
		initialTranslation = (new Matrix4f()).translation(0f,0f,-1f);
		speedBoost.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(135.0f));
		speedBoost.setLocalRotation(initialRotation);
		initialScale = (new Matrix4f()).scaling(0.25f);
		speedBoost.setLocalScale(initialScale);

		// build torus along X axis
		tor = new GameObject(GameObject.root(), torS);
		initialTranslation = (new Matrix4f()).translation(1,0,0);
		tor.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(0.25f);
		tor.setLocalScale(initialScale);

		// add X,Y,-Z axes
		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f,0f,0f));
		(y.getRenderStates()).setColor(new Vector3f(0f,1f,0f));
		(z.getRenderStates()).setColor(new Vector3f(0f,0f,1f));
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(.5f, .5f, .5f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);
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

		// build some action objects for doing things in response to user input
		FwdAction fwdAction = new FwdAction(this, protClient);
		TurnAction turnAction = new TurnAction(this);

		// attach the action objects to keyboard and gamepad components
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._1,
			fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.X,
			turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		setupNetworking();
	}

	public GameObject getAvatar() { return avatar; }

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

		// update inputs and camera
		im.update((float)elapsedTime);
		positionCameraBehindAvatar();
        physicsEngine.update(0.016f); // 60Hz step
		if (terrainFollowMode) {
		updateAvatarHeight();
        updateAvatarPhysics();
		}

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
				protClient.sendMoveMessage(avatar.getWorldLocation());
			}
			moveDirection = MovementDirection.NONE;
			nextPosition = null;
		}
		processNetworking((float)elapsedTime);
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
	{	Vector3f oldPos = avatar.getWorldLocation();
		Matrix4f oldRotation = avatar.getWorldRotation();
		
		switch (e.getKeyCode())
		{	case KeyEvent.VK_W:
			{	Vector4f fwdDirection = new Vector4f(0f, 0f, 1f, 1f).mul(oldRotation);
				fwdDirection.mul(0.05f);
				nextPosition = oldPos.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
				moveDirection = MovementDirection.FORWARD;
				break;
			}
			case KeyEvent.VK_S:
			{	Vector4f fwdDirection = new Vector4f(0f,0f,1f,1f).mul(oldRotation);
				fwdDirection.mul(-0.05f);
				nextPosition = oldPos.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
				moveDirection = MovementDirection.BACKWARD;
				break;
			}
			case KeyEvent.VK_D:
			{	Vector4f oldUp = new Vector4f(0f,1f,0f,1f).mul(oldRotation);
				Matrix4f rotAroundAvatarUp = new Matrix4f().rotation(-.01f, new Vector3f(oldUp.x(), oldUp.y(), oldUp.z()));
				Matrix4f newRotation = oldRotation;
				newRotation.mul(rotAroundAvatarUp);
				avatar.setLocalRotation(newRotation);
				break;
			}
			case KeyEvent.VK_A:
			{	Vector4f oldUp = new Vector4f(0f,1f,0f,1f).mul(oldRotation);
				Matrix4f rotAroundAvatarUp = new Matrix4f().rotation(.01f, new Vector3f(oldUp.x(), oldUp.y(), oldUp.z()));
				Matrix4f newRotation = oldRotation;
				newRotation.mul(rotAroundAvatarUp);
				avatar.setLocalRotation(newRotation);
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
			case KeyEvent.VK_F:
			{	
				terrainFollowMode = !terrainFollowMode;
				if (!terrainFollowMode) {
					avatar.setLocalLocation(new Vector3f(0, 50, 0));
					avatar.lookAt(new Vector3f(0,0,0));
					System.out.println("Free flight mode enabled.");
				} else {
					System.out.println("Terrain-following mode enabled.");
				}
			}
		}
		super.keyPressed(e);
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
        terrain = new GameObject(GameObject.root(), terrainShape, terrainTexture);
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
        maze = new GameObject(GameObject.root(), mazeShape, mazeTexture);
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
        avatar.setLocalLocation(new Vector3f(-1, 0, 0));
        avatar.setLocalRotation(new Matrix4f().rotationY((float)Math.toRadians(135)));
		
        double[] transform = toDoubleArray(avatar.getLocalTranslation().get(vals));
        PhysicsObject avatarPhys = physicsEngine.addCapsuleObject(physicsEngine.nextUID(), 1f, transform, 1f, 5f);
        avatar.setPhysicsObject(avatarPhys);
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