package com.fastchar.pay.entity.getset;

import com.fastchar.core.FastEntity;
import com.fastchar.pay.entity.FinalPayOrderEntity.*;
import com.fastchar.pay.entity.FinalPayOrderEntity;

import java.util.Date;

public abstract class AbstractFinalPayOrderEntity extends FastEntity<FinalPayOrderEntity> {
    public int getPayOrderId() {
        return getInt("payOrderId");
    }

    public void setPayOrderId(int payOrderId) {
        set("payOrderId", payOrderId);
    }

    public String getPayOrderCode() {
        return getString("payOrderCode");
    }

    public void setPayOrderCode(String payOrderCode) {
        set("payOrderCode", payOrderCode);
    }

    public String getPayOrderTitle() {
        return getString("payOrderTitle");
    }

    public void setPayOrderTitle(String payOrderTitle) {
        set("payOrderTitle", payOrderTitle);
    }

    public double getPayOrderMoney() {
        return getDouble("payOrderMoney");
    }

    public void setPayOrderMoney(double payOrderMoney) {
        set("payOrderMoney", payOrderMoney);
    }

    public String getPayOrderData() {
        return getString("payOrderData");
    }

    public void setPayOrderData(String payOrderData) {
        set("payOrderData", payOrderData);
    }

    public PayOrderStateEnum getPayOrderState() {
        return getEnum("payOrderState", PayOrderStateEnum.class);
    }

    public void setPayOrderState(PayOrderStateEnum payOrderState) {
        set("payOrderState", payOrderState.ordinal());
    }

    public PayOrderTypeEnum getPayOrderType() {
        return getEnum("payOrderType", PayOrderTypeEnum.class);
    }

    public void setPayOrderType(PayOrderTypeEnum payOrderType) {
        set("payOrderType", payOrderType.ordinal());
    }

    public Date getOrderDateTime() {
        return getDate("orderDateTime");
    }

    public void setOrderDateTime(Date orderDateTime) {
        set("orderDateTime", orderDateTime);
    }


    public int getUserId() {
        return getInt("userId");
    }

}
