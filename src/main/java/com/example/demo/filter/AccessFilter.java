package com.example.demo.filter;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public class AccessFilter extends ZuulFilter {
	private static Logger log = LoggerFactory.getLogger(AccessFilter.class);
	
	/**
	 * 过滤器的具体逻辑
	 * 这里我们通过ctx.setSendZuulResponse(false)令zuul过滤该请求，不对其进行路由，
	 * 然后通过ctx.setResponseStatusCode(401)设置了其返回的错误码，
	 * 当然我们也可以进一步优化我们的返回，比如，通过ctx.setResponseBody(body)对返回body内容进行编辑等。
	 */
	@Override
	public Object run() {
		RequestContext requestContext = RequestContext.getCurrentContext();
		HttpServletRequest request = requestContext.getRequest();
		
		log.info("%s request to %s",request.getMethod(),request.getRequestURL().toString());
		
		Object accessToken = request.getParameter("accessToken");
		if(accessToken == null) {
			log.warn("access token is empty");
			requestContext.setSendZuulResponse(false);//令zuul过滤该请求，不对其进行路由
			requestContext.setResponseStatusCode(401);//设置了其返回的错误码
			return null;
		}
		log.info("access token ok");//可以通过ctx.setResponseBody(body)对返回body内容进行编辑
		return null;
	}

	/**
	 * 判断该过滤器是否要执行，所以通过此函数可实现过滤器的开关。
	 */
	@Override
	public boolean shouldFilter() {
		return true;
	}

	/**
	 * 通过int值来定义过滤器的执行顺序
	 */
	@Override
	public int filterOrder() {
		return 0;
	}

	/**
	 * 返回一个字符串代表过滤器的类型，在zuul中定义了四种不同生命周期的过滤器类型，具体如下：
		pre：可以在请求被路由之前调用
		routing：在路由请求时候被调用
		post：在routing和error过滤器之后被调用
		error：处理请求时发生错误时被调用
	 */
	@Override
	public String filterType() {
		return "pre";
	}

}
