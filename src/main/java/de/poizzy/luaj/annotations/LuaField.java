package de.poizzy.luaj.annotations;

import de.poizzy.luaj.LuaValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exposes a Java field of a {@link de.poizzy.luaj.lib.LuaClass} to Lua as a
 * property on every instance (read and written via {@code __index}/{@code __newindex}).
 *
 * <p>Values are converted with {@code CoerceJavaToLua} on read and
 * {@code CoerceLuaToJava} (to the field's type) on write. {@code final} fields are
 * read-only and throw a {@link de.poizzy.luaj.LuaError} on assignment. Private and
 * static fields are supported.
 *
 * <p>Only works with {@link de.poizzy.luaj.lib.LuaClass}; ignored on plain modules.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface LuaField {
    /** Lua-side name; defaults to the Java field name if empty. */
    String name() default "";
}
