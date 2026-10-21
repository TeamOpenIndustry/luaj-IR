/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
package de.poizzy.luaj.lib;

import java.util.Random;

import static de.poizzy.luaj.LuaValue.*;

import de.poizzy.luaj.annotations.LuaFunction;
import de.poizzy.luaj.lib.jse.JseMathLib;
import de.poizzy.luaj.lib.jse.JsePlatform;
import de.poizzy.luaj.LuaDouble;
import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.function.LibFunction;

/**
 * Subclass of {@link LibFunction} which implements the lua standard
 * {@code math} library.
 * <p>
 * complete implementation based on math functions specific to JSE use
 * {@link JseMathLib}. In Particular the following math
 * functions are <b>not</b> implemented by this library:
 * <ul>
 * <li>acos</li>
 * <li>asin</li>
 * <li>atan</li>
 * <li>cosh</li>
 * <li>log</li>
 * <li>sinh</li>
 * <li>tanh</li>
 * <li>atan2</li>
 * </ul>
 * <p>
 * The implementations of {@code exp()} and {@code pow()} are constructed by
 * JSE platform.
 * <p>
 * Typically, this library is included as part of a call to either
 * {@link JsePlatform#standardGlobals()} or
 *
 * <pre>
 * {
 * 	&#64;code
 * 	Globals globals = JsePlatform.standardGlobals();
 * 	System.out.println(globals.get("math").get("sqrt").call(LuaValue.valueOf(2)));
 * }
 * </pre>
 * <p>
 * When using {@link JsePlatform} as in this example, the
 * subclass {@link JseMathLib} will be included, which also
 * includes this base functionality.
 * <p>
 * To instantiate and use it directly, link it into your globals table via
 * {@link LuaValue#load(LuaValue)} using code such as:
 *
 * <pre>
 * {
 * 	&#64;code
 * 	Globals globals = new Globals();
 * 	globals.load(new JseBaseLib());
 * 	globals.load(new PackageLib());
 * 	globals.load(new MathLib());
 * 	System.out.println(globals.get("math").get("sqrt").call(LuaValue.valueOf(2)));
 * }
 * </pre>
 * <p>
 * Doing so will ensure the library is properly initialized and loaded into the
 * globals table.
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
public class MathLib implements LuaModule {

    @Override
    public void init(LuaTable self, LuaValue name, LuaValue env) {
    }

    @Override
    public String name() {
        return "math";
    }

    /**
     * Pointer to the latest MathLib instance, used only to dispatch math.exp to
     * tha correct platform math library.
     */
    public static MathLib MATHLIB = null;
    private Random random = new Random();

    /**
     * Construct a MathLib, which can be initialized by calling it with a
     * modname string, and a global environment table as arguments using
     * {@link #call(LuaValue, LuaValue)}.
     */
    public MathLib() {
        MATHLIB = this;
    }

    /**
     * Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied
     * environment, adding the table to package.loaded, and returning table as
     * the return value.
     *
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env     the environment to load into, typically a Globals
     *                instance.
     */
    // Annotation-bound module exports.
    @LuaFunction
    public Varargs abs(Varargs args) {
        return valueOf(Math.abs(args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs ceil(Varargs args) {
        return valueOf(Math.ceil(args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs cos(Varargs args) {
        return valueOf(Math.cos(args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs deg(Varargs args) {
        return valueOf(Math.toDegrees(args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs exp(Varargs args) {
        return valueOf(dpow_lib(Math.E, args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs floor(Varargs args) {
        return valueOf(Math.floor(args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs fmod(Varargs args) {
        double x = args.checkdouble(1), y = args.checkdouble(2);
        return valueOf(y == 0 ? Double.NaN : x % y);
    }

    @LuaFunction
    public Varargs frexp(Varargs args) {
        double x = args.checkdouble(1);
        if (x == 0)
            return varargsOf(ZERO, ZERO);
        long bits = Double.doubleToLongBits(x);
        double m = ((bits & ~(-1L << 52)) + (1L << 52)) * (bits >= 0 ? .5 / (1L << 52) : -.5 / (1L << 52));
        double e = ((int) (bits >> 52) & 0x7ff) - 1022;
        return varargsOf(valueOf(m), valueOf(e));
    }

    @LuaFunction
    public Varargs ldexp(Varargs args) {
        return valueOf(args.checkdouble(1) * Math.pow(2, args.checkdouble(2)));
    }

    @LuaFunction
    public Varargs max(Varargs args) {
        LuaValue m = args.checknumber(1);
        for (int i = 2, n = args.narg(); i <= n; ++i) {
            LuaValue v = args.checknumber(i);
            if (m.lt_b(v))
                m = v;
        }
        return m;
    }

    @LuaFunction
    public Varargs min(Varargs args) {
        LuaValue m = args.checknumber(1);
        for (int i = 2, n = args.narg(); i <= n; ++i) {
            LuaValue v = args.checknumber(i);
            if (v.lt_b(m))
                m = v;
        }
        return m;
    }

    @LuaFunction
    public Varargs modf(Varargs args) {
        LuaValue n = args.arg1();
        /* number is its own integer part, no fractional part */
        if (n.islong())
            return varargsOf(n.tonumber(), valueOf(0.0));
        double x = n.checkdouble();
        /* integer part (rounds toward zero) */
        double intPart = x > 0 ? Math.floor(x) : Math.ceil(x);
        /* fractional part (test needed for inf/-inf) */
        double fracPart = x == intPart ? 0.0 : x - intPart;
        return varargsOf(valueOf(intPart), valueOf(fracPart));
    }

    @LuaFunction
    public Varargs pow(Varargs args) {
        return valueOf(dpow_d(args.checkdouble(1), args.checkdouble(2)));
    }

    @LuaFunction
    public Varargs random(Varargs args) {
        switch (args.narg()) {
            case 0:
                return valueOf(random.nextDouble());
            case 1:
                int upper = args.checkint(1);
                if (upper < 1) argerror(1, "interval is empty");
                return valueOf(1 + random.nextInt(upper));
            default:
                int lower = args.checkint(1);
                int upperBound = args.checkint(2);
                if (upperBound < lower) argerror(2, "interval is empty");
                return valueOf(lower + random.nextInt(upperBound + 1 - lower));
        }
    }

    @LuaFunction
    public Varargs randomseed(Varargs args) {
        random = new Random(args.checklong(1));
        return NONE;
    }

    @LuaFunction
    public Varargs rad(Varargs args) {
        return valueOf(Math.toRadians(args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs sin(Varargs args) {
        return valueOf(Math.sin(args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs sqrt(Varargs args) {
        return valueOf(Math.sqrt(args.checkdouble(1)));
    }

    @LuaFunction
    public Varargs tan(Varargs args) {
        return valueOf(Math.tan(args.checkdouble(1)));
    }


    /**
     * compute power using installed math library, or default if there is no
     * math library installed
     */
    public static LuaValue dpow(double a, double b) {
        return LuaDouble.valueOf(MATHLIB != null ? MATHLIB.dpow_lib(a, b) : dpow_default(a, b));
    }

    public static double dpow_d(double a, double b) {
        return MATHLIB != null ? MATHLIB.dpow_lib(a, b) : dpow_default(a, b);
    }

    /**
     * Hook to override default dpow behavior with faster implementation.
     */
    public double dpow_lib(double a, double b) {
        return dpow_default(a, b);
    }

    /**
     *
     */
    protected static double dpow_default(double a, double b) {
        if (b < 0)
            return 1 / dpow_default(a, -b);
        double p = 1;
        int whole = (int) b;
        for (double v = a; whole > 0; whole >>= 1, v *= v)
            if ((whole & 1) != 0)
                p *= v;
        if ((b -= whole) > 0) {
            int frac = (int) (0x10000 * b);
            for (; (frac & 0xffff) != 0; frac <<= 1) {
                a = Math.sqrt(a);
                if ((frac & 0x8000) != 0)
                    p *= a;
            }
        }
        return p;
    }

}
