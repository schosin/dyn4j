package org.dyn4j.sandbox;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ToolTipManager;

import org.dyn4j.Version;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.MouseJoint;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.sandbox.actions.MoveBodyAction;
import org.dyn4j.sandbox.actions.MoveWorldAction;
import org.dyn4j.sandbox.actions.RotateBodyAction;
import org.dyn4j.sandbox.actions.SelectBodyAction;
import org.dyn4j.sandbox.dialogs.AboutDialog;
import org.dyn4j.sandbox.events.BodyActionEvent;
import org.dyn4j.sandbox.input.Keyboard;
import org.dyn4j.sandbox.input.Mouse;
import org.dyn4j.sandbox.panels.WorldTreePanel;
import org.dyn4j.sandbox.utilities.Fps;
import org.dyn4j.sandbox.utilities.Icons;
import org.dyn4j.sandbox.utilities.RenderState;
import org.dyn4j.sandbox.utilities.RenderUtilities;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * Main class for the Sandbox application.
 * @author William Bittle
 * @version 1.0.0
 * @since 1.0.0
 */
public class Sandbox extends JFrame implements GLEventListener, ActionListener {
	/** The version id */
	private static final long serialVersionUID = -7050279589455803564L;

	/** The conversion factor from nano to base */
	private static final double NANO_TO_BASE = 1.0e9;
	
	/** The color for selected bodies/fixtures */
	private static final float[] SELECTED_COLOR = new float[] {0.5f, 0.5f, 1.0f, 1.0f};
	
	/** The canvas to draw to */
	private GLCanvas canvas;
	
	/** The canvas size */
	private Dimension canvasSize;
	
	/** The OpenGL animator */
	private Animator animator;
	
	/** The glut interface */
	private GLUT glut;
	
	/** The dynamics engine */
	private World world;
	
	/** The time stamp for the last iteration */
	private long last;
	
	/** The paused flag */
	private boolean paused = true;
	
	/** The scale factor */
	private double scale = 32.0;
	
	/** The offset from the origin in world coordinates */
	private Vector2 offset = new Vector2();
	
	/** The keyboard to accept and store key events */
	private Keyboard keyboard;
	
	/** The mouse to accept and store mouse events */
	private Mouse mouse;
	
	/** The frames per second monitor */
	private Fps fps;
	
	/** The rendering state */
	private RenderState renderState;
	
	// controls
	
	/** The world tree control */
	private WorldTreePanel pnlWorld;
	
	// Start/Stop/FPS toolbar
	
	/** The start button for the simulation */
	private JButton btnStart;
	
	/** The stop button for the simulation */
	private JButton btnStop;
	
	/** Label to show the frames per second */
	private JTextField lblFps;
	
	// Rendering toolbar
	
	/** The random color toggle button */
	private JToggleButton tglRandomColor;
	
	/** The stencil toggle button */
	private JToggleButton tglStencil;
	
	/** The show labels toggle button */
	private JToggleButton tglLabels;
	
	/** The anti-aliasing toggle button */
	private JToggleButton tglAntiAliasing;
	
	/** The vertical sync toggle button */
	private JToggleButton tglVerticalSync;
	
	/** The label origin toggle button */
	private JToggleButton tglOriginLabel;
	
	/** The zoom in button */
	private JButton btnZoomIn;
	
	/** The zoom out button */
	private JButton btnZoomOut;
	
	/** The move camera to origin button */
	private JButton btnToOrigin;
	
	// Mouse location toolbar
	
	/** The mouse location label */
	private JTextField lblMouseLocation;
	
	// Actions performed on the OpenGL canvas
	
	/** The select body action */
	private SelectBodyAction selectBodyAction = new SelectBodyAction();
	
	/** The move body action */
	private MoveBodyAction moveBodyAction = new MoveBodyAction();
	
	/** The joint used to move bodies when the simulation is running */
	private MouseJoint selectedBodyJoint;
	
	/** The rotate body action */
	private RotateBodyAction rotateBodyAction = new RotateBodyAction();
	
	/** The move world action */
	private MoveWorldAction moveWorldAction = new MoveWorldAction();
	
	//private EditBodyAction editBodyAction;
	//private RemoveBodyAction removeBodyAction;
	//private SelectFixtureAction selectFixtureAction;
	//private MoveFixtureAction moveFixtureAction;
	//private RotateFixtureAction rotateFixtureAction;
	//private RemoveFixtureAction removeFixtureAction;
	
	/**
	 * Default constructor.
	 */
	public Sandbox() {
		super("Sandbox - dyn4j v" + Version.getVersion());
		
		// create the world
		this.world = new World();
		
		// create the keyboard and mouse
		this.keyboard = new Keyboard();
		this.mouse = new Mouse();
		this.fps = new Fps();
		this.renderState = new RenderState();
		
		// create the world tree
		Dimension size = new Dimension(200, 600);
		this.pnlWorld = new WorldTreePanel(this, this.world);
		this.pnlWorld.setPreferredSize(size);
		this.pnlWorld.setMinimumSize(size);
		this.pnlWorld.addActionListener(this);
		
		// create the start/stop/fps tool bar
		
		JToolBar barStartStop = new JToolBar("Simulation", JToolBar.HORIZONTAL);
		barStartStop.setRollover(true);
		
		this.btnStart = new JButton(Icons.START);
		this.btnStart.addActionListener(this);
		this.btnStart.setActionCommand("start");
		this.btnStart.setToolTipText("Start Simulation");
		
		this.btnStop = new JButton(Icons.STOP);
		this.btnStop.addActionListener(this);
		this.btnStop.setActionCommand("stop");
		this.btnStop.setToolTipText("Stop Simulation");
		
		this.btnStart.setEnabled(true);
		this.btnStop.setEnabled(false);
		
		barStartStop.add(this.btnStart);
		barStartStop.add(this.btnStop);
		
		this.lblFps = new JTextField();
		this.lblFps.setMaximumSize(new Dimension(70, Short.MAX_VALUE));
		this.lblFps.setHorizontalAlignment(JTextField.RIGHT);
		this.lblFps.setColumns(7);
		this.lblFps.setEditable(false);
		this.lblFps.setToolTipText("Mouse Location (World Coordinates)");
		this.lblFps.setToolTipText("Frames / Second");
		
		barStartStop.addSeparator();
		barStartStop.add(this.lblFps);
		
		// create the settings toolbar
		
		JToolBar barSettings = new JToolBar("Settings", JToolBar.HORIZONTAL);
		barSettings.setRollover(true);
		
		this.tglRandomColor = new JToggleButton(Icons.COLOR);
		this.tglRandomColor.setToolTipText("Enable/Disable Random Body Colors");
		this.tglRandomColor.setActionCommand("color");
		this.tglRandomColor.addActionListener(this);
		this.tglRandomColor.setSelected(ApplicationSettings.isColorRandom());
		
		this.tglStencil = new JToggleButton(Icons.STENCIL);
		this.tglStencil.setToolTipText("Enable/Disable Body Stenciling");
		this.tglStencil.setActionCommand("stencil");
		this.tglStencil.addActionListener(this);
		this.tglStencil.setSelected(ApplicationSettings.isStenciled());
		
		this.tglLabels = new JToggleButton(Icons.LABEL);
		this.tglLabels.setToolTipText("Enable/Disable Body Labels");
		this.tglLabels.setActionCommand("label");
		this.tglLabels.addActionListener(this);
		this.tglLabels.setSelected(ApplicationSettings.isLabeled());
		
		this.tglAntiAliasing = new JToggleButton(Icons.AA);
		this.tglAntiAliasing.setToolTipText("Enable/Disable Anti-Aliasing");
		this.tglAntiAliasing.setActionCommand("aa");
		this.tglAntiAliasing.addActionListener(this);
		this.tglAntiAliasing.setSelected(ApplicationSettings.isAntiAliasingEnabled());
		
		this.tglVerticalSync = new JToggleButton(Icons.SYNC);
		this.tglVerticalSync.setToolTipText("Enable/Disable Vertical Sync");
		this.tglVerticalSync.setActionCommand("vertical-sync");
		this.tglVerticalSync.addActionListener(this);
		this.tglVerticalSync.setSelected(ApplicationSettings.isVerticalSyncEnabled());
		
		this.tglOriginLabel = new JToggleButton(Icons.ORIGIN);
		this.tglOriginLabel.setToolTipText("Enable/Disable Origin Label");
		this.tglOriginLabel.setActionCommand("origin");
		this.tglOriginLabel.addActionListener(this);
		this.tglOriginLabel.setSelected(ApplicationSettings.isOriginLabeled());
		
		this.btnZoomIn = new JButton(Icons.ZOOM_IN);
		this.btnZoomIn.setToolTipText("Zoom In");
		this.btnZoomIn.setActionCommand("zoom-in");
		this.btnZoomIn.addActionListener(this);
		
		this.btnZoomOut = new JButton(Icons.ZOOM_OUT);
		this.btnZoomOut.setToolTipText("Zoom Out");
		this.btnZoomOut.setActionCommand("zoom-out");
		this.btnZoomOut.addActionListener(this);
		
		this.btnToOrigin = new JButton(Icons.TO_ORIGIN);
		this.btnToOrigin.setToolTipText("Center the camera on the origin.");
		this.btnToOrigin.setActionCommand("to-origin");
		this.btnToOrigin.addActionListener(this);
		
		barSettings.add(this.tglRandomColor);
		barSettings.add(this.tglStencil);
		barSettings.add(this.tglLabels);
		barSettings.add(this.tglAntiAliasing);
		barSettings.add(this.tglVerticalSync);
		barSettings.add(this.tglOriginLabel);
		barSettings.add(this.btnZoomIn);
		barSettings.add(this.btnZoomOut);
		barSettings.add(this.btnToOrigin);
		
		// create the mouse location toolbar
		
		JToolBar barMouseLocation = new JToolBar("Mouse Location", JToolBar.HORIZONTAL);
		barMouseLocation.setFloatable(true);
		
		this.lblMouseLocation = new JTextField();
		this.lblMouseLocation.setHorizontalAlignment(JTextField.RIGHT);
		this.lblMouseLocation.setColumns(10);
		this.lblMouseLocation.setEditable(false);
		this.lblMouseLocation.setToolTipText("Mouse Location (World Coordinates)");
		
		barMouseLocation.add(this.lblMouseLocation);
		
		// help toolbar
		
		JToolBar barAbout = new JToolBar("About", JToolBar.HORIZONTAL);
		barAbout.setFloatable(true);
		
		JButton btnAbout = new JButton();
		btnAbout.setIcon(Icons.ABOUT);
		btnAbout.setToolTipText("About Sandbox");
		btnAbout.setActionCommand("about");
		btnAbout.addActionListener(this);
		
		barAbout.add(btnAbout);
		
		// add the toolbars to the layout
		
		JPanel pnlToolBar = new JPanel();
		pnlToolBar.setLayout(new BoxLayout(pnlToolBar, BoxLayout.X_AXIS));
		pnlToolBar.add(barStartStop);
		pnlToolBar.add(barSettings);
		pnlToolBar.add(Box.createHorizontalGlue());
		pnlToolBar.add(barMouseLocation);
		pnlToolBar.add(barAbout);
		
		// make sure tooltips show up on top of the heavy weight canvas
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		
		// attempt to set the icon
		this.setIconImage(Icons.SANDBOX_48.getImage());
		
		// setup OpenGL capabilities
		if (!GLProfile.isGL2Available()) {
			// TODO not good
		}
		GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		caps.setDoubleBuffered(true);
		// setup the stencil buffer to outline shapes
		caps.setStencilBits(1);
		// setting multisampling allows for better looking body outlines
		caps.setSampleBuffers(true);
		caps.setNumSamples(2);
		caps.setHardwareAccelerated(true);
		
		this.glut = new GLUT();
		
		this.canvasSize = new Dimension(600, 600);
		// create a canvas to paint to 
		this.canvas = new GLCanvas(caps);
		this.canvas.setPreferredSize(this.canvasSize);
		this.canvas.setMinimumSize(this.canvasSize);
		this.canvas.setIgnoreRepaint(true);
		// add this class as the gl event listener
		this.canvas.addGLEventListener(this);
		
		// add the mouse and keyboard listeners
		this.canvas.addKeyListener(this.keyboard);
		this.canvas.addMouseListener(this.mouse);
		this.canvas.addMouseMotionListener(this.mouse);
		this.canvas.addMouseWheelListener(this.mouse);
		
		// placing the GLCanvas in a JPanel allows the JSplitPane
		// to not cause an exception when the user moves the split
		JPanel pnlTest = new JPanel();
		pnlTest.setLayout(new BorderLayout());
		pnlTest.add(this.canvas);
		
		// add a split pane
		JSplitPane pneSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.pnlWorld, pnlTest);
		
		// setup the layout
		Container container = this.getContentPane();
		
		GroupLayout layout = new GroupLayout(container);
		container.setLayout(layout);
		
		layout.setAutoCreateGaps(true);
		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(pnlToolBar, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(pneSplit));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(pnlToolBar)
				.addComponent(pneSplit));
		
		// size everything
		this.pack();
		
		// move from (0, 0) since this hides some of the window frame
		this.setLocation(10, 10);
		
		// show the window
		this.setVisible(true);
		
		// setting this property will call the dispose methods on the GLCanvas
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// initialize the last update time
		this.last = System.nanoTime();
		
		// create an animator to animated the canvas
		this.animator = new Animator(this.canvas);
		this.animator.setRunAsFastAsPossible(false);
		this.animator.start();
	}
	
	/**
	 * Start active rendering the example.
	 */
	public void start() {
		// start the animator
		this.animator.start();
	}
	
	/**
	 * Stops the animator thread from running.
	 */
	public void stop() {
		this.animator.stop();
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();
		// World tree panel events
		if ("clear-all".equals(command)) {
			// clear the current selection
			this.selectBodyAction.end();
		} else if ("remove-body".equals(command)) {
			// check if it was this body
			BodyActionEvent bae = (BodyActionEvent)event;
			SandboxBody body = bae.getBody();
			if (body == this.selectBodyAction.getBody()) {
				this.selectBodyAction.end();
			}
		}
		// Sandbox events
		if ("start".equals(command)) {
			if (isPaused()) {
				// disable the world editor
				this.pnlWorld.setEnabled(false);
				this.btnStart.setEnabled(false);
				this.btnStop.setEnabled(true);
				// end the select body action
				this.selectBodyAction.end();
				setPaused(false);
			}
		} else if ("stop".equals(command)) {
			if (!isPaused()) {
				// enable the world editor
				this.pnlWorld.setEnabled(true);
				this.btnStart.setEnabled(true);
				this.btnStop.setEnabled(false);
				setPaused(true);
			}
		} else if ("color".equals(command)) {
			ApplicationSettings.setColorRandom(!ApplicationSettings.isColorRandom());
		} else if ("stencil".equals(command)) {
			ApplicationSettings.setStenciled(!ApplicationSettings.isStenciled());
		} else if ("label".equals(command)) {
			ApplicationSettings.setLabeled(!ApplicationSettings.isLabeled());
		} else if ("aa".equals(command)) {
			ApplicationSettings.setAntiAliasingEnabled(!ApplicationSettings.isAntiAliasingEnabled());
		} else if ("vertical-sync".equals(command)) {
			ApplicationSettings.setVerticalSyncEnabled(!ApplicationSettings.isVerticalSyncEnabled());
		} else if ("origin".equals(command)) {
			ApplicationSettings.setOriginLabeled(!ApplicationSettings.isOriginLabeled());
		} else if ("zoom-in".equals(command)) {
			this.scale *= 0.5;
		} else if ("zoom-out".equals(command)) {
			this.scale *= 2.0;
		} else if ("about".equals(command)) {
			AboutDialog.show(this);
		} else if ("to-origin".equals(command)) {
			this.offset.zero();
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.media.opengl.GLEventListener#init(javax.media.opengl.GLAutoDrawable)
	 */
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// get the OpenGL context
		GL2 gl = glDrawable.getGL().getGL2();
		
		int[] temp = new int[1];
		gl.glGetIntegerv(GL.GL_STENCIL_BITS, temp, 0);
		if (temp[0] <= 0) {
			// disable the stencil button
			this.tglStencil.setEnabled(false);
		}
		
		// set the matrix mode to projection
		gl.glMatrixMode(GL2.GL_PROJECTION);
		// initialize the matrix
		gl.glLoadIdentity();
		// set the view to a 2D view
		gl.glOrtho(-300, 300, -300, 300, 0, 1);
		
		// switch to the model view matrix
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		// initialize the matrix
		gl.glLoadIdentity();
		
		// set the clear color to white
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		// set the stencil clear value
		gl.glClearStencil(0);
		
		// disable depth testing since we are working in 2D
		gl.glDisable(GL.GL_DEPTH_TEST);
		// we dont need lighting either
		gl.glDisable(GL2.GL_LIGHTING);
		
		// enable blending for translucency
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				
		// set the swap interval to vertical-sync
		gl.setSwapInterval(1);
	}
	
	/* (non-Javadoc)
	 * @see javax.media.opengl.GLEventListener#display(javax.media.opengl.GLAutoDrawable)
	 */
	@Override
	public void display(GLAutoDrawable glDrawable) {
		// get the OpenGL context
		GL2 gl = glDrawable.getGL().getGL2();

		// turn on/off multi-sampling
		if (ApplicationSettings.isAntiAliasingEnabled()) {
			gl.glEnable(GL.GL_MULTISAMPLE);
		} else {
			gl.glDisable(GL.GL_MULTISAMPLE);
		}
		
		// turn on/off vertical sync
		if (ApplicationSettings.isVerticalSyncEnabled()) {
			gl.setSwapInterval(1);
		} else {
			gl.setSwapInterval(0);
		}
		
		// clear the screen
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);
		// switch to the model view matrix
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		// initialize the matrix (0,0) is in the center of the window
		gl.glLoadIdentity();
		
		// the main loop
		{
			// perform other operations at the end (it really
			// doesn't matter if its done at the start or end)
			this.update();
			
			// render the scene
			this.render(gl);
			
			// look for input
			this.poll();
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.media.opengl.GLEventListener#dispose(javax.media.opengl.GLAutoDrawable)
	 */
	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		// nothing to dispose from OpenGL right now
	}
	
	/* (non-Javadoc)
	 * @see javax.media.opengl.GLEventListener#reshape(javax.media.opengl.GLAutoDrawable, int, int, int, int)
	 */
	@Override
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
		// get the OpenGL context
		GL2 gl = glDrawable.getGL().getGL2();
		
		// resize the ortho view
		
		// set the matrix mode to projection
		gl.glMatrixMode(GL2.GL_PROJECTION);
		// initialize the matrix
		gl.glLoadIdentity();
		// set the view to a 2D view
		gl.glOrtho(-width / 2.0, width / 2.0, -height / 2.0, height / 2.0, 0, 1);
		
		// switch to the model view matrix
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		// initialize the matrix
		gl.glLoadIdentity();
		
		// set the size
		this.canvasSize = new Dimension(width, height);
	}

	/**
	 * Updates the world.
	 */
	private void update() {
		// get the current time
        long time = System.nanoTime();
        // get the elapsed time from the last iteration
        long diff = time - this.last;
        // set the last time
        this.last = time;
        
		// check if the state is paused
		if (!this.isPaused()) {
	    	// convert from nanoseconds to seconds
	    	double elapsedTime = (double)diff / NANO_TO_BASE;
	    	// obtain the lock on the world object
	    	synchronized (this.world) {
		        // update the world with the elapsed time
		        this.world.update(elapsedTime);
			}
		}
		
		// set the render state
		this.renderState.scale = scale;
		synchronized (this.world) {
			this.renderState.dt = this.world.getStep().getDeltaTime();
			this.renderState.invDt = this.world.getStep().getInverseDeltaTime();
		}
		this.renderState.size = this.canvasSize;
		this.renderState.offset = this.offset.copy();
		
		this.fps.update(diff);
		this.lblFps.setText(this.fps.getFpsString());
	}
	
	/**
	 * Renders the world.
	 * @param gl the OpenGL surface
	 */
	private void render(GL2 gl) {
		Dimension size = this.renderState.size;
		Vector2 offset = this.renderState.offset;
		double scale = this.renderState.scale;
		
		// apply a scaling transformation
		gl.glPushMatrix();
		gl.glScaled(scale, scale, scale);
		gl.glTranslated(offset.x, offset.y, 0.0);
		
		// draw selected stuff
		
		if (this.selectBodyAction.isActive()) {
			SandboxBody body = this.selectBodyAction.getBody();
			
			if (ApplicationSettings.isStenciled()) {
				// stenciling requires a larger radius
				RenderUtilities.outlineShapes(gl, body, 6, SELECTED_COLOR, this.renderState);
			} else {
				RenderUtilities.outlineShapes(gl, body, 4, SELECTED_COLOR, this.renderState);
			}
		}
		
		synchronized (world) {
			// render all the bodies in the world
			int bSize = this.world.getBodyCount();
			for (int i = 0; i < bSize; i++) {
				SandboxBody body = (SandboxBody)this.world.getBody(i);
				if (ApplicationSettings.isStenciled()) {
					RenderUtilities.outlineBody(gl, body, this.renderState, true);
				} else {
					body.render(gl);
				}
			}
			
			// render the joints
			int jSize = this.world.getJointCount();
			for (int i = 0; i < jSize; i++) {
				Joint joint = this.world.getJoint(i);
				
				RenderUtilities.drawJoint(gl, joint, this.renderState);
			}
		}
		
		gl.glPopMatrix();
		
		// draw other stuff
		
		gl.glPushMatrix();
		gl.glLoadIdentity();
		
		// draw labels
		
		if (ApplicationSettings.isLabeled()) {
			synchronized (world) {
				gl.glColor3f(0.0f, 0.0f, 0.0f);
				// render all the bodies in the world
				int bSize = this.world.getBodyCount();
				for (int i = 0; i < bSize; i++) {
					SandboxBody body = (SandboxBody)this.world.getBody(i);
					RenderUtilities.drawLabel(gl, glut, body, this.renderState, 5);
				}
			}
		}
		
		// draw origin label
		
		if (ApplicationSettings.isOriginLabeled()) {
			double ox = offset.x * scale;
			double oy = offset.y * scale;
			gl.glColor3f(0.0f, 0.0f, 0.0f);
			RenderUtilities.fillRectangleFromCenter(gl, ox, oy, 3, 3);
			gl.glRasterPos2d(2 + ox, -12 + oy);
			this.glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, "Origin");
		}
		
		// draw HUD like stuff
		
		// translate (0, 0) to the bottom left corner
		gl.glTranslated(-size.getWidth() * 0.5, -size.getHeight() * 0.5, 0.0);
		RenderUtilities.drawPixelScale(gl, this.glut, 5, 25, 3, 100, 20, this.renderState);
		
		gl.glPopMatrix();
	}
	
	/**
	 * Polls for input from the user.
	 */
	public void poll() {
		Dimension size = this.renderState.size;
		Vector2 offset = this.renderState.offset;
		double scale = this.renderState.scale;
		
		// see if the user has zoomed in or not
		if (this.mouse.hasScrolled()) {
			// get the scroll amount
			int scroll = this.mouse.getScrollAmount();
			// zoom in or out
			if (scroll < 0) {
				this.scale *= 0.5;
			} else {
				this.scale *= 2.0;
			}
		}
		
		// get the mouse location
		Point p = this.mouse.getLocation();
		if (p == null) {
			p = new Point();
		}
		Vector2 pw = this.screenToWorld(p, size, offset, scale);
		
		// update the mouse location
		if (this.mouse.hasMoved()) {
			this.lblMouseLocation.setText(RenderUtilities.formatVector2(pw));
		}
		
		// the mouse button 1 or 3 was clicked
		if (this.mouse.wasClicked(MouseEvent.BUTTON1) || this.mouse.wasClicked(MouseEvent.BUTTON3)) {
			// check if a body is already clicked
			if (this.selectBodyAction.isActive()) {
				// get the body
				SandboxBody body = this.selectBodyAction.getBody();
				// get the fixture
				BodyFixture fixture = this.getContainingFixture(body, pw);
				// make sure the click was inside the same body
				if (fixture != null) {
					// TODO start the select fixture action
				} else {
					// otherwise de-select the body
					this.selectBodyAction.end();
				}
			} else {
				// otherwise see if a body is being selected
				SandboxBody body = this.getBodyAtPoint(pw);
				// check for null
				if (body != null) {
					// begin the select body action
					this.selectBodyAction.begin(body);
				}
			}
		}
		
		// the mouse button 1 is pressed and being held
		if (this.mouse.isPressed(MouseEvent.BUTTON1)) {
			// check if a body is already selected
			if (this.selectBodyAction.isActive() && this.moveBodyAction.isActive()) {
				// get the body
				SandboxBody body = this.selectBodyAction.getBody();
				// check if the world is running
				if (this.isPaused()) {
					// get the difference
					Vector2 tx = pw.difference(this.moveBodyAction.getBeginPosition());
					// move the body with the mouse
					body.translate(tx);
					// update the action
					this.moveBodyAction.update(pw);
				} else {
					// update the mouse joint's target point
					if (this.selectedBodyJoint == null) {
						this.selectedBodyJoint = new MouseJoint(body, pw, 4.0, 0.7, 1000.0 * body.getMass().getMass());
						synchronized (this.world) {
							this.world.add(this.selectedBodyJoint);
						}
					} else {
						this.selectedBodyJoint.setTarget(pw);
					}
				}
			} else if (this.moveWorldAction.isActive()) {
				// then translate the offset
				// we need to get the current position in world coordinates using the old offset
				Vector2 pwt = this.screenToWorld(p, size, this.moveWorldAction.getOffset(), scale);
				// compute the difference in the new position to get the offset
				Vector2 tx = pwt.difference(this.moveWorldAction.getBeginPosition());
				// apply it to the offset
				this.offset.add(tx);
				// update the new mouse position
				this.moveWorldAction.update(pwt);
			} else {
				// otherwise see if a body is being selected
				SandboxBody body = this.getBodyAtPoint(pw);
				// check for null
				if (body != null) {
					// begin the select body action
					this.selectBodyAction.begin(body);
					// begin the move body action
					this.moveBodyAction.begin(pw);
				} else {
					// then assume the user wants to move the world
					this.moveWorldAction.begin(this.offset.copy(), pw, this);
				}
			}
		}
		
		// the mouse button 3 is pressed and being held
		if (this.mouse.isPressed(MouseEvent.BUTTON3)) {
			// check if a body is already selected
			if (this.selectBodyAction.isActive() && this.rotateBodyAction.isActive()) {
				// get the body
				SandboxBody body = this.selectBodyAction.getBody();
				// get the rotation
				Vector2 c = body.getWorldCenter();
				Vector2 v1 = c.to(this.rotateBodyAction.getBeginPosition());
				Vector2 v2 = c.to(pw);
				double theta = v1.getAngleBetween(v2);
				// move the body with the mouse
				body.rotate(theta, c);
				// update the action
				this.rotateBodyAction.update(pw);
			} else {
				// otherwise see if a body is being selected
				SandboxBody body = this.getBodyAtPoint(pw);
				// check for null
				if (body != null) {
					// begin the select body action
					this.selectBodyAction.begin(body);
					// begin the move body action
					this.rotateBodyAction.begin(pw);
				}
			}
		}
		
		// check if the mouse button 1 was released
		if (this.mouse.wasReleased(MouseEvent.BUTTON1)) {
			if (this.moveBodyAction.isActive()) {
				// end the action
				this.moveBodyAction.end();
				// end the move body joint
				if (this.selectedBodyJoint != null) {
					synchronized (this.world) {
						this.world.remove(this.selectedBodyJoint);
					}
					this.selectedBodyJoint = null;
				}
			}
			if (this.moveWorldAction.isActive()) {
				// end the action
				this.moveWorldAction.end(this);
			}
		}
		
		// check if the mouse button 1 was released
		if (this.mouse.wasReleased(MouseEvent.BUTTON3)) {
			if (this.rotateBodyAction.isActive()) {
				// end the action
				this.rotateBodyAction.end();
			}
		}
		
		
		this.mouse.clear();
	}
	
	/**
	 * Returns true if the simulation is paused.
	 * @return boolean
	 */
	public synchronized boolean isPaused() {
		return this.paused;
	}
	
	/**
	 * Sets the paused flag of the simulation.
	 * @param flag true if the simulation should be paused
	 */
	public synchronized void setPaused(boolean flag) {
		this.paused = flag;
		if (!flag) {
			// if the simulation is being unpaused then
			// reset the last time
			this.last = System.nanoTime();
		}
	}
	
	/**
	 * Returns the first body in the world's body list to contain the given point.
	 * <p>
	 * If no body is found at the given point, null is returned.
	 * @param point the world space point
	 * @return {@link SandboxBody}
	 */
	public SandboxBody getBodyAtPoint(Vector2 point) {
		int bSize = this.world.getBodyCount();
		// loop over all the bodies in the world
		for (int i = 0; i < bSize; i++) {
			Body body = this.world.getBody(i);
			// does the body contain the point
			if (contains(body, point)) {
				return (SandboxBody) body;
			}
		}
		return null;
	}
	
	/**
	 * Returns true if the given body contains the given point.
	 * @param point the world space point
	 * @param body the body
	 * @return boolean
	 */
	public boolean contains(Body body, Vector2 point) {
		Transform transform = body.getTransform();
		int fSize = body.getFixtureCount();
		// loop over the body fixtures
		for (int j = 0; j < fSize; j++) {
			BodyFixture bodyFixture = body.getFixture(j);
			Convex convex = bodyFixture.getShape();
			if (convex.contains(point, transform)) {
				// return the first body who contains a fixture
				// that contains the given point
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the fixture of the given body that the given point is inside.
	 * @param point the world space point
	 * @param body the body
	 * @return BodyFixture
	 */
	public BodyFixture getContainingFixture(Body body, Vector2 point) {
		Transform transform = body.getTransform();
		int fSize = body.getFixtureCount();
		// loop over the body fixtures
		for (int j = 0; j < fSize; j++) {
			BodyFixture bodyFixture = body.getFixture(j);
			Convex convex = bodyFixture.getShape();
			if (convex.contains(point, transform)) {
				// return the first body who contains a fixture
				// that contains the given point
				return bodyFixture;
			}
		}
		return null;
	}
	
	/**
	 * Converts from screen coordinates to world coordinates.
	 * @param p the screen point
	 * @param size the canvas size
	 * @param offset the canvas offset
	 * @param scale the screen to world scale factor
	 * @return Vector2 the world point
	 */
	public Vector2 screenToWorld(Point p, Dimension size, Vector2 offset, double scale) {
		Vector2 v = new Vector2();
		double x = p.x;
		double y = p.y;
		double w = size.getWidth();
		double h = size.getHeight();
		double ox = offset.x;
		double oy = offset.y;
		v.x = (x - w * 0.5) / scale - ox;
		v.y = -((y - h * 0.5) / scale + oy);
		return v;
	}
	
	/**
	 * The main method; uses zero arguments in the args array.
	 * @param args the command line arguments
	 */
	public static final void main(String[] args) {
	    new Sandbox();
	}
}