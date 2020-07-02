package com.fastchar.pay.interfaces;

import com.fastchar.core.FastAction;
import com.fastchar.pay.entity.FinalPayOrderEntity;

/**
 * 支付操作类
 * @author 沈建（Janesen）
 * @date 2020/5/26 09:48
 */
public interface IFastPayProvider {

    String getPayConfigCode(FinalPayOrderEntity.PayOrderTypeEnum payType, FastAction action);

}
