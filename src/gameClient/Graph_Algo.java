package gameClient;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * This empty class represents the set of graph-theory algorithms which should
 * be implemented as part of Ex2 - Do edit this class.
 * 
 * @author
 *
 */
public class Graph_Algo implements graph_algorithms {

	private graph g;

	public Graph_Algo(graph g) {
		init(g);
	}

	public Graph_Algo() {
	}

	public graph getGraph() {
		return g;
	}

	@Override
	public void init(graph g) {
		this.g = g;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see algorithms.graph_algorithms#init(java.lang.String) Just go over the file
	 * lines and parse
	 */
	@Override
	public void init(String file_name) {
		this.g = new DGraph();
		List<String> lines;
		try {
			lines = Files.readAllLines(Paths.get(file_name));
			for (String line : lines) {
				String[] parts = line.split(",");
				String[] tmp = parts[0].split(":");
				String[] xy = tmp[1].split("-");
				Point3D location = new Point3D(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
				int src = Integer.parseInt(tmp[0]);
				this.g.addNode(new node(src, location));
				for (int i = 1; i < parts.length; i++) {
					String[] parts2 = parts[i].split(";");
					int dst = Integer.parseInt(parts2[0]);
					double weight = Double.parseDouble(parts2[1]);
					this.g.connect(src, dst, weight);
				}
			}
		} catch (IOException e) {
			this.g = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void save(String file_name) {
		try (PrintWriter pw = new PrintWriter(new FileWriter(file_name))) {
			for (node_data n : g.getV()) {
				// format:
				// src,dst1;w1,dst2;w2,dst3;w3,...
				String dests = "";
				for (edge_data e : g.getE(n.getKey())) {
					dests += e.getDest() + ";" + e.getWeight() + ",";
				}
				// delete last comma
				if (!dests.isEmpty()) {
					dests = dests.substring(0, dests.length() - 1);
				}
				pw.println(n.getKey() + ":" + n.getLocation().x() + "-" + n.getLocation().y() + "," + dests);
			}
		} catch (IOException e1) {
			this.g = null;
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public boolean isConnected() {
		for (node_data n : g.getV()) {
			if (reachableFrom(n.getKey()).size() != this.g.nodeSize()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public double shortestPathDist(int src, int dest) {
		if (src == dest) {
			return 0;
		}
		List<node_data> path = shortestPath(src, dest);
		if (path == null) {
			return -1;
		}
		return path.get(path.size() - 1).getWeight();
	}

	@Override
	public List<node_data> shortestPath(int src, int dest) {
		computeShortestPaths(src);
		return shortestPathAux(src, dest);
	}

	// ASSUMPTION: computeShortestPaths(src) run before calling this function
	private List<node_data> shortestPathAux(int src, int dest) {
		List<node_data> path = new ArrayList<node_data>();
		if (src == dest) {
			path.add(g.getNode(src));
			return path;
		}
		int key = dest;
		while (key != src && key != -1) {
			path.add(g.getNode(key));
			key = g.getNode(key).getTag();
		}
		if (key == -1) {
			return null;
		}
		path.add(g.getNode(src));
		Collections.reverse(path);
		return path;
	}

	@Override
	public List<node_data> TSP(List<Integer> targets) {
		HashMap<Integer, HashMap<Integer, Double>> shortestPathsValue = new HashMap<Integer, HashMap<Integer, Double>>();
		HashMap<Integer, HashMap<Integer, List<node_data>>> shortestPathsNodes = new HashMap<Integer, HashMap<Integer, List<node_data>>>();
		for (int src : targets) {
			computeShortestPaths(src);
			HashMap<Integer, Double> shortestPathsValuesFromSource = new HashMap<Integer, Double>();
			HashMap<Integer, List<node_data>> shortestPathsNodesFromSource = new HashMap<Integer, List<node_data>>();
			for (int dest : targets) {
				if (dest == src)
					continue;
				// set min distance from src to dest
				shortestPathsValuesFromSource.put(dest, g.getNode(dest).getWeight());
				shortestPathsNodesFromSource.put(dest, shortestPathAux(src, dest));
			}
			shortestPathsValue.put(src, shortestPathsValuesFromSource);
			shortestPathsNodes.put(src, shortestPathsNodesFromSource);
		}
		targets.stream().toArray();
		Integer[] targetsArr = targets.stream().toArray(Integer[]::new);
		Integer[] shortest = new Integer[targetsArr.length];
		findShortest(targetsArr, shortest, shortestPathsValue);
		List<node_data> finalPath = new ArrayList<node_data>();
		int from;
		int to;
		for (int i = 0; i < shortest.length - 1; i++) {
			from = shortest[i];
			to = shortest[i + 1];
			List<node_data> nodes = shortestPathsNodes.get(from).get(to);
			if (nodes == null) {
				return null;
			}
			for (int j = 0; j < nodes.size(); j++) {
				if (j == nodes.size() - 1 && i != shortest.length - 2) {
					continue;
				}
				node_data n = nodes.get(j);
				finalPath.add(n);
			}
		}
		return finalPath;
	}

	private static void swap(Integer[] t, int i, int j) {
		int temp = t[i];
		t[i] = t[j];
		t[j] = temp;
	}

	private double findShortest(Integer[] targetsArr, Integer[] shortestPath,
			HashMap<Integer, HashMap<Integer, Double>> shortestPathsValue) {
		return findShortestAux(targetsArr, 0, Double.MAX_VALUE, shortestPath, shortestPathsValue);
	}

	private double findShortestAux(Integer[] targetsArr, int currentIndex, double shortest, Integer[] shortestPath,
			HashMap<Integer, HashMap<Integer, Double>> shortestPathsValue) {
		if (currentIndex == targetsArr.length - 1) {
			double value = calculatePath(targetsArr, shortestPathsValue);
			if (value < shortest) {
				copyArray(targetsArr, shortestPath);
				return value;
			}
			return shortest;
		}

		for (int i = currentIndex; i < targetsArr.length; i++) {
			swap(targetsArr, currentIndex, i);
			double value = findShortestAux(targetsArr, currentIndex + 1, shortest, shortestPath, shortestPathsValue);
			swap(targetsArr, currentIndex, i);
		}

		return shortest;
	}

	private static void copyArray(Integer[] targetsArr, Integer[] shortestPath) {
		for (int i = 0; i < targetsArr.length; i++) {
			shortestPath[i] = targetsArr[i];
		}
	}

	private double calculatePath(Integer[] targetsArr, HashMap<Integer, HashMap<Integer, Double>> shortestPathsValue) {
		double sum = 0;
		for (int i = 0; i < targetsArr.length - 1; i++) {
			int from = targetsArr[i];
			int to = targetsArr[i + 1];
			double distance = shortestPathsValue.get(from).get(to);
			sum += distance;
		}
		return sum;
	}

	@Override
	public graph copy() {
		DGraph newGraph = new DGraph();
		for (node_data n : g.getV()) {
			newGraph.addNode(new node(n));
			for (edge_data e : g.getE(n.getKey())) {
				newGraph.connect(e.getSrc(), e.getDest(), e.getWeight());
			}
		}
		return newGraph;
	}

	// node weight = min distance from src
	// node tag = previous vertex
	private void computeShortestPaths(int src) {
		// reset tags to -1
		for (node_data n : g.getV()) {
			n.setTag(-1);
			n.setWeight(Double.MAX_VALUE);
		}

		Comparator<node_data> nodesComparator = Comparator.comparing(node_data::getWeight, (w1, w2) -> {
			return w1.compareTo(w2);
		});

		node_data source = this.g.getNode(src);
		source.setWeight(0);
		PriorityQueue<node_data> queue = new PriorityQueue<node_data>(nodesComparator);
		queue.add(source);

		while (!queue.isEmpty()) {
			node_data n = queue.poll();

			for (edge_data e : g.getE(n.getKey())) {
				node_data dest = g.getNode(e.getDest());
				double weight = e.getWeight();
				double distanceThroughN = n.getWeight() + weight;
				if (distanceThroughN < dest.getWeight()) {
					queue.remove(dest);

					dest.setWeight(distanceThroughN); // set min distance
					dest.setTag(n.getKey()); // set previous
					queue.add(dest);
				}
			}
		}
	}

	// run a DFS to find all reachable vertices
	private Set<Integer> reachableFrom(int src) {
		Set<Integer> visited = new HashSet<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add(src);
		while (queue.size() > 0) {
			int current = queue.poll();
			Collection<edge_data> edges = this.g.getE(current);
			for (edge_data edge : edges) {
				int dest = edge.getDest();
				if (!visited.contains(dest)) {
					visited.add(dest);
					queue.add(dest);
				}
			}
		}
		return visited;
	}

}
