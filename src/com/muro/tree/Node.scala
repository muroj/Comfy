/*
 * 
 */
package com.muro.tree

import scala.collection.mutable.ListBuffer

class Node (val label: Any, val terminal:String = "") {
  var children:ListBuffer[Node] = new ListBuffer()
  var parent:Node = null
}

