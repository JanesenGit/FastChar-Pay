package com.fastchar.pay.interfaces;

import com.fastchar.core.FastHandler;
import com.fastchar.pay.entity.FinalPayOrderEntity;

/**
 * 支付监听类
 */
public interface IFastPayListener {

    void onBeforePay(FinalPayOrderEntity orderEntity, FastHandler handler);

    void onPayCallBack(FinalPayOrderEntity orderEntity);
}
