package com.fastchar.pay.interfaces;

import com.fastchar.pay.entity.FinalPayOrderEntity;

public interface IFastBalanceOrderProvider {

    /**
     * 获取当前用户的余额
     * @param orderEntity 订单对象
     * @return 余额
     */
    double getBalance(FinalPayOrderEntity orderEntity);

    /**
     * 判断当前用户的支付密码是否正确
     * @param orderEntity 订单对象
     * @param payPassword 支付密码
     * @return 布尔值
     */
    boolean validatePassword(FinalPayOrderEntity orderEntity, String payPassword);
}
