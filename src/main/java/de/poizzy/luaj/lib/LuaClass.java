package de.poizzy.luaj.lib;

import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;

public interface LuaClass extends LuaModule {
    void constructor(LuaValue self, Varargs args);
}
