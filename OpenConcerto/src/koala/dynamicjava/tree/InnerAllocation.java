/*
 * DynamicJava - Copyright (C) 1999 Dyade
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL DYADE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * Except as contained in this notice, the name of Dyade shall not be used in advertising or
 * otherwise to promote the sale, use or other dealings in this Software without prior written
 * authorization from Dyade.
 */

package koala.dynamicjava.tree;

import java.util.List;

import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This class represents the inner allocation nodes of the syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/04/25
 */

public class InnerAllocation extends Allocation implements ExpressionStatement, ExpressionContainer {
    /**
     * The arguments property name
     */
    public final static String ARGUMENTS = "arguments";

    /**
     * The outer object expression
     */
    private Expression expression;

    /**
     * The arguments to pass to the constructor
     */
    private List arguments;

    /**
     * Initializes the expression
     * 
     * @param exp the outer object
     * @param tp the type prefix
     * @param args the arguments of the constructor. null if no arguments.
     * @exception IllegalArgumentException if exp is null or tp is null
     */
    public InnerAllocation(final Expression exp, final Type tp, final List args) {
        this(exp, tp, args, null, 0, 0, 0, 0);
    }

    /**
     * Initializes the expression
     * 
     * @param exp the outer object
     * @param tp the type prefix
     * @param args the arguments of the constructor. null if no arguments.
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if exp is null or tp is null
     */
    public InnerAllocation(final Expression exp, final Type tp, final List args, final String fn, final int bl, final int bc, final int el, final int ec) {
        super(tp, fn, bl, bc, el, ec);

        if (exp == null) {
            throw new IllegalArgumentException("exp == null");
        }

        this.expression = exp;
        this.arguments = args;
    }

    /**
     * Returns the outer class instance expression
     */
    public Expression getExpression() {
        return this.expression;
    }

    /**
     * Sets the outer class instance expression
     * 
     * @exception IllegalArgumentException if e is null
     */
    public void setExpression(final Expression e) {
        if (e == null) {
            throw new IllegalArgumentException("e == null");
        }

        firePropertyChange(EXPRESSION, this.expression, this.expression = e);
    }

    /**
     * Returns the constructor arguments.
     * 
     * @return null if there is no argument.
     */
    public List getArguments() {
        return this.arguments;
    }

    /**
     * Sets the constructor arguments.
     */
    public void setArguments(final List l) {
        firePropertyChange(ARGUMENTS, this.arguments, this.arguments = l);
    }

    /**
     * Allows a visitor to traverse the tree
     * 
     * @param visitor the visitor to accept
     */
    @Override
    public Object acceptVisitor(final Visitor visitor) {
        return visitor.visit(this);
    }
}
