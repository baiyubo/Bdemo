package com.butterfly.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HttpClientUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientUtil.class);
    public static PoolingHttpClientConnectionManager cm;
    public static String EMPTY_STR = "";
    public static String UTF_8 = "UTF-8";

    public static void init() {
        if (cm == null) {
            cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(50);// 整个连接池最大连接数
            cm.setDefaultMaxPerRoute(5);// 每路由最大连接数，默认值是2
        }
    }

    /**
     * 通过连接池获取HttpClient
     *
     * @return
     */
    public static CloseableHttpClient getHttpClient() {
        init();
        return HttpClients.custom().setConnectionManager(cm).build();
    }

    /**
     * @param url
     * @return
     */
    public static String httpGetRequest(String url, Integer connectTimeout, Integer socketTimeout) {
        HttpGet httpGet = new HttpGet(url);
        return getResult(httpGet, connectTimeout, socketTimeout);
    }

    public static String httpGetRequest(String url, Map<String, Object> params, Integer connectTimeout, Integer socketTimeout) throws URISyntaxException {
        URIBuilder ub = new URIBuilder();
        ub.setPath(url);

        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        ub.setParameters(pairs);

        HttpGet httpGet = new HttpGet(ub.build());
        return getResult(httpGet, connectTimeout, socketTimeout);
    }

    public static String httpGetRequest(String url, Map<String, Object> headers, Map<String, Object> params, Integer connectTimeout, Integer socketTimeout)
            throws URISyntaxException {
        URIBuilder ub = new URIBuilder();
        ub.setPath(url);

        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        ub.setParameters(pairs);

        HttpGet httpGet = new HttpGet(ub.build());
        for (Map.Entry<String, Object> param : headers.entrySet()) {
            httpGet.addHeader(param.getKey(), String.valueOf(param.getValue()));
        }
        return getResult(httpGet, connectTimeout, socketTimeout);
    }

    public static String httpPostRequest(String url, Integer connectTimeout, Integer socketTimeout) {
        HttpPost httpPost = new HttpPost(url);
        return getResult(httpPost, connectTimeout, socketTimeout);
    }

    public static String httpPostRequest(String url, Map<String, Object> params, Integer connectTimeout, Integer socketTimeout) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        httpPost.setEntity(new UrlEncodedFormEntity(pairs, UTF_8));
        return getResult(httpPost, connectTimeout, socketTimeout);
    }

    public static String httpPostRequest(String url, Map<String, Object> headers, Map<String, Object> params, Integer connectTimeout, Integer socketTimeout)
            throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        for (Map.Entry<String, Object> param : headers.entrySet()) {
            httpPost.addHeader(param.getKey(), String.valueOf(param.getValue()));
        }
        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        httpPost.setEntity(new UrlEncodedFormEntity(pairs, UTF_8));
        return getResult(httpPost, connectTimeout, socketTimeout);
    }

    public static String httpPostRequest(String url, Map<String, Object> headers, JSONObject params, Integer connectTimeout, Integer socketTimeout, Boolean flag)
            throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        for (Map.Entry<String, Object> param : headers.entrySet()) {
            httpPost.addHeader(param.getKey(), String.valueOf(param.getValue()));
        }
        StringEntity s = new StringEntity(params.toString(), "UTF-8");
        s.setContentType("application/json;charset=UTF-8");//发送json数据需要设置contentType
        httpPost.setEntity(s);

        return getResult(httpPost, connectTimeout, socketTimeout);
    }

    public static String httpPostGzipRequest(String url, HashMap<String, Object> params, Integer connectTimeout, Integer socketTimeout)
            throws Exception {
        String encryptData = params.remove("encrypt_data").toString();
        URIBuilder ub = new URIBuilder();
        ub.setPath(url);
        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        ub.setParameters(pairs);
        URI uri = ub.build();
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(new GzipCompressingEntity(new StringEntity((encryptData))));
        return getResult(httpPost, connectTimeout, socketTimeout);
    }

    private static ArrayList<NameValuePair> covertParams2NVPS(Map<String, Object> params) {
        ArrayList<NameValuePair> pairs = new ArrayList<>();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            pairs.add(new BasicNameValuePair(param.getKey(), String.valueOf(param.getValue())));
        }
        return pairs;
    }

//    public static void downloadImage(String imgUrl, String filePath){
//        UploadImageService uploadImageService = SpringBeanUtil.getBean("uploadImageService",UploadImageService.class);
//        uploadImageService.downloadImage(imgUrl,filePath);
//    }

    /**
     * 下载图片
     *
     * @param
     */
    public static boolean download(String imgUrl, String filePath, Integer connectTimeout, Integer socketTimeout) {

        HttpGet get = new HttpGet(imgUrl);
        CloseableHttpResponse response = null;
        FileOutputStream fos = null;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).build();
        get.setConfig(requestConfig);
        try {

            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(get);

            if (response.getStatusLine().getStatusCode() == 200) {
                //得到实体
                HttpEntity entity = response.getEntity();

                byte[] data = EntityUtils.toByteArray(entity);

                //图片存入磁盘
                fos = new FileOutputStream(filePath);
                fos.write(data);
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                LOG.info("关闭response");
            } catch (IOException e) {
                LOG.error("httpClient调用close异常", e);
                e.printStackTrace();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 处理Http请求
     *
     * @param request
     * @return
     */
    public static String getResult(HttpRequestBase request, Integer connectTimeout, Integer socketTimeout) {
        //超时设置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).build();
        request.setConfig(requestConfig);
//         CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpClient httpClient = getHttpClient();
        CloseableHttpResponse response = null;
        try {
            LOG.info("开始执行请求");
            response = httpClient.execute(request);
            LOG.info("执行请求结束");
            // response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // long len = entity.getContentLength();// -1 表示长度未知
                String result = EntityUtils.toString(entity);
                // httpClient.close();
                return result;
            }
        } catch (ClientProtocolException e) {
            LOG.error("http接口调用异常ClientProtocolException", e);
            e.printStackTrace();
        } catch (IOException e) {
            LOG.error("http接口调用异常IOException", e);
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                LOG.info("关闭response");
            } catch (IOException e) {
                LOG.error("httpClient调用close异常", e);
                e.printStackTrace();
            }
        }
        return EMPTY_STR;
    }

    /**
     * 处理Http请求
     *
     * @param request
     * @return
     */
    public static byte[] getResult2(HttpRequestBase request, Integer connectTimeout, Integer socketTimeout) {
        //超时设置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).build();
        request.setConfig(requestConfig);
        // CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpClient httpClient = getHttpClient();
        CloseableHttpResponse response = null;
        try {
            LOG.info("开始执行请求");
            response = httpClient.execute(request);
            LOG.info("执行请求结束");
            // response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                byte[] bytes = EntityUtils.toByteArray(entity);


                // long len = entity.getContentLength();// -1 表示长度未知
                InputStream content = entity.getContent();
                // httpClient.close();
                return bytes;
            }
        } catch (ClientProtocolException e) {
            LOG.error("http接口调用异常ClientProtocolException", e);
            e.printStackTrace();
        } catch (IOException e) {
            LOG.error("http接口调用异常IOException", e);
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                LOG.info("关闭response");
            } catch (IOException e) {
                LOG.error("httpClient调用close异常", e);
                e.printStackTrace();
            }
        }
        return null;
    }


}