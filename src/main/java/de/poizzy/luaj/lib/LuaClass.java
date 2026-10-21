package de.poizzy.luaj.lib;

import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.function.VarArgFunction;

public interface LuaClass extends LuaModule {
    void constructor(VarArgFunction newInstance, Varargs args);
}
