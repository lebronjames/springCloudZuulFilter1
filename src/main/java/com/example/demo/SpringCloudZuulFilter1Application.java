package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

import com.example.demo.filter.AccessFilter;

@EnableZuulProxy
@SpringCloudApplication
public class SpringCloudZuulFilter1Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudZuulFilter1Application.class, args);
	}
	
	/**
	 * 实现了自定义过滤器之后，还需要实例化该过滤器才能生效
	 * @return
	 */
	@Bean
	public AccessFilter accessFilter() {
		return new AccessFilter();
	}
}
