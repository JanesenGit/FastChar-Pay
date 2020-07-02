package com.fastchar.pay.entity;
import com.fastchar.core.FastChar;
import com.fastchar.pay.entity.getset.AbstractFinalAliAuthorizeEntity;

public class FinalAliAuthorizeEntity extends AbstractFinalAliAuthorizeEntity {
    private static final long serialVersionUID = 1L;

    public static FinalAliAuthorizeEntity dao() {
        return FastChar.getOverrides().singleInstance(FinalAliAuthorizeEntity.class);
    }

    public static FinalAliAuthorizeEntity newInstance() {
        return FastChar.getOverrides().newInstance(FinalAliAuthorizeEntity.class);
    }

    @Override
    public String getTableName() {
        return "final_ali_authorize";
    }

    @Override
    public String getTableDetails() {
        return "支付宝授权信息";
    }

    @Override
    public void setDefaultValue() {

    }

    @Override
    public void convertValue() {
        super.convertValue();

    }


}
