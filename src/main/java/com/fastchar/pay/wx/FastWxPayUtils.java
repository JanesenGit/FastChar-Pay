package com.fastchar.pay.wx;

import com.fastchar.core.FastChar;
import com.fastchar.core.FastHandler;
import com.fastchar.pay.FastPayConfig;
import com.fastchar.utils.FastDateUtils;
import com.fastchar.utils.FastNumberUtils;
import com.fastchar.utils.FastStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;

public class FastWxPayUtils {

    private static String sign(String apiKey, Map<String, Object> params) {
        Set<String> keys = params.keySet();
        TreeSet<String> ts = new TreeSet<String>(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : ts) {
            sb.append(key);
            sb.append('=');
            sb.append(params.get(key));
            sb.append('&');
        }
        sb.append("key=").append(apiKey);
        return FastChar.getSecurity().MD5_Encrypt(sb.toString()).toUpperCase();
    }


    private static String buildPostParamsByXml(Map<String, Object> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        Set<String> keys = params.keySet();
        for (String key : keys) {
            sb.append("<").append(key).append(">");
            sb.append(params.get(key));
            sb.append("</").append(key).append(">");
        }
        sb.append("</xml>");
        return new String(sb.toString().getBytes(StandardCharsets.UTF_8), "ISO8859-1");
    }

    private static Map<String, String> convertXmlStringToMap(String xmlStr) {
        Map<String, String> map = new HashMap<String, String>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xmlStr)));
            Element root = doc.getDocumentElement();
            NodeList books = root.getChildNodes();
            if (books != null) {
                for (int i = 0; i < books.getLength(); i++) {
                    Node book = books.item(i);
                    if (book.getNodeType() == Node.ELEMENT_NODE) {
                        map.put(book.getNodeName(), book.getTextContent());
                    }
                }
            }
            return map;
        } catch (Exception ex) {
            return null;
        }
    }

    private static Map<String, String> convertXmlStreamToMap(InputStream inputStream) {
        Map<String, String> map = new HashMap<String, String>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(inputStream);
            Element root = doc.getDocumentElement();
            NodeList books = root.getChildNodes();
            if (books != null) {
                for (int i = 0; i < books.getLength(); i++) {
                    Node book = books.item(i);
                    if (book.getNodeType() == Node.ELEMENT_NODE) {
                        map.put(book.getNodeName(), book.getTextContent());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private static long genTimeStamp() {
        return System.currentTimeMillis() / 1000;
    }


    /**
     * 获得微信回调的map内容
     *
     * @param request 请求
     * @return map
     * @throws IOException 输入流异常
     */
    public static Map<String, String> getCallBack(HttpServletRequest request) throws IOException {
        return convertXmlStreamToMap(request.getInputStream());
    }


    /**
     * 验证微信支付回调
     */
    public static FastHandler verifyCallBack(FastWxPayConfig wxPayConfig, Map<String, String> dataMap) {
        FastHandler handler = new FastHandler();
        handler.setCode(-1);
        handler.setError("微信回调校验失败!");
        if (FastChar.getConfig(FastPayConfig.class).isDebug()) {
            handler.setError("调试模式下，直接认为微信回调失败!");
            handler.setCode(-1);
            return handler;
        }
        try {
            if (dataMap == null) {
                return handler;
            }
            String sign = dataMap.get("sign");
            String mch_id = dataMap.get("mch_id");
            if (FastStringUtils.isEmpty(sign)) {
                handler.setCode(-1);
                return handler;
            }
            if (FastStringUtils.isEmpty(mch_id)) {
                handler.setCode(-1);
                return handler;
            }
            if (!mch_id.equals(wxPayConfig.getMchId())) {
                handler.setCode(-1);
                return handler;
            }
            Set<String> keySet = dataMap.keySet();
            Map<String, Object> signMap = new HashMap<String, Object>();
            for (String string : keySet) {
                if (string.equals("sign")) continue;
                signMap.put(string, dataMap.get(string));
            }
            String currSign = FastWxPayUtils.sign(wxPayConfig.getApiKey(), signMap);
            if (sign.equals(currSign)) {
                handler.setCode(0);
                handler.setError("校验成功！");
                return handler;
            }
            return handler;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return handler;
    }


    /**
     * 是否交易完成
     *
     * @param dataMap
     * @return
     */
    public static boolean isTradeSuccess(Map<String, String> dataMap) {
        if ("SUCCESS".equalsIgnoreCase(FastStringUtils.defaultValue(dataMap.get("result_code"), "FAIL"))) {
            return true;
        }
        return false;
    }


    /**
     * 获得返回的金额 单位元
     */
    public static double getReturnMoney(Map<String, String> dataMap) {
        //total_fee
        int total_fee = FastNumberUtils.formatToInt(dataMap.get("total_fee"));//单位 分
        return FastNumberUtils.formatToDouble(total_fee) / 100.0;
    }


    /**
     * 接口是否请求成功
     */
    public static boolean isRequestSuccess(Map<String, String> result) {
        if (result == null) {
            return false;
        }
        return String.valueOf(result.get("return_code")).equalsIgnoreCase("SUCCESS");
    }


    /**
     * 业务是否请求成功
     */
    public static boolean isResultSuccess(Map<String, String> result) {
        if (result == null) {
            return false;
        }
        return String.valueOf(result.get("result_code")).equalsIgnoreCase("SUCCESS");
    }


    /**
     * 获取请求的接口
     */
    public static String getRequestMsg(Map<String, String> result) {
        if (result.containsKey("err_code_des")) {
            return String.valueOf(result.get("err_code_des"));
        }
        if (result.containsKey("return_msg")) {
            return String.valueOf(result.get("return_msg"));
        }
        return String.valueOf(result.get("msg"));
    }


    /**
     * 发起手机APP支付
     *
     * @param out_trade_no 支付订单号
     * @param body         订单标题
     * @param details      订单详情
     * @param money        支付金额
     * @param fromIP       请求的IP地址
     * @return 发起微信APP支付的配置信息
     */
    public static Map<String, Object> requestAppPay(FastWxPayConfig wxPayConfig, String out_trade_no,
                                                    String body,
                                                    String details, double money, String fromIP) throws Exception {

        if (FastStringUtils.isEmpty(wxPayConfig.getAppId())) {
            throw new Exception("微信AppId不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getMchId())) {
            throw new Exception("微信mchId不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getApiKey())) {
            throw new Exception("微信apiKey不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getNotifyUrl())) {
            throw new Exception("微信notifyUrl不可为空！请在WxPayConfig中配置！");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("appid", wxPayConfig.getAppId());
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("device_info", "WEB");
        params.put("nonce_str", FastStringUtils.buildOnlyCode("WX"));
        params.put("body", body);
        params.put("detail", details);
        params.put("out_trade_no", out_trade_no);
        params.put("total_fee", (int) (FastNumberUtils.formatToFloat(money) * 100) + "");
        params.put("spbill_create_ip", fromIP);
        if (fromIP.equalsIgnoreCase("localhost")) {
            params.put("spbill_create_ip", "192.168.0.0");
        }
        params.put("time_start", FastDateUtils.getDateString("yyyyMMddHHmmss"));
        params.put("notify_url", wxPayConfig.getNotifyUrl());
        params.put("trade_type", FastWxPayConfig.TradeTypeEnum.APP.name());

        params.put("sign", sign(wxPayConfig.getApiKey(), params));

        if (FastChar.getConstant().isDebug()) {
            FastChar.getLog().info("微信APP支付：" + params);
        }

        StringEntity stringEntity = new StringEntity(buildPostParamsByXml(params));
        HttpUriRequest wxPay = RequestBuilder.post()
                .setUri(new URI("https://api.mch.weixin.qq.com/pay/unifiedorder"))
                .setEntity(stringEntity)
                .build();

        CloseableHttpClient httpclient = HttpClients.custom()
                .build();
        try (CloseableHttpResponse response = httpclient.execute(wxPay)) {
            HttpEntity entity = response.getEntity();
            Map<String, String> result = convertXmlStringToMap(EntityUtils.toString(entity, "utf-8"));
            if (result == null) {
                return null;
            }
            EntityUtils.consume(entity);
            Map<String, Object> appSignParams = new HashMap<>();
            if (FastWxPayUtils.isRequestSuccess(result) && FastWxPayUtils.isResultSuccess(result)) {
                appSignParams.put("appid", wxPayConfig.getAppId());
                appSignParams.put("package", "Sign=WXPay");
                appSignParams.put("partnerid", wxPayConfig.getMchId());
                appSignParams.put("prepayid", result.get("prepay_id"));
                appSignParams.put("noncestr", FastStringUtils.buildOnlyCode("WX"));
                appSignParams.put("timestamp", String.valueOf(genTimeStamp()));
                appSignParams.put("sign", sign(wxPayConfig.getApiKey(), appSignParams));
                appSignParams.put("success", true);
            } else {
                appSignParams.put("success", false);
                appSignParams.put("msg", getRequestMsg(result));
            }
            return appSignParams;
        }
    }


    /**
     * 发起手微信公众号或小程序支付
     *
     * @param out_trade_no 支付订单号
     * @param body         订单标题
     * @param details      订单详情
     * @param money        支付金额
     * @param fromIP       请求的IP地址
     * @return 发起微信APP支付的配置信息
     */
    public static Map<String, Object> requestJSAPIPay(FastWxPayConfig wxPayConfig,
                                                      String openid,
                                                      String out_trade_no,
                                                      String body,
                                                      String details, double money, String fromIP) throws Exception {

        if (FastStringUtils.isEmpty(wxPayConfig.getAppId())) {
            throw new Exception("微信AppId不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getMchId())) {
            throw new Exception("微信mchId不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getApiKey())) {
            throw new Exception("微信apiKey不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getNotifyUrl())) {
            throw new Exception("微信notifyUrl不可为空！请在WxPayConfig中配置！");
        }


        Map<String, Object> params = new HashMap<String, Object>();
        params.put("appid", wxPayConfig.getAppId());
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("device_info", "WEB");
        params.put("nonce_str", FastStringUtils.buildOnlyCode("WX"));
        params.put("body", body);
        params.put("openid", openid);
        params.put("detail", details);
        params.put("out_trade_no", out_trade_no);
        params.put("total_fee", (int) (FastNumberUtils.formatToFloat(money) * 100) + "");
        params.put("spbill_create_ip", fromIP);
        if (fromIP.equalsIgnoreCase("localhost")) {
            params.put("spbill_create_ip", "192.168.0.0");
        }
        params.put("time_start", FastDateUtils.getDateString("yyyyMMddHHmmss"));
        params.put("notify_url", wxPayConfig.getNotifyUrl());
        params.put("trade_type", FastWxPayConfig.TradeTypeEnum.JSAPI.name());

        params.put("sign", sign(wxPayConfig.getApiKey(), params));


        if (FastChar.getConstant().isDebug()) {
            FastChar.getLog().info("微信JS支付：" + params);
        }

        StringEntity stringEntity = new StringEntity(buildPostParamsByXml(params));
        HttpUriRequest wxPay = RequestBuilder.post()
                .setUri(new URI("https://api.mch.weixin.qq.com/pay/unifiedorder"))
                .setEntity(stringEntity)
                .build();

        CloseableHttpClient httpclient = HttpClients.custom()
                .build();
        try (CloseableHttpResponse response = httpclient.execute(wxPay)) {
            HttpEntity entity = response.getEntity();
            Map<String, String> result = convertXmlStringToMap(EntityUtils.toString(entity, "utf-8"));
            if (result == null) {
                return null;
            }
            EntityUtils.consume(entity);
            Map<String, Object> appSignParams = new HashMap<String, Object>();
            if (FastWxPayUtils.isRequestSuccess(result) && FastWxPayUtils.isResultSuccess(result)) {
                appSignParams.put("appId", wxPayConfig.getAppId());
                appSignParams.put("package", "prepay_id=" + result.get("prepay_id"));
                appSignParams.put("signType", "MD5");
                appSignParams.put("nonceStr", FastStringUtils.buildOnlyCode("WX"));
                appSignParams.put("timeStamp", String.valueOf(genTimeStamp()));
                appSignParams.put("sign", sign(wxPayConfig.getApiKey(), appSignParams));
                appSignParams.put("success", true);
                appSignParams.put("msg", getRequestMsg(result));
            } else {
                appSignParams.put("success", false);
                appSignParams.put("msg", getRequestMsg(result));
            }
            return appSignParams;
        }
    }


    /**
     * 发起手机APP支付
     *
     * @param out_trade_no 支付订单号
     * @param body         订单标题
     * @param details      订单详情
     * @param money        支付金额
     * @param fromIP       请求的IP地址
     * @return 发起微信APP支付的配置信息
     */
    public static Map<String, Object> requestNativePay(FastWxPayConfig wxPayConfig, String out_trade_no,
                                                       String body,
                                                       String details, double money, String fromIP) throws Exception {

        if (FastStringUtils.isEmpty(wxPayConfig.getAppId())) {
            throw new Exception("微信AppId不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getMchId())) {
            throw new Exception("微信mchId不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getApiKey())) {
            throw new Exception("微信apiKey不可为空！请在WxPayConfig中配置！");
        }

        if (FastStringUtils.isEmpty(wxPayConfig.getNotifyUrl())) {
            throw new Exception("微信notifyUrl不可为空！请在WxPayConfig中配置！");
        }


        Map<String, Object> params = new HashMap<String, Object>();
        params.put("appid", wxPayConfig.getAppId());
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("device_info", "WEB");
        params.put("nonce_str", FastStringUtils.buildOnlyCode("WX"));
        params.put("body", body);
        params.put("detail", details);
        params.put("out_trade_no", out_trade_no);
        params.put("total_fee", (int) (FastNumberUtils.formatToFloat(money) * 100) + "");
        params.put("spbill_create_ip", fromIP);
        if (fromIP.equalsIgnoreCase("localhost")) {
            params.put("spbill_create_ip", "192.168.0.0");
        }
        params.put("time_start", FastDateUtils.getDateString("yyyyMMddHHmmss"));
        params.put("notify_url", wxPayConfig.getNotifyUrl());
        params.put("trade_type", FastWxPayConfig.TradeTypeEnum.NATIVE.name());

        params.put("sign", sign(wxPayConfig.getApiKey(), params));


        if (FastChar.getConstant().isDebug()) {
            FastChar.getLog().info("微信Native支付：" + params);
        }

        StringEntity stringEntity = new StringEntity(buildPostParamsByXml(params));
        HttpUriRequest wxPay = RequestBuilder.post()
                .setUri(new URI("https://api.mch.weixin.qq.com/pay/unifiedorder"))
                .setEntity(stringEntity)
                .build();

        CloseableHttpClient httpclient = HttpClients.custom()
                .build();
        try (CloseableHttpResponse response = httpclient.execute(wxPay)) {
            HttpEntity entity = response.getEntity();
            Map<String, String> result = convertXmlStringToMap(EntityUtils.toString(entity, "utf-8"));
            if (result == null) {
                return null;
            }
            EntityUtils.consume(entity);
            Map<String, Object> appSignParams = new HashMap<String, Object>();
            if (FastWxPayUtils.isRequestSuccess(result) && FastWxPayUtils.isResultSuccess(result)) {
                appSignParams.put("appid", wxPayConfig.getAppId());
                appSignParams.put("partnerid", wxPayConfig.getMchId());
                appSignParams.put("code", result.get("code_url"));
                appSignParams.put("msg", getRequestMsg(result));
                appSignParams.put("success", true);
            } else {
                appSignParams.put("msg", getRequestMsg(result));
                appSignParams.put("success", false);
            }
            return appSignParams;
        }
    }

    public static Map<?, ?> getUserInfo(String authorCode) {
        return getUserInfo(FastChar.getConfig(FastWxPayConfig.class), authorCode);
    }

    /**
     * 根据授权码获得用户信息
     *
     * @param authorCode
     * @return
     */
    public static Map<?, ?> getUserInfo(FastWxPayConfig wxPayConfig, String authorCode) {
        CloseableHttpResponse response;
        HttpEntity entity;
        try {

            if (FastStringUtils.isEmpty(wxPayConfig.getAppId())) {
                throw new Exception("微信AppId不可为空！请在WxPayConfig中配置！");
            }

            if (FastStringUtils.isEmpty(wxPayConfig.getAppSecret())) {
                throw new Exception("微信AppSecret不可为空！请在WxPayConfig中配置！");
            }

            String url = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                    "appid=" + wxPayConfig.getAppId()
                    + "&secret=" + wxPayConfig.getAppSecret()
                    + "&code=" + authorCode
                    + "&grant_type=authorization_code";
            HttpUriRequest authRequest = RequestBuilder.get()
                    .setUri(new URI(url))
                    .build();
            CloseableHttpClient httpclient = HttpClients.custom()
                    .build();
            response = httpclient.execute(authRequest);
            entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");

            Map<?, ?> authMap = FastChar.getJson().fromJson(result, Map.class);
            try {
                EntityUtils.consume(entity);
                response.close();
            } catch (Exception ignored) {
            }
            if (authMap.containsKey("openid")) {
                String openid = authMap.get("openid").toString();
                String access_token = authMap.get("access_token").toString();

                HttpUriRequest userRequest = RequestBuilder.get()
                        .setUri(new URI("https://api.weixin.qq.com/sns/userinfo?access_token=" + access_token + "&openid=" + openid))
                        .build();
                response = httpclient.execute(userRequest);
                entity = response.getEntity();
                String userResult = EntityUtils.toString(entity, "utf-8");
                Map<?, ?> authorMap = FastChar.getJson().fromJson(userResult, Map.class);
                try {
                    EntityUtils.consume(entity);
                    response.close();
                } catch (Exception ignored) {
                }
                return authorMap;
            } else {
                FastChar.getLog().error("微信授权获取用户信息失败！" + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 微信转账到用户账号里
     */
    public static Map<String, String> requestPayMoneyToUser(
            String re_user_name,
            String partner_trade_no,
            String body,
            double money,
            String fromIP,
            String openid) {
        FastWxPayConfig wxPayConfig = FastChar.getOverrides().singleInstance(FastWxPayConfig.class);
        return requestPayMoneyToUser(wxPayConfig, re_user_name, partner_trade_no, body, money, fromIP, openid);
    }

    /**
     * 微信转账到用户账号里
     */
    public static Map<String, String> requestPayMoneyToUser(
            FastWxPayConfig wxPayConfig,
            String re_user_name,
            String partner_trade_no,
            String body,
            double money,
            String fromIP,
            String openid) {
        try {

            if (FastStringUtils.isEmpty(wxPayConfig.getAppId())) {
                throw new Exception("微信AppId不可为空！请在WxPayConfig中配置！");
            }

            if (FastStringUtils.isEmpty(wxPayConfig.getMchId())) {
                throw new Exception("微信mchId不可为空！请在WxPayConfig中配置！");
            }

            if (FastStringUtils.isEmpty(wxPayConfig.getApiKey())) {
                throw new Exception("微信apiKey不可为空！请在WxPayConfig中配置！");
            }

            if (FastStringUtils.isEmpty(wxPayConfig.getCaKeyStorePath())) {
                throw new Exception("微信caKeyStorePath不可为空！请在WxPayConfig中配置！");
            }


            Map<String, Object> params = new HashMap<String, Object>();
            params.put("mch_appid", wxPayConfig.getAppId());
            params.put("mchid", wxPayConfig.getMchId());
            params.put("device_info", "WEB");
            params.put("nonce_str", FastStringUtils.buildOnlyCode("WX"));
            params.put("desc", body);
            params.put("partner_trade_no", partner_trade_no);
            params.put("amount", String.valueOf(FastNumberUtils.formatToInt(FastNumberUtils.formatToDouble(money, 2) * 100)));
            params.put("spbill_create_ip", fromIP);
            params.put("check_name", "NO_CHECK");//NO_CHECK：不校验真实姓名 FORCE_CHECK：强校验真实姓名
            params.put("re_user_name", re_user_name);
            params.put("openid", openid);
            params.put("sign", sign(wxPayConfig.getApiKey(), params));

            StringEntity stringEntity = new StringEntity(buildPostParamsByXml(params));
            HttpUriRequest wxPay = RequestBuilder.post()
                    .setUri(new URI("https://api.mch.weixin.qq.com/mmpaymkttransfers/promotion/transfers"))
                    .setEntity(stringEntity)
                    .build();


            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream instream = new FileInputStream(new File(wxPayConfig.getCaKeyStorePath()))) {
                keyStore.load(instream, wxPayConfig.getMchId().toCharArray());
            }

            SSLContext sslcontext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, wxPayConfig.getMchId().toCharArray())
                    .build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslcontext,
                    new String[]{"TLSv1"},
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());


            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .build();
            HttpEntity entity = null;
            try (CloseableHttpResponse response1 = httpclient.execute(wxPay)) {
                entity = response1.getEntity();
                return convertXmlStringToMap(EntityUtils.toString(entity, "utf-8"));
            } finally {
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 申请微信退款
     */
    public static Map<String, String> requestRefund(
            FastWxPayConfig wxPayConfig,
            String out_trade_no,
            String refund_reason,
            double total_money,
            double refund_money) {
        try {

            if (FastStringUtils.isEmpty(wxPayConfig.getAppId())) {
                throw new Exception("微信AppId不可为空！请在WxPayConfig中配置！");
            }

            if (FastStringUtils.isEmpty(wxPayConfig.getMchId())) {
                throw new Exception("微信mchId不可为空！请在WxPayConfig中配置！");
            }

            if (FastStringUtils.isEmpty(wxPayConfig.getApiKey())) {
                throw new Exception("微信apiKey不可为空！请在WxPayConfig中配置！");
            }

            if (FastStringUtils.isEmpty(wxPayConfig.getCaKeyStorePath())) {
                throw new Exception("微信caKeyStorePath不可为空！请在WxPayConfig中配置！");
            }


            Map<String, Object> params = new HashMap<String, Object>();
            params.put("appid", wxPayConfig.getAppId());
            params.put("mch_id", wxPayConfig.getMchId());
            params.put("nonce_str", FastStringUtils.buildOnlyCode("RF"));
            params.put("out_trade_no", out_trade_no);
            params.put("out_refund_no", "RF_" + out_trade_no);
            params.put("total_fee", String.valueOf(FastNumberUtils.formatToInt(FastNumberUtils.formatToDouble(total_money, 2) * 100)));
            params.put("refund_fee", String.valueOf(FastNumberUtils.formatToInt(FastNumberUtils.formatToDouble(refund_money, 2) * 100)));
            params.put("refund_desc", refund_reason);

            params.put("sign", sign(wxPayConfig.getApiKey(), params));

            StringEntity stringEntity = new StringEntity(buildPostParamsByXml(params));
            HttpUriRequest wxPay = RequestBuilder.post()
                    .setUri(new URI("https://api.mch.weixin.qq.com/secapi/pay/refund"))
                    .setEntity(stringEntity)
                    .build();


            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream instream = new FileInputStream(new File(wxPayConfig.getCaKeyStorePath()))) {
                keyStore.load(instream, wxPayConfig.getMchId().toCharArray());
            }

            SSLContext sslcontext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, wxPayConfig.getMchId().toCharArray())
                    .build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslcontext,
                    new String[]{"TLSv1"},
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());


            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .build();
            HttpEntity entity = null;
            try (CloseableHttpResponse response1 = httpclient.execute(wxPay)) {
                entity = response1.getEntity();
                return convertXmlStringToMap(EntityUtils.toString(entity, "utf-8"));
            } finally {
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


}
