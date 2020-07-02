package com.fastchar.pay.ali;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.fastchar.core.FastChar;
import com.fastchar.core.FastHandler;
import com.fastchar.pay.FastPayConfig;
import com.fastchar.utils.FastStringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class AliPayUtils {


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
                String trade_status = request.getParameter("trade_status");
                //支付成功
                if ("TRADE_FINISHED".equals(trade_status) || "TRADE_SUCCESS".equals(trade_status)) {
                    handler.setCode(0);
                    handler.setError("校验成功！");
                    return handler;
                }
            } else {
                handler.setError("签名错误！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return handler;
    }


    private static AlipayClient getAlipayClient( FastAliPayConfig aliPayConfig) throws Exception {

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

        if (FastStringUtils.isEmpty(aliPayConfig.getNotifyUrl())) {
            throw new Exception("支付宝notifyUrl不可为空！请在AliPayConfig中配置！");
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
    public static String requestAppPay(FastAliPayConfig aliPayConfig,
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

        AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
        if (response.isSuccess()) {
            return response.getBody();
        } else {
            FastChar.getLog().error("发起支付宝APP支付失败：" + FastChar.getJson().toJson(response));
        }
        return null;
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
    public static String requestPagePay(FastAliPayConfig aliPayConfig,
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

        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (response.isSuccess()) {
            return response.getBody();
        } else {
            FastChar.getLog().error("发起支付宝APP支付失败：" + FastChar.getJson().toJson(response));
        }
        return null;
    }


    public static Map<?, ?> getUserInfo(String authorCode) throws Exception {
        return getUserInfo(FastChar.getConfig(FastAliPayConfig.class), authorCode);
    }

    public static Map<?, ?> getUserInfo(FastAliPayConfig aliPayConfig,String authorCode) throws Exception {
        AlipayClient alipayClient = getAlipayClient(aliPayConfig);

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


}
