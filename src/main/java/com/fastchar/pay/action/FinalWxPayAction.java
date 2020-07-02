package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.core.FastChar;
import com.fastchar.core.FastHandler;
import com.fastchar.pay.ali.FastAliPayConfig;
import com.fastchar.pay.entity.FinalPayOrderEntity;
import com.fastchar.pay.interfaces.IFastPayListener;
import com.fastchar.pay.interfaces.IFastPayProvider;
import com.fastchar.pay.wx.FastWxPayConfig;
import com.fastchar.pay.wx.WxPayUtils;
import com.fastchar.utils.FastStringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FinalWxPayAction extends FastAction {
    @Override
    protected String getRoute() {
        return "/pay/wx";
    }


    /**
     * 微信回调
     */
    public void callback() throws IOException {
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

        Map<String, String> callBack = WxPayUtils.getCallBack(getRequest());
        FastHandler handler = WxPayUtils.verifyCallBack(wxPayConfig,callBack);
        if (handler.getCode() == 0) {
            String out_trade_no = callBack.get("out_trade_no");
            double returnMoney = WxPayUtils.getReturnMoney(callBack);

            FinalPayOrderEntity details = FinalPayOrderEntity.dao().getDetails(out_trade_no);
            if (details != null && details.getInt("payOrderBack") == FinalPayOrderEntity.PayOrderBackEnum.未回调.ordinal()) {
                details.set("payOrderMoney", returnMoney);
                details.set("payOrderState", FinalPayOrderEntity.PayOrderStateEnum.已支付.ordinal());
                details.set("payOrderBack", FinalPayOrderEntity.PayOrderBackEnum.已回调.ordinal());
                details.update();

                IFastPayListener iFastPayListener = FastChar.getOverrides().singleInstance(false, IFastPayListener.class);
                if (iFastPayListener != null) {
                    iFastPayListener.onPayCallBack(details);
                }
            }
            responseText("success");
        }
        responseText(500, handler.getError());
    }


    /**
     * 发起微信APP支付
     * 参数：
     * userId 用户Id【必填】
     * orderPrefix 订单前缀【必填】 生成订单的时候使用前缀，例如：BUY20191235123123412，前缀：BUY
     * orderTitle 订单标题【必填】
     * orderMoney 订单金额(元)【必填】{double}
     * orderData 订单附带数据
     * #return
     * 返回data对象的json属性说明：
     * wx：发起微信支付的参数，直接传入微信sdk中即可！
     * order：支付订单的信息
     */
    public void app() throws Exception {
        setLogResponse(true);
        FastWxPayConfig wxPayConfig = null;
        IFastPayProvider iFastPayProvider = FastChar.getOverrides().singleInstance(false, IFastPayProvider.class);
        if (iFastPayProvider != null) {
            String payConfigCode = iFastPayProvider.getPayConfigCode(FinalPayOrderEntity.PayOrderTypeEnum.微信_APP, this);
            if (FastStringUtils.isNotEmpty(payConfigCode)) {
                wxPayConfig = FastChar.getConfig(payConfigCode, FastWxPayConfig.class);
            }
        }
        if (wxPayConfig == null) {
            wxPayConfig = FastChar.getConfig(FastWxPayConfig.class);
        }
        if (FastStringUtils.isEmpty(wxPayConfig.getNotifyUrl())) {
            wxPayConfig.setNotifyUrl(getProjectHost() + "pay/wx/callback");
        }

        String orderPrefix = getParam("orderPrefix", true).toUpperCase();
        String orderTitle = getParam("orderTitle", true);
        double orderMoney = getParamToDouble("orderMoney", true);
        if (orderMoney <= 0.01) {
            responseJson(-1, "订单金额不得小于0.01元！");
        }
        FinalPayOrderEntity payOrderEntity = new FinalPayOrderEntity();
        String payOrderCode = FastStringUtils.buildOnlyCode(orderPrefix);
        payOrderEntity.set("payOrderCode", payOrderCode);
        payOrderEntity.set("payOrderTitle", orderTitle);
        payOrderEntity.set("payOrderMoney", orderMoney);
        payOrderEntity.set("payOrderType", FinalPayOrderEntity.PayOrderTypeEnum.微信_APP.ordinal());
        payOrderEntity.set("payOrderData", getParam("orderData"));
        payOrderEntity.setAll(getParamToMap());

        IFastPayListener iFastPayListener = FastChar.getOverrides().singleInstance(false, IFastPayListener.class);
        if (iFastPayListener != null) {
            FastHandler handler = new FastHandler();
            iFastPayListener.onBeforePay(payOrderEntity, handler);
            if (handler.getCode() != 0) {
                responseJson(-1, handler.getError());
            }
        }


        Map<String, Object> appPay = WxPayUtils.requestAppPay(wxPayConfig, payOrderCode, orderTitle, orderTitle, orderMoney, getRemoteIp());
        if (appPay != null) {
            String msg = appPay.get("msg").toString();
            if (msg.toLowerCase().equals("ok")) {
                if (payOrderEntity.save()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("wx", appPay);
                    data.put("order", payOrderEntity);
                    responseJson(0, "请求成功！", data);
                } else {
                    responseJson(-1, payOrderEntity.getError());
                }
            } else {
                responseJson(-1, "微信APP支付调用失败，服务器端错误：" + msg);
            }
        }
        responseJson(-1, "操作失败！请稍后重试！");
    }


    /**
     * 发起微信公众号或者小程序支付
     * 参数：
     * userId 用户Id【必填】
     * openid 微信OpenId【必填】
     * orderPrefix 订单前缀【必填】 生成订单的时候使用前缀，例如：BUY20191235123123412，前缀：BUY
     * orderTitle 订单标题【必填】
     * orderMoney 订单金额(元)【必填】{double}
     * orderData 订单附带数据
     * #return
     * 返回data对象的json属性说明：
     * wx：发起微信支付的参数，直接传入微信sdk中即可！
     * order：支付订单的信息
     */
    public void js() throws Exception {
        setLogResponse(true);
        FastWxPayConfig wxPayConfig = null;
        IFastPayProvider iFastPayProvider = FastChar.getOverrides().singleInstance(false, IFastPayProvider.class);
        if (iFastPayProvider != null) {
            String payConfigCode = iFastPayProvider.getPayConfigCode(FinalPayOrderEntity.PayOrderTypeEnum.微信_JS, this);
            if (FastStringUtils.isNotEmpty(payConfigCode)) {
                wxPayConfig = FastChar.getConfig(payConfigCode, FastWxPayConfig.class);
            }
        }
        if (wxPayConfig == null) {
            wxPayConfig = FastChar.getConfig(FastWxPayConfig.class);
        }
        if (FastStringUtils.isEmpty(wxPayConfig.getNotifyUrl())) {
            wxPayConfig.setNotifyUrl(getProjectHost() + "pay/wx/callback/js");
        }

        String orderPrefix = getParam("orderPrefix", true).toUpperCase();
        String orderTitle = getParam("orderTitle", true);
        String openid = getParam("openid", true);
        double orderMoney = getParamToDouble("orderMoney", true);
        if (orderMoney <= 0.01) {
            responseJson(-1, "订单金额不得小于0.01元！");
        }
        FinalPayOrderEntity payOrderEntity = new FinalPayOrderEntity();
        String payOrderCode = FastStringUtils.buildOnlyCode(orderPrefix);
        payOrderEntity.set("payOrderCode", payOrderCode);
        payOrderEntity.set("payOrderTitle", orderTitle);
        payOrderEntity.set("payOrderMoney", orderMoney);
        payOrderEntity.set("payOrderType", FinalPayOrderEntity.PayOrderTypeEnum.微信_JS.ordinal());
        payOrderEntity.set("payOrderData", getParam("orderData"));
        payOrderEntity.setAll(getParamToMap());

        IFastPayListener iFastPayListener = FastChar.getOverrides().singleInstance(false, IFastPayListener.class);
        if (iFastPayListener != null) {
            FastHandler handler = new FastHandler();
            iFastPayListener.onBeforePay(payOrderEntity, handler);
            if (handler.getCode() != 0) {
                responseJson(-1, handler.getError());
            }
        }


        Map<String, Object> appPay = WxPayUtils.requestJSAPIPay(wxPayConfig, openid, payOrderCode, orderTitle, orderTitle, orderMoney, getRemoteIp());
        if (appPay != null) {
            String msg = appPay.get("msg").toString();
            if (msg.toLowerCase().equals("ok")) {
                if (payOrderEntity.save()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("wx", appPay);
                    data.put("order", payOrderEntity);
                    responseJson(0, "请求成功！", data);
                } else {
                    responseJson(-1, payOrderEntity.getError());
                }
            } else {
                responseJson(-1, "微信JS支付调用失败，服务器端错误：" + msg);
            }
        }
        responseJson(-1, "操作失败！请稍后重试！");
    }


    /**
     * 发起微信电脑网站支付,扫商户二维码支付
     * 参数：
     * userId 用户Id【必填】
     * orderPrefix 订单前缀【必填】 生成订单的时候使用前缀，例如：BUY20191235123123412，前缀：BUY
     * orderTitle 订单标题【必填】
     * orderMoney 订单金额(元)【必填】{double}
     * orderData 订单附带数据
     * #return
     * 返回data对象的json属性说明：
     * wx：发起微信支付参数集合，其中code为生成二维码的内容
     * order：支付订单的信息
     */
    public void page() throws Exception {
        setLogResponse(true);
        FastWxPayConfig wxPayConfig = null;
        IFastPayProvider iFastPayProvider = FastChar.getOverrides().singleInstance(false, IFastPayProvider.class);
        if (iFastPayProvider != null) {
            String payConfigCode = iFastPayProvider.getPayConfigCode(FinalPayOrderEntity.PayOrderTypeEnum.微信_Native, this);
            if (FastStringUtils.isNotEmpty(payConfigCode)) {
                wxPayConfig = FastChar.getConfig(payConfigCode, FastWxPayConfig.class);
            }
        }
        if (wxPayConfig == null) {
            wxPayConfig = FastChar.getConfig(FastWxPayConfig.class);
        }
        if (FastStringUtils.isEmpty(wxPayConfig.getNotifyUrl())) {
            wxPayConfig.setNotifyUrl(getProjectHost() + "pay/wx/callback/native");
        }

        String orderPrefix = getParam("orderPrefix", true).toUpperCase();
        String orderTitle = getParam("orderTitle", true);
        double orderMoney = getParamToDouble("orderMoney", true);
        if (orderMoney <= 0.01) {
            responseJson(-1, "订单金额不得小于0.01元！");
        }
        FinalPayOrderEntity payOrderEntity = new FinalPayOrderEntity();
        String payOrderCode = FastStringUtils.buildOnlyCode(orderPrefix);
        payOrderEntity.set("payOrderCode", payOrderCode);
        payOrderEntity.set("payOrderTitle", orderTitle);
        payOrderEntity.set("payOrderMoney", orderMoney);
        payOrderEntity.set("payOrderType", FinalPayOrderEntity.PayOrderTypeEnum.微信_Native.ordinal());
        payOrderEntity.set("payOrderData", getParam("orderData"));
        payOrderEntity.setAll(getParamToMap());

        IFastPayListener iFastPayListener = FastChar.getOverrides().singleInstance(false, IFastPayListener.class);
        if (iFastPayListener != null) {
            FastHandler handler = new FastHandler();
            iFastPayListener.onBeforePay(payOrderEntity, handler);
            if (handler.getCode() != 0) {
                responseJson(-1, handler.getError());
            }
        }


        Map<String, Object> appPay = WxPayUtils.requestNativePay(wxPayConfig, payOrderCode, orderTitle, orderTitle, orderMoney, getRemoteIp());
        if (appPay != null) {
            String msg = appPay.get("msg").toString();
            if (msg.toLowerCase().equals("ok")) {
                if (payOrderEntity.save()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("wx", appPay);
                    data.put("order", payOrderEntity);
                    responseJson(0, "请求成功！", data);
                } else {
                    responseJson(-1, payOrderEntity.getError());
                }
            } else {
                responseJson(-1, "微信APP支付调用失败，服务器端错误：" + msg);
            }
        }
        responseJson(-1, "操作失败！请稍后重试！");
    }


}
