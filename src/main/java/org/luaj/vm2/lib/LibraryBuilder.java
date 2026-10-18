package org.luaj.vm2.lib;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Arrays;

public class LibraryBuilder {
    public static void addModule(LuaModule module, LuaValue env) throws Throwable {
        LuaTable table = new LuaTable();

        Class<?> clazz = module.getClass();

        String moduleName = module.name();

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        module.init(LuaValue.EMPTYSTRING, env);

        Class<?> parent = clazz;
        while (parent != null && parent != Object.class) {
            for (Method method : parent.getDeclaredMethods()) {
                LuaFunction tag = method.getAnnotation(LuaFunction.class);
                if (tag == null) continue;

                String name = tag.name().isEmpty() ? method.getName() : tag.name();

                int methodArgs = method.getParameterCount();

                MethodType interfaceMethodType;

                Class<?> type;
                if (methodArgs == 1 && method.getParameterTypes()[0] == Varargs.class) {
                    type = Functions.VarArgs.class;
                    interfaceMethodType = MethodType.methodType(Varargs.class, Varargs.class);
                } else {
                    switch (methodArgs) {
                        case 0:
                            type = Functions.ZeroArgs.class;
                            interfaceMethodType = MethodType.methodType(LuaValue.class);
                            break;
                        case 1:
                            type = Functions.OneArgs.class;
                            interfaceMethodType = MethodType.methodType(LuaValue.class, LuaValue.class);
                            break;
                        case 2:
                            type = Functions.TwoArgs.class;
                            interfaceMethodType = MethodType.methodType(LuaValue.class, Arrays.asList(LuaValue.class, LuaValue.class));
                            break;
                        case 3:
                            type = Functions.ThreeArgs.class;
                            interfaceMethodType = MethodType.methodType(LuaValue.class, Arrays.asList(LuaValue.class, LuaValue.class, LuaValue.class));
                            break;
                        default:
                            // TODO better exception
                            throw new UnsupportedOperationException();
                    }
                }



                MethodHandle methodHandle = lookup.unreflect(method).bindTo(module);

                MethodType factoryType = MethodType.methodType(type);

                CallSite callSite = LambdaMetafactory.metafactory(lookup, "call", factoryType, interfaceMethodType, methodHandle, methodHandle.type());

                LibFunction finalFunction;

                if (type == Functions.VarArgs.class) {
                    finalFunction = new VarArgFunction((Functions.VarArgs) callSite.getTarget().invoke());
                } else {
                    switch (methodArgs) {
                        case 0:
                            finalFunction = new ZeroArgFunction((Functions.ZeroArgs) callSite.getTarget().invoke());
                            break;
                        case 1:
                            finalFunction = new OneArgFunction((Functions.OneArgs) callSite.getTarget().invoke());
                            break;
                        case 2:
                            finalFunction = new TwoArgFunction((Functions.TwoArgs) callSite.getTarget().invoke());
                            break;
                        case 3:
                            finalFunction = new ThreeArgFunction((Functions.ThreeArgs) callSite.getTarget().invoke());
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

                table.set(name, finalFunction);
            }
            parent = parent.getSuperclass();
        }

        if (moduleName.equals("_G")) {
            LuaValue key = LuaValue.NIL;
            while (true) {
                Varargs entry = table.next(key);
                key = entry.arg1();

                if (key.isnil()) {
                    break;
                }

                LuaValue func = entry.arg(2);

                env.set(key, func);
            }
        } else {
            String name = moduleName.isEmpty() ? clazz.getName() : moduleName;
            env.set(name, table);
            if (!env.get("package").isnil()) {
                env.get("package").get("loaded").set(name, table);
            }
        }
    }
}
