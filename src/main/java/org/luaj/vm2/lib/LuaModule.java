package org.luaj.vm2.lib;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/**
 * A library module that can be initialized in a Lua environment.
 */
public interface LuaModule {
    /**
     * Initialize this module in {@code env}.
     *
     * @param name the name supplied by the module loader
     * @param env the environment receiving the module
     */
    void init(LuaTable self, LuaValue name, LuaValue env);

    default String name() {
        return "";
    }
}
