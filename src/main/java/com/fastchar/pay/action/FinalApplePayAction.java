package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.core.FastChar;
import com.fastchar.core.FastHandler;
import com.fastchar.pay.ali.AliPayUtils;
import com.fastchar.pay.ali.FastAliPayConfig;
import com.fastchar.pay.apple.ApplePurchaseUtils;
import com.fastchar.pay.entity.FinalPayOrderEntity;
import com.fastchar.pay.interfaces.IFastPayListener;
import com.fastchar.pay.interfaces.IFastPayProvider;
import com.fastchar.utils.FastStringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 沈建（Janesen）
 * @date 2020/6/8 14:48
 */
public class FinalApplePayAction extends FastAction {
    @Override
    protected String getRoute() {
        return "/pay/apple";
    }


    /**
     * 苹果内购回调，需要ios开发人员自主调用
     * 参数：
     * receipt 支付凭证，base64格式
     * payOrder 支付单号
     */
    public void callback() throws Exception {
        String receipt = getParam("receipt", true);
        String payOrder = getParam("payOrder", true);

        FastHandler handler = ApplePurchaseUtils.validReceipt(receipt);
        if (handler.getCode() == 0) {
            FinalPayOrderEntity details = FinalPayOrderEntity.dao().getDetails(payOrder);
            if (details != null && details.getInt("payOrderBack") == FinalPayOrderEntity.PayOrderBackEnum.未回调.ordinal()) {
                details.set("payOrderState", FinalPayOrderEntity.PayOrderStateEnum.已支付.ordinal());
                details.set("payOrderBack", FinalPayOrderEntity.PayOrderBackEnum.已回调.ordinal());
                details.update();

                IFastPayListener iFastPayListener = FastChar.getOverrides().singleInstance(false, IFastPayListener.class);
                if (iFastPayListener != null) {
                    iFastPayListener.onPayCallBack(details);
                }
                responseJson(0, "回调成功！");
            }
        }
        responseJson(-1, handler.getError());
    }


    /**
     * 发起苹果内购支付
     * 参数：
     * userId 用户Id【必填】
     * orderPrefix 订单前缀【必填】 生成订单的时候使用前缀，例如：BUY20191235123123412，前缀：BUY
     * orderTitle 订单标题【必填】
     * orderMoney 订单金额(元)【必填】{double}
     * orderData 订单附带数据
     * #return
     * 返回data对象的json属性说明：
     * order：支付订单的信息
     */
    public void purchase() throws Exception {
        setLogResponse(true);

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
        payOrderEntity.set("payOrderData", getParam("orderData"));
        payOrderEntity.set("payOrderType", FinalPayOrderEntity.PayOrderTypeEnum.苹果内购.ordinal());
        payOrderEntity.setAll(getParamToMap());

        IFastPayListener iFastPayListener = FastChar.getOverrides().singleInstance(false, IFastPayListener.class);
        if (iFastPayListener != null) {
            FastHandler handler = new FastHandler();
            iFastPayListener.onBeforePay(payOrderEntity, handler);
            if (handler.getCode() != 0) {
                responseJson(-1, handler.getError());
            }
        }

        if (payOrderEntity.save()) {
            Map<String, Object> data = new HashMap<>();
            data.put("order", payOrderEntity);
            responseJson(0, "请求成功！", data);
        } else {
            responseJson(-1, payOrderEntity.getError());
        }
        responseJson(-1, "操作失败！请稍后重试！");
    }

}
