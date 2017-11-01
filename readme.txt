SpringCloud服务网关Zuul
1) pom.xml中除了增加Zuul(spring-cloud-starter-zuul)、eureka(spring-cloud-starter-eureka)的依赖
2) 应用主类使用@EnableZuulProxy注解开启Zuul
@EnableZuulProxy
@SpringCloudApplication
//相当于@SpringBootApplication、@EnableDiscoveryClient、@EnableCircuitBreaker 
public class Application {
	public static void main(String[] args) {
		new SpringApplicationBuilder(Application.class).web(true).run(args);
	}
}
3)application.properties中配置Zuul应用的基础信息，如：应用名、服务端口等。
spring.application.name=api-gateway
server.port=5555
4)zuul路由配置
# routes to url
zuul.routes.api-a-url.path=/api-a-url/**
zuul.routes.api-a-url.url=http://localhost:2222/
该配置，定义了，所有到Zuul的中规则为：/api-a-url/**的访问都映射到http://localhost:2222/上，
也就是说当我们访问http://localhost:5555/api-a-url/add?a=1&b=2的时候，Zuul会将该请求路由到：
http://localhost:2222/add?a=1&b=2上。

通过url映射的方式对于Zuul来说，并不是特别友好，Zuul需要知道我们所有为服务的地址，才能完成所有的映射配置。
而实际上，我们在实现微服务架构时，服务名与服务实例地址的关系在eureka server中已经存在了，
所以只需要将Zuul注册到eureka server上去发现其他服务，我们就可以实现对serviceId的映射
zuul.routes.api-a.path=/api-a/**
zuul.routes.api-a.serviceId=service-A
zuul.routes.api-b.path=/api-b/**
zuul.routes.api-b.serviceId=service-B
eureka.client.serviceUrl.defaultZone=http://localhost:1111/eureka/

5)url访问测试
http://localhost:5555/api-a/add?a=1&b=2：通过serviceId映射访问service-A中的add服务
http://localhost:5555/api-b/add?a=1&b=2：通过serviceId映射访问service-B中的add服务
http://localhost:5555/api-a-url/add?a=1&b=2：通过url映射访问service-A中的add服务

6)
	在完成了服务路由之后，我们对外开放服务还需要一些安全措施来保护客户端只能访问它应该访问到的资源。
所以我们需要利用Zuul的过滤器来实现我们对外服务的安全控制。
在服务网关中定义过滤器只需要继承ZuulFilter抽象类实现其定义的四个抽象函数就可对请求进行拦截与过滤。
	比如下面的例子，定义了一个Zuul过滤器，实现了在请求被路由之前检查请求中是否有accessToken参数，
若有就进行路由，若没有就拒绝访问，返回401 Unauthorized错误。

public class AccessFilter extends ZuulFilter  {
    private static Logger log = LoggerFactory.getLogger(AccessFilter.class);
    @Override
    public String filterType() {
        return "pre";
    }
    @Override
    public int filterOrder() {
        return 0;
    }
    @Override
    public boolean shouldFilter() {
        return true;
    }
    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
        Object accessToken = request.getParameter("accessToken");
        if(accessToken == null) {
            log.warn("access token is empty");
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(401);
            return null;
        }
        log.info("access token ok");
        return null;
    }
}
自定义过滤器的实现，需要继承ZuulFilter，需要重写实现下面四个方法：

filterType：返回一个字符串代表过滤器的类型，在zuul中定义了四种不同生命周期的过滤器类型，具体如下：
pre：可以在请求被路由之前调用
routing：在路由请求时候被调用
post：在routing和error过滤器之后被调用
error：处理请求时发生错误时被调用
filterOrder：通过int值来定义过滤器的执行顺序
shouldFilter：返回一个boolean类型来判断该过滤器是否要执行，所以通过此函数可实现过滤器的开关。
在上例中，我们直接返回true，所以该过滤器总是生效。
run：过滤器的具体逻辑。需要注意，这里我们通过ctx.setSendZuulResponse(false)令zuul过滤该请求，
不对其进行路由，然后通过ctx.setResponseStatusCode(401)设置了其返回的错误码，
当然我们也可以进一步优化我们的返回，比如，通过ctx.setResponseBody(body)对返回body内容进行编辑等。

7)在实现了自定义过滤器之后，还需要实例化该过滤器才能生效，我们只需要在应用主类中增加如下内容：
@EnableZuulProxy
@SpringCloudApplication
public class Application {
	public static void main(String[] args) {
		new SpringApplicationBuilder(Application.class).web(true).run(args);
	}
	@Bean
	public AccessFilter accessFilter() {
		return new AccessFilter();
	}
}
8)启动该服务网关后，访问：
http://localhost:5556/api-a/add?a=1&b=2：返回401错误
http://localhost:5556/api-a/add?a=1&b=2&accessToken=token：正确路由到server-A，并返回计算内容