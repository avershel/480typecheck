package edu.jmu.decaf;

import java.util.List;
import java.util.ArrayList;

/**
 * Static analysis; perform type checking.
 * 
 * Postvisit routines perform type checking using the Decaf type rules.
 */
public class TypeCheck extends StaticAnalysis
{
	List<ASTFunction> funcs = new ArrayList<ASTFunction>();
	List<ASTVariable> vars = new ArrayList<ASTVariable>();
	
    /**
     * If operator && or ||, should compute boolean result return
     * true, else return false;
     * 
     * @param b is binary operator
     * @return true if is boolean operator
     */
	public boolean boolOp(ASTBinaryExpr.BinOp b)
	{
		return ((b == ASTBinaryExpr.BinOp.AND) || (b == ASTBinaryExpr.BinOp.OR));
	}
	
	/**
	 * If operator == or !=.
	 * @param b
	 * @return
	 */
	public boolean eqOp(ASTBinaryExpr.BinOp b)
	{
		return ((b == ASTBinaryExpr.BinOp.EQ) || (b == ASTBinaryExpr.BinOp.NE));
	}

	/**
	 * If operator is <, >, =>, or <=.
	 * @param b is binary operator
	 * @return true if relational operator
	 */
	public boolean relOp(ASTBinaryExpr.BinOp b)
	{
		return ((b == ASTBinaryExpr.BinOp.GE) || (b == ASTBinaryExpr.BinOp.GT) || (b == ASTBinaryExpr.BinOp.LE)
				|| (b == ASTBinaryExpr.BinOp.LT));
	}
    
	/**
     * Checks for main function in program. If main function exists
     * than checks it adheres to decaf rules.
     * @param funcs is ArrayList of ASTFunction nodes in ASTProgram
     * 		  node
     * @return true if funcs has main function
     */
    public boolean checkForMain(List<ASTFunction> funcs)
    {
    	boolean main = false;

    	for (ASTFunction f : funcs)
    	{
    		if (f.name.equals("main"))
    		{
    			main = true;
    			if (f.returnType != ASTNode.DataType.INT)
    			{
    				addError("Main function must return type int " + f.getSourceInfo().toString());
    			}
    			
				if (!f.parameters.isEmpty())
				{
					addError("Main function cannot have parameters " + f.getSourceInfo().toString());
				}
    		}
    	}    	 	
    	return main;
    }
    
    /**
     * 
     * @param node
     */
    public void checkReturnTypes(ASTFunction node)
    {
    	int i = 0;
    	

    		ASTBlock b = node.body;
    		for(ASTStatement s : b.statements)
    		{
    			if(s instanceof ASTReturn)
    			{
    				i++;
    				
    				if(i > 1)
    				{
    					addError("Function illegally contains multiple return statements " + node.returnType.toString()
    							);
    				}
    				ASTReturn r = (ASTReturn)s;
    				if(node.returnType == ASTNode.DataType.VOID)
    				{
    					if(r.hasValue())
    					{
    						addError("Illegal return statement for void function");
    					}
    				}
    				else if(getType(r.value) != node.returnType)
    				{
    					addError("Function must return type " + node.returnType.toString()
    					+ " " + r.getSourceInfo().toString());
    				}
    			}
    		}
    	
    }
    
    /**
     * Retrieves symbol information for a given symbol name. Searches for
     * symbol tables up the parent tree if there is no table at the given
     * node.
     * @param node {@link ASTNode} to search
     * @param name Decaf symbol name
     * @return Symbol information
     * @throws InvalidProgramException Thrown if the symbol is not found
     */
    public static Symbol lookupSymbol(ASTNode node, String name)
            throws InvalidProgramException
    {
        if (node.annotations.containsKey("symbolTable")) {
            SymbolTable table = (SymbolTable)node.annotations.get("symbolTable");
            return table.lookup(name);
        } else if (node.getParent() != null) {
            return lookupSymbol(node.getParent(), name);
        } else {
            throw new InvalidProgramException("Symbol not found: " + name);
        }
    }

    /**
     * Type inferencing for Binary Expressions.
     * 
     * @param ex is Binary Expression 
     * @return data type computed by expression
     */
    public ASTNode.DataType getType(ASTBinaryExpr ex)
    {
    	// expression are not of same type
    	if (getType(((ASTBinaryExpr)ex).leftChild) != getType(((ASTBinaryExpr)ex).rightChild))
    	{
    		addError("Values must be of same type " + ex.getSourceInfo().toString());
    	}
    	// operators && or || can only act on boolean types
    	else if (boolOp(((ASTBinaryExpr)ex).operator))
    	{
    		return ASTNode.DataType.BOOL;
    	// arithmetic operations can only act on int types
    	} else if (mathOp(((ASTBinaryExpr)ex).operator))
    	{
    		if ((getType(((ASTBinaryExpr)ex).leftChild) == ASTNode.DataType.INT)
    				&& (getType(((ASTBinaryExpr)ex).rightChild) == ASTNode.DataType.INT))
    		{
    			return ASTNode.DataType.INT;
    		}
    	// relational operations can only act on int types
    	} else if (relOp(((ASTBinaryExpr)ex).operator))
    	{
    		if ((getType(((ASTBinaryExpr)ex).leftChild) == ASTNode.DataType.INT)
    				&& (getType(((ASTBinaryExpr)ex).rightChild) == ASTNode.DataType.INT))
    		{
    			return ASTNode.DataType.BOOL;
    		}
    	// equality operations can only act on expression of the same type
    	} else if (eqOp(((ASTBinaryExpr)ex).operator))
    	{
    		if ((getType(((ASTBinaryExpr)ex).leftChild) ==  (getType(((ASTBinaryExpr)ex).rightChild))))
    		{
    			return ASTNode.DataType.BOOL;
    		}

    	}
    	return null;
    }
    
    /**
     * Determines ASTExpression type and passes it
     * to appropriate type inferencing method.
     * @param ex is ASTExpression
     * @return data type of ASTExpression ex
     */
    public ASTNode.DataType getType(ASTExpression ex)
    {
    	if(ex instanceof ASTBinaryExpr)
    	{
    		return getType((ASTBinaryExpr) ex);
    	}else if(ex instanceof ASTUnaryExpr)
    	{
    		return getType((ASTUnaryExpr) ex);

    	}else if(ex instanceof ASTFunctionCall)
    	{
    		return getType((ASTFunctionCall) ex);

    	}else if(ex instanceof ASTLocation)
    	{
    		// check index of ASTLocation
    		return getType((ASTLocation) ex);

    	}else if(ex instanceof ASTLiteral)
    	{
    		return getType((ASTLiteral) ex);
    	}
    	else
    	{
    		addError("Invalid Expression " + ex.getSourceInfo().toString());
    		return null;
    	}
    }
     
    /**
     * If operator is arithmetic operations.
     * @return true if arthmetic operation
     */
    public boolean mathOp(ASTBinaryExpr.BinOp b)
    {
    	return ((b == ASTBinaryExpr.BinOp.ADD) || (b == ASTBinaryExpr.BinOp.DIV) || (b == ASTBinaryExpr.BinOp.MOD)
    			|| (b == ASTBinaryExpr.BinOp.MUL) || (b == ASTBinaryExpr.BinOp.SUB));
    }

    /**
     * Overrides ASTDefaultVisitor postVisit method.
     * Type checks void function call statements.
     * @param node is current ASTVoidFunctionCall node
     */
    public void postVisit(ASTVoidFunctionCall node)
    {
    	for (ASTFunction f : funcs)
    	{
    		if (f.name.equals(node.name))
    		{
    			checkParams(f.parameters, node.arguments);
    			return;
    		}
    	}
    	addError("Calling undeclared function " + node.getSourceInfo().toString());
    }
   
    /**
     * Overrides ASTDefaulyVisitor postVisit method.
     * Type check for ASTProgram nodes.
     * @node is current ASTProgram node
     */
    public void postVisit(ASTProgram node)
    {
    	funcs.addAll(node.functions);
    	vars.addAll(node.variables);
    	
    	if (!checkForMain(funcs))
    	{
    		addError("Program must contain a main function");
    	}
    }
  
    /**
     * Override ASTDefaultVisitor postVisit method.
     * Type checks function header statements.
     * @node is current ASTFunction node
     */
    public void postVisit(ASTFunction node)
    {
    	try 
    	{
    		for(ASTVariable v: node.body.variables)
    		{
    			if(isArray(v))
    			{
    				addError("arrays may only be declared in global scope");
    			}
    		}
    		lookupSymbol(node, node.name);
    	} catch (InvalidProgramException ipe)
    	{
    		addError("Duplicate function names " + node.getSourceInfo().toString());
    	}

    	checkReturnTypes(node);
    }
    
    /**
     * Checks length of variable. If greater than 1
     * return true, else return false.
     * @param v is current ASTVariable node
     * @return true if variable is an array
     */
    public boolean isArray(ASTVariable v)
    {
    	if(v.arrayLength > 1)
    	{
    		return true;

    	}
    	return false;
    }
    
    /**
     * Overrides ASTDefaultVisitor postVisit method.
     * Type checks assignment statements.
     * @param node is current ASTAssignment node
     */
    public void postVisit(ASTAssignment node)
    {
    	ASTLocation loc = node.location;
    	ASTExpression ex = node.value;
    	
    	if(getType(loc) != getType(ex))
    	{
    		addError("Must assign value of the same type " + node.getSourceInfo().toString());
    	}
    }
    
    /**
     * Overrides ASTDefaultVisitor postVisit method.
     * Type check for conditional statements.
     * @node is current ASTConditional node
     */
    public void postVisit(ASTConditional node)
    {
    	if (getType(node.condition) != ASTNode.DataType.BOOL)
    	{
    		addError("Condtionals must test for boolean values " + node.condition.getSourceInfo().toString());
    	}
    }
    
    /**
     * Overrides ASTDefaultVisitor postVisit method.
     * Type checks while statements.
     * @node is current ASTWhileLoop node
     */
    public void postVisit(ASTWhileLoop node)
    {
    	if (getType(node.guard) != ASTNode.DataType.BOOL)
    	{
    		addError("While loops must test for boolean values " + node.guard.getSourceInfo().toString());
    	}
    }

    /**
     * Type inferencing for location expressions.
     * @param node is current ASTLocation node
     * @return data type of location
     */
    public ASTNode.DataType getType(ASTLocation node) {
    	try {
    		if(node.hasIndex())
    		{
    			if(getType(node.index) != ASTNode.DataType.INT)
    			{
    				addError("Index of array must be of type INT");
    			}
    			else
    			{
    				int length = lookupSymbol(node, node.name).length;
    				if(length <=0)
    				{
    					
    					addError("Length of array must be greater than 0");
    				}
    			}
    		}
    		
    		return lookupSymbol(node, node.name).type;
    	} catch (InvalidProgramException e) {
    		addError("Symbol not found:  " + node.name);
    		return null;
    	}
    }
    
    /**
     * Type inferencing for unary expression.
     * @param node is current ASTUnaryExpr node
     * @return data type computed by expression
     */
    public ASTNode.DataType getType(ASTUnaryExpr node) {
    	if (node.operator == ASTUnaryExpr.UnaryOp.NEG) {
    		if (getType(node.child) != ASTNode.DataType.INT) {
    			addError("Can only negate int types " + node.getSourceInfo().toString());
    		}
    	} else if (node.operator == ASTUnaryExpr.UnaryOp.NOT) {
    		if (getType(node.child) != ASTNode.DataType.BOOL) {
    			addError("Can only NOT boolean types " + node.getSourceInfo().toString());
    		}
    	}

    	return getType(node.child);
    }

    /**
     * Type inferencing for literals.
     * @param node is current ASTLiteral node
     * @return data type of literal
     */
    public ASTNode.DataType getType(ASTLiteral node) {
    	return node.type;
    }

    /**
     * Type inferencing for function call expressions.
     * @param node is ASTFunctionCall node
     * @return data type computer by function call
     */
    public ASTNode.DataType getType(ASTFunctionCall node) {
    	try {
    		for (ASTFunction f : funcs)
    		{
    			if (f.name.equals(node.name))
    			{
    				checkParams(f.parameters, node.arguments);
    				return lookupSymbol(node, node.name).type;
    			}
    		}
    		
    	} catch (InvalidProgramException e) {
    		addError("Method not found:  " + node.name);
    		return null;

    	}
    	return null;
    }
    
    /**
     * Check parameters of called function match arguments 
     * in function call.
     * @param p is list of parameters of called function
     * @param args is list of arguments for function call expression
     */
    public void checkParams(List<ASTFunction.Parameter> p, List<ASTExpression> args)
    {
    	for (int i = 0; i < args.size(); i++)
    	{
    		if (p.get(i).type != getType(args.get(i)))
    		{
    			addError("Arguments do not match parameters for function ");
    		}
    	}
    }
    
    public void postVisit(ASTBreak node)
    {
    	if (node.getParent() != null)
    	{
    		if(node.getParent() instanceof ASTBlock)
    		{
    			ASTNode b = node.getParent();
    	    	if (b.getParent() != null)
    	    	{

    	    		if(b.getParent() instanceof ASTWhileLoop)
    	    		{
    	    			return;

    	    		}
    	    		else
    	    		{
    	    			addError("invalid break statement outside whileloop");
    	    		}
    	    	}
	    		else
	    		{
	    			addError("invalid break statement outside whileloop");
	    		}
    		}
    		else
    		{
    			addError("invalid break statement outside whileloop");
    		}
    	}
		else
		{
			addError("invalid break statement outside whileloop");
		}
    }
    
    public void postVisit(ASTContinue node)
    {
    	if (node.getParent() != null)
    	{
    		if(node.getParent() instanceof ASTBlock)
    		{
    			ASTNode b = node.getParent();
    	    	if (b.getParent() != null)
    	    	{

    	    		if(b.getParent() instanceof ASTWhileLoop)
    	    		{
    	    			return;
    	    			//System.out.println("PARENT IS while");

    	    		}
    	    		else
    	    		{
    	    			addError("invalid continue statement outside whileloop");
    	    		}
    	    	}
	    		else
	    		{
	    			addError("invalid continue statement outside whileloop");
	    		}
    		}
    		else
    		{
    			addError("invalid continue statement outside whileloop");
    		}
    	}
		else
		{
			addError("invalid continue statement outside whileloop");
		}
    }
}
