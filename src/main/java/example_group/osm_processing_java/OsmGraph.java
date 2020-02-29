package example_group.osm_processing_java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Singletone pattern used
 * Only one instance exists over the whole app
 */

public class OsmGraph {

	// edges id generated by calculacateEdgeId() method below:
	private Map<Long, OsmEdge> edges;
	// access through node id:
	private Map<Long, OsmNode> nodes;
	
	/**
	 * Singleton specific properties:
	 */
	
	private static OsmGraph INSTANCE = new OsmGraph();
	
	private OsmGraph() {
		this.edges = new HashMap<Long, OsmEdge>();
		this.nodes = new HashMap<Long, OsmNode>();
	}
	
	public static OsmGraph getInstance() { 
		return INSTANCE;
	}

	
	/**
	 * Create graph edges
	 * @param ways
	 * @param objects
	 */
	public void parseMapWays(ArrayList<MapWay> ways, Map<Long, MapObject> objects) {
		
		for (MapWay way: ways) {
			
			// Create first edge between the first and the last objects:
			HashMap<Long, MapObject> objectsOnWay = (HashMap<Long, MapObject>) way.getObjects();
			
			if (objectsOnWay.isEmpty() == false && objectsOnWay.size() >= 2) {
				
				OsmNode start = this.selectNode(way.pollFirstObject());
				OsmNode target = this.selectNode(way.pollLastObject());
				
				OsmEdge edge = new OsmEdge(way, start, target);
				this.edges.put(this.calculateEdgeId(edge), edge);
				
				// iterate through other objects on way (first and last one polled):	
				for (MapObject object: way.getObjectsList()) {
					// check whether the object was referenced by other ways:
					if (object.linkCounter > 1) {
						// if so, split way into two edges at this point:
						edge = splitEdgeAt(object, edge, way);
					}
					
				}
			}
		}
		
		// add edges to nodes and calculate final distance:
		for (OsmEdge e: this.getEdgesList()) {
			e.getStartNode().addEdge(e);
			e.getEndNode().addEdge(e);
			// calculate distance:
			e.calculateDistance();
		}
		
	}
	
	/**
	 * get node from nodes to corresponding object
	 * if no node there -> create new node
	 * @param object
	 * @return corresponding node
	 */
	private OsmNode selectNode(MapObject object) {
		OsmNode node = this.nodes.get(object.getID());
		if (node == null) {
			node = new OsmNode(object);
			this.nodes.put(object.getID(), node);
		}
		
		return node;
	}
	
	/**
	 * @param obj object at which edge will be splitted:
	 * @param edge in input: start--target connection
	 * @param baseWay base for new edge
	 * Method creates start-X-target such connection
	 * Where X is a new node created from @param obj
	 * @return the right part of above connection: X-target
	 */
	private OsmEdge splitEdgeAt(MapObject obj, OsmEdge edge, MapWay baseWay) {
		// remove old edge between two nodes:
		this.edges.remove(this.calculateEdgeId(edge));
		// get node at the middle:
		OsmNode node = this.selectNode(obj);
		// connect it to start and target of edge:
		OsmEdge leftEdge = new OsmEdge(baseWay, edge.getStartNode(), node);
		OsmEdge rightEdge = new OsmEdge(baseWay, node, edge.getEndNode());
		
		this.edges.put(this.calculateEdgeId(leftEdge), leftEdge);
		this.edges.put(this.calculateEdgeId(rightEdge), rightEdge);
		
		return rightEdge; 
	}
	
	/**
	 * custom created edges have no id
	 * TODO find more memory-efficient way to id edges
	 * TODO e.g. by using hash values or something
	 * @param edge
	 * @return
	 */
	private Long calculateEdgeId(OsmEdge edge) {
		return (long)(edge.getStartNode().getID()+edge.getEndNode().getID());
	}
	
	/**
	 * Getters:
	 */

	public List<OsmEdge> getEdgesList() {
		return new LinkedList<OsmEdge>(this.edges.values());
	}
	

	public ArrayList<OsmEdge> getEdges() {
		ArrayList<OsmEdge> edges = new ArrayList<OsmEdge>();
		for(OsmNode node : this.nodes.values()) {
			ArrayList<OsmEdge> incidentEdges = node.getIncidentEdges();
			edges.addAll(incidentEdges);
		}
		return edges;
	}
	
	public List<OsmNode> getNodesList() {
		return new LinkedList<OsmNode>(this.nodes.values());
	}

	
}