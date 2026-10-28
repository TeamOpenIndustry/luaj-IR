package de.poizzy.luaj.lib;

import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaValue;

/**
 * A library that can be loaded into a Lua environment via
 * {@link LuaValue#load(LuaModule)} 
 *
 * <p>All {@link de.poizzy.luaj.annotations.LuaFunction} methods are collected into a
 * table registered under {@link #name()} (and in {@code package.loaded} if present).
 * If the name is {@code "_G"}, the functions are installed as globals instead.
 */
public interface LuaModule {
    /**
     * Called once on load, before the annotated functions are added to {@code self}.
     * Use it to register constants or other non-annotated values. Not called for
     * {@link LuaClass}.
     *
     * @param self the module table
     * @param name currently always {@link LuaValue#EMPTYSTRING}, not the module name
     * @param env  the environment being loaded into
     */
    default void init(LuaTable self, LuaValue name, LuaValue env) {};

    /**
     * @return the module name in Lua; if empty, the fully qualified Java class name
     *         is used; {@code "_G"} installs functions as globals
     */
    String name();
}
