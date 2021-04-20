package com.fastchar.pay.ali;

import com.fastchar.annotation.AFastClassFind;
import com.fastchar.interfaces.IFastConfig;

@AFastClassFind(value = "com.alipay.api.AlipayClient", url = "https://mvnrepository.com/artifact/com.alipay.sdk/alipay-sdk-java")
public class FastAliPayConfig implements IFastConfig {

    /**
     * 设置支付宝签名方式，默认是RSA，如果使用RSA2，请配置为RSA2
     * 注意，如果使用支付宝默认创建的2.0应用，请必须设置为RSA2格式
     */
    private String algorithm = "RSA";
    private String signAlgorithms = "SHA1WithRSA";
    private String defaultCharset = "utf-8";

    /**
     * 服务商的PID
     */
    private String sysServiceProviderId = null;
    /**
     * 商户PID
     */
    private String partner = null;
    /**
     * 商户收款账号
     */
    private String seller = null;
    /**
     * 商户私钥，pkcs8格式
     */
    private String rsaPrivate = null;
    /**
     * 支付宝公钥（不是应用公钥）
     */
    private String rsaPublic = null;

    /**
     * 支付宝支付回调路径
     */
    private String notifyUrl = null;


    /**
     * 支付应用的APPID
     */
    private String appId = null;


    /**
     * h5支付成功后返回的网页
     */
    private String wapReturnUrl = null;

    /**
     * 商户授权码，用于代该商户发起业务请求，例如发起支付等
     */
    private String appAuthToken = null;

    public String getSysServiceProviderId() {
        return sysServiceProviderId;
    }

    public FastAliPayConfig setSysServiceProviderId(String sysServiceProviderId) {
        this.sysServiceProviderId = sysServiceProviderId;
        return this;
    }

    public String getPartner() {
        return partner;
    }

    /**
     * 支付宝商户PID，从支付宝商户平台中查看
     *
     * @param partner 商户PID
     * @return 当前对象
     */
    public FastAliPayConfig setPartner(String partner) {
        this.partner = partner;
        return this;
    }

    public String getSeller() {
        return seller;
    }

    /**
     * 支付宝收款账号，一般为支付宝的登录账号
     *
     * @param seller 收款账户
     * @return 当前对象
     */
    public FastAliPayConfig setSeller(String seller) {
        this.seller = seller;
        return this;
    }


    public String getRsaPrivate() {
        return rsaPrivate;
    }


    /**
     * 支付宝商户私钥，pkcs8格式，自己使用支付宝工具生成，并前往支付宝开发平台进行配置
     *
     * @param rsaPrivate 私钥
     * @return 当前对象
     */
    public FastAliPayConfig setRsaPrivate(String rsaPrivate) {
        this.rsaPrivate = rsaPrivate;
        return this;
    }

    public String getRsaPublic() {
        return rsaPublic;
    }

    /**
     * 支付宝公钥（不是应用公钥） 从支付宝开发平台中查看，一般用户支付宝提现转账等功能
     *
     * @param rsaPublic 公钥
     * @return 当前对象
     */
    public FastAliPayConfig setRsaPublic(String rsaPublic) {
        this.rsaPublic = rsaPublic;
        return this;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * 支付回调地址
     *
     * @param notifyUrl 回调地址
     * @return 当前对象
     */
    public FastAliPayConfig setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
        return this;
    }

    public String getAppId() {
        return appId;
    }

    /**
     * 支付宝应用Id，从支付宝开发平台中查看
     *
     * @param appId 应用ID
     * @return 当前对象
     */
    public FastAliPayConfig setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public String getWapReturnUrl() {
        return wapReturnUrl;
    }

    /**
     * 设置H5支付成功后跳转的页面
     *
     * @param wapReturnUrl 跳转的页面
     * @return 当前对象
     */
    public FastAliPayConfig setWapReturnUrl(String wapReturnUrl) {
        this.wapReturnUrl = wapReturnUrl;
        return this;
    }

    public String getAppAuthToken() {
        return appAuthToken;
    }

    /**
     * 设置授权的Token
     *
     * @param appAuthToken 授权token
     * @return 当前对象
     */
    public FastAliPayConfig setAppAuthToken(String appAuthToken) {
        this.appAuthToken = appAuthToken;
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * 设置支付宝签名方式，默认是RSA，如果使用RSA2，请配置为RSA2
     * 注意，如果使用支付宝默认创建的2.0应用，请必须设置为RSA2格式
     *
     * @param algorithm 签名方式
     * @return 当前对象
     */
    public FastAliPayConfig setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public String getSignAlgorithms() {
        return signAlgorithms;
    }

    /**
     * 设置服务商的PID
     *
     * @param signAlgorithms 服务商的PID
     * @return 当前对象
     */
    public FastAliPayConfig setSignAlgorithms(String signAlgorithms) {
        this.signAlgorithms = signAlgorithms;
        return this;
    }

    public String getDefaultCharset() {
        return defaultCharset;
    }

    /**
     * 设置编码方式
     *
     * @param defaultCharset 编码方式，默认utf-8
     * @return 当前对象
     */
    public FastAliPayConfig setDefaultCharset(String defaultCharset) {
        this.defaultCharset = defaultCharset;
        return this;
    }
}
