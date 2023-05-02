//import code_swarm.WikiNode;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.LinkedList;
import javax.vecmath.Vector2f;

/**
 * @brief Describe all physicals interactions between nodes (files and persons)
 *
 * Designed for Wikipedia visualization
 *
 * @see PhysicsEngine Physical Engine Interface
 */
public class PhysicsEngineCircle implements PhysicsEngine {

  private Properties cfg;

  private float FORCE_EDGE_MULTIPLIER;
  private float FORCE_CALCULATION_RANDOMIZER;
  private float FORCE_NODES_MULTIPLIER;
  private float FORCE_TO_SPEED_MULTIPLIER;
  private float SPEED_TO_POSITION_MULTIPLIER;

  /**
   * Method for initializing parameters.
   * @param c The code_swarm object that we are using.
   * @param p Properties from the config file.
   */
  public void setup(code_swarm c, Properties p) {
    cfg = p;
    FORCE_EDGE_MULTIPLIER = Float.parseFloat(cfg.getProperty("edgeMultiplier", "1.0"));
    FORCE_CALCULATION_RANDOMIZER = Float
        .parseFloat(cfg.getProperty("calculationRandomizer", "0.01"));
    FORCE_NODES_MULTIPLIER = Float.parseFloat(cfg.getProperty("nodesMultiplier", "1.0"));
    FORCE_TO_SPEED_MULTIPLIER = Float.parseFloat(cfg.getProperty("speedMultiplier", "1.0"));
    SPEED_TO_POSITION_MULTIPLIER = Float.parseFloat(cfg.getProperty("drag", "0.5"));
  }

  /**
   * Method to ensure upper and lower bounds
   * @param value Value to check
   * @param min Floor value
   * @param max Ceiling value
   * @return value if between min and max, min if < max if >
   */
  private float constrain(float value, float min, float max) {
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    }

    return value;
  }

  /**
   * Calculate the attractive/repulsive force between a person and one of its file along their link (the edge).
   *
   * @param edge the link between a person and one of its file
   * @return force force calculated between those two nodes
   */
  private Vector2f calculateForceAlongAnEdge(code_swarm.Edge edge) {
    float distance;
    float deltaDistance;
    Vector2f force = new Vector2f();
    Vector2f tforce = new Vector2f();

    // distance calculation
    tforce.sub(edge.nodeTo.mPosition, edge.nodeFrom.mPosition);
    distance = tforce.length();
    // absolute value of weight
    int absWeight = Math.abs(edge.nodeTo.changeWords);
    // set the vector tforce length to weight
    tforce.scale(absWeight / distance);
    if (distance > 0) {
      // speed calculation (increase when distance is different from targeted len")
      deltaDistance = (edge.len - distance) / (distance * 3);
      // ponderation using a re-mapping life from 0-255 scale to 0-1.0 range
      // This allows nodes to drift apart as their life decreases.
      deltaDistance *= ((float)edge.life / edge.LIFE_INIT);

      // force projection onto x and y axis
      tforce.scale(deltaDistance*FORCE_EDGE_MULTIPLIER);

      force.set(tforce);
    }

    // 90 degrees rotate
    float rotatedX = -force.y;
    float rotatedY = force.x;
    Vector2f rotatedVector = new Vector2f(rotatedX, rotatedY);

    return rotatedVector;
  }

  /**
   * Calculate the repulsive force between two similar nodes (either files or persons).
   *
   * @param nodeA [in]
   * @param nodeB [in]
   * @return force force calculated between those two nodes
   */
  private Vector2f calculateForceBetweenNodes(code_swarm.Node nodeA, code_swarm.Node nodeB) {
    float lensq;
    Vector2f force = new Vector2f();
    Vector2f normVec = new Vector2f();

    /**
     * Get the distance between nodeA and nodeB
     */
    normVec.sub(nodeA.mPosition, nodeB.mPosition);
    lensq = normVec.lengthSquared();
    /**
     * If there is a collision, which means the distance is zero
     */
    if (lensq == 0) {
      force.set((float) Math.random() * FORCE_CALCULATION_RANDOMIZER,
          (float) Math.random() * FORCE_CALCULATION_RANDOMIZER);
    } else if (lensq < 10000) {
      /**
       * No collision and distance is close enough to actually matter.
       */
      normVec.scale(FORCE_NODES_MULTIPLIER / lensq);
      force.set(normVec);
    }

    return force;
  }

  /**
   * Apply a force to a node, converting acceleration to speed.
   *
   * @param node [in] Node the node to which the force apply
   * @param force [in] force a force Vector representing the force on a node
   *
   */
  private void applyForceToSpeed(code_swarm.Node node, Vector2f force) {
    float dlen;
    Vector2f mod = new Vector2f(force);

    /**
     * Taken from Newton's 2nd law.  F=ma
     */
    dlen = mod.length();
    if (dlen > 0) {
      mod.scale(node.mass * FORCE_TO_SPEED_MULTIPLIER);
      node.mSpeed.add(mod);
    }
  }

  /**
   * Apply a force to a node, converting acceleration to speed.
   *
   * @param node the node to which the force apply
   */
  private void applySpeedToPosition(code_swarm.Node node) {
    float div;
    // This block enforces a maximum absolute velocity.
//    if (node.mSpeed.length() > node.maxSpeed) {
//      Vector2f mag = new Vector2f(node.mSpeed.x / node.maxSpeed, node.mSpeed.y / node.maxSpeed);
//      div = mag.length();
//      node.mSpeed.scale(1 / div);
//    }
    float actualSpeed = node.mSpeed.length();
    float adjustedSpeed = mapToSpeed(actualSpeed);
    node.mSpeed.scale(adjustedSpeed / actualSpeed);
    // This block convert Speed to Position
    node.mPosition.add(node.mSpeed);
//    System.out.println("adjusted speed is " + node.mSpeed.length());

    // Apply no drag
//    node.mSpeed.scale(SPEED_TO_POSITION_MULTIPLIER);
  }

  private void applySpeedToWikiPosition(code_swarm.Node node) {
    float div;
    // This block enforces a maximum absolute velocity.
    if (node.mSpeed.length() > node.maxSpeed) {
      Vector2f mag = new Vector2f(node.mSpeed.x / node.maxSpeed, node.mSpeed.y / node.maxSpeed);
      div = mag.length();
      node.mSpeed.scale(1 / div);
    }
//    float actualSpeed = node.mSpeed.length();
//    float adjustedSpeed = mapToSpeed(actualSpeed);
//    node.mSpeed.scale(adjustedSpeed / actualSpeed);
    // This block convert Speed to Position
    node.mPosition.add(node.mSpeed);

    // Apply no drag
//    node.mSpeed.scale(SPEED_TO_POSITION_MULTIPLIER);
  }

  /**
   * map an integer within 10000 to a size of 0-20
   * @param x
   * @return new speed
   */
  public float mapToSpeed(float x) {
    if (x > 10000)
      return 20;
    else if (x < 0)
      return 0;
    return (float) (x * 20 / 10000);
  }


  public void initializeFrame() {
  }

  public void finalizeFrame() {
  }

  /**
   * Method that allows Physics Engine to modify forces between files and people during the relax stage
   *
   * @param edges the edges to which the force apply (both ends)
   *
   * @return Returns a LinkedList of edges which are still alive.
   *
   */
  public LinkedList<code_swarm.Edge> onRelaxEdges(LinkedList<code_swarm.Edge> edges) {
    for (code_swarm.Edge edge : edges) {
      Vector2f force = new Vector2f();

      // Calculate force between the node "from" and the node "to"
      force = calculateForceAlongAnEdge(edge);

      if (edge.weight < 0) {
        force.negate();
      }

      // transmit (applying) fake force projection to file and person nodes
      applyForceToSpeed(edge.nodeTo, force);
      force.negate(); // force is inverted for the other end of the edge
      applyForceToSpeed(edge.nodeFrom, force);
//      System.out.println(
//          "From " + edge.nodeFrom.name + " to " + edge.nodeTo.name + " force is " + force.length());
//      System.out.println(edge.nodeTo.name + " speed is " + edge.nodeTo.mSpeed.length());
    }
    return edges;
  }

  /**
   * Modify Speed / Position during the relax phase.
   *
   * @param fNodes the nodes to which the force apply
   *
   * @return Returns a LinkedList of file nodes which are still alive.
   *
   */
  public LinkedList<code_swarm.WikiNode> onRelaxNodes(LinkedList<code_swarm.WikiNode> fNodes) {
    for (code_swarm.WikiNode fNode : fNodes) {
      Vector2f forceBetweenFiles = new Vector2f();
      Vector2f forceSummation = new Vector2f();

      // Calculation of repulsive force between persons
      for (code_swarm.WikiNode n : fNodes) {
        if (n != fNode) {
          // elemental force calculation, and summation
          forceBetweenFiles = calculateForceBetweenNodes(fNode, n);
          forceSummation.add(forceBetweenFiles);
        }
      }
      // Apply repulsive force from other files to this Node
      applyForceToSpeed(fNode, forceSummation);
    }
    return fNodes;
  }

  /**
   * Modify Speed / Position during the relax phase.
   *
   * @param pNodes the nodes to which the force apply
   *
   * @return Returns a LinkedList of person nodes which are still alive.
   *
   * @Note Position Change = Speed x Time, Time=1 usually
   */
  public PriorityQueue<code_swarm.PersonNode> onRelaxPeople(PriorityQueue<code_swarm.PersonNode> pNodes) {
    for (code_swarm.PersonNode pNode : pNodes) {
      Vector2f forceBetweenPersons = new Vector2f();
      Vector2f forceSummation = new Vector2f();

      // Calculation of repulsive force between persons
      for (code_swarm.PersonNode p : pNodes) {
        if (p != pNode) {
          // elemental force calculation, and summation
          forceBetweenPersons = calculateForceBetweenNodes(pNode, p);
          forceSummation.add(forceBetweenPersons);
        }
      }
      // Apply repulsive force from other people to this Node
      applyForceToSpeed(pNode, forceSummation);

    }
    return pNodes;
  }

  /**
   * Modify alive elements during the update phase.
   *
   * @param edges the nodes to which the force apply
   *
   * @return Returns a LinkedList of edges which are still alive.
   *
   */
  public LinkedList<code_swarm.Edge> onUpdateEdges(LinkedList<code_swarm.Edge> edges) {
    LinkedList<code_swarm.Edge> stillLiving = new LinkedList<code_swarm.Edge>();

    while (!edges.isEmpty()) {
      code_swarm.Edge edge = edges.removeFirst();
      stillLiving.addLast(edge);
    }
    return stillLiving;
  }

  /**
   * Modify alive elements during the update phase.
   *
   * @param fNodes the file nodes to which the force apply
   *
   * @return Returns a LinkedList of nodes which are still alive.
   *
   */
  public LinkedList<code_swarm.WikiNode> onUpdateNodes(LinkedList<code_swarm.WikiNode> fNodes) {
    LinkedList<code_swarm.WikiNode> stillLiving = new LinkedList<code_swarm.WikiNode>();
    while (!fNodes.isEmpty()) {
      code_swarm.WikiNode fNode = fNodes.removeFirst();
      // Apply Speed to Position on nodes
      applySpeedToWikiPosition(fNode);

      // ensure coherent resulting position
      fNode.mPosition.set(constrain(fNode.mPosition.x, 0.0f, (float) code_swarm.width),
          constrain(fNode.mPosition.y, 0.0f, (float) code_swarm.height));

      stillLiving.addLast(fNode);
    }
    return stillLiving;
  }

  /**
   * Modify alive elements during the update phase.
   *
   * @param pNodes the person nodes to which the force apply
   *
   * @return Returns a LinkedList of nodes which are still alive after the function call.
   *
   */
  public PriorityQueue<code_swarm.PersonNode> onUpdatePeople(
      PriorityQueue<code_swarm.PersonNode> pNodes) {
    PriorityQueue<code_swarm.PersonNode> stillLiving = new PriorityQueue<code_swarm.PersonNode>();
    while (!pNodes.isEmpty()) {
      code_swarm.PersonNode pNode = pNodes.poll();
      // Apply Speed to Position on nodes
      applySpeedToPosition(pNode);

      // ensure coherent resulting position
      pNode.mPosition.set(constrain(pNode.mPosition.x, 0.0f, (float) code_swarm.width),
          constrain(pNode.mPosition.y, 0.0f, (float) code_swarm.height));

      // shortening life
//      if (pNode.decay()) {
//        stillLiving.addLast(pNode);
//      }
      stillLiving.offer(pNode);
    }
    return stillLiving;
  }

  /**
   *
   * @return Vector2f vector holding the starting location for a Person Node
   */
  public Vector2f pStartLocation() {
    Vector2f vec = new Vector2f(code_swarm.width * (float) Math.random(),
        code_swarm.height * (float) Math.random());
    return vec;
  }

  /**
   *
   * @return Vector2f vector holding the starting location for a File Node
   */
  public Vector2f fStartLocation() {
    Vector2f vec = new Vector2f(code_swarm.width * (float) 0.5, code_swarm.height * (float) 0.5);
    return vec;
  }

  /**
   *
   * @param mass Mass of person
   * @return Vector2f vector holding the starting velocity for a Person Node
   */
  public Vector2f pStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass * ((float) Math.random() * 2 - 1),
        mass * ((float) Math.random() * 2 - 1));
    return vec;
  }

  /**
   *
   * @param mass Mass of File Node
   * @return Vector2f vector holding the starting velocity for a File Node
   */
  public Vector2f fStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass * ((float) Math.random() * 2 - 1),
        mass * ((float) Math.random() * 2 - 1));
    return vec;
  }
}

