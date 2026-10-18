package org.luaj.vm2.lib;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public class Functions {
    @FunctionalInterface
    public interface ZeroArgs {
       LuaValue call();
    }
    @FunctionalInterface
    public interface OneArgs {
        LuaValue call(LuaValue arg);
    }
    @FunctionalInterface
    public interface TwoArgs {
        LuaValue call(LuaValue arg1, LuaValue arg2);
    }
    @FunctionalInterface
    public interface ThreeArgs {
        LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3);
    }
    @FunctionalInterface
    public interface VarArgs {
        Varargs call(Varargs args);
    }
}
