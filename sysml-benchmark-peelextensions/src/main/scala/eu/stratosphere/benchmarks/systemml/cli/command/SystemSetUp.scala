/**
 * Copyright (C) 2014 TU Berlin (peel@dima.tu-berlin.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.stratosphere.benchmarks.systemml.cli.command.command

import java.lang.{System => Sys}

import net.sourceforge.argparse4j.inf.{Namespace, Subparser}
import org.peelframework.core.beans.system.{Lifespan, System}
import org.peelframework.core.cli.command.Command
import org.peelframework.core.config._
import org.peelframework.core.graph.createGraph
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/** Set up a system. */
@Service("system:setup")
class SystemSetUp extends Command {

  override val help = "set up a system"

  override def register(parser: Subparser) = {
    // arguments
    parser.addArgument("system")
      .`type`(classOf[String])
      .dest("app.system.id")
      .metavar("SYSTEM")
      .help("system bean ID")
  }

  override def configure(ns: Namespace) = {
    // set ns options and arguments to system properties
    Sys.setProperty("app.system.id", ns.getString("app.system.id"))
  }

  override def run(context: ApplicationContext) = {
    val systemID = Sys.getProperty("app.system.id")

    logger.info(s"Setting up system '$systemID' and dependencies with SUITE or EXPERIMENT lifespan")
    val sys = context.getBean(systemID, classOf[System])
    val graph = createGraph(sys)

    // update config
    sys.config = loadConfig(graph, sys)
    for (n <- graph.descendants(sys)) n match {
      case s: Configurable => s.config = sys.config
      case _ => Unit
    }

    // setup
    for (n <- graph.reverse.traverse(); if graph.descendants(sys).contains(n)) n match {
      case s: System => if ((Lifespan.PROVIDED :: Lifespan.SUITE :: Lifespan.EXPERIMENT :: Nil contains s.lifespan) ) {
        if ((Lifespan.PROVIDED :: Nil contains s.lifespan)) {
          s.isUp = false
          s.setUp()
        } else {
          if (!s.isUp) {
            s.setUp()
          }
        }
      }
      case _ => Unit
    }
  }
}