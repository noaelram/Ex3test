package gameClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;
import gameClient.MyGameGUI.Fruit;
import gameClient.MyGameGUI.Robot;

public class KML_Logger {

	// the filename to save to (without KML extension)
	private String fileName;

	// KML lib objects
	private Kml kml;
	private Document doc;
	private Folder folderStatic;
	private Folder folderDynamic;

	// some iconds that we are going to use
	private final String robot1icon = "http://maps.google.com/mapfiles/kml/paddle/1.png";
	private final String robot2icon = "http://maps.google.com/mapfiles/kml/paddle/2.png";
	private final String robot3icon = "http://maps.google.com/mapfiles/kml/paddle/3.png";
	private final String robot4icon = "http://maps.google.com/mapfiles/kml/paddle/4.png";
	private final String robot5icon = "http://maps.google.com/mapfiles/kml/paddle/5.png";
	private final String[] robotsicons = new String[] { robot1icon, robot2icon, robot3icon, robot4icon, robot5icon };
	private final String vertexicon = "http://maps.google.com/mapfiles/kml/pal5/icon21.png";
	private final String appleicon = "http://maps.google.com/mapfiles/kml/pal4/icon50.png";
	private final String banannaicon = "http://maps.google.com/mapfiles/kml/pal4/icon47.png";

	// a c'tor to initiate the fields
	public KML_Logger(String filename) {
		fileName = filename;
		kml = new Kml();
		doc = kml.createAndSetDocument().withName("Game Map").withOpen(true);
		folderStatic = doc.createAndAddFolder();
		folderStatic.withName("Static").withOpen(true);
		folderDynamic = doc.createAndAddFolder();
		folderDynamic.withName("Dynamic").withOpen(true);
	}

	// save the file to disk
	public void outputToFile() throws FileNotFoundException {
		// print and save
		kml.marshal(new File("docs/" + fileName + ".kml"));
	}

	// given a graph, add all static info to KML file, like: nodes
	// and init all styles we are going to use for robots, fruits and nodes
	public void addStaticData(DGraph gameGraph) {
		if (gameGraph == null)
			return;

		// node style
		Icon icon = new Icon().withHref(vertexicon);
		Style style = doc.createAndAddStyle();
		style.withId("style_vertex") // set the stylename to use this style from the placemark
				.createAndSetIconStyle().withScale(1.0).withIcon(icon); // set size and icon
		style.createAndSetLabelStyle().withColor("ff43b3ff").withScale(0.5); // set color and size of the continent name

		// apple style
		icon = new Icon().withHref(appleicon);
		style = doc.createAndAddStyle();
		style.withId("style_apple") // set the stylename to use this style from the placemark
				.createAndSetIconStyle().withScale(1.0).withIcon(icon); // set size and icon
		style.createAndSetLabelStyle().withColor("ff43b3ff").withScale(0.3); // set
		// color and size of the continent name

		// banana style
		icon = new Icon().withHref(banannaicon);
		style = doc.createAndAddStyle();
		style.withId("style_banana") // set the stylename to use this style from the placemark
				.createAndSetIconStyle().withScale(1.0).withIcon(icon); // set size and icon
		style.createAndSetLabelStyle().withColor("ff43b3ff").withScale(0.3); // set
		// color and size of the continent name

		// robots style
		for (int i = 1; i <= 5; i++) {
			icon = new Icon().withHref(robotsicons[i - 1]);
			style = doc.createAndAddStyle();
			style.withId("style_robot" + i) // set the stylename to use this style from the placemark
					.createAndSetIconStyle().withScale(1.0).withIcon(icon); // set size and icon
			style.createAndSetLabelStyle().withColor("ff43b3ff").withScale(0.5); // set color and size of the continent
																					// name
		}

		// update static unchangeable graph data
		for (node_data n : gameGraph.getV()) {
			Placemark placemark = folderStatic.createAndAddPlacemark();
			placemark.withName("Node " + String.valueOf(n.getKey())).withStyleUrl("#style_vertex").createAndSetLookAt()
					.withLongitude(n.getLocation().x()).withLatitude(n.getLocation().y()).withAltitude(0);// .withRange(12000000);
			placemark.createAndSetPoint().addToCoordinates(n.getLocation().x(), n.getLocation().y());
		}
	}

	// given a time left for the game t, create a legal timestamp for KML file
	private String getTimeStamp(long t) {
		// sec = a second from 0 to 60
		long sec = 60 - (60000 - t) / 1000;
		String s = "";
		// check if need to add leading zero
		if (0 <= sec && sec <= 9) {
			s = "0";
		}
		s += sec;
		return "2020-01-25T07:30:" + s + "Z";
	}

	// add dynamic data like robots and fruits
	// this function is called every 100 miliseconds in the game
	// t = the time left to the game (miliseconds)
	public void addDynamicData(List<Robot> robots, List<Fruit> fruits, long t) {
		// set robots location
		if (robots != null) {
			// for every robot add a placemark with timestamp for 100 miliseconds and use
			// the style we defined for it
			for (Robot r : robots) {
				Placemark placemark = folderDynamic.createAndAddPlacemark();
				placemark.withName("Robot " + String.valueOf(r.id)).withStyleUrl("#style_robot" + (r.id + 1))
						.createAndSetLookAt().withLongitude(r.pos.x()).withLatitude(r.pos.y()).withAltitude(0);
				// set coordinates
				placemark.createAndSetPoint().addToCoordinates(r.pos.x(), r.pos.y());
				// define the timestamp
				TimeSpan ts = placemark.createAndSetTimeSpan();
				ts.setBegin(getTimeStamp(t));
				ts.setEnd(getTimeStamp(t + 100));
			}
		}

		// set fruits location
		if (fruits != null) {
			// for every fruit add a placemark with timestamp for 100 miliseconds and use
			// the style we defined for it
			for (Fruit f : fruits) {
				Placemark placemark = folderDynamic.createAndAddPlacemark();
				placemark.withName((f.isApple ? "apple" : "banana"))
						.withStyleUrl("#style_" + (f.isApple ? "apple" : "banana")).createAndSetLookAt()
						.withLongitude(f.pos.x()).withLatitude(f.pos.y()).withAltitude(0);
				// set coordinates
				placemark.createAndSetPoint().addToCoordinates(f.pos.x(), f.pos.y());
				// define the timestamp
				TimeSpan ts = placemark.createAndSetTimeSpan();
				ts.setBegin(getTimeStamp(t));
				ts.setEnd(getTimeStamp(t + 100));
			}
		}
	}
}