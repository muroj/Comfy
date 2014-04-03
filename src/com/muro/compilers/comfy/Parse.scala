
package com.muro.compilers.comfy

import com.muro.tree.Node
import com.muro.tree.Tree

/**
 * Parse is responsible for building a parse tree from the specified sequence
 * of tokens.
 */
object Parse {

  def parse(tokenStream: scala.collection.mutable.Queue[Token]):Tree = {

    /**
     * A reference to the token currently being parsed.
     */
    var currentToken = tokenStream.dequeue()

    /**
     * The concrete syntax tree resulting from parse.
     */
    var parseTree:Tree = new Tree()

    /**
     * Parses a program. This method kicks off the parse phase.
     *
     * Program ::== Block $
     */
    def program() = {

      println("Parsing program...")
      
      var head = parseTree.insert(new Node(Production.Program))
      
      block()

      if (isCurrentTokenValid(Tag.T_endOfProgram))
        head.children.append(new Node(Terminal.EndOfProgram))
      else
        println("ERROR: A program should end with the end-of-program marker.")
    }

    /**
     * Parses a block.
     * 
     * Block ::== { StatementList }
     * 
     */
    def block(): Unit = {

      println("Parsing block...")
      
      // As a result of this call, Block is the current node.
      var head = parseTree.insert(new Node(Production.Block))
      
      if (isCurrentTokenValid(Tag.T_openBrace))
        head.children.append(new Node(Terminal.OpenBrace))
      else
        println("ERROR: A block should begin with an opening brace.")

      statementList()

      if (isCurrentTokenValid(Tag.T_closeBrace))
        head.children.append(new Node(Terminal.CloseBrace))
      else
        println("ERROR: A block should end with an closing brace.")
    }

    /**
     * Parses a sequence of statements.
     *
     * StatementList ::== Statement StatementList
     *               ::== emptyList
     */
    def statementList() = {

      println("Parsing statement list...")
            
      // Insert the StatementList node into the parse tree...
      var head = parseTree.insert(new Node(Production.StatementList))
      
      // Parse the list until a closing brace is encountered.
      while (currentToken.tag != Tag.T_closeBrace) {
        statement()
        parseTree.current = head
      }
    }
    
    /**
     * Parses a statement.
     * 
     * Statement ::== PrintStatement
     *           ::== AssignmentStatement
     *           ::== VarDecl
     *           ::== WhileStatement
     *           ::== IfStatement
     *           ::== Block
     */
    def statement() = {

      println("Parsing statement...")
      
      var head = parseTree.insert(new Node(Production.Statement))
      
      if (currentToken.tag == Tag.T_print)
        printStatement()
      else if (currentToken.tag == Tag.T_id)
        assignmentStatement()
      else if (currentToken.tag == Tag.T_type)
        varDecl()
      else if (currentToken.tag == Tag.T_while)
        whileStatement()
      else if (currentToken.tag == Tag.T_if)
        ifStatement()
      else if (currentToken.tag == Tag.T_openBrace)
        block()
      else
        println("ERROR: Expecting a statement.");
    }

    /**
     * Parses a print statement.
     * 
     * PrintStatement ::== print ( Expr )
     */
    def printStatement() = {

      println("Parsing print statement...")
      
      var head = parseTree.insert(new Node(Production.PrintStatement))
      
      if (isCurrentTokenValid(Tag.T_print))
        head.children.append(new Node(Terminal.Print))
      else
        println("ERROR: A print statement should begin with the print keyword.")

      if (isCurrentTokenValid(Tag.T_openParen))
        head.children.append(new Node(Terminal.OpenParen))
      else
        println("ERROR: The print keyword should be followed by an opening parenthesis.")

      expression()

      if (isCurrentTokenValid(Tag.T_closeParen))
        head.children.append(new Node(Terminal.CloseParen))
      else
        println("ERROR: The print keyword should be followed by a closing parenthesis.")
    }

    /**
     * Parses an assignment statement.
     *
     * AssignmentStatement ::== Id = Expr
     */
    def assignmentStatement() = {

      println("Parsing assignment statement...")
      var head = parseTree.insert(new Node(Production.AssignmentStatement))
      
      // Check for an Id
      if (isCurrentTokenValid(Tag.T_id))
        head.children.append(new Node(Production.Id, currentToken.attr))
      else
        println("ERROR: Expecting an ID for an assignment statement.")

      // Check for an equals sign.
      if (isCurrentTokenValid(Tag.T_assignOp))
        head.children.append(new Node(Terminal.AssignmentOp))
      else
        println("ERROR: Expecting an assignment operator.")

      // Parse the right-hand side of the assignment.
      expression()
    }

    /**
     * Parses a variable declaration statement.
     *
     * VarDecl ::== type Id
     */
    def varDecl() = {

      println("Parsing variable declaration statement...")
      var head = parseTree.insert(new Node(Production.VarDecl))
      
      if (isCurrentTokenValid(Tag.T_type))
        head.children.append(new Node(Production.Type, currentToken.attr))
      else
        println("ERROR: Expecting a valid type keyword.")

      if (isCurrentTokenValid(Tag.T_id))
        head.children.append(new Node(Production.Id, currentToken.attr))
      else
        println("ERROR: Expecting an identifier.")
    }

    /**
     * Parses a while statement.
     *
     * WhileStatement ::== while BooleanExpr Block
     */
    def whileStatement() = {

      println("Parsing while statement...")
      var head = parseTree.insert(new Node(Production.WhileStatement))
      
      // A while statement should begin with the while keyword.
      if (isCurrentTokenValid(Tag.T_while))
        head.children.append(new Node(Terminal.While))
      else
        println("ERROR: Expecting \"while\" keyword.")

      // Parse the conditional expression
      booleanExpr()

      // Parse the body of the while loop
      block()
    }

    /**
     * Parse an if statement.
     *
     * IfStatement ::== if BooleanExpr Block
     */
    def ifStatement() = {

      println("Parsing if statement...")
      var head = parseTree.insert(new Node(Production.IfStatement))
      
      if (isCurrentTokenValid(Tag.T_if))
        head.children.append(new Node(Terminal.If))
      else
        println("ERROR: Expecting \"if\" keyword.")

      // Parse the conditional expression
      booleanExpr()

      // Parse the body of the if statement
      block()
    }
    
    /**
     * Parses an expression.
     * 
     * Expr ::== IntExpr
     *      ::== StringExpr
     *      ::== BooleanExpr
     *      ::== Id
     */
    def expression(): Unit = {

      println("Parsing expression...")
      
      var head = parseTree.insert(new Node(Production.Expr))
      
      if (currentToken.tag == Tag.T_numLiteral)
        intExpr()
      else if (currentToken.tag == Tag.T_stringLiteral)
        stringExpr()
      else if (currentToken.tag == Tag.T_openParen ||
        currentToken.tag == Tag.T_boolLiteral)
        booleanExpr()
      else if (currentToken.tag == Tag.T_id) {
        println("Found identifier expression")
        idExpr()
      } else {
        println("ERROR: Expecting an expression.")
      }
      
      // Move the current pointer up to the parent.
      parseTree.current = parseTree.current.parent
    }

    /**
     * Parses an int expression.
     *
     * IntExpr ::== digit intop Expr
     */
    def intExpr() = {

      println("Parsing integer expression...")
      
      var head = parseTree.insert(new Node(Production.IntExpr))
      
      // An integer expression begins with...an integer.
      if (isCurrentTokenValid(Tag.T_numLiteral))
        head.children.append(new Node(Production.Digit, currentToken.attr))
      else
        println("ERROR: Expecting a numeric literal.")

      // Look-ahead to determine whether this is an addition expression.
      if (currentToken.tag == Tag.T_plusOp) {
        head.children.append(new Node(Production.Intop, currentToken.attr))
        currentToken = tokenStream.dequeue
        expression()
      }
    }

    /**
     * Parses a string expression.
     *
     * StringExpr ::== " CharList "
     */
    def stringExpr() = {

      println("Parsing string expression...")
      
      var head = parseTree.insert(new Node(Production.StringExpr))
      
      if (isCurrentTokenValid(Tag.T_stringLiteral))
        head.children.append(new Node(Production.CharList, currentToken.attr))
      else
        println("ERROR: Expecting a string constant.")
    }

    /**
     * Parses a boolean expresion.
     *
     * BooleanExpr ::== ( Expr boolop Expr )
     *             ::== boolval
     */
    def booleanExpr() = {

      println("Parsing boolean expression...")
      
      var head = parseTree.insert(new Node(Production.BooleanExpr))
      
      // Determine whether this is a bool literal...
      if (currentToken.tag == Tag.T_boolLiteral) {
        head.children.append(new Node(Production.Boolval, currentToken.attr))
        currentToken = tokenStream.dequeue
      } else {
        // ...or a boolean expression.
        if (isCurrentTokenValid(Tag.T_openParen))
          head.children.append(new Node(Terminal.OpenParen))
        else
          println("ERROR: A boolean expression begins with an open parenthesis.")
        
        expression()
        
        parseTree.current = parseTree.current.parent
        
        if (isCurrentTokenValid(Tag.T_boolOp))
          head.children.append(new Node(Production.Boolop, currentToken.attr))
        else
          println("ERROR: A boolean expression must contain a boolean operator.")
        
        expression()
        
        parseTree.current = parseTree.current.parent
        
        if (isCurrentTokenValid(Tag.T_closeParen))
          head.children.append(new Node(Terminal.CloseParen))
        else
          println("ERROR: A boolean expression ends with a closing parenthesis.")
      }
    }
    
    def idExpr() {
      
      println("Parsing identifier expression...")
      
      var head = parseTree.insert(new Node(Production.IdExpr))
      
      head.children.append(new Node(Production.Id, currentToken.attr))
      
      currentToken = tokenStream.dequeue
    }

    /**
     * Determines whether the current token matches the specified expected token.
     */
    def isCurrentTokenValid(expected: Tag.Value): Boolean = {
      
      println("Expecting " + Tag.getDescription(expected))
      println("Found " + Tag.getDescription(currentToken.tag))

      // Check whether the current token matches the expected token.
      var result: Boolean = currentToken.tag == expected

      // If so, grab the next token from the stream.
      if (result && !tokenStream.isEmpty) {
        currentToken = tokenStream.dequeue
      }

      // Return the result.
      result
    }
    
    // Kick-off parse
    program()
    
    // Return the resulting CST.
    parseTree
  }
}
