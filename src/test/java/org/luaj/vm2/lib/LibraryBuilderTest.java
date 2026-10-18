package org.luaj.vm2.lib;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

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
