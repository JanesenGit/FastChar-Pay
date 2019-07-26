package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.core.FastChar;
import com.fastchar.pay.entity.FinalPayOrderEntity;
import com.fastchar.pay.interfaces.IFastPayListener;
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
        Map<String, String> callBack = WxPayUtils.getCallBack(getRequest());
        if (WxPayUtils.verifyCallBack(callBack)) {
            String out_trade_no = callBack.get("out_trade_no");
            double returnMoney = WxPayUtils.getReturnMoney(callBack);

            FinalPayOrderEntity details = FinalPayOrderEntity.dao().getDetails(out_trade_no);
            if (details != null) {
                details.set("payOrderMoney", returnMoney);
                details.set("payOrderState", FinalPayOrderEntity.PayOrderStateEnum.已支付.ordinal());
                details.update();

                IFastPayListener iFastPayListener = FastChar.getOverrides().singleInstance(false, IFastPayListener.class);
                if (iFastPayListener != null) {
                    iFastPayListener.onPayCallBack(details);
                }
            }
            responseText("success");
        }
        responseJson(-1, "微信回调校验失败！");
    }


    /**
     * 发起微信APP支付
     * 参数：
     * userId 用户Id【必填】
     * orderPrefix 订单前缀【必填】 生成订单的时候使用前缀，例如：BUY20191235123123412，前缀：BUY
     * orderTitle 订单标题【必填】
     * orderMoney 订单金额(元)【必填】{double}
     * orderData 订单附带数据
     */
    public void app() throws Exception {
        FastWxPayConfig wxPayConfig = FastChar.getConfig(FastWxPayConfig.class);
        if (FastStringUtils.isEmpty(wxPayConfig.getNotifyUrl())) {
            wxPayConfig.setNotifyUrl(getProjectHost() + "pay/wx/callback");
        }

        int userId = getParamToInt("userId", true);
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
        payOrderEntity.set("userId", userId);
        payOrderEntity.set("payOrderData", getParam("orderData"));
        payOrderEntity.set("payOrderType", FinalPayOrderEntity.PayOrderTypeEnum.微信.ordinal());

        Map<String, Object> appPay = WxPayUtils.requestAppPay(payOrderCode, orderTitle, orderTitle, orderMoney, getRemoveIp());
        if (appPay != null) {
            String msg = appPay.get("msg").toString();
            if (msg.toLowerCase().equals("ok")) {
                if (payOrderEntity.save()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("wx", appPay);
                    data.put("order", payOrderEntity);
                    responseJson(0, "请求成功！", data);
                }
            } else {
                responseJson(-1, "微信支付调用失败，服务器端错误：" + msg);
            }
        }
        responseJson(-1, "操作失败！请稍后重试！");
    }

}
