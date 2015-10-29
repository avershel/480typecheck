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
     * If operator should compute boolean result return
     * true, else return false;
     * 
     * @param b is binary operator
     * @return true if is boolean operator
     */
    public boolean boolOp(ASTBinaryExpr.BinOp b)
    {
    	return ((b == ASTBinaryExpr.BinOp.AND) || (b == ASTBinaryExpr.BinOp.OR) || (b == ASTBinaryExpr.BinOp.EQ)
    			|| (b == ASTBinaryExpr.BinOp.GE) || (b == ASTBinaryExpr.BinOp.GT) || (b == ASTBinaryExpr.BinOp.LE)
    			|| (b == ASTBinaryExpr.BinOp.LT) || (b == ASTBinaryExpr.BinOp.NE));
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
    				if (!f.parameters.isEmpty())
    				{
    					addError("Main function cannot have parameters " + f.getSourceInfo().toString());
    				}
    				addError("Main function must return type int " + f.getSourceInfo().toString());
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
    	
    	if(node.returnType == ASTNode.DataType.VOID)
    	{
    		ASTBlock b = node.body;
    		for(ASTStatement s : b.statements)
    		{
    			if(s instanceof ASTReturn)
    			{
    				ASTReturn r = (ASTReturn)s;
    				i++;
    				
    				if(i > 1)
    				{
    					addError("Function illegally contains multiple return statements " + node.returnType.toString());
    				}
    				addError("Function should have no return value " + r.getSourceInfo().toString());
    			}
    		}
    	}
    	else
    	{
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
    				
    				if(getType(r.value) != node.returnType)
    				{
    					addError("Function must return type " + node.returnType.toString()
    					+ " " + r.getSourceInfo().toString());
    				}
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
     * Type inferencing.
     * 
     * @param ex
     * @return
     */
    public ASTNode.DataType getType(ASTBinaryExpr ex)
    {
    	System.out.println("getting type of -------->" + ex.toString());

    	if (getType(((ASTBinaryExpr)ex).leftChild) != getType(((ASTBinaryExpr)ex).rightChild))
    	{
    		addError("Values must be of same type " + ex.getSourceInfo().toString());
    	}
    	else if (boolOp(((ASTBinaryExpr)ex).operator))
    	{
    		return ASTNode.DataType.BOOL;
    	} else if (mathOp(((ASTBinaryExpr)ex).operator))
    	{
    		if ((getType(((ASTBinaryExpr)ex).leftChild) == ASTNode.DataType.INT)
    				&& (getType(((ASTBinaryExpr)ex).rightChild) == ASTNode.DataType.INT))
    		{
    			return ASTNode.DataType.INT;
    		}
    	}
    	return null;
    }
     
    public ASTNode.DataType getType(ASTExpression ex)
    {
    	System.out.println("getting type of -------->" + ex.toString());

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
    
    
    public boolean mathOp(ASTBinaryExpr.BinOp b)
    {
    	return ((b == ASTBinaryExpr.BinOp.AND) || (b == ASTBinaryExpr.BinOp.DIV) || (b == ASTBinaryExpr.BinOp.MOD)
    			|| (b == ASTBinaryExpr.BinOp.MUL) || (b == ASTBinaryExpr.BinOp.SUB));
    }

    /**
     * Type check for ASTProgram nodes.
     */
    public void postVisit(ASTProgram node)
    {
    	funcs = node.functions;
    	vars = node.variables;
    	
    	if (!checkForMain(funcs))
    	{
    		addError("Program must contain a main function");
    	}
    }
  
    public void postVisit(ASTFunction node)
    {
    	System.out.println("visitng function");
    	try 
    	{
    		lookupSymbol(node, node.name);
    	} catch (InvalidProgramException ipe)
    	{
    		addError("Duplicate function names " + node.getSourceInfo().toString());
    	}
    	
    	checkReturnTypes(node);
    }
    
    public void postVisit(ASTAssignment node)
    {
    	System.out.println("visitng assignment");

    	ASTLocation loc = node.location;
    	ASTExpression ex = node.value;
    	if(getType(loc) != getType(ex))
    	{
    		addError("Must assign value of the same type " + node.getSourceInfo().toString());
    	}
    }
    
    /**
     * Type check for conditionals.
     */
    public void postVisit(ASTConditional node)
    {
    	System.out.println("visitng conditional");

    	if (getType(node.condition) != ASTNode.DataType.BOOL)
    	{
    		addError("Condtionals must test for boolean values " + node.condition.getSourceInfo().toString());
    	}
    }
    
    public void postVisit(ASTWhileLoop node)
    {
    	if (getType(node.guard) != ASTNode.DataType.BOOL)
    	{
    		addError("While loops must test for boolean values " + node.guard.getSourceInfo().toString());
    	}
    }

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

    			if(!vars.contains(new ASTVariable(node.name, lookupSymbol(node, node.name).type, lookupSymbol(node, node.name).length)))
    			{
    				addError("Arrays must only be declared in global scope");

    			}
    		}
    		return lookupSymbol(node, node.name).type;
    	} catch (InvalidProgramException e) {
    		addError("Symbol not found:  " + node.name);
    		return null;
    	}
    }
    
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

    public ASTNode.DataType getType(ASTLiteral node) {
    	return node.type;
    }

    public ASTNode.DataType getType(ASTFunctionCall node) {
    	System.out.println("FUNCTION CALL = " + node.toString());
    	try {
    		return lookupSymbol(node, node.name).type;
    	} catch (InvalidProgramException e) {
    		addError("Method not found:  " + node.name);
    		return null;
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
    	    		if(node.getParent() instanceof ASTWhileLoop)
    	    		{
    	    			
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
}
