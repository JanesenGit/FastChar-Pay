package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.core.FastChar;
import com.fastchar.core.FastHandler;
import com.fastchar.pay.entity.FinalPayErrorEntity;
import com.fastchar.pay.entity.FinalPayOrderEntity;
import com.fastchar.pay.interfaces.IFastBalanceOrderProvider;
import com.fastchar.pay.interfaces.IFastBalanceProvider;
import com.fastchar.pay.interfaces.IFastPayListener;
import com.fastchar.utils.FastStringUtils;

public class FinalBalanceAction extends FastAction {
    @Override
    protected String getRoute() {
        return "/pay/balance";
    }


    /**
     * 发起余额支付
     * 参数：
     * userId 用户编号【必填】{int}
     * payPassword 支付密码【必填】
     * orderPrefix 订单前缀【必填】 生成订单的时候使用前缀，例如：BUY20191235123123412，前缀：BUY
     * orderTitle 订单标题【必填】
     * orderMoney 订单金额(元)【必填】{double}
     * orderData 订单附带数据
     */
    public void app() throws Exception {
        int userId = getParamToInt("userId", true);
        String payPassword = getParam("payPassword", true);
        String orderPrefix = getParam("orderPrefix", true).toUpperCase();
        String orderTitle = getParam("orderTitle", true);
        double orderMoney = getParamToDouble("orderMoney", true);
        if (orderMoney <= 0.01) {
            responseJson(-1, "订单金额不得小于0.01元！");
        }

        int errorCount = FinalPayErrorEntity.dao().countTodayError(userId);
        int nextCount = Math.max(4 - errorCount, 0);
        String errorInfo = null;
        if (nextCount > 0) {
            errorInfo = "今日还剩余" + nextCount + "次！";
        }else{
            responseJson(-1, "您今日支付密码错误次数已超限！请明日再试！");
        }

        FinalPayOrderEntity payOrderEntity = new FinalPayOrderEntity();
        String payOrderCode = FastStringUtils.buildOnlyCode(orderPrefix);
        payOrderEntity.set("payOrderCode", payOrderCode);
        payOrderEntity.set("payOrderTitle", orderTitle);
        payOrderEntity.set("payOrderMoney", orderMoney);
        payOrderEntity.set("payOrderState", FinalPayOrderEntity.PayOrderStateEnum.已支付.ordinal());
        payOrderEntity.set("payOrderType", FinalPayOrderEntity.PayOrderTypeEnum.余额.ordinal());
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

        IFastBalanceProvider iFastBalanceListener = FastChar.getOverrides()
                .singleInstance(false, IFastBalanceProvider.class);

        FinalPayErrorEntity payErrorEntity = new FinalPayErrorEntity();
        payErrorEntity.set("userId", userId);

        if (iFastBalanceListener != null) {
            boolean validatePassword = iFastBalanceListener.validatePassword(userId, payPassword);
            if (!validatePassword) {
                payErrorEntity.save();
                responseJson(-1, "支付失败！支付密码错误！" + errorInfo);
            }

            double balance = iFastBalanceListener.getBalance(userId);
            if (orderMoney > balance) {
                responseJson(-1, "支付失败！您的余额不足！");
            }
        }

        IFastBalanceOrderProvider iFastBalanceOrderProvider = FastChar.getOverrides()
                .singleInstance(false, IFastBalanceOrderProvider.class);
        if (iFastBalanceOrderProvider != null) {
            boolean validatePassword = iFastBalanceOrderProvider.validatePassword(payOrderEntity, payPassword);
            if (!validatePassword) {
                payErrorEntity.save();
                responseJson(-1, "支付失败！支付密码错误！" + errorInfo);
            }

            double balance = iFastBalanceOrderProvider.getBalance(payOrderEntity);
            if (orderMoney > balance) {
                responseJson(-1, "支付失败！您的余额不足！");
            }
        }


        if (payOrderEntity.save()) {
            if (iFastPayListener != null) {
                iFastPayListener.onPayCallBack(payOrderEntity);
            }
            payErrorEntity.delete("userId");
            responseJson(0, "支付成功！");
        }
        responseJson(-1, "操作失败！请稍后重试！");
    }

}
