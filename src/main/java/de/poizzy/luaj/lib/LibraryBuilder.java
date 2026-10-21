package de.poizzy.luaj.lib;

import de.poizzy.luaj.*;
import de.poizzy.luaj.annotations.LuaField;
import de.poizzy.luaj.annotations.LuaFunction;
import de.poizzy.luaj.function.*;
import de.poizzy.luaj.lib.jse.CoerceJavaToLua;
import de.poizzy.luaj.lib.jse.CoerceLuaToJava;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class LibraryBuilder {
    private static final String CONSTRUCTOR = "CONSTRUCTOR";
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ClassValue<FunctionBinding[]> FUNCTION_BINDINGS = new ClassValue<FunctionBinding[]>() {
        @Override
        protected FunctionBinding[] computeValue(Class<?> type) {
            try {
                return computeFunctions(type);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to bind Lua module " + type.getName(), e);
            }
        }
    };
    private static final ClassValue<VariableBinding[]> VARIABLE_BINDINGS = new ClassValue<VariableBinding[]>() {
        @Override
        protected VariableBinding[] computeValue(Class<?> type) {
            return computeVariables(type);
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

    private static final class VariableBinding {
        public final LuaString name;
        public final Class<?> type;
        public final MethodHandle getter;
        public final MethodHandle setter;

        public VariableBinding(LuaString name, Class<?> type, MethodHandle getter, MethodHandle setter) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.setter = setter;
        }

        public LuaValue get(Object target) {
            try {
                return CoerceJavaToLua.coerce(getter.invokeExact(target));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public void set(Object target, LuaValue value) {
            if (setter == null) throw new LuaError("field '" + name + "' is read-only");
            try {
                setter.invokeExact(target, CoerceLuaToJava.coerce(value, type));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class FunctionBinding {
        public final String key;
        public final Kind kind;
        public final boolean isStatic;
        public final MethodHandle factory;

        public FunctionBinding(String key, Kind kind, boolean isStatic, MethodHandle factory) {
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

    private static FunctionBinding[] computeFunctions(Class<?> clazz) throws Throwable {
        Map<String, FunctionBinding> found = new LinkedHashMap<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                LuaFunction tag = method.getAnnotation(LuaFunction.class);
                if (tag == null) continue;
                String name = tag.name().isEmpty() ? method.getName() : tag.name();
                if (found.containsKey(name)) continue;
                found.put(name, createFunction(clazz, method, name));
            }
        }
        return found.values().toArray(new FunctionBinding[0]);
    }

    private static VariableBinding[] computeVariables(Class<?> clazz) {
        Map<String, VariableBinding> found = new LinkedHashMap<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                LuaField tag = field.getAnnotation(LuaField.class);
                if (tag == null) continue;

                String name = tag.name().isEmpty() ? field.getName() : tag.name();
                if (found.containsKey(name)) continue;
                found.put(name, createVariable(field.getType(), field, name));
            }
        }
        return found.values().toArray(new VariableBinding[0]);
    }

    private static VariableBinding createVariable(Class<?> type, Field field, String name) {
        try {
            field.setAccessible(true);
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            boolean isFinal = Modifier.isFinal(field.getModifiers());

            MethodHandle g = LOOKUP.unreflectGetter(field);
            if (isStatic) {
                g = MethodHandles.dropArguments(g, 0, Object.class);
            }
            MethodHandle getter = g.asType(MethodType.methodType(Object.class, Object.class));

            MethodHandle setter = null;
            if (!isFinal) {
                MethodHandle s = LOOKUP.unreflectSetter(field);
                if (isStatic) {
                    s = MethodHandles.dropArguments(s, 0, Object.class);
                }
                setter = s.asType(MethodType.methodType(void.class, Object.class, Object.class));
            }
            return new VariableBinding(LuaValue.valueOf(name), type, getter, setter);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access @LuaField " + field, e);
        }
    }

    private static FunctionBinding createFunction(Class<?> clazz, Method method, String name) throws Throwable {
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
        return new FunctionBinding(name, kind, isStatic, factory);
    }

    private static void register(LuaValue env, String name, LuaTable table) {
        env.set(name, table);
        if (!env.get("package").isnil()) {
            env.get("package").get("loaded").set(name, table);
        }
    }

    private static void initLuaClass(Class<?> type, String name, LuaValue env, FunctionBinding[] bindings) throws Throwable {
        Constructor<?> c = type.getDeclaredConstructor();
        c.setAccessible(true);
        MethodHandle ctor = LOOKUP.unreflectConstructor(c).asType(MethodType.methodType(Object.class));

        VariableBinding[] variableBindings = VARIABLE_BINDINGS.get(type);
        Map<LuaValue, VariableBinding> variables = new HashMap<>();
        for (VariableBinding v : variableBindings) {
            variables.put(v.name, v);
        }

        LuaTable meta = new LuaTable();
        meta.set(LuaValue.INDEX, new TwoArgFunction((t, k) -> {
            VariableBinding v = variables.get(k);
            return v == null ? LuaValue.NIL : v.get(((LuaInstance) t).self);
        }));
        meta.set(LuaValue.NEWINDEX, new ThreeArgFunction((t, k, val) -> {
            VariableBinding v = variables.get(k);
            if (v == null) {
                t.rawset(k, val);
            } else {
                v.set(((LuaInstance) t).self, val);
            }
            return LuaValue.NIL;
        }));

        LuaTable classTable = new LuaTable();
        classTable.set("new", new VarArgFunction(args -> {
            try {
                Object o = (Object) ctor.invokeExact();
                LuaClass self = (LuaClass) o;

                LuaInstance instance = new LuaInstance(0, bindings.length, self);
                for (FunctionBinding b : bindings) {
                    instance.set(b.key, b.instantiate(self));
                }

                instance.setmetatable(meta);

                self.constructor(instance, args);
                return instance;
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new LuaError(e);
            }
        }));

        register(env, name, classTable);
    }


    public static void addModule(LuaModule module, LuaValue env) throws Throwable {
        FunctionBinding[] bindings = FUNCTION_BINDINGS.get(module.getClass());

        String moduleName = module.name().isEmpty() ? module.getClass().getName() : module.name();

        Class<?> moduleClass = module.getClass();
        if (module instanceof LuaClass) {
            initLuaClass(moduleClass, moduleName, env, bindings);
            return;
        }

        LuaTable table = new LuaTable(0, bindings.length);

        module.init(table, LuaValue.EMPTYSTRING, env);

        for (FunctionBinding b : bindings) {
            table.set(b.key, b.instantiate(module));
        }


        if (moduleName.equals("_G")) {
            LuaValue key = LuaValue.NIL;
            while (true) {
                Varargs entry = table.next(key);
                key = entry.arg1();
                if (key.isnil()) break;
                env.set(key, entry.arg(2));
            }
        } else {
            register(env, moduleName, table);
        }
    }
}