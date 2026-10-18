package org.luaj.vm2.lib;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class LibraryBuilder {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ClassValue<Binding[]> BINDINGS = new ClassValue<Binding[]>() {
        @Override
        protected Binding[] computeValue(Class<?> type) {
            try {
                return computeClasses(type);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to bind Lua module " + type.getName(), e);
            }
        }
    };

    private enum Kind {
        VARARGS(Functions.VarArgs.class, MethodType.methodType(Varargs.class, Varargs.class)),
        ZERO(Functions.ZeroArgs.class, MethodType.methodType(LuaValue.class)),
        ONE(Functions.OneArgs.class, MethodType.methodType(LuaValue.class, LuaValue.class)),
        TWO(Functions.TwoArgs.class, MethodType.methodType(LuaValue.class, LuaValue.class, LuaValue.class)),
        THREE(Functions.ThreeArgs.class, MethodType.methodType(LuaValue.class, LuaValue.class, LuaValue.class, LuaValue.class));

        final Class<?> iface;
        final MethodType samType;

        Kind(Class<?> iface, MethodType samType) {
            this.iface = iface;
            this.samType = samType;
        }
    }

    private static final class Binding {
        public final LuaString key;
        public final Kind kind;
        public final boolean isStatic;
        public final MethodHandle factory;

        public Binding(LuaString key, Kind kind, boolean isStatic, MethodHandle factory) {
            this.key = key;
            this.kind = kind;
            this.isStatic = isStatic;
            this.factory = factory;
        }

        public LibFunction instantiate(Object receiver) throws Throwable {
            Object impl = isStatic ? factory.invokeExact() : factory.invokeExact(receiver);
            switch (kind) {
                case VARARGS: return new VarArgFunction((Functions.VarArgs) impl);
                case ZERO: return new ZeroArgFunction((Functions.ZeroArgs) impl);
                case ONE: return new OneArgFunction((Functions.OneArgs) impl);
                case TWO: return new TwoArgFunction((Functions.TwoArgs) impl);
                default: return new ThreeArgFunction((Functions.ThreeArgs) impl);
            }
        }
    }

    private static Binding[] computeClasses(Class<?> clazz) throws Throwable {
        Map<String, Binding> found = new LinkedHashMap<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                LuaFunction tag = method.getAnnotation(LuaFunction.class);
                if (tag == null) continue;

                String name = tag.name().isEmpty() ? method.getName() : tag.name();
                if (found.containsKey(name)) continue;
                found.put(name, createBinding(clazz, method, name));
            }
        }
        return found.values().toArray(new Binding[0]);
    }

    private static Binding createBinding(Class<?> clazz, Method method, String name) throws Throwable {
        Class<?>[] params = method.getParameterTypes();
        Kind kind;
        if (params.length == 1 && params[0] == Varargs.class) {
            kind = Kind.VARARGS;
        } else {
            switch (params.length) {
                case 0: {
                    kind = Kind.ZERO;
                    break;
                }
                case 1: {
                    kind = Kind.ONE;
                    break;
                }
                case 2: {
                    kind = Kind.TWO;
                    break;
                }
                case 3: {
                    kind = Kind.THREE;
                    break;
                }
                default: throw new UnsupportedOperationException(
                        "@LuaFunction " + clazz.getName() + "#" + method.getName()
                                + " has " + params.length + " parameters; max is 3 (or a single Varargs)");
            }
        }

        boolean isStatic = Modifier.isStatic(method.getModifiers());
        MethodHandle impl = LOOKUP.unreflect(method);
        MethodType instantiated = isStatic ? impl.type() : impl.type().dropParameterTypes(0, 1);
        MethodType factoryType = isStatic ? MethodType.methodType(kind.iface) : MethodType.methodType(kind.iface, clazz);

        CallSite site = LambdaMetafactory.metafactory(LOOKUP, "call", factoryType, kind.samType, impl, instantiated);
        MethodHandle factory = isStatic ? site.getTarget().asType(MethodType.methodType(Object.class)) : site.getTarget().asType(MethodType.methodType(Object.class, Object.class));
        return new Binding(LuaValue.valueOf(name), kind, isStatic, factory);
    }


    public static void addModule(LuaModule module, LuaValue env) throws Throwable {
        Binding[] bindings = BINDINGS.get(module.getClass());

        LuaTable table = new LuaTable(0, bindings.length);
        module.init(table, LuaValue.EMPTYSTRING, env);

        for (Binding b : bindings) {
            table.set(b.key, b.instantiate(module));
        }

        String moduleName = module.name();
        if (moduleName.equals("_G")) {
            LuaValue key = LuaValue.NIL;
            while (true) {
                Varargs entry = table.next(key);
                key = entry.arg1();
                if (key.isnil()) break;
                env.set(key, entry.arg(2));
            }
        } else {
            String name = moduleName.isEmpty() ? module.getClass().getName() : moduleName;
            env.set(name, table);
            if (!env.get("package").isnil()) {
                env.get("package").get("loaded").set(name, table);
            }
        }
    }
}
