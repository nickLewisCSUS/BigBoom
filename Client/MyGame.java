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

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;

	private int counter=0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime, amt;

	private GameObject tor, avatar, x, y, z, playerHealthBar;
	private ObjShape torS, ghostS, dolS, linxS, linyS, linzS, playerHealthBarS;
	private TextureImage doltx, ghostT, playerHealthBarT;
	private Light light;


	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

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
	{	torS = new Torus(0.5f, 0.2f, 48);
		ghostS = new Sphere();
		dolS = new ImportedModel("dolphinHighPoly.obj");
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		playerHealthBarS = new Cube();
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.png");
		ghostT = new TextureImage("redDolphin.jpg");
		playerHealthBarT = new TextureImage("red.png");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialRotation, initialScale;

		// build dolphin avatar
		avatar = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(-1f,0f,1f);
		avatar.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(135.0f));
		avatar.setLocalRotation(initialRotation);

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

		playerHealthBar = new GameObject(avatar, playerHealthBarS, playerHealthBarT);
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
		TurnAction turnAction = new TurnAction(this, protClient);
		ToggleHealthBarAction toggleHealthBar = new ToggleHealthBarAction();

		// attach the action objects to keyboard and gamepad components
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._1,
			fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.X,
			turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._2,
			toggleHealthBar, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

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

		// Update health bar position and scale
		playerHealthBar.setLocalTranslation(new Matrix4f().translation(0f, 0.4f, 0f));
		float healthRatio = currentHealth / maxHealth;
		float baseLength = 0.25f;
		playerHealthBar.setLocalScale(new Matrix4f().scaling(baseLength * healthRatio, 0.001f, 0.001f));
		playerHealthBar.getRenderStates().setColor(new Vector3f(1f, 0f, 0f));

		// Update input and networking
		im.update((float)elapsedTime);
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
	{	switch (e.getKeyCode())
		{	case KeyEvent.VK_W:
			{	Vector3f oldPosition = avatar.getWorldLocation();
				Vector4f fwdDirection = new Vector4f(0f,0f,1f,1f);
				fwdDirection.mul(avatar.getWorldRotation());
				fwdDirection.mul(0.05f);
				Vector3f newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
				avatar.setLocalLocation(newPosition);
				protClient.sendMoveMessage(avatar.getWorldLocation(), avatar.getWorldRotation());
				break;
			}
			case KeyEvent.VK_D:
			{	Matrix4f oldRotation = new Matrix4f(avatar.getWorldRotation());
				Vector4f oldUp = new Vector4f(0f,1f,0f,1f).mul(oldRotation);
				Matrix4f rotAroundAvatarUp = new Matrix4f().rotation(-.01f, new Vector3f(oldUp.x(), oldUp.y(), oldUp.z()));
				Matrix4f newRotation = oldRotation;
				newRotation.mul(rotAroundAvatarUp);
				avatar.setLocalRotation(newRotation);

				// Send updated rotation to other clients!
				protClient.sendMoveMessage(avatar.getWorldLocation(), avatar.getWorldRotation());

				break;
			}

			case KeyEvent.VK_H:
			{
				showHealthBar = !showHealthBar;
				break;
			}
			case KeyEvent.VK_K: // test damage with 'K' key
				currentHealth -= 10;
				if (currentHealth < 0) currentHealth = 0;
				protClient.sendHealthUpdate(currentHealth);
				break;
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
}