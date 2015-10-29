package edu.jmu.decaf;

import java.io.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for static/semantic analysis
 */
public class TestAnalysis extends TestCase
{
    /**
     * Initialization
     *
     * @param testName name of the test case
     */
    public TestAnalysis(String testName)
    {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite(TestAnalysis.class);
    }

    /**
     * Parse source code with invalid syntax; should throw an exception
     * @param text Decaf source code
     */
    protected static void assertInvalid(String text)
    {
        StaticAnalysis.resetErrors();
        boolean passed = false;
        try {
            ASTProgram program = (new MyDecafParser()).parse(
                      (new MyDecafLexer()).lex(text));
            program.traverse(new BuildParentLinks());
            program.traverse(new BuildSymbolTables());
            program.traverse(new TypeCheck());
            String errors = StaticAnalysis.getErrorString();
            if (errors.length() > 0) {
                throw new InvalidProgramException(errors);
            }
        } catch (IOException ex) {
        } catch (InvalidTokenException ex) {
        } catch (InvalidSyntaxException ex) {
        } catch (InvalidProgramException ex) {
            passed = true;
        }
        assertTrue(passed);
    }

    /**
     * Parse source code with valid syntax (often a utility method for another,
     * more comprehensive test).
     * @param text Decaf source code
     * @return Parsed syntax tree
     */
    protected static ASTProgram parseValidProgram(String text)
    {
        StaticAnalysis.resetErrors();
        ASTProgram program = null;
        try {
            program = (new MyDecafParser()).parse(
                      (new MyDecafLexer()).lex(text));
            program.traverse(new BuildParentLinks());
            program.traverse(new BuildSymbolTables());
            program.traverse(new TypeCheck());
            String errors = StaticAnalysis.getErrorString();
            if (errors.length() > 0) {
                //System.out.println(errors);
                throw new InvalidProgramException(errors);
            }
        } catch (IOException ex) {
            assertTrue(false);
        } catch (InvalidTokenException ex) {
            assertTrue(false);
        } catch (InvalidSyntaxException ex) {
            assertTrue(false);
        } catch (InvalidProgramException ex) {
            assertTrue(false);
        }
        return program;
    }
    
    /**
     * Parse source code with invalid syntax; should not throw an exception
     * and should return a non-null AST node
     * @param text Decaf source code
     */
    protected static void assertValid(String text)
    {
        assertNotNull(parseValidProgram(text));
    }

    public void testEmptyMain()
    {
        ASTProgram program = parseValidProgram("def int main() { return 0; }");
        // valid parse
        assertNotNull(program);
        // contains symbol table
        assertTrue(program.annotations.containsKey("symbolTable"));
        SymbolTable table = (SymbolTable)program.annotations.get("symbolTable");
        try {
            // has "main" symbol
            Symbol mainSym = table.lookup("main");
            // "main" symbol is a function
            assertEquals(mainSym.location, Symbol.MemLoc.STATIC_FUNC);
        } catch (InvalidProgramException ex) {
            assertTrue(false);
        }
    }

    public void testDupGlobalVar()         { assertInvalid("int a; int a;"); }
    public void testCorrectTypes()         { assertValid(  "def int main() { int i; i = 3+4; return 0; }"); }
    public void testExpTypeMismatch()      { assertInvalid("def int main() { int i; i = true+4; return 0; }"); }
    public void testVarTypeMismatch()      { assertInvalid("int x; def int main() { x=false; return 0; }"); }
    public void testInvalidConditional()   { assertInvalid("def int main() { if (6) { return 0; } }"); }
    public void testInvalidBreak()         { assertInvalid("def int main() { break; return 0; } "); }
    public void testInvalidContinue()         { assertInvalid("def int main() { continue; return 0; } "); }
    public void testMismatchedParameters() { assertInvalid("def int main() { foo(true, true); return 0; } " + 
                                                       "def void foo(int i, bool b) { return ; } "); }
    
    public void testCorrectbreak()         { assertValid(  "def int main() { int i; i = 3; while(i < 10){break;} return 0; }"); }
    public void testCorrectContinue()         { assertValid(  "def int main() { int i; i = 3; while(i < 10){continue;} return 0; }"); }
    
    public void testA()         { assertValid("def int main() { bool b; int i; b = true; if (b) { i = 6; } }"); }
    public void testB()         { assertValid("def int main() { if (3 < 4) { return true;} }"); }
    public void testMismatchedParameters2() { assertInvalid("int a; def int main() { a = foo(true, true); return 0; } " + 
            					"def int foo(int c, bool b) { return 3; } "); }
}
