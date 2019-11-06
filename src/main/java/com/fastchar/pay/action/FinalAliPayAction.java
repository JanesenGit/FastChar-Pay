package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.core.FastChar;
import com.fastchar.pay.ali.FastAliPayConfig;
import com.fastchar.pay.ali.AliPayUtils;
import com.fastchar.pay.entity.FinalPayOrderEntity;
import com.fastchar.pay.interfaces.IFastPayListener;
import com.fastchar.utils.FastStringUtils;

import java.util.HashMap;
import java.util.Map;

public class FinalAliPayAction extends FastAction {
    @Override
    protected String getRoute() {
        return "/pay/ali";
    }

    /**
     * 支付宝回调
     */
    public void callback() {
        if (AliPayUtils.verifyCallBack(getRequest())) {
            String out_trade_no = getParam("out_trade_no");
            double total_amount = getParamToDouble("total_amount");

            FinalPayOrderEntity details = FinalPayOrderEntity.dao().getDetails(out_trade_no);
            if (details != null && details.getInt("payOrderBack") == FinalPayOrderEntity.PayOrderBackEnum.未回调.ordinal()) {
                details.set("payOrderMoney", total_amount);
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
        responseJson(-1, "支付宝回调校验失败！");
    }


    /**
     * 发起支付宝APP支付
     * 参数：
     * userId 用户Id【必填】
     * orderPrefix 订单前缀【必填】 生成订单的时候使用前缀，例如：BUY20191235123123412，前缀：BUY
     * orderTitle 订单标题【必填】
     * orderMoney 订单金额(元)【必填】{double}
     * orderData 订单附带数据
     * #return
     * 返回data对象的json属性说明：
     * url：发起支付宝支付的url参数，直接传入支付宝sdk中即可！
     * order：支付订单的信息
     */
    public void app() throws Exception {
        FastAliPayConfig aliPayConfig = FastChar.getConfig(FastAliPayConfig.class);
        if (FastStringUtils.isEmpty(aliPayConfig.getNotifyUrl())) {
            aliPayConfig.setNotifyUrl(getProjectHost() + "pay/ali/callback");
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
        payOrderEntity.set("payOrderType", FinalPayOrderEntity.PayOrderTypeEnum.支付宝.ordinal());

        String appPay = AliPayUtils.requestAppPay(payOrderCode, orderTitle, orderTitle, orderMoney);
        if (FastStringUtils.isNotEmpty(appPay)) {
            if (payOrderEntity.save()) {
                Map<String, Object> data = new HashMap<>();
                data.put("url", appPay);
                data.put("order", payOrderEntity);
                responseJson(0, "请求成功！", data);
            }
        }
        responseJson(-1, "操作失败！请稍后重试！");
    }

}
