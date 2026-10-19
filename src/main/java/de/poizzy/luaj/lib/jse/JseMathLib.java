/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package de.poizzy.luaj.lib.jse;

import static de.poizzy.luaj.LuaValue.*;

import de.poizzy.luaj.lib.MathLib;
import de.poizzy.luaj.Globals;
import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.function.LibFunction;
import de.poizzy.luaj.lib.LuaFunction;

/**
 * Subclass of {@link LibFunction} which implements the lua standard
 * {@code math} library.
 * <p>
 * platform. See {@link MathLib} for the exception list.
 * <p>
 * Typically, this library is included as part of a call to
 * {@link JsePlatform#standardGlobals()}
 *
 * <pre>
 * {
 * 	&#64;code
 * 	Globals globals = JsePlatform.standardGlobals();
 * 	System.out.println(globals.get("math").get("sqrt").call(LuaValue.valueOf(2)));
 * }
 * </pre>
 * <p>
 * For special cases where the smallest possible footprint is desired, a minimal
 * set of libraries could be loaded directly via {@link Globals#load(LuaValue)}
 * using code such as:
 *
 * <pre>
 * {
 * 	&#64;code
 * 	Globals globals = new Globals();
 * 	globals.load(new JseBaseLib());
 * 	globals.load(new PackageLib());
 * 	globals.load(new JseMathLib());
 * 	System.out.println(globals.get("math").get("sqrt").call(LuaValue.valueOf(2)));
 * }
 * </pre>
 * <p>
 * However, other libraries such as <em>CoroutineLib</em> are not loaded in this
 * case.
 * <p>
 * This has been implemented to match as closely as possible the behavior in the
 * corresponding library in C.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see JseMathLib
 * @see <a href="http://www.lua.org/manual/5.2/manual.html#6.6">Lua 5.2 Math Lib
 * Reference</a>
 */
public class JseMathLib extends MathLib {

    public JseMathLib() {
    }

    /**
     * Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied
     * environment, adding the table to package.loaded, and returning table as
     * the return value.
     * <p>
     * Specifically, adds all library functions that can be implemented directly
     * and tanh.
     *
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env     the environment to load into, which must be a Globals
     *                instance.
     */
    @LuaFunction
    public Varargs acos(Varargs a) {
        return valueOf(Math.acos(a.checkdouble(1)));
    }

    @LuaFunction
    public Varargs asin(Varargs a) {
        return valueOf(Math.asin(a.checkdouble(1)));
    }

    @LuaFunction
    public Varargs atan(Varargs a) {
        return valueOf(Math.atan(a.checkdouble(1)));
    }

    @LuaFunction
    public Varargs atan2(Varargs a) {
        return valueOf(Math.atan2(a.checkdouble(1), a.checkdouble(2)));
    }

    @LuaFunction
    public Varargs cosh(Varargs a) {
        double x = a.checkdouble(1);
        return valueOf((Math.exp(x) + Math.exp(-x)) / 2);
    }

    @LuaFunction
    public Varargs log(Varargs a) {
        return valueOf(Math.log(a.checkdouble(1)));
    }

    @LuaFunction
    public Varargs sinh(Varargs a) {
        double x = a.checkdouble(1);
        return valueOf((Math.exp(x) - Math.exp(-x)) / 2);
    }

    @LuaFunction
    public Varargs tanh(Varargs a) {
        double x = a.checkdouble(1);
        return valueOf(Math.tanh(x));
    }

    public void init(LuaTable self, LuaValue modname, LuaValue env) {
        super.init(self, modname, env);
    }

    /**
     * Faster, better version of pow() used by arithmetic operator ^
     */
    @Override
    public double dpow_lib(double a, double b) {
        return Math.pow(a, b);
    }

}
