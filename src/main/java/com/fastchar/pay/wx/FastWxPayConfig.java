package com.fastchar.pay.wx;

import com.fastchar.interfaces.IFastConfig;
import com.fastchar.pay.exception.WxPayException;

import java.io.File;

public class FastWxPayConfig implements IFastConfig {

    /**
     * 应用的AppId 前往微信开发平台查看
     */
    public String appId;

    /**
     * 应用的AppSecret 前往微信开发平台查看
     */
    public String appSecret;

    /**
     * 微信公众账号AppId
     */
    public String publicAppId;

    /**
     * 商户号
     */
    public String mchId;

    /**
     * 子商户号
     */
    public String subMchId;

    /**
     * API密钥，在商户平台设置
     */
    public String apiKey;


    /**
     * 微信支付回调路径
     */
    public String notifyUrl;


    /**
     * 调用企业退款接口时，需要用到退款证书，需要到微信的商户平台下载
     */
    public String caKeyStorePath;

    public String getAppId() {
        return appId;
    }

    /**
     * 设置微信应用Id，从微信开放平台中获得
     *
     * @param appId 微信应用AppId
     * @return 当前对象
     */
    public FastWxPayConfig setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public String getAppSecret() {
        return appSecret;
    }

    /**
     * 设置微信应用密钥AppSecret，从微信开放平台中获得
     *
     * @param appSecret 密钥
     * @return 当前对象
     */
    public FastWxPayConfig setAppSecret(String appSecret) {
        this.appSecret = appSecret;
        return this;
    }

    public String getPublicAppId() {
        return publicAppId;
    }

    /**
     * 设置微信公众账号AppId，从微信公众平台获得
     *
     * @param publicAppId 公众号的AppId
     * @return 当前对象
     */
    public FastWxPayConfig setPublicAppId(String publicAppId) {
        this.publicAppId = publicAppId;
        return this;
    }

    public String getMchId() {
        return mchId;
    }

    /**
     * 设置微信商户号Id，从微信商户平台获得
     *
     * @param mchId 微信商户Id
     * @return 当前对象
     */
    public FastWxPayConfig setMchId(String mchId) {
        this.mchId = mchId;
        return this;
    }

    public String getSubMchId() {
        return subMchId;
    }

    /**
     * 设置微信子商户号Id，从微信商户平台获得，一般当微信商户角色为服务商的时候，需要用到子商户
     *
     * @param subMchId 子商户号Id
     * @return 当前对象
     */
    public FastWxPayConfig setSubMchId(String subMchId) {
        this.subMchId = subMchId;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    /**
     * 支付密钥，自己可使用MD5生成，然后前往微信商户平台配置api支付密钥
     *
     * @param apiKey api密钥
     * @return 当前对象
     */
    public FastWxPayConfig setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * 支付回调地址
     *
     * @param notifyUrl 回调通知地址
     * @return 当前对象
     */
    public FastWxPayConfig setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
        return this;
    }

    public String getCaKeyStorePath() {
        return caKeyStorePath;
    }

    /**
     * 微信支付操作证书路径，一般用户微信提现或转账使用
     *
     * @param caKeyStorePath 证书路径
     * @return 当前对象
     */
    public FastWxPayConfig setCaKeyStorePath(String caKeyStorePath) {
        this.caKeyStorePath = caKeyStorePath;
        if (!new File(caKeyStorePath).exists()) {
            throw new WxPayException("微信支付操作证书路径【" + caKeyStorePath + "】不存在！");
        }
        return this;
    }


    public enum TradeTypeEnum{
        JSAPI,//公众号获取小程序支付
        NATIVE,//原生扫码支付
        APP,//app支付
        MICROPAY,//刷卡支付
        MWEB//H5支付
    }
}
