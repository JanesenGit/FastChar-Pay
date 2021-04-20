package com.fastchar.pay.entity;

import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.fastchar.core.FastChar;
import com.fastchar.core.FastHandler;
import com.fastchar.pay.ali.FastAliPayConfig;
import com.fastchar.pay.ali.FastAliPayUtils;
import com.fastchar.pay.entity.getset.AbstractFinalPayOrderEntity;
import com.fastchar.pay.interfaces.IFastPayProvider;
import com.fastchar.pay.wx.FastWxPayConfig;
import com.fastchar.pay.wx.FastWxPayUtils;
import com.fastchar.utils.FastDateUtils;

import java.util.*;

import com.fastchar.utils.FastStringUtils;

public class FinalPayOrderEntity extends AbstractFinalPayOrderEntity {
    public static FinalPayOrderEntity dao() {
        return FastChar.getOverrides().singleInstance(FinalPayOrderEntity.class);
    }

    @Override
    public String getTableName() {
        return "final_pay_order";
    }

    @Override
    public String getTableDetails() {
        return "支付订单";
    }

    @Override
    public void setDefaultValue() {
        set("payOrderState", PayOrderStateEnum.待支付.ordinal());
        set("payOrderType", 0);
        set("payOrderBack", 0);
        set("userId", -1);
        set("orderDateTime", FastDateUtils.getDateString());
    }

    public enum PayOrderStateEnum {
        待支付,
        已支付,
        已完成,
        已结束,
        已关闭,
        已失败,
        已退款
    }

    public enum PayOrderTypeEnum {
        余额,
        支付宝_APP,
        微信_APP,
        微信_JS,
        支付宝_JS,
        苹果内购,
        支付宝_Page,
        微信_Native
    }

    public enum PayOrderBackEnum {
        未回调,
        已回调
    }


    /**
     * 获得数据详情
     */
    public FinalPayOrderEntity getDetails(String payOrderCode) {
        String sqlStr = "select * from final_pay_order as t " +
                " where t.payOrderCode = ?  ";
        return selectFirstBySql(sqlStr, payOrderCode);
    }


    /**
     * 订单退款
     *
     * @param payOrderCode 支付单号
     * @param reason       退款原因
     */
    public FastHandler refund(String payOrderCode, String reason) {
        return refund(payOrderCode, -1, reason);
    }

    /**
     * 订单退款
     *
     * @param payOrderCode 支付单号
     * @param money        退款金额
     * @param reason       退款原因
     */
    public FastHandler refund(String payOrderCode, double money, String reason) {
        FastHandler handler = new FastHandler();
        FinalPayOrderEntity details = getDetails(payOrderCode);
        if (details == null) {
            handler.setCode(-1);
            handler.setError("支付订单信息不存在！");
            return handler;
        }
        if (money == -1) {
            money = details.getPayOrderMoney();
        }
        try {
            if (details.getPayOrderType() == PayOrderTypeEnum.支付宝_APP
                    || details.getPayOrderType() == PayOrderTypeEnum.支付宝_JS
                    || details.getPayOrderType() == PayOrderTypeEnum.支付宝_Page) {
                FastAliPayConfig aliPayConfig = null;
                if (FastStringUtils.isNotEmpty(getPayConfigCode())) {
                    aliPayConfig = FastChar.getConfig(getPayConfigCode(), FastAliPayConfig.class);
                }
                if (aliPayConfig == null) {
                    aliPayConfig = FastChar.getConfig(FastAliPayConfig.class);
                }

                AlipayTradeRefundResponse response = FastAliPayUtils.requestRefund(aliPayConfig,
                        payOrderCode, reason, money);

                if (response.isSuccess()) {
                    handler.setCode(0);
                    handler.setError("退款请求成功！");
                    return handler;
                }
                handler.setCode(-1);
                handler.setError(response.getMsg() + response.getSubMsg());
                return handler;
            } else if (details.getPayOrderType() == PayOrderTypeEnum.微信_Native
                    || details.getPayOrderType() == PayOrderTypeEnum.微信_APP
                    || details.getPayOrderType() == PayOrderTypeEnum.微信_JS) {
                FastWxPayConfig wxPayConfig = null;
                if (FastStringUtils.isNotEmpty(getPayConfigCode())) {
                    wxPayConfig = FastChar.getConfig(getPayConfigCode(), FastWxPayConfig.class);
                }
                if (wxPayConfig == null) {
                    wxPayConfig = FastChar.getConfig(FastWxPayConfig.class);
                }
                Map<String, String> result = FastWxPayUtils.requestRefund(wxPayConfig, payOrderCode, reason, details.getPayOrderMoney(),
                        money);
                if (result == null) {
                    handler.setCode(-1);
                    handler.setError("微信退款接口请求失败！");
                    return handler;
                }
                if (FastWxPayUtils.isRequestSuccess(result) && FastWxPayUtils.isResultSuccess(result)) {
                    handler.setCode(0);
                    handler.setError("退款请求成功！");
                    return handler;
                } else {
                    handler.setCode(-1);
                    handler.setError("微信退款接口请求失败！" + FastWxPayUtils.getRequestMsg(result));
                    return handler;
                }
            } else {
                handler.setCode(-1);
                handler.setError("暂不支持此支付类型退款！");
                return handler;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return handler;
    }

}
