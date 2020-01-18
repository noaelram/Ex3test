package gameClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import gameClient.MyGameGUI.Fruit;
import gameClient.MyGameGUI.Robot;

public class GameManager {
	private MyGameGUI gui;
	/*
	 * This array holds the state of every robot (we called it a "mission")
	 */
	private RobotState arr[];

	/*
	 * The state of every robot (the "mission")
	 */
	class RobotState {
		// the current mission's path the robot needs to accomplish
		List<node_data> path = null;
		// the final edge this robot needs to reach
		// we use this field because we don't want two robots to do the same mission
		// so before we assign a new mission to a robot, we check that no other robot do
		// this mission (i.e the mission is available)
		edge finalEdge;
	}

	public GameManager(MyGameGUI myGameGUI) {
		this.gui = myGameGUI;
		this.arr = new RobotState[this.gui.getNumRobotsShouldBe()];
		// init robots missions state
		for (int i = 0; i < arr.length; i++) {
			this.arr[i] = new RobotState();
		}
	}

	// a function to determine the next node of a given robot with given src node
	public int nextNode(int rid, int src) {
		// if there is no mission to this robot (usually at the beginning of the game)
		// or this robot finished his mission
		if (noMission(rid) || finishMission(rid, src)) {
			// set a new mission to the robot
			setNewMission(rid, src);
		}
		// move to next node in current mission
		return moveInMission(rid, src);
	}

	private void setNewMission(int rid, int currentNode) {
		// get all edges with fruits on them
		List<edge_data> edgesWithFruits = getEdgesWithFruits();

		// let's found an unused edge
		for (edge_data e : edgesWithFruits) {
			// continue if edge already in use
			if (edgeInUse(e)) {
				continue;
			} else {
				// found a target edge
				int targetNode = e.getDest();
				// initiate Graph_Algo
				Graph_Algo algo = new Graph_Algo(this.gui.gameGraph);

				if (currentNode == targetNode) {
					break;
				}

				// get shortest path from currentNode to targetNode
				List<node_data> path = algo.shortestPath(currentNode, targetNode);

				// if no path, continue to the next edge
				if (path == null) {
					continue;
				}

				// set mission path and mission edge to this robot and return
				arr[rid].path = path;
				arr[rid].finalEdge = (edge) e;
				return;
			}
		}

		// in any other case, move on random edge
		List<node_data> newMission = new ArrayList<node_data>();
		newMission.add(gui.gameGraph.getNode(currentNode));
		node_data nextNode = getNextRandom(currentNode);
		newMission.add(nextNode);
		arr[rid].path = newMission;
		arr[rid].finalEdge = new edge(currentNode, nextNode.getKey(), 0);
	}

	// choosing random vertex from available ones
	private node_data getNextRandom(int currentNode) {
		int ans = -1;
		// get all edges from currentNode
		Collection<edge_data> ee = gui.gameGraph.getE(currentNode);
		Iterator<edge_data> itr = ee.iterator();
		int s = ee.size();
		// random number between 1 to number of edges
		int r = (int) (Math.random() * s);
		int i = 0;
		while (i < r) {
			itr.next();
			i++;
		}
		// get dest node
		ans = itr.next().getDest();
		// returning dest node
		return gui.gameGraph.getNode(ans);
	}

	// check if edge is already in use bo robot
	private boolean edgeInUse(edge_data e) {
		// iterate robots state array
		for (int i = 0; i < arr.length; i++) {
			// if no mission, skip
			if (arr[i].finalEdge == null)
				continue;

			edge_data other = arr[i].finalEdge;
			// check if edge is already on use by the robot
			if (e.getSrc() == other.getSrc() && e.getDest() == other.getDest())
				return true;
		}
		return false;
	}

	// returning the next node in path
	private int moveInMission(int rid, int src) {
		return arr[rid].path.get(getIndexInPath(rid, src) + 1).getKey();
	}

	// returning the index of key in robot's current path
	private int getIndexInPath(int rid, int key) {
		// if no path return -1
		if (noMission(rid)) {
			return -1;
		}

		// else iterate current path and check for key equality
		int idx = -1;
		for (int i = 0; i < arr[rid].path.size(); i++) {
			if (arr[rid].path.get(i).getKey() == key) {
				idx = i;
				break;
			}
		}
		// return the index
		return idx;
	}

	private boolean finishMission(int rid, int src) {
		// if no mission than the robot also finish mission
		if (noMission(rid)) {
			return true;
		}

		// if the current node is the last node in mission path, that the robot finished
		// its mission
		int idx = getIndexInPath(rid, src);
		return idx == -1 || idx == (arr[rid].path.size() - 1);
	}

	// there is no mission if path is null
	private boolean noMission(int rid) {
		return arr[rid].path == null;
	}

	// get all edges with fruits on them
	private List<edge_data> getEdgesWithFruits() {
		// get all fruits
		List<Fruit> fruits = gui.getFruits();
		// init new empty list of edges
		List<edge_data> edges = new ArrayList<edge_data>();
		// for every fruit
		for (Fruit f : fruits) {
			// get its edge
			edge_data e = gui.gedEdgeFromPoint(f.pos);
			// depending on the fruit type (apple/banana), create the new edge
			if ((f.isApple && e.getSrc() > e.getDest()) || (!f.isApple && e.getSrc() < e.getDest())) {
				e = new edge(e.getDest(), e.getSrc(), 0);
			}
			// add it to the collection
			edges.add(e);
		}
		return edges;
	}

	public void placeRobots() {
		// num robots in level
		int robotsNum = gui.getNumRobotsShouldBe();
		// get all fruits
		List<Fruit> fruits = gui.getFruits();
		// comparator to compare fruits by their value because we want to choose the
		// higher value fruits first
		fruits = fruits.stream().sorted((f1, f2) -> (-1) * Double.compare(f1.value, f2.value))
				.collect(Collectors.toList());

		// for every robot
		for (int i = 0; i < robotsNum; i++) {
			int nodeToAddTo;
			// if there are more fruits
			if (i < fruits.size()) {
				Fruit f = fruits.get(i);
				// set this node to the robot
				nodeToAddTo = f.nearestNode;
			} else {
				// if no fruits left, random a node to the robot
				Random r = new Random();
				List<node_data> nodes = gui.gameGraph.getV().stream().collect(Collectors.toList());
				nodeToAddTo = nodes.get(r.nextInt(nodes.size())).getKey();
			}
			gui.game.addRobot(nodeToAddTo);
		}
	}

	// a public function to choose next robot for the robot r
	public void nextNode(Robot r) {
		// choose dest
		int dest = nextNode(r.id, r.src);
		// call chooseNextEdge on robot and dest
		gui.game.chooseNextEdge(r.id, dest);
	}
}