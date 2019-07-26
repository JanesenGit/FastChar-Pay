package com.fastchar.pay.interfaces;

public interface IFastBalanceProvider {

    /**
     * 获取当前用户的余额
     * @param userId 用户Id
     * @return 余额
     */
    double getBalance(int userId);

    /**
     * 判断当前用户的支付密码是否正确
     * @param userId 用户Id
     * @param payPassword 支付密码
     * @return 布尔值
     */
    boolean validatePassword(int userId, String payPassword);


}
