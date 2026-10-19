package de.poizzy.luaj.lib;

import org.junit.jupiter.api.Test;
import de.poizzy.luaj.Globals;
import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.lib.jse.JsePlatform;

import static org.junit.jupiter.api.Assertions.*;

class LibraryModulesTest {

    @Test
    void loadsAndExposesEveryStandardLibrary() {
        Globals globals = JsePlatform.standardGlobals();

        assertTrue(globals.get("assert").isfunction());
        assertTrue(globals.get("package").istable());
        assertTrue(globals.get("bit32").istable());
        assertTrue(globals.get("table").istable());
        assertTrue(globals.get("string").istable());
        assertTrue(globals.get("math").istable());
        assertTrue(globals.get("os").istable());

        assertSame(globals.get("package"), globals.get("package").get("loaded").get("package"));
        assertSame(globals.get("math"), globals.get("package").get("loaded").get("math"));
    }

    @Test
    void baseLibraryFunctionsWork() {
        Globals globals = JsePlatform.standardGlobals();

        assertEquals("number", globals.get("type").call(LuaValue.valueOf(1)).tojstring());
        assertEquals("value", globals.get("assert").call(LuaValue.valueOf("value")).tojstring());

        LuaTable table = new LuaTable();
        table.set(1, "one");
        table.set(2, "two");
        Varargs args = globals.get("next").invoke(table, LuaValue.valueOf(1));
        assertEquals("two", args.arg(2).tojstring());
        assertEquals(0, globals.get("ipairs").invoke(table).arg(3).toint());
    }

    @Test
    void bit32TableLibraryWorks() {
        Globals globals = JsePlatform.standardGlobals();

        assertEquals(4294967294L,
                globals.get("bit32").get("bnot").call(LuaValue.valueOf(1)).tolong());
        assertEquals(7,
                globals.get("bit32").get("band").call(
                        LuaValue.valueOf(7), LuaValue.valueOf(15)).toint());
        assertEquals("a,b", globals.get("table").get("concat").call(
                tableOf("a", "b"), LuaValue.valueOf(",")).tojstring());
    }

    @Test
    void stringAndMathLibrariesWork() {
        Globals globals = JsePlatform.standardGlobals();

        assertEquals("OK", globals.get("string").get("upper")
                .call(LuaValue.valueOf("ok")).tojstring());
        assertEquals(3, globals.get("string").get("len")
                .call(LuaValue.valueOf("abc")).toint());
        assertEquals(4, globals.get("math").get("abs")
                .call(LuaValue.valueOf(-4)).toint());
        assertEquals(1, globals.get("math").get("floor")
                .call(LuaValue.valueOf(1.9)).toint());
    }

    @Test
    void debugLibraryLoadsSeparately() {
        Globals globals = JsePlatform.debugGlobals();

        assertTrue(globals.get("debug").istable());
        assertTrue(globals.get("debug").get("getinfo").isfunction());
        assertTrue(globals.get("debug").get("traceback").isfunction());
    }

    @Test
    void libraryBuilderBindsAnnotatedMethods() {
        Globals globals = new Globals();
        globals.load(new TestModule());

        assertEquals("hello", globals.get("test_module").get("hello")
                .call().tojstring());
        assertEquals(5, globals.get("test_module").get("add")
                .call(LuaValue.valueOf(2), LuaValue.valueOf(3)).toint());
    }

    private static LuaTable tableOf(String... values) {
        LuaTable table = new LuaTable();
        for (int i = 0; i < values.length; i++)
            table.set(i + 1, values[i]);
        return table;
    }

    private static final class TestModule implements LuaModule {
        @Override
        public void init(LuaTable self, LuaValue name, LuaValue env) {
        }

        @Override
        public String name() {
            return "test_module";
        }

        @LuaFunction
        public LuaValue hello() {
            return LuaValue.valueOf("hello");
        }

        @LuaFunction
        public LuaValue add(LuaValue left, LuaValue right) {
            return left.add(right);
        }
    }
}
