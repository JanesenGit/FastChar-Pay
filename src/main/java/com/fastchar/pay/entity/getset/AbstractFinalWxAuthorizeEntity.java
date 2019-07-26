package com.fastchar.pay.entity.getset;
import com.fastchar.core.FastEntity;
import com.fastchar.pay.entity.FinalWxAuthorizeEntity;

public abstract class AbstractFinalWxAuthorizeEntity extends FastEntity<FinalWxAuthorizeEntity> {
	public String getOpenid() {return getString("openid");}
	public AbstractFinalWxAuthorizeEntity setOpenid(String openid) { set("openid", openid);return this;}
	public String getNickname() {return getString("nickname");}
	public AbstractFinalWxAuthorizeEntity setNickname(String nickname) { set("nickname", nickname);return this;}
	public String getHeadimgurl() {return getString("headimgurl");}
	public AbstractFinalWxAuthorizeEntity setHeadimgurl(String headimgurl) { set("headimgurl", headimgurl);return this;}
	public int getSex() {return getInt("sex");}
	public AbstractFinalWxAuthorizeEntity setSex(int sex) { set("sex", sex);return this;}
	public String getProvince() {return getString("province");}
	public AbstractFinalWxAuthorizeEntity setProvince(String province) { set("province", province);return this;}
	public String getCity() {return getString("city");}
	public AbstractFinalWxAuthorizeEntity setCity(String city) { set("city", city);return this;}
	public String getCountry() {return getString("country");}
	public AbstractFinalWxAuthorizeEntity setCountry(String country) { set("country", country);return this;}
	public String getUnionid() {return getString("unionid");}
	public AbstractFinalWxAuthorizeEntity setUnionid(String unionid) { set("unionid", unionid);return this;}
}