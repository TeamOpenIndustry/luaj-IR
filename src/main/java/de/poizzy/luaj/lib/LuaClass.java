package de.poizzy.luaj.lib;

import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.function.VarArgFunction;

/**
 * A {@link LuaModule} exposed to Lua as an instantiable class, registered under
 * {@link #name()} as a table with a {@code new} function:
 * <pre>{@code
 * local obj = MyClass.new(1, 2)
 * }</pre>
 *
 * <p>Each {@code new} call creates a fresh Java object (via its no-arg constructor,
 * any visibility), binds its {@link de.poizzy.luaj.annotations.LuaFunction} methods
 * and {@link de.poizzy.luaj.annotations.LuaField} fields to a Lua instance, then calls
 * {@link #constructor}. The object passed to {@code addModule} is only used to
 * determine the class and name.
 */
public interface LuaClass extends LuaModule {
    /**
     * Lua-side constructor, called after the instance is fully set up.
     *
     * @param newInstance the class's {@code new} function
     * @param args        the arguments passed to {@code new} from Lua
     */
    void constructor(VarArgFunction newInstance, Varargs args);
}
