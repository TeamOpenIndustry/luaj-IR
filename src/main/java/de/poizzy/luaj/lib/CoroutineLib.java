/*******************************************************************************
 * Copyright (c) 2007-2011 LuaJ. All rights reserved.
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

import static de.poizzy.luaj.LuaValue.*;

import de.poizzy.luaj.lib.jse.JsePlatform;
import de.poizzy.luaj.Globals;
import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaThread;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.function.LibFunction;
import de.poizzy.luaj.function.VarArgFunction;

/**
 * Subclass of {@link LibFunction} which implements the lua standard
 * {@code coroutine} library.
 * <p>
 * The coroutine library in luaj has the same behavior as the coroutine library
 * in C, but is implemented using Java Threads to maintain the call state
 * between invocations. Therefore it can be yielded from anywhere, similar to
 * the "Coco" yield-from-anywhere patch available for C-based lua. However,
 * coroutines that are yielded but never resumed to complete their execution may
 * not be collected by the garbage collector.
 * <p>
 * Typically, this library is included as part of a call to either
 * {@link JsePlatform#standardGlobals()} or
 *
 * <pre>
 * {
 * 	&#64;code
 * 	Globals globals = JsePlatform.standardGlobals();
 * 	System.out.println(globals.get("coroutine").get("running").call());
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
 * 	globals.load(new CoroutineLib());
 * 	System.out.println(globals.get("coroutine").get("running").call());
 * }
 * </pre>
 * <p>
 *
 * @see LibFunction
 * @see JsePlatform
 * @see <a href="http://www.lua.org/manual/5.2/manual.html#6.2">Lua 5.2
 * Coroutine Lib Reference</a>
 */
public class CoroutineLib implements LuaModule {

    @Override
    public void init(LuaTable self, LuaValue name, LuaValue env) {
        globals = env.checkglobals();
    }

    @Override
    public String name() {
        return "coroutine";
    }

    static int coroutine_count = 0;

    Globals globals;

    /**
     * Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied
     * environment, adding the table to package.loaded, and returning table as
     * the return value.
     *
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env     the environment to load into, which must be a Globals
     *                instance.
     */
    // Annotation-bound module exports.
    @LuaFunction
    public Varargs create(Varargs args) {
        return new LuaThread(globals, args.checkfunction(1));
    }

    @LuaFunction
    public Varargs resume(Varargs args) {
        final LuaThread t = args.checkthread(1);
        return t.resume(args.subargs(2));
    }

    @LuaFunction
    public Varargs running(Varargs args) {
        final LuaThread r = globals.running;
        return varargsOf(r, valueOf(r.isMainThread()));
    }

    @LuaFunction
    public Varargs status(Varargs args) {
        return valueOf(args.checkthread(1).getStatus());
    }

    @LuaFunction
    public Varargs yield(Varargs args) {
        return globals.yield(args);
    }

    @LuaFunction
    public Varargs wrap(Varargs args) {
        final LuaThread luathread = new LuaThread(globals, args.checkfunction(1));
        return new wrapper(luathread);
    }


    static final class wrapper extends VarArgFunction {
        final LuaThread luathread;

        wrapper(LuaThread luathread) {
            this.luathread = luathread;
        }

        @Override
        public Varargs invoke(Varargs args) {
            final Varargs result = luathread.resume(args);
            if (result.arg1().toboolean()) {
                return result.subargs(2);
            } else {
                return error(result.arg(2).tojstring());
            }
        }
    }
}
