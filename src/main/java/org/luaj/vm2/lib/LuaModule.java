package org.luaj.vm2.lib;

import org.luaj.vm2.LuaValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface LuaModule {
    void init(LuaValue name, LuaValue env);

    default String name() {
        return "";
    }
}
