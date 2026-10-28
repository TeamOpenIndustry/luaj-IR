package de.poizzy.luaj.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exposes a Java method to Lua. In a {@link de.poizzy.luaj.lib.LuaModule} it is added
 * to the module table; in a {@link de.poizzy.luaj.lib.LuaClass} it is added to each
 * instance.
 *
 * <p>Supported signatures (static or instance, no automatic coercion):
 * <pre>{@code
 * LuaValue f()
 * LuaValue f(LuaValue a)
 * LuaValue f(LuaValue a, LuaValue b)
 * LuaValue f(LuaValue a, LuaValue b, LuaValue c)
 * Varargs  f(Varargs args)
 * }</pre>
 * Anything else fails when the module is loaded. The method must be accessible
 * (public). Superclass methods are included. Overload Methods are not supported!
 * Use Varargs instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LuaFunction {
    /** Lua-side name; defaults to the Java method name if empty. */
    String name() default "";
}
