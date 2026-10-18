/*******************************************************************************
 * Copyright (c) 2010-2011 Luaj.org. All rights reserved.
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

import java.io.InputStream;
import java.nio.file.FileSystems;

import static org.luaj.vm2.LuaValue.*;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

/**
 * Subclass of {@link LibFunction} which implements the lua standard package and
 * module library functions.
 *
 * <h3>Lua Environment Variables</h3> The following variables are available to
 * lua scrips when this library has been loaded:
 * <ul>
 * <li><code>"package.loaded"</code> Lua table of loaded modules.
 * <li><code>"package.path"</code> Search path for lua scripts.
 * <li><code>"package.preload"</code> Lua table of uninitialized preload
 * functions.
 * <li><code>"package.searchers"</code> Lua table of functions that search for
 * object to load.
 * </ul>
 *
 * <h3>Java Environment Variables</h3> These Java environment variables affect
 * the library behavior:
 * <ul>
 * <li><code>"luaj.package.path"</code> Initial value for
 * <code>"package.path"</code>. Default value is <code>"?.lua"</code>
 * </ul>
 *
 * <h3>Loading</h3> Typically, this library is included as part of a call to
 * either {@link org.luaj.vm2.lib.jse.JsePlatform#standardGlobals()} or
 *
 * <pre>
 *  {@code
 * Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("require").call"foo") );
 * }
 * </pre>
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
 * 	System.out.println(globals.get("require").call("foo"));
 * }
 * </pre>
 *
 * <h3>Limitations</h3> This library has been implemented to match as closely as
 * possible the behavior in the corresponding library in C. However, the default
 * filesystem search semantics are different and delegated to the bas library as
 * outlined in the {@link BaseLib} and {@link org.luaj.vm2.lib.jse.JseBaseLib}
 * documentation.
 * <p>
 *
 * @see LibFunction
 * @see BaseLib
 * @see org.luaj.vm2.lib.jse.JseBaseLib
 * @see org.luaj.vm2.lib.jse.JsePlatform
 * @see <a href="http://www.lua.org/manual/5.2/manual.html#6.3">Lua 5.2 Package
 * Lib Reference</a>
 */
public class PackageLib implements LuaModule {

    @Override
    public void init(LuaTable self, LuaValue name, LuaValue env) {
        globals = env.checkglobals();
        package_ = self;
        package_.set(_LOADED, new LuaTable());
        package_.set(_PRELOAD, new LuaTable());
        package_.set(_PATH, LuaValue.valueOf(DEFAULT_LUA_PATH));
        package_.set("config", FILE_SEP + "\n;\n?\n!\n-\n");
        LuaTable searchers = new LuaTable();
        searchers.set(1, preload_searcher);
        searchers.set(2, lua_searcher);
        searchers.set(3, java_searcher);
        package_.set(_SEARCHERS, searchers);
        globals.package_ = this;
    }

    @Override
    public String name() {
        return "package";
    }

    /**
     * The default value to use for package.path. This can be set with the
     * system property <code>"luaj.package.path"</code>, and is
     * <code>"?.lua"</code> by default.
     */
    public static final String DEFAULT_LUA_PATH;

    static {
        String path = null;
        try {
            path = System.getProperty("luaj.package.path");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        if (path == null) {
            path = "?.lua";
        }
        DEFAULT_LUA_PATH = path;
    }

    static final LuaString _LOADED = valueOf("loaded");
    private static final LuaString _LOADLIB = valueOf("loadlib");
    static final LuaString _PRELOAD = valueOf("preload");
    static final LuaString _PATH = valueOf("path");
    static final LuaString _SEARCHPATH = valueOf("searchpath");
    static final LuaString _SEARCHERS = valueOf("searchers");
    static final LuaString _SEEALL = valueOf("seeall");

    /**
     * The globals that were used to load this library.
     */
    Globals globals;

    /**
     * The table for this package.
     */
    LuaTable package_;

    /**
     * Loader that loads from {@code preload} table if found there
     */
    public VarArgFunction preload_searcher = new VarArgFunction(args -> {
        LuaString name = args.checkstring(1);
        LuaValue val = package_.get(_PRELOAD).get(name);
        return val.isnil() ? valueOf("\n\tno field package.preload['" + name + "']") : val;
    });

    /**
     * Loader that loads as a lua script using the lua path currently in
     */
    public VarArgFunction lua_searcher = new VarArgFunction(args -> {
        LuaString name = args.checkstring(1);

        // get package path
        LuaValue path = package_.get(_PATH);
        if (!path.isstring())
            return valueOf("package.path is not a string");

        // get the searchpath function.
        Varargs v = package_.get(_SEARCHPATH).invoke(varargsOf(name, path));

        // Did we get a result?
        if (!v.isstring(1))
            return v.arg(2).tostring();
        LuaString filename = v.arg1().strvalue();

        // Try to load the file.
        v = globals.loadfile(filename.tojstring());
        if (v.arg1().isfunction())
            return LuaValue.varargsOf(v.arg1(), filename);

        // report error
        return varargsOf(NIL, valueOf("'" + filename + "': " + v.arg(2).tojstring()));
    });

    /**
     * Loader that loads as a Java class. Class must have public constructor and
     * be a LuaValue.
     */
    public VarArgFunction java_searcher = new VarArgFunction(args -> {
        String name = args.checkjstring(1);
        String classname = toClassname(name);
        Class<?> c;
        LuaValue v;
        try {
            c = Class.forName(classname);
            v = (LuaValue) c.newInstance();
            if (v.isfunction())
                v.initupvalue1(globals);
            return varargsOf(v, globals);
        } catch (ClassNotFoundException cnfe) {
            return valueOf("\n\tno class '" + classname + "'");
        } catch (Exception e) {
            return valueOf("\n\tjava load failed on '" + classname + "', " + e);
        }
    });

    private static final LuaString _SENTINEL = valueOf("\u0001");

    private static final String FILE_SEP = FileSystems.getDefault().getSeparator();

    public PackageLib() {
    }

    /**
     * Perform one-time initialization on the library by adding package
     * functions to the supplied environment, and returning it as the return
     * value. It also creates the package.preload and package.loaded tables for
     * use by other libraries.
     *
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env     the environment to load into, typically a Globals
     *                instance.
     */
    @org.luaj.vm2.lib.LuaFunction
    public Varargs require(Varargs args) {
        LuaString name = args.checkstring(1);
        LuaValue loaded = package_.get(_LOADED), result = loaded.get(name);
        if (result.toboolean()) {
            if (result == _SENTINEL) error("loop or previous error loading module '" + name + "'");
            return result;
        }
        LuaTable tbl = package_.get(_SEARCHERS).checktable();
        StringBuffer sb = new StringBuffer();
        Varargs loader;
        for (int i = 1; ; i++) {
            LuaValue searcher = tbl.get(i);
            if (searcher.isnil()) error("module '" + name + "' not found: " + name + sb);
            loader = searcher.invoke(name);
            if (loader.isfunction(1)) break;
            if (loader.isstring(1)) sb.append(loader.tojstring(1));
        }
        loaded.set(name, _SENTINEL);
        result = loader.arg1().call(name, loader.arg(2));
        if (!result.isnil()) loaded.set(name, result);
        else if ((result = loaded.get(name)) == _SENTINEL) loaded.set(name, result = LuaValue.TRUE);
        return result;
    }

    @org.luaj.vm2.lib.LuaFunction
    public Varargs loadlib(Varargs args) {
        args.checkstring(1);
        return varargsOf(NIL, valueOf("dynamic libraries not enabled"), valueOf("absent"));
    }

    @org.luaj.vm2.lib.LuaFunction
    public Varargs searchpath(Varargs args) {
        String name = args.checkjstring(1), path = args.checkjstring(2), sep = args.optjstring(3, "."), rep = args.optjstring(4, FILE_SEP);
        int e = -1, n = path.length();
        StringBuffer sb = null;
        name = name.replace(sep.charAt(0), rep.charAt(0));
        while (e < n) {
            int b = e + 1;
            e = path.indexOf(';', b);
            if (e < 0) e = n;
            String t = path.substring(b, e);
            int q = t.indexOf('?');
            String f = q >= 0 ? t.substring(0, q) + name + t.substring(q + 1) : t;
            InputStream is = globals.finder.findResource(f);
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
                return valueOf(f);
            }
            if (sb == null) sb = new StringBuffer();
            sb.append("\n\t").append(f);
        }
        return varargsOf(NIL, valueOf(sb.toString()));
    }

    @org.luaj.vm2.lib.LuaFunction
    public Varargs seeall(Varargs args) {
        LuaTable mt = new LuaTable();
        mt.set(LuaValue.INDEX, globals);
        args.checktable(1).setmetatable(mt);
        return NONE;
    }


    /**
     * Convert lua filename to valid class name
     */
    public static String toClassname(String filename) {
        int n = filename.length();
        int j = n;
        if (filename.endsWith(".lua"))
            j -= 4;
        for (int k = 0; k < j; k++) {
            char c = filename.charAt(k);
            if (!isClassnamePart(c) || c == '/' || c == '\\') {
                StringBuffer sb = new StringBuffer(j);
                for (int i = 0; i < j; i++) {
                    c = filename.charAt(i);
                    sb.append(isClassnamePart(c) ? c : c == '/' || c == '\\' ? '.' : '_');
                }
                return sb.toString();
            }
        }
        return n == j ? filename : filename.substring(0, j);
    }

    private static boolean isClassnamePart(char c) {
        if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')
            return true;
        switch (c) {
            case '.':
            case '$':
            case '_':
                return true;
            default:
                return false;
        }
    }
}
