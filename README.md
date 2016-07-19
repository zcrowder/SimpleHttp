# SimpleHttp
http api 封装httpclient，简化http请求

## 支持的功能
1. 支持get,post,body
2. 支持cookie
3. 链式请求
4. 支持链接超时
5. 连接重试

## post请求
> SimpleHttp.build().url(testUrl).params(new HashMap()).post();
			
## get请求
> SimpleHttp.build().url(testUrl).get();
			
## body请求
> SimpleHttp.build().url(testUrl).params("").post();