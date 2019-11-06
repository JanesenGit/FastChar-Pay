package com.fastchar.pay.wx;

import com.fastchar.core.FastChar;
import com.fastchar.utils.FastDateUtils;
import com.fastchar.utils.FastNumberUtils;
import com.fastchar.utils.FastStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.*;

public class WxPayUtils {

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
        return new String(sb.toString().getBytes("utf-8"), "ISO8859-1");
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
            ex.printStackTrace();
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
        } catch (Exception ex) {
            ex.printStackTrace();
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
    public static boolean verifyCallBack(Map<String, String> dataMap) {
        FastWxPayConfig wxPayConfig = FastChar.getOverrides().singleInstance(FastWxPayConfig.class);
        String sign = dataMap.get("sign");
        String mch_id = dataMap.get("mch_id");
        if (!mch_id.equals(wxPayConfig.getMchId())) {
            return false;
        }
        Set<String> keySet = dataMap.keySet();
        Map<String, Object> signMap = new HashMap<String, Object>();
        for (String string : keySet) {
            if (string.equals("sign")) continue;
            signMap.put(string, dataMap.get(string));
        }
        String currSign = WxPayUtils.sign(wxPayConfig.getApiKey(), signMap);
        if (sign.equals(currSign)) {
            //支付成功
            return "SUCCESS".equals(dataMap.get("return_code"));
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
     * 发起手机APP支付
     *
     * @param out_trade_no 支付订单号
     * @param body         订单标题
     * @param details      订单详情
     * @param money        支付金额
     * @param fromIP       请求的IP地址
     * @return 发起微信APP支付的配置信息
     */
    public static Map<String, Object> requestAppPay(String out_trade_no,
                                                    String body,
                                                    String details, double money, String fromIP) throws Exception {
        FastWxPayConfig wxPayConfig = FastChar.getOverrides().singleInstance(FastWxPayConfig.class);


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
        params.put("time_start", FastDateUtils.getDateString("yyyyMMddHHmmss"));
        params.put("notify_url", wxPayConfig.getNotifyUrl());
        params.put("trade_type", FastWxPayConfig.TradeTypeEnum.APP.name());

        params.put("sign", sign(wxPayConfig.getApiKey(), params));


        if (FastChar.getConstant().isDebug()) {
            FastChar.getLog().info("微信支付：[" + out_trade_no + "] [" + body + "] [" + money + "] [" + wxPayConfig.getNotifyUrl() + "]");
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
            appSignParams.put("appid", wxPayConfig.getAppId());
            appSignParams.put("package", "Sign=WXPay");
            appSignParams.put("partnerid", wxPayConfig.getMchId());
            appSignParams.put("prepayid", result.get("prepay_id"));
            appSignParams.put("noncestr", FastStringUtils.buildOnlyCode("WX"));
            appSignParams.put("timestamp", String.valueOf(genTimeStamp()));
            appSignParams.put("sign", sign(wxPayConfig.getApiKey(), appSignParams));
            appSignParams.put("msg", result.get("return_msg"));
            return appSignParams;
        }
    }


    /**
     * 根据授权码获得用户信息
     * @param authorCode
     * @return
     */
    public static Map getUserInfo(String authorCode) {
        CloseableHttpResponse response;
        HttpEntity entity;
        try {
            FastWxPayConfig wxPayConfig = FastChar.getOverrides().singleInstance(FastWxPayConfig.class);

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

            Map authMap = FastChar.getJson().fromJson(result, Map.class);
            try {
                EntityUtils.consume(entity);
                response.close();
            } catch (Exception ignored) {
            }
            String openid = authMap.get("openid").toString();
            String access_token = authMap.get("access_token").toString();

            HttpUriRequest userRequest = RequestBuilder.get()
                    .setUri(new URI("https://api.weixin.qq.com/sns/userinfo?access_token=" + access_token + "&openid=" + openid))
                    .build();
            response = httpclient.execute(userRequest);
            entity = response.getEntity();
            String userResult = EntityUtils.toString(entity, "utf-8");
            Map authorMap = FastChar.getJson().fromJson(userResult, Map.class);
            try {
                EntityUtils.consume(entity);
                response.close();
            } catch (Exception ignored) {
            }
            return authorMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
