package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.core.FastChar;
import com.fastchar.pay.ali.AliPayUtils;
import com.fastchar.pay.ali.FastAliPayConfig;
import com.fastchar.pay.entity.FinalAliAuthorizeEntity;
import com.fastchar.pay.entity.FinalPayOrderEntity;
import com.fastchar.pay.entity.FinalWxAuthorizeEntity;
import com.fastchar.pay.interfaces.IFastPayProvider;
import com.fastchar.pay.wx.WxPayUtils;
import com.fastchar.utils.FastStringUtils;

import java.util.Map;

public class FinalAliUserAction extends FastAction {
    @Override
    protected String getRoute() {
        return "/ali/user";
    }

    /**
     * 根据支付宝授权码获得支付宝用户信息
     * 参数：
     * auth_code 授权码【必填】
     */
    public void author() throws Exception {
        FinalPayOrderEntity.PayOrderTypeEnum type = FinalPayOrderEntity.PayOrderTypeEnum.支付宝_APP;
        if (getUrlParams().size() > 0) {
            String urlParam = FastStringUtils.defaultValue(getUrlParam(0), "");
            if (urlParam.equalsIgnoreCase("page")) {
                type = FinalPayOrderEntity.PayOrderTypeEnum.支付宝_Page;
            }
        }

        FastAliPayConfig aliPayConfig = null;
        IFastPayProvider iFastPayProvider = FastChar.getOverrides().singleInstance(false, IFastPayProvider.class);
        if (iFastPayProvider != null) {
            String payConfigCode = iFastPayProvider.getPayConfigCode(type, this);
            if (FastStringUtils.isNotEmpty(payConfigCode)) {
                aliPayConfig = FastChar.getConfig(payConfigCode, FastAliPayConfig.class);
            }
        }
        if (aliPayConfig == null) {
            aliPayConfig = FastChar.getConfig(FastAliPayConfig.class);
        }
        Map auth_code = AliPayUtils.getUserInfo(aliPayConfig, getParam("auth_code", true));
        if (auth_code != null) {
            FinalAliAuthorizeEntity authorizeEntity = FinalAliAuthorizeEntity.newInstance();
            authorizeEntity.putAll(auth_code);
            authorizeEntity.push("user_id");
            responseJson(0, "获取成功", authorizeEntity);
        }
        responseJson(-1, "微信授权失败！请稍后重试！");
    }
}
