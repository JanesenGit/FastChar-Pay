package com.fastchar.pay.entity;

import com.fastchar.core.FastChar;
import com.fastchar.pay.entity.getset.AbstractFinalPayOrderEntity;
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
        set("payOrderState", 0);
        set("payOrderType", 0);
        set("orderDateTime", FastDateUtils.getDateString());
    }

    public enum PayOrderStateEnum {
        待支付,
        已支付,
        已完成
    }

    public enum PayOrderTypeEnum {
        余额,
        支付宝,
        微信
    }


    /**
     * 获得数据详情
     */
    public FinalPayOrderEntity getDetails(String payOrderCode) {
        String sqlStr = "select * from final_pay_order as t " +
                " where t.payOrderCode = ?  ";
        return selectFirstBySql(sqlStr, payOrderCode);
    }

}
