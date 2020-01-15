package gameClient;

import java.util.Collection;
import java.util.HashMap;

public class DGraph implements graph {

	private HashMap<Integer, node_data> nodes;
	private HashMap<Integer, HashMap<Integer, edge_data>> edges; // src key -> list of dest keys with edge data
	private int edgesCounter;
	private int changes;

	public DGraph() {
		this.nodes = new HashMap<Integer, node_data>();
		this.edges = new HashMap<Integer, HashMap<Integer, edge_data>>();
		this.edgesCounter = 0;
		this.changes = 0;
	}

	@Override
	public node_data getNode(int key) {
		return this.nodes.get(key);
	}

	@Override
	public edge_data getEdge(int src, int dest) {
		return this.edges.get(src) != null ? this.edges.get(src).get(dest) : null;
	}

	@Override
	public void addNode(node_data n) {
		int key = n.getKey();
		this.nodes.put(key, n);
		this.edges.put(key, new HashMap<Integer, edge_data>());
		this.changes++;
	}

	@Override
	public void connect(int src, int dest, double w) {
		if (w < 0) {
			return;
		}
		this.changes++;
		this.edges.get(src).put(dest, new edge(src, dest, w));
		this.edgesCounter++;
	}

	@Override
	public Collection<node_data> getV() {
		return this.nodes.values();
	}

	@Override
	public Collection<edge_data> getE(int node_id) {
		return this.edges.get(node_id) == null ? null : this.edges.get(node_id).values();
	}

	@Override
	public node_data removeNode(int key) {
		// remove the node
		node_data removed = this.nodes.remove(key);
		if (removed == null) {
			// node does not exist in graph
			return null;
		}
		this.changes++;
		// remove all the edges from key
		this.edgesCounter -= this.edges.get(key).size();
		this.edges.remove(key);
		// remove all the edges to key
		for (node_data n : getV()) {
			int src = n.getKey();
			// delete edge (src, key) if exists
			removeEdge(src, key);
		}
		return removed;
	}

	@Override
	public edge_data removeEdge(int src, int dest) {
		HashMap<Integer, edge_data> srcEdges = this.edges.get(src);
		edge_data removed = null;
		if (srcEdges != null) {
			// if edge exist
			removed = srcEdges.remove(dest); // remove it
			if (removed != null) {
				// if removed
				this.changes++;
				this.edgesCounter--;
			}
		}
		return removed;
	}

	@Override
	public int nodeSize() {
		return nodes.size();
	}

	@Override
	public int edgeSize() {
		return edgesCounter;
	}

	@Override
	public int getMC() {
		return this.changes;
	}

}
