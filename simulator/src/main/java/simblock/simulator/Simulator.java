/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Modifications copyright (C) 2021 University of Pisa, Dept. of Ingegneria dell'Informazione
 */

package simblock.simulator;

import static simblock.simulator.Timer.getCurrentTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import simblock.block.Block;
import simblock.node.Node;
import java.net.URI;
import java.net.URISyntaxException;
/**
 * Import immutable arbitrary-precision integers.
 */
import java.math.BigInteger;
/**
 * Writes text to a character-output stream
 */
import java.io.BufferedWriter;
/**
 * Abstract representation of file and directory pathnames.
 */
import java.io.File;
/**
 * Convenience class for writing character files
 */
import java.io.FileWriter;
/**
 * Signals that an I/O exception of some sort has occurred
 */
import java.io.IOException;
/**
 * Prints formatted representations of objects to a text-output stream
 */
import java.io.PrintWriter;
/**
 * Date-time without a time-zone in the ISO-8601 calendar system
 */
import java.time.LocalDateTime;
 
/**
 * The type Simulator is tasked with maintaining the list of simulated nodes and managing the
 * block interval. It observes and manages the arrival of new blocks at the simulation level.
 */
public class Simulator {

  /**
   * A list of nodes that will be used in a simulation.
   */
  private static final ArrayList<Node> simulatedNodes = new ArrayList<>();

  /**
   * The target block interval in milliseconds.
   */
  private static long targetInterval;

  /**
   * Path to config file.
   */
  public static URI CONF_FILE_URI;

  /**
   * Output path.
   */
  public static URI OUT_FILE_URI;

  static {
    try {
      CONF_FILE_URI = ClassLoader.getSystemResource("simulator.conf").toURI();
      OUT_FILE_URI = CONF_FILE_URI.resolve(new URI("../output/"));
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get simulated nodes list.
   *
   * @return the array list
   */
  public static ArrayList<Node> getSimulatedNodes() {
    return simulatedNodes;
  }

  /**
   * Get target block interval.
   *
   * @return the target block interval in milliseconds
   */
  public static long getTargetInterval() {
    return targetInterval;
  }

  /**
   * Sets the target block interval.
   *
   * @param interval - block interval in milliseconds
   */
  public static void setTargetInterval(long interval) {
    targetInterval = interval;
  }

  /**
   * Add node to the list of simulated nodes.
   *
   * @param node the node
   */
  public static void addNode(Node node) {
    simulatedNodes.add(node);
  }

  /**
   * Remove node from the list of simulated nodes.
   *
   * @param node the node
   */
  @SuppressWarnings("unused")
  public static void removeNode(Node node) {
    simulatedNodes.remove(node);
  }

  /**
   * Add node to the list of simulated nodes and immediately try to add the new node as a
   * neighbor to all simulated
   * nodes.
   *
   * @param node the node
   */
  @SuppressWarnings("unused")
  public static void addNodeWithConnection(Node node) {
    node.joinNetwork();
    addNode(node);
    for (Node existingNode : simulatedNodes) {
      existingNode.addNeighbor(node);
    }
  }

  /**
   * A list of observed {@link Block} instances.
   */
  private static final ArrayList<Block> observedBlocks = new ArrayList<>();

  /**
   * A list of observed block propagation times. The map key represents the id of the node that
   * has seen the
   * block, the value represents the difference between the current time and the block minting
   * time, effectively
   * recording the absolute time it took for a node to witness the block.
   */
  private static final ArrayList<LinkedHashMap<Integer, BigInteger>> observedPropagations =
      new ArrayList<>();

  private static FileWriter fw = null;

  private static PrintWriter pw = null;

  /**
   * Handle the arrival of a new block. For every observed block, propagation information is
   * updated, and for a new
   * block propagation information is created.
   *
   * @param block the block
   * @param node  the node
   */
  public static void arriveBlock(Block block, Node node) {
    // If block is already seen by any node
    if (observedBlocks.contains(block)) {
      // Get the propagation information for the current block
      LinkedHashMap<Integer, BigInteger> propagation = observedPropagations.get(
          observedBlocks.indexOf(block)
      );
      // Update information for the new block
      propagation.put(node.getNodeID(), getCurrentTime().subtract(block.getTime()));
    } else {
      // If the block has not been seen by any node and there is no memory allocated
      //TODO move magic number to constant
      if (observedBlocks.size() > 10) {
        if (fw == null && pw == null) {
          try {
            fw = new FileWriter(new File(OUT_FILE_URI.resolve("./latency_" + LocalDateTime.now().toString() + ".txt")), true);
            pw = new PrintWriter(new BufferedWriter(fw));
            //pw.println(block + ":" + block.getHeight());
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
        // After the observed blocks limit is reached, log and remove old blocks by FIFO principle
        printPropagation(observedBlocks.get(0), observedPropagations.get(0), pw);
        observedBlocks.remove(0);
        observedPropagations.remove(0);
      }
      // If the block has not been seen by any node and there is additional memory
      LinkedHashMap<Integer, BigInteger> propagation = new LinkedHashMap<>();
      propagation.put(node.getNodeID(), getCurrentTime().subtract(block.getTime()));
      // Record the block as seen
      observedBlocks.add(block);
      // Record the propagation time
      observedPropagations.add(propagation);
    }
  }

  /**
   * Print propagation information about the propagation of the provided block  in the format:
   *
   * <p><em>node_ID, propagation_time</em>
   *
   * <p><em>propagation_time</em>: The time from when the block of the block ID is generated to
   * when the
   * node of the <em>node_ID</em> is reached.
   *
   * @param block       the block
   * @param propagation the propagation of the provided block as a list of {@link Node} IDs and
   *                    propagation times
   */
  public static void printPropagation(Block block, LinkedHashMap<Integer, BigInteger> propagation, PrintWriter printW) {
    // Print block and its height
    //TODO block does not have a toString method, what is printed here
    //System.out.println(block + ":" + block.getHeight());
    

    for (Map.Entry<Integer, BigInteger> timeEntry : propagation.entrySet()) {
      //System.out.println(timeEntry.getKey() + "," + timeEntry.getValue());
      if (timeEntry.getValue().compareTo(new BigInteger("0")) == 0)
        continue;
      printW.println(timeEntry.getValue());
    }

    //System.out.println();

 
  }

  /**
   * Print propagation information about all blocks, internally relying on
   * {@link Simulator#printPropagation(Block, LinkedHashMap)}.
   */
  public static void printAllPropagation() {
    if (fw == null && pw == null) {
      try {
        fw = new FileWriter(new File(OUT_FILE_URI.resolve("./latency_" + LocalDateTime.now().toString() + ".txt")), true);
        pw = new PrintWriter(new BufferedWriter(fw));
        //pw.println(block + ":" + block.getHeight());
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
    for (int i = 0; i < observedBlocks.size(); i++) {
      printPropagation(observedBlocks.get(i), observedPropagations.get(i), pw);
    }

    pw.close();
  }
}
