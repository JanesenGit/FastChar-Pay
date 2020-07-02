package com.fastchar.pay;

import com.fastchar.core.FastChar;
import com.fastchar.interfaces.IFastConfig;

public class FastPayConfig implements IFastConfig {
    private boolean debug;

    public boolean isDebug() {
        return debug;
    }

    public FastPayConfig setDebug(boolean debug) {
        this.debug = debug;
        if (debug) {
            FastChar.getLog().error("特别注意：支付插件在调试模式下，支付宝和微信的公网回调默认都是失败！");
        }
        return this;
    }
}
