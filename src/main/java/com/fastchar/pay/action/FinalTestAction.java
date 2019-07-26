package com.fastchar.pay.action;

import com.fastchar.core.FastAction;
import com.fastchar.core.FastChar;
import com.fastchar.pay.FastPayConfig;
import com.fastchar.pay.entity.FinalPayOrderEntity;
import com.fastchar.pay.interfaces.IFastPayListener;

public class FinalTestAction extends FastAction {
    @Override
    protected String getRoute() {
        return "/pay/test";
    }


    /**
     * 模拟支付回调成功
     * 参数：
     * orderCode 订单编号【必填】
     */
    public void success() {
        if (!FastChar.getConfig(FastPayConfig.class).isDebug()) {
            responseJson(-1, "禁止模拟回调！");
            return;
        }

        String orderCode = getParam("orderCode", true);
        FinalPayOrderEntity details = FinalPayOrderEntity.dao().getDetails(orderCode);
        if (details != null) {
            details.setPayOrderState(FinalPayOrderEntity.PayOrderStateEnum.已支付);
            if (details.update()) {
                IFastPayListener iFastPayListener = FastChar.getOverrides().singleInstance(IFastPayListener.class);
                if (iFastPayListener != null) {
                    iFastPayListener.onPayCallBack(details);
                }
                responseJson(0, "回调成功！");
            }
        }
        responseJson(-1, "操作失败！订单不存在！");
    }

}
