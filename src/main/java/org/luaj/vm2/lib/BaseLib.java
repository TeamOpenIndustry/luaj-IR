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
package org.luaj.vm2.lib;

import java.io.IOException;
import java.io.InputStream;

import static org.luaj.vm2.LuaValue.*;

import org.luaj.vm2.Globals;
import org.luaj.vm2.Lua;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

/**
 * Subclass of {@link LibFunction} which implements the lua basic library
 * functions.
 * <p>
 * This contains all library functions listed as "basic functions" in the lua
 * system by default, {@link BaseLib} implements {@link ResourceFinder} using
 * The default loader chain in {@link PackageLib} will use these as well.
 * <p>
 * To use basic library functions that include a {@link ResourceFinder} based on
 * directory lookup, use {@link org.luaj.vm2.lib.jse.JseBaseLib} instead.
 * <p>
 * Typically, this library is included as part of a call to either
 * {@link org.luaj.vm2.lib.jse.JsePlatform#standardGlobals()} or
 *
 * <pre>
 * {
 * 	&#64;code
 * 	Globals globals = JsePlatform.standardGlobals();
 * 	globals.get("print").call(LuaValue.valueOf("hello, world"));
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
 * 	globals.get("print").call(LuaValue.valueOf("hello, world"));
 * }
 * </pre>
 * <p>
 * Doing so will ensure the library is properly initialized and loaded into the
 * globals table.
 * <p>
 * This is a direct port of the corresponding library in C.
 *
 * @see org.luaj.vm2.lib.jse.JseBaseLib
 * @see ResourceFinder
 * @see Globals#finder
 * @see LibFunction
 * @see org.luaj.vm2.lib.jse.JsePlatform
 * @see <a href="http://www.lua.org/manual/5.2/manual.html#6.1">Lua 5.2 Base Lib
 * Reference</a>
 */
public class BaseLib implements ResourceFinder, LuaModule {

    Globals globals;

    @Override
    public void init(LuaTable self, LuaValue name, LuaValue env) {
        globals = env.checkglobals();
        globals.finder = this;
        globals.baselib = this;
        env.set("_G", env);
        env.set("_VERSION", Lua._VERSION);
    }

    @Override
    public String name() {
        return "_G";
    }

    /**
     * Perform one-time initialization on the library by adding base functions
     * to the supplied environment, and returning it as the return value.
     *
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env     the environment to load into, which must be a Globals
     *                instance.
     */
    // Annotation-bound module exports.
    @LuaFunction(name = "assert")
    public Varargs luaAssert(Varargs args) {
        LuaValue v = args.arg1();
        if (v.isnil() || (v.isboolean() && !v.toboolean()))
            throw new LuaError(args.optjstring(2, "assertion failed!"));
        return args;
    }

    @LuaFunction
    public Varargs collectgarbage(Varargs args) {
        String s = args.optjstring(1, "collect");
        if ("collect".equals(s)) {
            System.gc();
            return ZERO;
        } else if ("count".equals(s)) {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            return varargsOf(valueOf(used / 1024.), valueOf(used % 1024));
        } else if ("step".equals(s)) {
            System.gc();
            return LuaValue.TRUE;
        } else {
            argerror(1, "invalid option '" + s + "'");
        }
        return NIL;
    }

    @LuaFunction
    public Varargs dofile(Varargs args) {
        args.argcheck(args.isstring(1) || args.isnil(1), 1, "filename must be string or nil");
        String filename = args.isstring(1) ? args.tojstring(1) : null;
        Varargs v = filename == null ? loadStream(globals.STDIN, "=stdin", "bt", globals)
                : loadFile(args.checkjstring(1), "bt", globals);
        if (v.isnil(1)) throw new LuaError(v.tojstring(2));
        return v.arg1().invoke();
    }

    @LuaFunction
    public Varargs error(Varargs args) {
        LuaValue arg1 = args.arg1(), arg2 = args.arg(2);
        if (arg1.isnil()) throw new LuaError(NIL);
        if (!arg1.isstring() || arg2.optint(1) == 0) throw new LuaError(arg1);
        throw new LuaError(arg1.tojstring(), arg2.optint(1));
    }

    @LuaFunction
    public Varargs getmetatable(Varargs args) {
        LuaValue mt = args.arg1().getmetatable();
        return mt != null ? mt.rawget(METATABLE).optvalue(mt) : NIL;
    }

    @LuaFunction
    public Varargs load(Varargs args) {
        LuaValue ld = args.arg1();
        if (!ld.isstring() && !ld.isfunction()) {
            throw new LuaError(
                    "bad argument #1 to 'load' (string or function expected, got " + ld.typename() + ")");
        }
        String source = args.optjstring(2, ld.isstring() ? ld.tojstring() : "=(load)");
        String mode = args.optjstring(3, "bt");
        LuaValue env = args.optvalue(4, globals);
        return loadStream(ld.isstring() ? ld.strvalue().toInputStream() : new StringInputStream(ld.checkfunction()),
                source, mode, env);
    }

    @LuaFunction
    public Varargs loadfile(Varargs args) {
        args.argcheck(args.isstring(1) || args.isnil(1), 1, "filename must be string or nil");
        String filename = args.isstring(1) ? args.tojstring(1) : null;
        String mode = args.optjstring(2, "bt");
        LuaValue env = args.optvalue(3, globals);
        return filename == null ? loadStream(globals.STDIN, "=stdin", mode, env) : loadFile(filename, mode, env);
    }

    @LuaFunction
    public Varargs pcall(Varargs args) {
        LuaValue func = args.checkvalue(1);
        if (globals != null && globals.debuglib != null)
            globals.debuglib.onCall(func.checkfunction());
        try {
            return varargsOf(TRUE, func.invoke(args.subargs(2)));
        } catch (LuaError le) {
            final LuaValue m = le.getMessageObject();
            return varargsOf(FALSE, m != null ? m : NIL);
        } catch (Exception e) {
            final String m = e.getMessage();
            return varargsOf(FALSE, valueOf(m != null ? m : e.toString()));
        } finally {
            if (globals != null && globals.debuglib != null)
                globals.debuglib.onReturn();
        }
    }

    @LuaFunction
    public Varargs print(Varargs args) {
        LuaValue tostring = globals.get("tostring");
        for (int i = 1; i <= args.narg(); i++) {
            if (i > 1) globals.STDOUT.print('\t');
            globals.STDOUT.print(tostring.call(args.arg(i)).strvalue().tojstring());
        }
        globals.STDOUT.print('\n');
        return NONE;
    }

    @LuaFunction
    public Varargs rawequal(Varargs args) {
        return valueOf(args.arg1().raweq(args.arg(2)));
    }

    @LuaFunction
    public Varargs rawget(Varargs args) {
        return args.checktable(1).rawget(args.arg(2));
    }

    @LuaFunction
    public Varargs rawlen(Varargs args) {
        return valueOf(args.arg1().rawlen());
    }

    @LuaFunction
    public Varargs rawset(Varargs args) {
        LuaTable table = args.checktable(1);
        LuaValue key = args.arg(2);
        if (!key.isvalidkey()) argerror(2, "table index is nil");
        table.rawset(key, args.arg(3));
        return table;
    }

    @LuaFunction
    public Varargs select(Varargs args) {
        int n = args.narg() - 1;
        if (args.arg1().equals(valueOf("#")))
            return valueOf(n);
        int i = args.checkint(1);
        if (i == 0 || i < -n)
            argerror(1, "index out of range");
        return args.subargs(i < 0 ? n + i + 2 : i + 1);
    }

    @LuaFunction
    public Varargs setmetatable(Varargs args) {
        LuaTable table = args.checktable(1);
        LuaValue old = table.getmetatable();
        if (old != null && !old.rawget(METATABLE).isnil())
            throw new LuaError("cannot change a protected metatable");
        return table.setmetatable(args.arg(2).isnil() ? null : args.checktable(2));
    }

    @LuaFunction
    public Varargs tonumber(Varargs args) {
        LuaValue value = args.arg1(), base = args.arg(2);
        return base.isnil() ? value.tonumber() : value.checkstring().tonumber(base.checkint());
    }

    @LuaFunction
    public Varargs tostring(Varargs args) {
        LuaValue h = args.arg1().metatag(TOSTRING);
        if (!h.isnil()) return h.call(args.arg1());
        LuaValue value = args.arg1().tostring();
        return value.isnil() ? valueOf(args.arg1().tojstring()) : value;
    }

    @LuaFunction
    public Varargs type(Varargs args) {
        return valueOf(args.arg1().typename());
    }

    @LuaFunction
    public Varargs xpcall(Varargs args) {
        try {
            return varargsOf(TRUE, args.arg1().invoke(args.subargs(3)));
        } catch (LuaError e) {
            return varargsOf(FALSE, e.getMessageObject());
        }
    }

    public VarArgFunction next = new VarArgFunction(args -> args.checktable(1).next(args.arg(2)));

    @LuaFunction
    public Varargs next(Varargs args) {
        return next.invoke(args);
    }

    @LuaFunction
    public Varargs pairs(Varargs args) {
        LuaValue meta = args.arg1().getmetatable(), p;
        if (meta != null && (p = meta.get(LuaValue.PAIRS)).isfunction()) return p.invoke(args);
        return varargsOf(next, args.checktable(1), NIL);
    }

    @LuaFunction
    public Varargs ipairs(Varargs args) {
        LuaValue meta = args.arg1().getmetatable();
        LuaValue __ipairs;
        if (meta != null && (__ipairs = meta.get(LuaValue.IPAIRS)).isfunction()) {
            return __ipairs.invoke(args);
        }
        return varargsOf(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs a) {
                return a.checktable(1).inext(a.arg(2));
            }
        }, args.checktable(1), ZERO);
    }


    /**
     * ResourceFinder implementation
     *
     */
    @Override
    public InputStream findResource(String filename) {
        return getClass().getResourceAsStream(filename.startsWith("/") ? filename : "/" + filename);
    }

    /**
     * Load from a named file, returning the chunk or nil,error of can't load
     *
     * @param env
     * @param mode
     * @return Varargs containing chunk, or NIL,error-text on error
     */
    public Varargs loadFile(String filename, String mode, LuaValue env) {
        InputStream is = globals.finder.findResource(filename);
        if (is == null)
            return varargsOf(NIL, valueOf("cannot open " + filename + ": No such file or directory"));
        try {
            return loadStream(is, "@" + filename, mode, env);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Varargs loadStream(InputStream is, String chunkname, String mode, LuaValue env) {
        try {
            if (is == null)
                return varargsOf(NIL, valueOf("not found: " + chunkname));
            return globals.load(is, chunkname, mode, env);
        } catch (Exception e) {
            return varargsOf(NIL, valueOf(e.getMessage()));
        }
    }

    private static class StringInputStream extends InputStream {
        final LuaValue func;
        byte[] bytes;
        int offset, remaining = 0;

        StringInputStream(LuaValue func) {
            this.func = func;
        }

        @Override
        public int read() throws IOException {
            if (remaining < 0)
                return -1;
            if (remaining == 0) {
                LuaValue s = func.call();
                if (s.isnil())
                    return remaining = -1;
                LuaString ls = s.strvalue();
                bytes = ls.m_bytes;
                offset = ls.m_offset;
                remaining = ls.m_length;
                if (remaining <= 0)
                    return -1;
            }
            --remaining;
            return 0xFF & bytes[offset++];
        }
    }
}
