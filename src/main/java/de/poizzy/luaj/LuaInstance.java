package de.poizzy.luaj;

import de.poizzy.luaj.lib.LuaClass;

public class LuaInstance extends LuaTable {
    public final LuaClass self;
    public LuaInstance(int narray, int nhash, LuaClass self) {
        super(narray, nhash);
        this.self = self;
    }
}
