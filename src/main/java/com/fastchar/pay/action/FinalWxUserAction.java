package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.core.FastChar;
import com.fastchar.pay.entity.FinalPayOrderEntity;
import com.fastchar.pay.entity.FinalWxAuthorizeEntity;
import com.fastchar.pay.interfaces.IFastPayProvider;
import com.fastchar.pay.wx.FastWxPayConfig;
import com.fastchar.pay.wx.FastWxPayUtils;
import com.fastchar.utils.FastStringUtils;

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
        FinalPayOrderEntity.PayOrderTypeEnum type = FinalPayOrderEntity.PayOrderTypeEnum.微信_APP;
        if (getUrlParams().size() > 0) {
            String urlParam = FastStringUtils.defaultValue(getUrlParam(0), "");
            if (urlParam.equalsIgnoreCase("native")) {
                type = FinalPayOrderEntity.PayOrderTypeEnum.微信_Native;
            } else if (urlParam.equalsIgnoreCase("js")) {
                type = FinalPayOrderEntity.PayOrderTypeEnum.微信_JS;
            }
        }

        FastWxPayConfig wxPayConfig = null;
        IFastPayProvider iFastPayProvider = FastChar.getOverrides().singleInstance(false, IFastPayProvider.class);
        if (iFastPayProvider != null) {
            String payConfigCode = iFastPayProvider.getPayConfigCode(type, this);
            if (FastStringUtils.isNotEmpty(payConfigCode)) {
                wxPayConfig = FastChar.getConfig(payConfigCode, FastWxPayConfig.class);
            }
        }
        if (wxPayConfig == null) {
            wxPayConfig = FastChar.getConfig(FastWxPayConfig.class);
        }

        Map auth_code = FastWxPayUtils.getUserInfo(wxPayConfig, getParam("auth_code", true));
        if (auth_code != null) {
            FinalWxAuthorizeEntity authorizeEntity = FinalWxAuthorizeEntity.newInstance();
            authorizeEntity.putAll(auth_code);
            authorizeEntity.push("openid");
            responseJson(0, "获取成功", authorizeEntity);
        }
        responseJson(-1, "微信授权失败！请稍后重试！");
    }
}
