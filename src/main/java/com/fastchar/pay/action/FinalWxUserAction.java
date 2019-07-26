package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.pay.entity.FinalWxAuthorizeEntity;
import com.fastchar.pay.wx.WxPayUtils;
import java.util.Map;

public class FinalWxUserAction extends FastAction {
    @Override
    protected String getRoute() {
        return "/wx/user";
    }

    /**
     * 根据微信授权码获得微信用户信息
     * 参数：
     * auth_code 授权码【必填】
     */
    public void author() {
        Map auth_code = WxPayUtils.getUserInfo(getParam("auth_code", true));
        if (auth_code != null) {
            FinalWxAuthorizeEntity authorizeEntity = FinalWxAuthorizeEntity.newInstance();
            authorizeEntity.putAll(auth_code);
            authorizeEntity.push("openid");
            responseJson(0, "获取成功", authorizeEntity);
        }
        responseJson(-1, "微信授权失败！请稍后重试！");
    }
}
