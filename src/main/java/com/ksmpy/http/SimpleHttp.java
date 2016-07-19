package com.ksmpy.http;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BestMatchSpecFactory;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 1.支持get,post,body
 * 2.支持cookie
 * 3.链式请求
 * 4.支持链接超时
 * 5.连接重试
 * 
 * SimpleHttp.build().header(headerMap).url("http://xxx.com").trys(2).get();
 * SimpleHttp.build().header(headerMap).url("http://xxx.com").trys(1).post(params);
 * 
 * @author zcrowder
 *
 */
public class SimpleHttp {
	
	public static SimpleHttp.Http build() {
		return new Http();
	}
	
	/**
	 * 获取url地址参数，参数名称全部转换为小写
	 * @param url
	 * @return
	 */
	public static Map<String,Object> params(String url) {
		Map<String,Object> paramsMap = new HashMap<String,Object>();
		if(StringUtils.isBlank(url)) return paramsMap;
		
		String[] arrSplit = null;
		String[] arrParam = null;
		String[] arrKeyValue = null;

		url = url.trim().toLowerCase();
		arrSplit = url.split("[?]");
		if (arrSplit.length > 1) {
			if (arrSplit[1] != null) {
				arrParam = arrSplit[1].split("[&]");
				for (int i = 0; i < arrParam.length; i++) {
					arrKeyValue = arrParam[i].split("=");
					if(arrKeyValue.length == 2){
						paramsMap.put(arrKeyValue[0], arrKeyValue[1]);
					}
				}
			}
		} else {
			arrParam = url.split("[&]");
			for (int i = 0; i < arrParam.length; i++) {
				arrKeyValue = arrParam[i].split("=");
				if(arrKeyValue.length == 2){
					paramsMap.put(arrKeyValue[0], arrKeyValue[1]);
				}
			}
		}

		return paramsMap;
	}
	
	public static String params(Map<String, Object> params) {
		StringBuffer sb = new StringBuffer();

		Iterator<String> iter = params.keySet().iterator();
		while (iter.hasNext()) {
			String name = (String) iter.next();
			if (StringUtils.isBlank(String.valueOf(params.get(name))))
				continue;
			try {
				sb.append("&")
						.append(name)
						.append("=")
						.append(URLEncoder.encode(
								String.valueOf(params.get(name)), "utf-8"));
			} catch (Exception e) {
			}
		}

		return sb.substring(1);
	}

	public static class Http {
		protected final Logger logger = LoggerFactory.getLogger(this.getClass());
		
		private String url = "";
		private Map<String,String> headers = new HashMap<String,String>();
		private Map<String,Object> params = new HashMap<String,Object>();
		private String body = "";
		private int trys = 0;
		private CookieStore cookieStore;
		private HttpClientContext context = null;  
		private int timeout = 2000;
		private String response = "";
		private String mockResponse = "";
		private String encode = "utf-8";
		
		public Http url(String url){
			this.url = url;
			return this;
		}
		
		public Http timeout(int timeout){
			this.timeout = timeout;
			return this;
		}
		
		public Http header(Map<String,String> headerMap){
			this.headers = headerMap;
			return this;
		}
		
		public Http trys(int trys){
			this.trys = trys;
			return this;
		}
		
		public Http encode(String encode){
			this.encode = encode;
			return this;
		}
		
		public Http storeCookie(){
			this.cookieStore = context.getCookieStore();
			return this;
		}
		
		public Http params(Map<String,Object> params){
			this.params = params;
			return this;
		}
		
		public Http params(String body){
			this.body = body;
			return this;
		}
		
		public Http get(){
			return get("");
		}
		
		public Http get(String mockResponse){
			if(StringUtils.isNotBlank(mockResponse)){
				this.mockResponse = mockResponse;
			}
			
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setSocketTimeout(timeout).setConnectTimeout(timeout)
					.setConnectionRequestTimeout(timeout)
					.setStaleConnectionCheckEnabled(true).build();

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultRequestConfig(defaultRequestConfig).build();
						
			boolean isTry = true;
			while(trys >= 0 && isTry){
				try {
					if(!this.params.isEmpty()){
						url = url + "?" + SimpleHttp.params(this.params);
					}
					
					HttpGet httpget = new HttpGet(url);
					// Create a custom response handler
					ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
						public String handleResponse(final HttpResponse response)
								throws ClientProtocolException, IOException {
							int status = response.getStatusLine().getStatusCode();
							if (status >= 200 && status < 1024 ) {
								HttpEntity entity = response.getEntity();
								return entity != null ? EntityUtils.toString(entity)
										: null;
							} else {
								throw new ClientProtocolException(
										"Unexpected response status: " + status);
							}
						}
					};
					
					context = HttpClientContext.create();  
			        Registry<CookieSpecProvider> registry = RegistryBuilder  
			                .<CookieSpecProvider> create()  
			                .register(CookieSpecs.BEST_MATCH, new BestMatchSpecFactory())  
			                .register(CookieSpecs.BROWSER_COMPATIBILITY,  
			                        new BrowserCompatSpecFactory()).build();  
			        context.setCookieSpecRegistry(registry);  
			        context.setCookieStore(cookieStore);
			        
					Iterator<String> header = headers.keySet().iterator();
					while (header.hasNext()) {
						String key = (String) header.next();
						String value = String.valueOf(headers.get(key));
						httpget.setHeader(key, value);
					}
					this.response = httpclient.execute(httpget, responseHandler,context);
					isTry = false;
				} catch (Exception e) {
					--trys;
					if(StringUtils.isBlank(this.mockResponse)){
						this.response = e.getMessage();
					}
					logger.error(e.getMessage(),e);
				} finally {
					try {
						httpclient.close();
					} catch (IOException e) {
					}
				}
			}
			
			return this;
		}
		
		public Http post(String mockResponse){
			if(StringUtils.isNotBlank(mockResponse)){
				this.mockResponse = mockResponse;
			}
			
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setSocketTimeout(timeout).setConnectTimeout(timeout)
					.setConnectionRequestTimeout(timeout)
					.setStaleConnectionCheckEnabled(true).build();

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultRequestConfig(defaultRequestConfig).build();
			
			HttpPost httpPost = new HttpPost(url);

			HttpEntity requestEntity = null;
			if(!this.params.isEmpty()){
				List<NameValuePair> formparams = new ArrayList<NameValuePair>();
				Iterator<String> iter = this.params.keySet().iterator();
				while (iter.hasNext()) {
					String key = (String) iter.next();
					String value = String.valueOf(params.get(key));
					formparams.add(new BasicNameValuePair(key, value));
				}
				
				try {
					requestEntity = new UrlEncodedFormEntity(formparams, encode);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} else if(StringUtils.isNotBlank(this.body)){
				requestEntity = new StringEntity(this.body, encode);
			} 			

			boolean isTry = true;
			while(trys >= 0 && isTry){
				CloseableHttpResponse response = null;
				try {
					context = HttpClientContext.create();  
			        Registry<CookieSpecProvider> registry = RegistryBuilder  
			                .<CookieSpecProvider> create()  
			                .register(CookieSpecs.BEST_MATCH, new BestMatchSpecFactory())  
			                .register(CookieSpecs.BROWSER_COMPATIBILITY,  
			                        new BrowserCompatSpecFactory()).build();  
			        context.setCookieSpecRegistry(registry);  
			        context.setCookieStore(cookieStore);
					httpPost.setEntity(requestEntity);
					
					Iterator<String> head = headers.keySet().iterator();
					while (head.hasNext()) {
						String key = (String) head.next();
						String value = String.valueOf(headers.get(key));
						httpPost.setHeader(key, value);
					}
					
					response = httpclient.execute(httpPost,context);
	
					HttpEntity entity = response.getEntity();
					// do something useful with the response body
					// and ensure it is fully consumed
					this.response = EntityUtils.toString(entity);
					response.close();
					isTry = false;
				} catch (Exception e) {
					--trys;
					if(StringUtils.isBlank(this.mockResponse)){
						this.response = e.getMessage();
					}
					logger.error(e.getMessage(),e);
				} finally {
					try {
						httpclient.close();
					} catch (IOException e) {
					}
				}
			}
			return this;
		}
		
		public Http post(Map<String,String> params,String mockResponse){
			Iterator<String> iter = params.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				String value = String.valueOf(params.get(key));
				this.params.put(key, value);
			}
			
			post(mockResponse);
			return this;
		}
		
		public Http post(Map<String,String> params){
			post(params,"");
			return this;
		}
		
		public Http post(){
			this.post("");
			return this;
		}
		
		public String response(){
			if(StringUtils.isBlank(this.response)){
				return this.mockResponse;
			}
			return this.response;
		}
	}
	
	public static void main(String[] args) {
		String testUrl = "http://www.baidu.com";
		for (int i = 0; i < 4; i++) {
			String params = "uname=xxx&password=xxx&alt=xxx&execution=e323s1&eventId=submit";
			Map<String, Object> pMap = SimpleHttp.params(params);
			//链式地址访问
			//先访问http://sso.mysnail.com/login
			//再访问http://plan2.mysnail.com/task/myWork.html
			Http http = SimpleHttp.build()
								  .url("http://xxx")
								  .trys(1)
								  .params(pMap)
								  .post()
								  .storeCookie()
								  .url("http://xxx")
								  .get();
			
			
			//post
			SimpleHttp.build().url(testUrl).params(new HashMap()).post();
			
			//get
			SimpleHttp.build().url(testUrl).get();
			
			//body
			SimpleHttp.build().url(testUrl).params("").post();
			
			//文件图片
			
			System.out.println(http.response());
			System.out.println(http.cookieStore.getCookies());
		}
	}
}
