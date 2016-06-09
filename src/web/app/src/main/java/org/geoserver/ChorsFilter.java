package org.geoserver;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

public class ChorsFilter implements Filter {

	public void init(FilterConfig fConfig) throws ServletException { }

	public void destroy() {	}

	public void doFilter(
		ServletRequest request, ServletResponse response, 
		FilterChain chain) throws IOException, ServletException {

		String method = ((HttpServletRequest)request).getMethod();
/*
		((HttpServletResponse)response).addHeader(
			"Access-Control-Allow-Origin", "http://localhost:9001"
		);
		((HttpServletResponse)response).addHeader(
				"Access-Control-Allow-Credentials", "true"
		);
		((HttpServletResponse)response).addHeader(
				"Access-Control-Allow-Methods", method
		);
		((HttpServletResponse)response).addHeader(
				"Access-Control-Request-Headers", HttpHeaders.AUTHORIZATION
		);*/
		HttpServletResponse resp = (HttpServletResponse) response;
		resp.setHeader("Access-Control-Allow-Origin", "http://localhost:9001");
		resp.setHeader("Access-Control-Allow-Credentials", "true");
		resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
		resp.setHeader("Access-Control-Allow-Headers", ((HttpServletRequest)request).getHeader("Access-Control-Request-Headers"));
		if (!"OPTIONS".equals(method)) {
			chain.doFilter(request, response);
		}
	}

}
