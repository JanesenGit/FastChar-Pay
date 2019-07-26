package com.fastchar.pay;

import com.fastchar.interfaces.IFastConfig;

public class FastPayConfig implements IFastConfig {
    private boolean debug;

    public boolean isDebug() {
        return debug;
    }

    public FastPayConfig setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }
}
