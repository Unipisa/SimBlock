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

package simblock.task;

import static simblock.simulator.Network.getLatency;

import simblock.node.Node;
import java.math.BigInteger;

/**
 * The type Abstract message task.
 */
public abstract class AbstractMessageTask implements Task {
  /**
   * The sending entity.
   */
  private final Node from;
  /**
   * The receiving entity.
   */
  private final Node to;

  /**
   * Instantiates a new Abstract message task.
   *
   * @param from the sending entity
   * @param to   the receiving entity
   */
  public AbstractMessageTask(Node from, Node to) {
    this.from = from;
    this.to = to;
  }

  /**
   * Get the sending node.
   *
   * @return the <em>from</em> node
   */
  public Node getFrom() {
    return this.from;
  }

  /**
   * Get the receiving node.
   *
   * @return the <em>to</em> node
   */
  public Node getTo() {
    return this.to;
  }

  /**
   * Get the message delay with regards to respective regions.
   *
   * @return the message sending interval
   */
  public BigInteger getInterval() {
    BigInteger latency = getLatency(this.from.getRegion(), this.to.getRegion());
    // Add 10 milliseconds here, why?
    //TODO
    BigInteger processing = new BigInteger("10"); 
    return latency.add(processing);
  }

  /**
   * Receive message at the <em>to</em> side.
   */
  public void run() {
    this.to.receiveMessage(this);
  }

}