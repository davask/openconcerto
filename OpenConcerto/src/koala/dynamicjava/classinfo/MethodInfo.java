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

package koala.dynamicjava.classinfo;

/**
 * The instances of the classes that implement this interface provide informations about methods.
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/06/03
 */

public interface MethodInfo {
    /**
     * Returns the modifiers for the method represented by this object
     */
    int getModifiers();

    /**
     * Returns a Class object that represents the return type of the method represented by this
     * object
     */
    ClassInfo getReturnType();

    /**
     * Returns the name of the underlying method
     */
    String getName();

    /**
     * Returns an array of class infos that represent the parameter types, in declaration order, of
     * the method represented by this object
     */
    ClassInfo[] getParameterTypes();

    /**
     * Returns an array of class infos that represent the types of the exceptions declared to be
     * thrown by the underlying method
     */
    ClassInfo[] getExceptionTypes();

}
