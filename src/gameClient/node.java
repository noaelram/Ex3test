package gameClient;

public class node implements node_data {
	private int key;
	private Point3D location;
	private double weight;
	private String info;
	private int tag;

	public node(node_data n) {
		this.key = n.getKey();
		this.location = n.getLocation() == null ? null : new Point3D(n.getLocation());
		this.weight = n.getWeight();
		this.info = n.getInfo();
		this.tag = n.getTag();
	}

	public node(int key, Point3D location) {
		this.key = key;
		this.location = location == null ? null : new Point3D(location);
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public Point3D getLocation() {
		return location;
	}

	public void setLocation(Point3D location) {
		this.location = location;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}

}
