package de.poizzy.luaj.lib.jse;

import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaUserdata;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.function.OneArgFunction;
import de.poizzy.luaj.function.VarArgFunction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * LuaValue that represents a Java instance of Map type.
 *
 * This class is not used directly.
 * It is returned by calls to {@link CoerceJavaToLua#coerce(Object)}
 * when a Map is supplied.
 * @see CoerceJavaToLua
 * @see CoerceLuaToJava
 */
class JavaMap<T, U> extends LuaUserdata {

    public JavaMap(Map<T, U> obj) {
        super(obj);

        LuaTable map_metatable = new LuaTable();
        map_metatable.rawset(LuaValue.PAIRS, new MapPairs());
        map_metatable.rawset(LuaValue.LEN, new LenFunction());
        setmetatable(map_metatable);
    }

    @Override
    public LuaValue get(LuaValue key) {
        return CoerceJavaToLua.coerce(((Map<?,?>)m_instance).get(CoerceLuaToJava.coerce(key, Object.class)));
    }

    @Override
    public void set(LuaValue key, LuaValue value) {
        ((Map)m_instance).put(CoerceLuaToJava.coerce(key, Object.class), CoerceLuaToJava.coerce(value, Object.class));
    }




    private static final class MapPairs extends VarArgFunction {

        @Override
        public Varargs invoke(Varargs args) {
            Map<?, ?> map  = (Map<?, ?>) ((JavaMap) args.arg1()).m_instance;
            List<Object> keyset = Arrays.asList(map.keySet().toArray());

            return varargsOf(new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    Map<?, ?> map  = (Map<?, ?>) ((JavaMap) args.arg1()).m_instance;
                    int index = keyset.indexOf(CoerceLuaToJava.coerce(args.arg(2), Object.class));
                    if (++index < keyset.size()) {
                        Object next = keyset.get(index);
                        return varargsOf(CoerceJavaToLua.coerce(next), CoerceJavaToLua.coerce(map.get(next)));
                    }
                    return NIL;
                }
            }, args.arg1(), NIL);
        }

    }


    private static final class LenFunction extends OneArgFunction {

        public LuaValue call(LuaValue u) {
            return LuaValue.valueOf(((Map<?, ?>)((JavaMap)u).m_instance).size());
        }

    }

}
