package com.fastchar.pay.ali;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.fastchar.core.FastChar;
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
    public static boolean verifyCallBack(HttpServletRequest request) {
        try {
            Map<String, String> signParams = new HashMap<String, String>();
            Enumeration<String> names = request.getParameterNames();
            while (names.hasMoreElements()) {
                String element = names.nextElement();
                signParams.put(element, request.getParameter(element));
            }
            FastAliPayConfig aliPayConfig = FastChar.getOverrides().singleInstance(FastAliPayConfig.class);

            boolean signVerified = AlipaySignature.rsaCheckV1(signParams,
                    aliPayConfig.getRsaPublic(),
                    aliPayConfig.getDefaultCharset(),
                    aliPayConfig.getAlgorithm()); //调用SDK验证签名

            if (signVerified) {
                String seller_id = request.getParameter("seller_id");
                String seller_email = request.getParameter("seller_email");
                if (!seller_id.equals(aliPayConfig.getPartner())) {
                    return false;
                } else if (!seller_email.equals(aliPayConfig.getSeller())) {
                    return false;
                }
                String trade_status = request.getParameter("trade_status");
                //支付成功
                if ("TRADE_FINISHED".equals(trade_status) || "TRADE_SUCCESS".equals(trade_status)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
    public static String requestAppPay(String out_trade_no, String body,
                                       String details, double money) throws Exception {

        FastAliPayConfig aliPayConfig = FastChar.getOverrides().singleInstance(FastAliPayConfig.class);

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

        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                aliPayConfig.getAppId(),
                aliPayConfig.getRsaPrivate(),
                "json",
                aliPayConfig.getDefaultCharset(),
                aliPayConfig.getRsaPublic(),
                aliPayConfig.getAlgorithm());

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
            FastChar.getLog().info("支付宝支付：[" + out_trade_no + "] [" + body + "] [" + money + "] [" + aliPayConfig.getNotifyUrl() + "]");
        }

        AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
        if (response.isSuccess()) {
            return response.getBody();
        } else {
            FastChar.getLog().error("发起支付宝APP支付失败：" + FastChar.getJson().toJson(response));
        }
        return null;
    }


}
