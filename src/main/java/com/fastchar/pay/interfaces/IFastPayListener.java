package com.fastchar.pay.interfaces;

import com.fastchar.pay.entity.FinalPayOrderEntity;

public interface IFastPayListener {

    void onPayCallBack(FinalPayOrderEntity orderEntity);
}
