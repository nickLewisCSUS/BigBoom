package tage;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.gl2.GLUT;
import org.joml.*;

/**
* Manages up to two HUD strings, implemented as GLUT strings.
* This class is instantiated automatically by the engine.
* Note that this class utilizes deprectated OpenGL functionality.
* <p>
* The available fonts are:
* <ul>
* <li> GLUT.BITMAP_8_BY_13
* <li> GLUT.BITMAP_9_BY_15
* <li> GLUT.BITMAP_TIMES_ROMAN_10
* <li> GLUT.BITMAP_TIMES_ROMAN_24
* <li> GLUT.BITMAP_HELVETICA_10
* <li> GLUT.BITMAP_HELVETICA_12
* <li> GLUT.BITMAP_HELVETICA_18
* </ul>
* @author Scott Gordon
*/

public class HUDmanager
{	private GLCanvas myCanvas;
	private GLUT glut = new GLUT();
	private Engine engine;

	private String HUD1string, HUD2string, HUD3string, HUD4string, HUD5string;
	private float[] HUD1color, HUD2color, HUD3color, HUD4color, HUD5color;
	private int HUD1font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD2font = GLUT.BITMAP_TIMES_ROMAN_24;	
	private int HUD3font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD4font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD5font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD1x, HUD1y, HUD2x, HUD2y, HUD3x, HUD3y, HUD4x, HUD4y, HUD5x, HUD5y;

	// The constructor is called by the engine, and should not be called by the game application.
	// It initializes the two HUDs to empty strings.

	protected HUDmanager(Engine e)
	{	engine = e;
		HUD1string = "";
		HUD2string = "";
		HUD3string = "";
		HUD4string = "";
		HUD5string = "";
		HUD1color = new float[3];
		HUD2color = new float[3];
		HUD3color = new float[3];
		HUD4color = new float[3];
		HUD5color = new float[3];
	}
	
	protected void setGLcanvas(GLCanvas g) { myCanvas = g; }

	protected void drawHUDs()
	{	GL4 gl4 = (GL4) GLContext.getCurrentGL();
		GL4bc gl4bc = (GL4bc) gl4;

		gl4.glUseProgram(0);

		gl4bc.glColor3f(HUD1color[0], HUD1color[1], HUD1color[2]);
		gl4bc.glWindowPos2d (HUD1x, HUD1y);
		glut.glutBitmapString(HUD1font, HUD1string);

		gl4bc.glColor3f(HUD2color[0], HUD2color[1], HUD2color[2]);
		gl4bc.glWindowPos2d (HUD2x, HUD2y);
		glut.glutBitmapString (HUD2font, HUD2string);
		
		gl4bc.glColor3f(HUD3color[0], HUD3color[1], HUD3color[2]);
		gl4bc.glWindowPos2d (HUD3x, HUD3y);
		glut.glutBitmapString (HUD3font, HUD3string);
		
		gl4bc.glColor3f(HUD4color[0], HUD4color[1], HUD4color[2]);
		gl4bc.glWindowPos2d (HUD4x, HUD4y);
		glut.glutBitmapString (HUD4font, HUD4string);
		
		gl4bc.glColor3f(HUD5color[0], HUD5color[1], HUD5color[2]);
		gl4bc.glWindowPos2d (HUD5x, HUD5y);
		glut.glutBitmapString (HUD5font, HUD5string);
	}

	/** sets HUD #1 to the specified text string, color, and location */
	public void setHUD1(String string, Vector3f color, int x, int y)
	{	HUD1string = string;
		HUD1color[0]=color.x(); HUD1color[1]=color.y(); HUD1color[2]=color.z();
		HUD1x = x;
		HUD1y = y;
	}

	/** sets HUD #2 to the specified text string, color, and location */
	public void setHUD2(String string, Vector3f color, int x, int y)
	{	HUD2string = string;
		HUD2color[0]=color.x(); HUD2color[1]=color.y(); HUD2color[2]=color.z();
		HUD2x = x;
		HUD2y = y;
	}
	
	/** sets HUD #3 to the specified text string, color, and location */
	public void setHUD3(String string, Vector3f color, int x, int y)
	{	HUD3string = string;
		HUD3color[0]=color.x(); HUD3color[1]=color.y(); HUD3color[2]=color.z();
		HUD3x = x;
		HUD3y = y;
	}

	/** sets HUD #3 to the specified text string, color, and location */
	public void setHUD4(String string, Vector3f color, int x, int y)
	{	HUD4string = string;
		HUD4color[0]=color.x(); HUD4color[1]=color.y(); HUD4color[2]=color.z();
		HUD4x = x;
		HUD4y = y;
	}
	
		/** sets HUD #3 to the specified text string, color, and location */
	public void setHUD5(String string, Vector3f color, int x, int y)
	{	HUD5string = string;
		HUD5color[0]=color.x(); HUD5color[1]=color.y(); HUD5color[2]=color.z();
		HUD5x = x;
		HUD5y = y;
	}

	/** sets HUD #1 font - available fonts are listed above. */
	public void setHUD1font(int font) { HUD1font = font; }

	/** sets HUD #2 font - available fonts are listed above. */
	public void setHUD2font(int font) { HUD2font = font; }
	
	/** sets HUD #3 font - available fonts are listed above. */
	public void setHUD3font(int font) { HUD3font = font; }
	
	/** sets HUD #4 font - available fonts are listed above. */
	public void setHUD4font(int font) { HUD4font = font; }
	
	/** sets HUD #5 font - available fonts are listed above. */
	public void setHUD5font(int font) { HUD5font = font; }
}