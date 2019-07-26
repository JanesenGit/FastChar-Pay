package com.fastchar.pay.entity;

import com.fastchar.core.FastChar;

import com.fastchar.pay.entity.getset.AbstractFinalWxAuthorizeEntity;

public class FinalWxAuthorizeEntity extends AbstractFinalWxAuthorizeEntity {
    public static FinalWxAuthorizeEntity dao() {
        return FastChar.getOverrides().singleInstance(FinalWxAuthorizeEntity.class);
    }

    public static FinalWxAuthorizeEntity newInstance() {
        return FastChar.getOverrides().newInstance(FinalWxAuthorizeEntity.class);
    }

    @Override
    public String getTableName() {
        return "final_wx_authorize";
    }

    @Override
    public String getTableDetails() {
        return "微信授权信息";
    }

    @Override
    public void setDefaultValue() {
        set("sex", 0);
    }


    public FinalWxAuthorizeEntity getAuthor(String openid) {
        String sqlStr = "select * from final_wx_authorize where openid = ?  ";
        return selectFirstBySql(sqlStr,openid);
    }

}
