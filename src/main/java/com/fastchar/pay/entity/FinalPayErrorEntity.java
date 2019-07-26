package com.fastchar.pay.entity;

import com.fastchar.core.FastChar;
import com.fastchar.core.FastEntity;
import com.fastchar.utils.FastDateUtils;

import java.util.*;

import com.fastchar.utils.FastStringUtils;

public class FinalPayErrorEntity extends FastEntity<FinalPayErrorEntity> {
    public static FinalPayErrorEntity dao() {
        return FastChar.getOverrides().singleInstance(FinalPayErrorEntity.class);
    }

    @Override
    public String getTableName() {
        return "final_pay_error";
    }

    @Override
    public String getTableDetails() {
        return "支付密码错误";
    }

    @Override
    public void setDefaultValue() {
        set("userId", 0);
        set("errorDateTime", FastDateUtils.getDateString());
    }

    /**
     * 将关联查询的数据单独封装到对应的实体对象里
     */
    private void pluckEntity(String... alias) {
        String[] linkAlias = new String[0];
        for (int i = 0; i < linkAlias.length; i++) {
            if (i < alias.length) {
                linkAlias[i] = alias[i];
            }
        }

    }

    /**
     * 获得数据详情
     */
    public FinalPayErrorEntity getDetails(int errorId) {
        List<String> linkColumns = new ArrayList<>();
        String sqlStr = "select t.*," + FastStringUtils.join(linkColumns, ",") + " from final_pay_error as t" +
                " " +
                " where t.errorId = ?  ";
        FinalPayErrorEntity entity = selectFirstBySql(sqlStr, errorId);
        if (entity != null) {
            entity.pluckEntity();
        }
        return entity;
    }


    public int countTodayError(int userId) {
        String sqlStr = "select count(1) as c from final_pay_error where userId = ? and date_format(errorDateTime,'%Y-%m-%d') = ? ";
        FinalPayErrorEntity result = selectFirstBySql(sqlStr, userId, FastDateUtils.getDateString("yyyy-MM-dd"));
        if (result != null) {
            return result.getInt("c");
        }
        return 0;
    }


}
