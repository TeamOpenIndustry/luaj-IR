package de.poizzy.luaj.lib;

import org.junit.jupiter.api.Test;
import de.poizzy.luaj.Globals;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.lib.jse.JsePlatform;

import static org.junit.jupiter.api.Assertions.*;

class LibraryBuilderTest {

    @Test
    void loadsAnnotationBasedLibrary() {
        Globals globals = JsePlatform.standardGlobals();

        assertEquals(4294967294L,
                globals.get("bit32").get("bnot")
                        .call(LuaValue.valueOf(1)).tolong());
        assertEquals("OK",
                globals.get("string").get("upper")
                        .call(LuaValue.valueOf("ok")).tojstring());
    }
}
