package gameClient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.json.JSONException;
import org.json.JSONObject;

import Server.Game_Server;
import Server.game_service;
import oop_dataStructure.OOP_DGraph;
import oop_dataStructure.oop_edge_data;
import oop_dataStructure.oop_node_data;


public class MyGameGUI extends JFrame {
	/*
	 * Some GUI components
	 */
	private JRadioButton userButton = new JRadioButton("Manual");
	private JRadioButton algoButton = new JRadioButton("Algorithm");
	private JCheckBox kmlOutput = new JCheckBox("output Game to KML file? (only algorithm, without GUI)");
	private JLabel scenarioLabel = new JLabel("Scenario [0-23]:");
	private JTextField scenarioField = new JTextField();
	private JButton startButton = new JButton("Start");
	private GameManager manager;
	private double finalScore;
	Color colors[] = { Color.BLACK, Color.ORANGE, Color.pink, Color.GRAY, Color.CYAN };
	String colorsStr[] = { "BLACK", "ORANGE", "PINK", "GRAY", "CYAN" };
	private JLabel timeLabel = new JLabel("Time left to game: -");
	private JLabel scoreLabel = new JLabel("Score: -");
	// the server of the game
	// the level graph
	public DGraph gameGraph;
	// the thread that is going to run in the background
	private TimeAndScoreThread timeAndScoreThread;
	// a boolean that is true if we are going to output to KML file
	private boolean outputToKML;
	
	// two modes of the game: user, algorithm
		enum Mode {
			algorithm, user
		}
		
		// get the mode of the game from the radio button
		private Mode getMode() {
			if (algoButton.isSelected()) {
				return Mode.algorithm;
			}
			return Mode.user;
		}
		
		// a simple Robot class to hold the robot data from the JSON
		class Robot {
			public double value;
			public int id;
			public int src;
			public int dest;
			public double speed;
			public Point3D pos;
		}
		
		// a simple Fruit class to hold the fruit data from the JSON
		class Fruit {
			public double value;
			public boolean isApple;
			public int nearestNode;
			public Point3D pos;
		}
		
		// a helper function to create Fruit object from String (JSON)
		Fruit createFruit(String fruit) {
			try {
				JSONObject line;
				Fruit r = new Fruit();
				line = new JSONObject(fruit);
				JSONObject ttt = line.getJSONObject("Fruit");
				r.value = ttt.getDouble("value");
				r.isApple = ttt.getInt("type") == 1;
				String[] parts = ttt.getString("pos").split(",");
				r.pos = new Point3D(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), 0);
				r.nearestNode = getNearestNode(r.pos);
				return r;
			} catch (JSONException e) {
				e.printStackTrace();
			}
			throw new RuntimeException();
		}
		
		// get the nearest node to a given point
		public int getNearestNode(Point3D pos) {
			int minNode = 0;
			double minDistance = Double.MAX_VALUE;
			// just iterate all nodes and calcualte the distance
			for (node_data n : gameGraph.getV()) {
				if (n.getLocation().distance2D(pos) < minDistance) {
					minDistance = n.getLocation().distance2D(pos);
					minNode = n.getKey();
				}
			}
			return minNode;
		}
		
		// get a list of fruits in the game
		List<Fruit> getFruits() {
			List<Fruit> fruits = new ArrayList<Fruit>();
			for (String fruit : game.getFruits()) {
				fruits.add(createFruit(fruit));
			}
			return fruits;
		}
		
		// get a list of robots in the game
		List<Robot> getRobots() {
			List<Robot> robots = new ArrayList<Robot>();
			for (String robot : game.getRobots()) {
				robots.add(createRobot(robot));
			}
			return robots;
		}
		
		// get the number of robots in the game from the JSON of the game
		public int getNumRobotsShouldBe() {
			try {
				JSONObject line;
				line = new JSONObject(game.toString());
				JSONObject ttt = line.getJSONObject("GameServer");
				return ttt.getInt("robots");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			throw new RuntimeException();
		}
		
		// a helper function to create Robot object from String (JSON)
		Robot createRobot(String robot) {
			try {
				JSONObject line;
				Robot r = new Robot();
				line = new JSONObject(robot);
				JSONObject ttt = line.getJSONObject("Robot");
				r.id = ttt.getInt("id");
				r.value = ttt.getDouble("value");
				r.src = ttt.getInt("src");
				r.dest = ttt.getInt("dest");
				r.speed = ttt.getDouble("speed");
				String[] parts = ttt.getString("pos").split(",");
				r.pos = new Point3D(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), 0);
				return r;
			} catch (JSONException e) {
				e.printStackTrace();
			}
			throw new RuntimeException();
		}
		
		// a function to move the robots next to the next edge
		private List<Robot> moveRobots() {
			// call server API game.move
			List<String> logs = game.move();
			List<Robot> robots = new ArrayList<Robot>();
			if (logs == null) {
				return robots;
			}
			for (String robot : logs) {
				robots.add(createRobot(robot));
			}
			// return the new robots state
			return robots;
		}
		
		/*
		 * The thread that will manage the game in the background
		 */
		public class TimeAndScoreThread extends Thread {
			// to stop the thread
			private boolean exit = false;

			public void exit() {
				exit = true;
			}

			public void run() {

				// initial the canvas to draw
				initCanvas();
				// while no exist and the game is running
				while (!exit && game.isRunning()) {
					// draw the current graph
					MyGameGUI.this.drawGraph();
					// get the time left to the game
					long t = game.timeToEnd();

					// set the text on the screen
					timeLabel.setText("Time left to game: " + (t / 1000));

					// if need to output to KML, add current dynamic data to KML
					if (outputToKML) {
						kmlLogger.addDynamicData(getRobots(), getFruits(), 60000 - t);
					}

					List<Robot> robots = moveRobots();

					int i = 0;
					for (Robot r : robots) {
						// if dest==-1, we need to choose next edge to the robot
						if (r.dest == -1) {
							// if mode is algorithm, let the algorithm decide
							if (MyGameGUI.this.getMode() == Mode.algorithm) {
								manager.nextNode(r);
							} else {
								// else, the user needs to choose so popup a messagebox to choose
								int dest;
								do {
									String nextNode = JOptionPane.showInputDialog(
											"Please choose next node for robot with color = " + colorsStr[i]);
									dest = Integer.parseInt(nextNode);
									// validate that the next node is a valid node
								} while (game.isRunning() && gameGraph.getEdge(r.src, dest) == null);
								// choose next edge (server API call)
								game.chooseNextEdge(r.id, dest);
							}
						}
						i++;
					}

					try {
						// sleep for 100 miliseconds
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// the game is over, so calculate the total score
				timeLabel.setText("Time left to game: 0");
				double score = 0;
				// iterate all the robots and sum score
				for (Robot robot : getRobots()) {
					score += robot.value;
				}
				// show the score to the screen
				finalScore = score;
				scoreLabel.setText("Score: " + (score));
				// if needs to output to KML, call outputToFile
				if (outputToKML) {
					try {
						kmlLogger.outputToFile();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}

			}
		}
		
		private Point3D scaleLocation(Point3D p) {
			return new Point3D(scaleX(p.x()), scaleY(p.y()));
		}
		
		// we use this function to determine the node that the user chose to put the
		// robot on (in the start of the game)
		public int getChosenNode(Point3D p) {
			for (node_data n : gameGraph.getV()) {
				// if the distance is less than 10, return the node
				if (scaleLocation(n.getLocation()).distance3D(p) < 10) {
					return n.getKey();
				}
			}
			// no node is chosen
			return -1;
		}
		
		// initial place for the robots
		public void placeRobots() {
			// if algorithm mode -> let the algorith mdecide
			if (getMode() == Mode.algorithm) {
				manager.placeRobots();
				return;
			}
			// if user mode, init canvas and draw the graph so the user can choose initial
			// nodes for the robots
			if (getMode() == Mode.user) {
				initCanvas();
				drawGraph();
			}
			int robotsNum = getNumRobotsShouldBe();
			for (int i = 0; i < robotsNum; i++) {
				// for each robot let the user choose a next node
				System.out.println("Please select initial location for robot number " + (i + 1) + "/" + robotsNum);
				// while there is no node that the user chose
				while (true) {
					// if the mouse was pressed by the user
					if (StdDraw.isMousePressed()) {
						// get mouse location
						double x = StdDraw.mouseX();
						double y = StdDraw.mouseY();
						Point3D p = new Point3D(x, y, 0);
						// get chosen node by the user
						int chosenNode = getChosenNode(p);
						if (chosenNode != -1) {
							// add robot in this location
							game.addRobot(chosenNode);
							// draw the graph again
							drawGraph();
							// break the loop because node was chosen for the robot
							break;
						}
					}
				}
			}
		}
		
		// update the local graph from the graph in the server
		public void updateGraph() {
			OOP_DGraph g = new OOP_DGraph();
			g.init(game.getGraph());
			gameGraph = copy(g);
		}
		
		// start new game with level
		public void startWithLevel(int level) {
			// output to kml file only user checked the checkbox in gui and mode is
			// algorithm
			outputToKML = kmlOutput.isSelected() && getMode() == Mode.algorithm;

			if (timeAndScoreThread != null) {
				timeAndScoreThread.exit();
			}
			// get the game from the server
			game = Game_Server.getServer(level);
			// update local graph
			updateGraph();

			// if algorithm mode, initiate a manager
			if (Mode.algorithm == getMode()) {
				manager = new GameManager(this);
			}

			// place the robots in the game
			placeRobots();

			// initiate kmlLogger if needs to output to KML file
			if (outputToKML) {
				// open new kml file to update
				kmlLogger = new KML_Logger(level + "");
				kmlLogger.addStaticData(gameGraph);
			}

			// call start the game
			game.startGame();

			// start the background thread
			timeAndScoreThread = new TimeAndScoreThread();
			timeAndScoreThread.start();
		}
		
		public MyGameGUI() {
			// start button
			this.startButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// start new game with given level when user press the start button
					startWithLevel(Integer.parseInt(scenarioField.getText()));
				}
			});
			// initialize the GUI
			initializeUI();
		}
		
		// round a double to number of places after dot
		public static double round(double value, int places) {
			if (places < 0)
				throw new IllegalArgumentException();

			BigDecimal bd = BigDecimal.valueOf(value);
			bd = bd.setScale(places, RoundingMode.HALF_UP);
			return bd.doubleValue();
		}
		
		// a fields to hold the max and min values of the data in order to scale it
		double maxX;
		double maxY;
		double minX;
		double minY;
		int canvasWidth = 600;
		int canvasHeight = 600;
		
		public void initCanvas() {
			// not drawing the graph when outputing to KML file
			if (outputToKML) {
				return;
			}
			// init game graph
			graph g = gameGraph;

			// set the canvas size
			StdDraw.setCanvasSize(canvasWidth, canvasHeight);
			// init min/max fields
			maxX = g.getV().parallelStream().map(n -> n.getLocation().x()).max(Double::compareTo).get();
			maxY = g.getV().parallelStream().map(n -> n.getLocation().y()).max(Double::compareTo).get();
			minX = g.getV().parallelStream().map(n -> n.getLocation().x()).min(Double::compareTo).get();
			minY = g.getV().parallelStream().map(n -> n.getLocation().y()).min(Double::compareTo).get();
			// so that node will not be on the edge of the screen
			double delta = 30;
			StdDraw.setXscale(-delta, canvasWidth + delta);
			StdDraw.setYscale(-delta, canvasHeight + delta);
		}
		
		/**
		 * a scale function
		 * 
		 * @param data  denote some data to be scaled
		 * @param r_min the minimum of the range of your data
		 * @param r_max the maximum of the range of your data
		 * @param t_min the minimum of the range of your desired target scaling
		 * @param t_max the maximum of the range of your desired target scaling
		 * @return
		 */
		private double scale(double data, double r_min, double r_max, double t_min, double t_max) {

			double res = ((data - r_min) / (r_max - r_min)) * (t_max - t_min) + t_min;
			return res;
		}
		
		// copy a graph from the server to the DGraph type (local)
		private DGraph copy(OOP_DGraph g) {
			DGraph newGraph = new DGraph();
			for (oop_node_data n : g.getV()) {
				newGraph.addNode(new node(n.getKey(), new Point3D(n.getLocation().x(), n.getLocation().y())));
			}
			for (oop_node_data n : g.getV()) {
				for (oop_edge_data e : g.getE(n.getKey())) {
					newGraph.connect(e.getSrc(), e.getDest(), e.getWeight());
				}
			}
			return newGraph;
		}
		
		private double scaleX(double data) {
			return scale(data, minX, maxX, 0, canvasWidth);
		}

		private double scaleY(double data) {
			return scale(data, minY, maxY, 0, canvasHeight);
		}
		
		// copy for draw is copying the graph while rounding the data
		private graph copyForDraw(DGraph g) {
			DGraph newGraph = new DGraph();
			for (node_data n : g.getV()) {
				double xs = scaleX(n.getLocation().x());
				double xy = scaleY(n.getLocation().y());
				newGraph.addNode(new node(n.getKey(), new Point3D(xs, xy)));
			}
			for (node_data n : g.getV()) {
				for (edge_data e : g.getE(n.getKey())) {
					newGraph.connect(e.getSrc(), e.getDest(), round(e.getWeight(), 1));
				}
			}
			return newGraph;
		}
		
		public void drawGraph() {
			if (outputToKML) {
				return;
			}
			graph g = copyForDraw(gameGraph);

			if (g.nodeSize() == 0) {
				return;
			}

			StdDraw.clear();

			// Draw vertices
			for (node_data n : g.getV()) {
				StdDraw.setPenColor(Color.BLUE);
				StdDraw.setPenRadius(0.01);
				StdDraw.setFont(new Font("TimesRoman", Font.BOLD, 15));
				Point3D l = n.getLocation();
				double x = l.x();
				double y = l.y();
				StdDraw.point(x, y);
				StdDraw.setPenColor(Color.BLUE);
				StdDraw.setPenRadius(0.01);
				StdDraw.text(x, y, Integer.toString(n.getKey()));
			}
			// Draw edges
			for (node_data n : g.getV()) {
				for (edge_data e : g.getE(n.getKey())) {
					node_data n2 = g.getNode(e.getDest());
					StdDraw.setPenColor(Color.gray);
					StdDraw.setPenRadius(0.001);
					StdDraw.line(n.getLocation().x(), n.getLocation().y(), n2.getLocation().x(), n2.getLocation().y());
					StdDraw.text(n.getLocation().x() + ((n2.getLocation().x() - n.getLocation().x()) * 0.5),
							n.getLocation().y() + ((n2.getLocation().y() - n.getLocation().y()) * 0.5),
							Double.toString(round(e.getWeight(), 1)));
					StdDraw.setPenColor(Color.YELLOW);
					StdDraw.setPenRadius(0.01);
					StdDraw.point(n.getLocation().x() + ((n2.getLocation().x() - n.getLocation().x()) * 0.9),
							n.getLocation().y() + ((n2.getLocation().y() - n.getLocation().y()) * 0.9));
				}
			}

			int c = 0;
			for (Robot robot : getRobots()) {
				StdDraw.setPenColor(colors[c]);
				StdDraw.setPenRadius(0.03);
				Point3D l = scaleLocation(robot.pos);
				double x = l.x();
				double y = l.y();
				StdDraw.point(x, y);
				c++;
			}

			for (Fruit f : getFruits()) {
				Point3D l = f.pos;
				double x = scaleX(l.x());
				double y = scaleY(l.y());
				StdDraw.setPenColor(f.isApple ? Color.red : Color.yellow);
				StdDraw.setFont(new Font("TimesRoman", Font.BOLD, 15));
				StdDraw.text(x, y, f.isApple ? "A" : "B");
			}
		}
		
		private void initializeUI() {

			this.setTitle("Graph GUI");
			Dimension fieldSize = new Dimension(70, 24);
			this.scenarioField.setPreferredSize(fieldSize);

			JPanel north2 = new JPanel();
			north2.setLayout(new FlowLayout());
			north2.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			north2.add(scenarioLabel);
			north2.add(scenarioField);
			north2.add(startButton);

			algoButton.setSelected(true);
			ButtonGroup group = new ButtonGroup();
			group.add(userButton);
			group.add(algoButton);

			JPanel north3 = new JPanel();
			north3.setLayout(new FlowLayout());
			north3.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			north3.add(userButton);
			north3.add(algoButton);

			JPanel north = new JPanel();
			north.setLayout(new BorderLayout());
			north.add(north2, BorderLayout.NORTH);
			north.add(north3, BorderLayout.SOUTH);

			JPanel center = new JPanel();
			center.setLayout(new BorderLayout());
			center.add(timeLabel, BorderLayout.NORTH);
			center.add(kmlOutput, BorderLayout.SOUTH);
			kmlOutput.setSelected(true);

			JPanel south = new JPanel();
			south.add(scoreLabel);

			// add panels to main frame
			this.setLocationRelativeTo(null);
			this.setBounds(100, 100, 500, 200);
			this.setLayout(new BorderLayout());
			this.add(north, BorderLayout.NORTH);
			this.add(center, BorderLayout.CENTER);
			this.add(south, BorderLayout.SOUTH);
		}
		
		public static void main(String[] a) throws InterruptedException {
			MyGameGUI m = new MyGameGUI();
			m.setVisible(true);
			// if there are parameters to the program
			// you can run the program with parameter that it's the scenario we want to run
			if (a.length > 0) {
				m.kmlOutput.setSelected(true); // for kml output
				m.algoButton.setSelected(true); // for using algo mode
				int scenario = Integer.parseInt(a[0]);
				m.startWithLevel(scenario);
				m.timeAndScoreThread.join();
				System.out.println("Scenario=" + scenario + " , Score=" + m.finalScore);
				System.exit(0);
			}
		}
		
		// find the edge that are close to pos
		public edge_data gedEdgeFromPoint(Point3D pos) {
			double minDistance = Double.MAX_VALUE;
			edge_data minEdge = null;
			// go over all the edges in graph
			for (node_data n : gameGraph.getV()) {
				for (edge_data e : gameGraph.getE(n.getKey())) {
					// calcualte the distance of the point from the edge
					double distance = distancePointFromEdge(pos, gameGraph.getNode(e.getSrc()).getLocation(),
							gameGraph.getNode(e.getDest()).getLocation());
					// if distance is NaN, the point is on the edge
					if (Double.isNaN(distance)) {
						minDistance = Double.MIN_VALUE;
						minEdge = e;
					} else if (minDistance > distance) {
						// found a closer edge
						minDistance = distance;
						minEdge = e;
					}
				}
			}

			return minEdge;
		}
		
		private double distancePointFromEdge(Point3D p, Point3D s, Point3D e) {
			return distBetweenPointAndLine(p.x(), p.y(), s.x(), s.y(), e.x(), e.y());
		}
		
		private double distBetweenPointAndLine(double x, double y, double x1, double y1, double x2, double y2) {
			// A - the standalone point (x, y)
			// B - start point of the line segment (x1, y1)
			// C - end point of the line segment (x2, y2)
			// D - the crossing point between line from A to BC

			double AB = distBetween(x, y, x1, y1);
			double BC = distBetween(x1, y1, x2, y2);
			double AC = distBetween(x, y, x2, y2);

			// Heron's formula
			double s = (AB + BC + AC) / 2;
			double area = (double) Math.sqrt(s * (s - AB) * (s - BC) * (s - AC));

			// but also area == (BC * AD) / 2
			// BC * AD == 2 * area
			// AD == (2 * area) / BC
			double AD = (2 * area) / BC;
			return AD;
		}
		
		private double distBetween(double x, double y, double x1, double y1) {
			double xx = x1 - x;
			double yy = y1 - y;

			return (float) Math.sqrt(xx * xx + yy * yy);
		}
}