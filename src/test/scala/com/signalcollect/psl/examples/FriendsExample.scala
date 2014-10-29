/*
 *  @author Philip Stutz
 *  @author Sara Magliacane
 *
 *  Copyright 2014 University of Zurich & VU University Amsterdam
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.signalcollect.psl.examples

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.signalcollect.TestAnnouncements

import com.signalcollect.admm.Wolf
import com.signalcollect.psl.Inferencer
import com.signalcollect.psl.InferencerConfig
import com.signalcollect.psl.Grounding
import com.signalcollect.psl.model.GroundedRule
import com.signalcollect.psl.model.GroundedPredicate
import com.signalcollect.psl.model.GroundedRule
import com.signalcollect.psl.model.PredicateInRule
import com.signalcollect.psl.model.PSLToCvxConverter
import com.signalcollect.psl.parser.PslParser
import com.signalcollect.psl.parser.ParsedPslFile

import scala.annotation.tailrec

/**
 * Small example that plays around with default values and contrasting rules.
 */
class FriendsExample extends FlatSpec with Matchers with TestAnnouncements {

  val friends = """
	predicate: 		friends(_, _)
    
  // Gives a default soft truth value of 1/(4+1) to unknown predicates.
  rule [1]: => friends(A,B)
  rule [4]: => !friends(A,B)
    
	fact: friends(anna, bob)
  fact: !friends(bob, carl)
	"""
  "FriendsExample" should "provide a solution consistent for friends, with a default value of 0.2" in {
    val pslData = PslParser.parse(friends)

    val config = InferencerConfig(objectiveLoggingEnabled = true, 
        absoluteEpsilon = 1e-08, relativeEpsilon = 1e-03, isBounded = true)
    val inferenceResults = Inferencer.runInference(pslData, config = config)

    println(inferenceResults)
    val objectiveFunctionVal = inferenceResults.objectiveFun.get

    objectiveFunctionVal should be(3.2 +- 1e-5)
  }

  val freenemies = """
	predicate: friends(_, _)
  // negative weights make a convex problem concave... not converging.
  // is this case it's actually a linear problem, so it works.
  rule [weight = -1, distanceMeasure = linear]: => friends(A,B)
	fact: friends(anna, bob)
  fact: !friends(bob, carl)
	"""
  "FriendsExample" should "provide a solution consistent for freenemies, an example with negative weights" in {
    val pslData = PslParser.parse(freenemies)

    val config = InferencerConfig(objectiveLoggingEnabled = true, 
        absoluteEpsilon = 10e-08, relativeEpsilon = 10e-03, isBounded = true)
    val inferenceResults = Inferencer.runInference(pslData, config = config)

    println(inferenceResults)
    val objectiveFunctionVal = inferenceResults.objectiveFun.get

    objectiveFunctionVal should be(0.0 +- 1e-5)
  }

  // No friends except the explicitly mentioned (as std in CWA).
  val enemies = """
	predicate: 		friends(_, _)
  rule [1]: => !friends(A,B)
	fact: friends(anna, bob)
  fact: !friends(bob, carl)
	"""
  "FriendsExample" should "provide a solution consistent for enemies, an example with negative prior" in {
    val pslData = PslParser.parse(enemies)

    val config = InferencerConfig(objectiveLoggingEnabled = true, 
        absoluteEpsilon = 10e-08, relativeEpsilon = 10e-03, isBounded = true)
    val inferenceResults = Inferencer.runInference(pslData, config = config)

    println(inferenceResults)
    val objectiveFunctionVal = inferenceResults.objectiveFun.get

    objectiveFunctionVal should be(0.0 +- 1e-5)
  }

}
