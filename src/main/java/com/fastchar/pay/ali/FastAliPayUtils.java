package com.fastchar.pay.ali;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayResponse;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.fastchar.core.FastChar;
import com.fastchar.core.FastHandler;
import com.fastchar.pay.FastPayConfig;
import com.fastchar.utils.FastStringUtils;
import com.google.gson.Gson;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class FastAliPayUtils {


    /**
     * 验证支付宝回调
     *
     * @param request
     * @return
     */
    public static FastHandler verifyCallBack(FastAliPayConfig aliPayConfig, HttpServletRequest request) {
        FastHandler handler = new FastHandler();
        handler.setError("支付宝回调校验失败!");
        handler.setCode(-1);
        if (FastChar.getConfig(FastPayConfig.class).isDebug()) {
            handler.setError("调试模式下，直接认为支付宝回调失败!");
            handler.setCode(-1);
            return handler;
        }
        try {
            Map<String, String> signParams = new HashMap<String, String>();
            Enumeration<String> names = request.getParameterNames();
            while (names.hasMoreElements()) {
                String element = names.nextElement();
                signParams.put(element, request.getParameter(element));
            }
            String seller_id = request.getParameter("seller_id");
            if (FastStringUtils.isEmpty(seller_id)) {
                handler.setCode(-1);
                return handler;
            }

            if (!seller_id.equals(aliPayConfig.getPartner())) {
                handler.setCode(-1);
                return handler;
            }

            boolean signVerified = AlipaySignature.rsaCheckV1(signParams,
                    aliPayConfig.getRsaPublic(),
                    aliPayConfig.getDefaultCharset(),
                    aliPayConfig.getAlgorithm()); //调用SDK验证签名

            if (signVerified) {
                handler.setCode(0);
                handler.setError("校验成功！");
                return handler;
            } else {
                handler.setError("签名错误！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return handler;
    }


    /**
     * 判断回调是否交易成功
     */
    public static boolean isTradeSuccess(HttpServletRequest request) {
        String trade_status = request.getParameter("trade_status");
        //支付成功
        if ("TRADE_SUCCESS".equalsIgnoreCase(trade_status)) {
            return true;
        }
        return false;
    }

    /**
     * 判断回调是否交易关闭
     *
     * @param request
     * @return
     */
    public static boolean isTradeClose(HttpServletRequest request) {
        String trade_status = request.getParameter("trade_status");
        if ("TRADE_CLOSED".equalsIgnoreCase(trade_status)) {
            return true;
        }
        return false;
    }

    /**
     * 判断回调是否交易关闭
     *
     * @param request
     * @return
     */
    public static boolean isTradeFinish(HttpServletRequest request) {
        String trade_status = request.getParameter("trade_status");
        if ("TRADE_FINISHED".equalsIgnoreCase(trade_status)) {
            return true;
        }
        return false;
    }

    /**
     * 判断回调是否等待交易
     *
     * @param request
     * @return
     */
    public static boolean isTradeWait(HttpServletRequest request) {
        String trade_status = request.getParameter("trade_status");
        if ("WAIT_BUYER_PAY".equalsIgnoreCase(trade_status)) {
            return true;
        }
        return false;
    }


    public static String getRequestMsg(AlipayResponse response) {
        if (response == null) {
            return "支付宝异常！";
        }
        return response.getMsg() + " " + response.getSubMsg();
    }
    private static AlipayClient getAlipayClient(FastAliPayConfig aliPayConfig) throws Exception {

        return getAlipayClient(aliPayConfig, true);
    }

    private static AlipayClient getAlipayClient(FastAliPayConfig aliPayConfig,boolean fromPay) throws Exception {

        if (FastStringUtils.isEmpty(aliPayConfig.getAppId())) {
            throw new Exception("支付宝appId不可为空！请在AliPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(aliPayConfig.getRsaPrivate())) {
            throw new Exception("支付宝rsaPrivate不可为空！请在AliPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(aliPayConfig.getRsaPublic())) {
            throw new Exception("支付宝rsaPublic不可为空！请在AliPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(aliPayConfig.getPartner())) {
            throw new Exception("支付宝partner不可为空！请在AliPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(aliPayConfig.getSeller())) {
            throw new Exception("支付宝seller不可为空！请在AliPayConfig中配置！");
        }
        if (fromPay) {
            if (FastStringUtils.isEmpty(aliPayConfig.getNotifyUrl())) {
                throw new Exception("支付宝notifyUrl不可为空！请在AliPayConfig中配置！");
            }
        }

        return new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                aliPayConfig.getAppId(),
                aliPayConfig.getRsaPrivate(),
                "json",
                aliPayConfig.getDefaultCharset(),
                aliPayConfig.getRsaPublic(),
                aliPayConfig.getAlgorithm());
    }


    /**
     * 发起支付宝APP支付
     *
     * @param out_trade_no 订单编号
     * @param body         商品标题
     * @param details      介绍
     * @param money        金额
     * @return 支付宝支付信息
     */
    public static AlipayTradeAppPayResponse requestAppPay(FastAliPayConfig aliPayConfig,
                                                          String out_trade_no, String body,
                                                          String details, double money) throws Exception {
        AlipayClient alipayClient = getAlipayClient(aliPayConfig);

        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setBody(body);
        model.setSubject(details);
        model.setOutTradeNo(out_trade_no);
        model.setTimeoutExpress("30m");
        model.setTotalAmount(String.valueOf(money));
        model.setProductCode("QUICK_MSECURITY_PAY");
        request.setBizModel(model);
        request.setNotifyUrl(aliPayConfig.getNotifyUrl());
        if (FastChar.getConstant().isDebug()) {
            FastChar.getLog().info("支付宝APP支付：" + FastChar.getJson().toJson(model));
        }
        return alipayClient.sdkExecute(request);
    }


    /**
     * 发起支付宝网站页面支付
     *
     * @param out_trade_no 订单编号
     * @param body         商品标题
     * @param details      介绍
     * @param money        金额
     * @return 支付宝支付信息
     */
    public static AlipayTradePagePayResponse requestPagePay(FastAliPayConfig aliPayConfig,
                                                            String out_trade_no,
                                                            String body,
                                                            String details,
                                                            double money,
                                                            String returnUrl) throws Exception {
        AlipayClient alipayClient = getAlipayClient(aliPayConfig);

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setBody(body);
        model.setSubject(details);
        model.setOutTradeNo(out_trade_no);
        model.setTimeoutExpress("30m");
        model.setTotalAmount(String.valueOf(money));
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        request.setBizModel(model);
        request.setNotifyUrl(aliPayConfig.getNotifyUrl());
        request.setReturnUrl(returnUrl);
        if (FastChar.getConstant().isDebug()) {
            FastChar.getLog().info("支付宝Page支付：" + FastChar.getJson().toJson(model));
        }

        return alipayClient.pageExecute(request);
    }


    /**
     * 请求订单退款
     */
    public static AlipayTradeRefundResponse requestRefund(FastAliPayConfig aliPayConfig,
                                                          String out_trade_no,
                                                          String refundReason,
                                                          double refundMoney) throws Exception {

        AlipayClient alipayClient = getAlipayClient(aliPayConfig);

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(out_trade_no);
        model.setRefundAmount(String.valueOf(refundMoney));
        model.setRefundReason(refundReason);
        model.setOutRequestNo("RF_" + out_trade_no);

        request.setBizModel(model);
        request.setNotifyUrl(aliPayConfig.getNotifyUrl());
        if (FastChar.getConstant().isDebug()) {
            FastChar.getLog().info("支付宝订单退款：" + FastChar.getJson().toJson(model));
        }
        return alipayClient.execute(request);
    }


    /**
     * 根据支付宝用户授权码获取用户信息
     */
    public static Map<?, ?> getUserInfo(String authorCode) throws Exception {
        return getUserInfo(FastChar.getConfig(FastAliPayConfig.class), authorCode);
    }

    /**
     * 根据支付宝用户授权码获取用户信息
     */
    public static Map<?, ?> getUserInfo(FastAliPayConfig aliPayConfig, String authorCode) throws Exception {
        AlipayClient alipayClient = getAlipayClient(aliPayConfig, false);

        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setGrantType("authorization_code");
        request.setCode(authorCode.replace("\"", ""));

        AlipaySystemOauthTokenResponse response = alipayClient.execute(request);

        if (response.isSuccess()) {
            AlipayUserInfoShareRequest reqInfoShareRequest = new AlipayUserInfoShareRequest();
            String access_token = response.getAccessToken();
            AlipayUserInfoShareResponse userinfoShareResponse = alipayClient.execute(reqInfoShareRequest, access_token);
            return FastChar.getJson().fromJson(userinfoShareResponse.getBody(), Map.class);
        }
        return null;
    }


    /**
     * 支付宝转账
     *
     * @param orderCode 订单编号
     * @param aliUserId 支付宝的用户Id
     * @param money     资金
     * @param remark    备注
     */
    public static AlipayFundTransToaccountTransferResponse requestForwardToUserId(
            String orderCode,
            String aliUserId, double money, String remark) {
        return requestForwardToUserId(FastChar.getConfig(FastAliPayConfig.class), orderCode, aliUserId, money, remark);
    }

    /**
     * 支付宝转账
     *
     * @param aliPayConfig 支付宝配置对象
     * @param orderCode    订单编号
     * @param aliUserId    支付宝的用户Id
     * @param money        资金
     * @param remark       备注
     */
    public static AlipayFundTransToaccountTransferResponse requestForwardToUserId(
            FastAliPayConfig aliPayConfig,
            String orderCode,
            String aliUserId, double money, String remark) {
        try {
            AlipayClient alipayClient = getAlipayClient(aliPayConfig,false);
            AlipayFundTransToaccountTransferRequest request = new AlipayFundTransToaccountTransferRequest();

            Map<String, String> bizInfo = new HashMap<>();

            bizInfo.put("out_biz_no", orderCode);
            bizInfo.put("payee_type", "ALIPAY_USERID");
            bizInfo.put("payee_account", aliUserId);
            bizInfo.put("amount", String.valueOf(money));
            bizInfo.put("remark", remark);

            Gson gson = new Gson();
            request.setBizContent(gson.toJson(bizInfo));
            return alipayClient.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 支付宝转账
     *
     * @param orderCode  订单编号
     * @param aliAccount 支付宝登录账户，手机号码或者邮箱
     * @param money      资金
     * @param remark     备注
     */
    public static AlipayFundTransToaccountTransferResponse requestForwardToUserAccount(
            String orderCode,
            String aliAccount,
            String aliRealName,
            double money, String remark) {
        return requestForwardToUserAccount(FastChar.getConfig(FastAliPayConfig.class), orderCode, aliAccount, aliRealName, money, remark);
    }


    /**
     * 支付宝转账
     *
     * @param aliPayConfig 支付宝配置对象
     * @param orderCode    订单编号
     * @param aliAccount   支付宝登录账户，手机号码或者邮箱
     * @param money        资金
     * @param remark       备注
     */
    public static AlipayFundTransToaccountTransferResponse requestForwardToUserAccount(
            FastAliPayConfig aliPayConfig,
            String orderCode,
            String aliAccount,
            String aliRealName,
            double money, String remark) {
        try {
            AlipayClient alipayClient = getAlipayClient(aliPayConfig,false);
            AlipayFundTransToaccountTransferRequest request = new AlipayFundTransToaccountTransferRequest();

            Map<String, String> bizInfo = new HashMap<>();

            bizInfo.put("out_biz_no", orderCode);
            bizInfo.put("payee_type", "ALIPAY_LOGONID");
            bizInfo.put("payee_account", aliAccount);
            bizInfo.put("payee_real_name", aliRealName);
            bizInfo.put("amount", String.valueOf(money));
            bizInfo.put("remark", remark);

            Gson gson = new Gson();
            request.setBizContent(gson.toJson(bizInfo));
            return alipayClient.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
