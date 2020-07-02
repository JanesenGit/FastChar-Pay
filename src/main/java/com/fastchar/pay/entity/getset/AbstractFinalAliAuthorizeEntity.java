package com.fastchar.pay.entity.getset;
import com.fastchar.core.FastEntity;
import com.fastchar.pay.entity.FinalAliAuthorizeEntity;

public abstract class AbstractFinalAliAuthorizeEntity extends FastEntity<FinalAliAuthorizeEntity> {
	public String getAvatar() {return getString("avatar");}
	public AbstractFinalAliAuthorizeEntity setAvatar(String avatar) { set("avatar", avatar);return this;}
	public String getCity() {return getString("city");}
	public AbstractFinalAliAuthorizeEntity setCity(String city) { set("city", city);return this;}
	public String getGender() {return getString("gender");}
	public AbstractFinalAliAuthorizeEntity setGender(String gender) { set("gender", gender);return this;}
	public String getIs_certified() {return getString("is_certified");}
	public AbstractFinalAliAuthorizeEntity setIs_certified(String is_certified) { set("is_certified", is_certified);return this;}
	public String getIs_student_certified() {return getString("is_student_certified");}
	public AbstractFinalAliAuthorizeEntity setIs_student_certified(String is_student_certified) { set("is_student_certified", is_student_certified);return this;}
	public String getNick_name() {return getString("nick_name");}
	public AbstractFinalAliAuthorizeEntity setNick_name(String nick_name) { set("nick_name", nick_name);return this;}
	public String getProvince() {return getString("province");}
	public AbstractFinalAliAuthorizeEntity setProvince(String province) { set("province", province);return this;}
	public String getUser_id() {return getString("user_id");}
	public AbstractFinalAliAuthorizeEntity setUser_id(String user_id) { set("user_id", user_id);return this;}
	public String getUser_status() {return getString("user_status");}
	public AbstractFinalAliAuthorizeEntity setUser_status(String user_status) { set("user_status", user_status);return this;}
	public String getUser_type() {return getString("user_type");}
	public AbstractFinalAliAuthorizeEntity setUser_type(String user_type) { set("user_type", user_type);return this;}
}