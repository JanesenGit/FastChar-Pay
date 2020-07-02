package com.fastchar.pay.apple;

import com.fastchar.core.FastChar;
import com.fastchar.core.FastHandler;
import com.fastchar.core.FastMapWrap;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * 苹果内购 工具类
 * @author 沈建（Janesen）
 * @date 2020/6/8 11:58
 */
public class ApplePurchaseUtils {


    public static FastHandler validReceipt(String receipt) throws Exception {
        FastHandler handler = new FastHandler();
        handler.setError("校验成功！");
        handler.setCode(0);

        String resultJson = sendToBuy(receipt);

        FastMapWrap mapWrap = FastMapWrap.newInstance(FastChar.getJson().fromJson(resultJson, Map.class));
        if (mapWrap.getInt("status") == 0) {//校验通过
            return handler;
        } else if (mapWrap.getInt("status") == 21007) {//是沙盒的数据
            resultJson = sendToSandbox(receipt);
            mapWrap = FastMapWrap.newInstance(FastChar.getJson().fromJson(resultJson, Map.class));
        }
        if (mapWrap.getInt("status") == 0) {//校验通过
            return handler;
        }else{
            handler.setCode(-1);
            handler.setError(convertError(mapWrap.getInt("status")));
        }
        return handler;
    }


    //沙盒环境
    private static String sendToSandbox(String receipt) throws Exception {
        RequestBuilder requestBuilder = RequestBuilder.post()
                .setUri(new URI("https://sandbox.itunes.apple.com/verifyReceipt"));

        Map<String, Object> data = new HashMap<>();
        data.put("receipt-data", receipt
                .replace("\n", "")
                .replace("\r", ""));

        requestBuilder.setEntity(new StringEntity(FastChar.getJson().toJson(data),"utf-8"));

        CloseableHttpClient httpclient = HttpClients.custom()
                .build();
        try (CloseableHttpResponse response = httpclient.execute(requestBuilder.build())) {
            HttpEntity entity = response.getEntity();
            String returnText = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);

            return returnText;
        }
    }


    //正式环境
    private static String sendToBuy(String receipt) throws Exception {
        RequestBuilder requestBuilder = RequestBuilder.post()
                .setUri(new URI("https://buy.itunes.apple.com/verifyReceipt"));

        Map<String, Object> data = new HashMap<>();
        data.put("receipt-data", receipt
                .replace("\n", "")
                .replace("\r", ""));

        requestBuilder.setEntity(new StringEntity(FastChar.getJson().toJson(data),"utf-8"));
        CloseableHttpClient httpclient = HttpClients.custom()
                .build();
        try (CloseableHttpResponse response = httpclient.execute(requestBuilder.build())) {
            HttpEntity entity = response.getEntity();
            String returnText = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return returnText;
        }
    }


    private static String convertError(int status) {
        switch (status) {
            case 21000:
                return "AppStore无法读取提供的JSON数据！";
            case 21002:
                return "收据数据不符合格式！";
            case 21003:
                return "收据无法被验证！";
            case 21004:
                return "提供的共享密钥和账户的共享密钥不一致！";
            case 21005:
                return "收据服务器当前不可用！";
            case 21006:
                return "订阅服务已经过期！";
            case 21007:
                return "收据信息是沙盒测试中使用（sandbox），不可发送到正式环境中验证！";
            case 21008:
                return "收据信息是正式环境中使用，不可发送到测试环境中验证！";
        }
        return "未知错误！";
    }

}
