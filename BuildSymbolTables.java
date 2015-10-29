package edu.jmu.decaf;

import java.util.*;

/**
 * Static analysis pass to construct symbol tables. Visits an AST, maintaining
 * a stack of active symbol tables and annotating various AST nodes with the
 * appropriate symbol tables.
 */
public class BuildSymbolTables extends StaticAnalysis
{
    /**
     * Stack of symbol tables, representing all active nested scopes.
     */
    protected Deque<SymbolTable> tableStack;

    public BuildSymbolTables()
    {
        tableStack = new ArrayDeque<SymbolTable>();
    }

    /**
     * Return the innermost active symbol table.
     */
    protected SymbolTable getCurrentTable()
    {
        assert(tableStack.size() > 0);
        return tableStack.peek();
    }

    /**
     * Create a new innermost symbol table scope and push it on the stack.
     */
    protected SymbolTable initializeScope()
    {
        SymbolTable table = null;
        if (tableStack.size() > 0) {
            table = new SymbolTable(getCurrentTable());
        } else {
            table = new SymbolTable();
        }
        tableStack.push(table);
        return table;
    }

    /**
     * Pop the stack and move outwards one scope level.
     */
    protected void finalizeScope()
    {
        assert(tableStack.size() > 0);
        tableStack.pop();
    }

    /**
     * Add a symbol for the given function to the current (innermost) scope.
     */
    protected void insertFunctionSymbol(ASTFunction node)
    {
        try {
            List<ASTNode.DataType> ptypes = new ArrayList<ASTNode.DataType>();
            for (ASTFunction.Parameter p : node.parameters) {
                ptypes.add(p.type);
            }
            Symbol symbol = new Symbol(node.name, node.returnType, ptypes);
            getCurrentTable().insert(node.name, symbol);
        } catch (InvalidProgramException ex) {
            addError(ex);
        }
    }

    /**
     * Add a symbol for the given variable to the current (innermost) scope.
     */
    protected void insertVariableSymbol(ASTVariable node)
    {
        try {
            if (node.type == ASTNode.DataType.VOID) {
                throw new InvalidProgramException("Variable '" + node.name + "' cannot be null!");
            }
            if (node.arrayLength == 0) {
                throw new InvalidProgramException("Array '" + node.name + "' must have non-zero length!");
            }
            Symbol symbol = new Symbol(node.name, node.type, node.arrayLength);
            getCurrentTable().insert(node.name, symbol);
        } catch (InvalidProgramException ex) {
            addError(ex);
        }
    }

    /**
     * Add a symbol for the given function parameter to the current (innermost) scope.
     */
    protected void insertParamSymbol(ASTFunction.Parameter p)
    {
        try {
            Symbol symbol = new Symbol(p.name, p.type);
            getCurrentTable().insert(p.name, symbol);
        } catch (InvalidProgramException ex) {
            addError(ex);
        }
    }
    
    public void preVisit(ASTProgram node)
    {
    	node.annotations.put("symbolTable", initializeScope());
    }
    
    public void postVisit(ASTProgram node)
    {
    	finalizeScope();
    }
    
    public void preVisit(ASTVariable node)
    {

    	insertVariableSymbol(node);
    }
    
    public void preVisit(ASTBlock node)
    {
    	node.annotations.put("symbolTable", initializeScope());

    }
    
    public void postVisit(ASTBlock node)
    {
    	finalizeScope();
    }
    
    public void preVisit(ASTFunction node)
    {
    	node.annotations.put("symbolTable", initializeScope());
    	insertFunctionSymbol(node);

    	for(ASTFunction.Parameter p : node.parameters)
    	{
    		insertParamSymbol(p);
    	}
    }
    
    public void postVisit(ASTFunction node)
    {
    	finalizeScope();
    }

    
}
