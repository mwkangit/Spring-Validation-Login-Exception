# Practice Setting



**Project : exception**

```groovy
plugins {
	id 'org.springframework.boot' version '2.6.4'
	id 'io.spring.dependency-management' version '1.0.11.RELEASE'
	id 'java'
}

group = 'hello'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-web'
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
	useJUnitPlatform()
}
```



# Exception & Error Page



### Exception



- #### 서블릿은 2가지 방식으로 예외 처리를 지원한다.

  - #### `Exception`(예외)로 일반적으로 사용하는 예외이다. WAS  Servlet Container까지 예외가 날라간다.

  - #### `response.sendError(HTTP 상태 코드, 오류 메시지)`로 response는 HttpServletResponse이다.

- #### 실행 도중 예외를 잡지 못하고 처음 실행한 main() 메소드(스레드)를 넘어서 예외가 던져지면 예외 정보를 남기고 해당 스레드는 종료된다. 즉, 상위 메소드(부모)에게 예외를 옮긴다.

- #### 웹 어플리케이션은 사용자 요청별로 별도의 스레드가 할당되고 서블릿 컨테이너 안에서 실행된다.

- #### 어플리케이션에서 예외가 발생했는데 어디선가 try ~ catch로 예외를 잡아서 처리하면 아무런 문제가 없다. 하지만 예외를 잡지 못하고 서블릿 밖으로 까지 예외가 전달될 수 있다.

  - #### WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)

  - #### 결국 톰캣 같은 WAS 까지 예외가 전달된다.



```properties
# Spring Boot basic  exception page(Whitelabel Error Page)
server.error.whitelabel.enabled=false
```

- #### 스프링 부트가 제공하는 기본 예외 페이지로 꺼두었다.



```java
@Slf4j
@Controller
public class ServletExController {

    @GetMapping("/error-ex")
    public void errorEx(){
        throw new RuntimeException("예외 발생!");
    }

    @GetMapping("/error-404")
    public void error404(HttpServletResponse response) throws IOException {
        response.sendError(404, "404 오류!");

    }

    @GetMapping("/error-400")
    public void error400(HttpServletResponse response) throws IOException {
        response.sendError(400, "400 오류!");
    }

    @GetMapping("/error-500")
    public void error500(HttpServletResponse response) throws IOException {
        response.sendError(500, "500 오류!");

    }
}
```

- #### 사용자 요청시 각 예외와 상태코드를 확인할 수 있다.

- #### `throw`사용 시 500 상태 코드가 발생한다. 즉, `Exception`의 경우 서버 내부에서 처리할 수 없는 오류가 발생한 것으로 생각한 것이다.

- #### `response.sendError(HTTP 상태 코드, 오류 메시지)`는 호출한다고 당장 예외가 발생하는 것은 아니지만 서블릿 컨테이너에게 오류가 발생했다는 점을 전달할 수 있다. 즉, exception을 터뜨리는 것은 아니지만 알려줄 순 있는 것이다.

  - #### WAS(sendError 호출 기록 확인) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(response.sendError())

  - #### response 내부에 오류가 발생했다는 상태를 저장한 것이다.

  - #### 서블릿 컨테이너는 고객에게 응답 전에 response에 sendError()가 호출되었는지 확인한다. 호출되었다면 설정한 오류 코드에 맞추어 기본 오류 페이지를 보여준다.

- #### 예외 발생하면 Servlet Container은 500으로 처리하거나 sendError()로 온 것으로 처리한다.



#### Error Page



```java
@Component
public class WebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error-page/404");
        ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error-page/500");

        ErrorPage errorPageEx = new ErrorPage(RuntimeException.class, "/error-page/500");

        factory.addErrorPages(errorPage404, errorPage500, errorPageEx);
    }
}
```

- #### `response.sendError(404)` 는 errorPage404 호출한다.

- #### `response.sendError(500)` 는 errorPage500 호출한다.

- #### `RuntimeException` 또는 그 자식 타입의 예외는 errorPageEx 호출한다.

- #### 현재 스프링 부트를 통해서 서블릿 컨테이너를 실행하기 때문에 스프링 부트가 제공하는 기능을 사용해서 서블릿 오류 페이지를 등록한다.

- #### 오류 페이지는 예외를 다룰 때 해당 예외와 그 자식 타입의 오류를 함께 처리한다.

- #### 오류가 발생했을 때 처리할 수 있는 컨트롤러가 필요하다. 즉, 현재 위 코드에서 작성한 URL 호출된다.



```java
@Slf4j
@Controller
public class ErrorPageController {

    @RequestMapping("/error-page/404")
    public String errorPage404(HttpServletRequest request, HttpServletResponse response){
        log.info("errorPage 404");
        return "error-page/404";
    }

    @RequestMapping("/error-page/500")
    public String errorPage500(HttpServletRequest request, HttpServletResponse response){
        log.info("errorPage 500");
        return "error-page/500";
    }
}
```

- #### 오류 발생 시 화면을 보여주기 위한 컨트롤러이다.



#### Error Page Flow



- #### 서블릿은 예외가 발생해서 서블릿 밖으로 전달되거나 response.sendError()가 호출 되었을 때 설정된 오류 페이지를 찾는다.

- #### WAS는 해당 예외를 처리하는 오류 페이지 정보를 확인인다.

  `new ErrorPage(RuntimeException.class, "/error-page/500")`

  - #### WAS는 여기서 RuntimeException 발생 시 `/error-page/500` URL을 호출하게 된다.

- #### 오류페이지 요청 흐름

  - #### WAS `/error-page/500` 다시 요청 -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러(/error-page/500) -> View

- #### 예외 발생과 오류 페이지 요청 흐름

  - #### WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)

  - #### WAS `/error-page/500` 다시 요청 -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러(/error-page/500) -> View

- #### 중요한 점은 클라이언트는 서버 내부에서 이런 일이 일어나는 것을 전혀 모른다는 것이다. 오직 서버 내부에서 오류 페이지를 찾기 위해 추가적인 호출을 한다.



```java
@Slf4j
@Controller
public class ErrorPageController {

    //RequestDispatcher 상수로 정의되어 있음
    public static final String ERROR_EXCEPTION = "javax.servlet.error.exception";
    public static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
    public static final String ERROR_MESSAGE = "javax.servlet.error.message";
    public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
    public static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";
    public static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";

    
    @RequestMapping(value = "/error-page/500", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> errorPage500Api(
            HttpServletRequest request, HttpServletResponse response){
        log.info("API errorPage 500");

        HashMap<String , Object> result = new HashMap<>();
        // exception 가져오기
        Exception ex = (Exception)request.getAttribute(ERROR_EXCEPTION);
        // 상태코드 가져오기
        result.put("status", request.getAttribute(ERROR_STATUS_CODE));
        // 메시지 가져오기 // ApiExceptionController getMember() 에서 메시지 넣었다
        result.put("message", ex.getMessage());

        Integer statusCode = (Integer)request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        return new ResponseEntity<>(result, HttpStatus.valueOf(statusCode));
    }

    private void printErrorInfo(HttpServletRequest request){
        log.info("ERROR_EXCEPTION: {}", request.getAttribute(ERROR_EXCEPTION));
        log.info("ERROR_EXCEPTION_TYPE: {}", request.getAttribute(ERROR_EXCEPTION_TYPE));
        log.info("ERROR_MESSAGE: {}", request.getAttribute(ERROR_MESSAGE));
        log.info("ERROR_REQUEST_URI: {}", request.getAttribute(ERROR_REQUEST_URI));
        log.info("ERROR_SERVLET_NAME: {}", request.getAttribute(ERROR_SERVLET_NAME));
        log.info("ERROR_STATUS_CODE: {}", request.getAttribute(ERROR_STATUS_CODE));
        log.info("dispatcherType: {}", request.getDispatcherType());
    }

}
```

- #### WAS는 오류 페이지를 단순히 다시 요청하는 것만 아니라 오류 정보를 request의 attribute에 추가해서 넘겨준다.

- #### `javax.servlet.error.exception` : 예외

- #### `javax.servlet.error.exception_type` : 예외 타입

- #### `javax.servlet.error.message` : 오류 메시지

- #### `javax.servlet.error.request_uri` : 클라이언트 요청 URI

- #### `javax.servlet.error.servlet_name` : 오류가 발생한 서블릿 이름

- #### `javax.servlet.error.status_code` : HTTP 상태 코드



#### Filter DispatcherType



- #### 예외로 요청할 경우 이미 필터나 인터셉터 체크를 완료한 시점에 예외가 발생한 것이기 때문에 또 체크하면 비효율적이다. 결국 클라이언트로 부터 발생한 정상 요청인지 아니면 오류 페이지를 출력하기 위한 내부 요청인지 구분할 수 있어야 한다. 이때 `DispatcherType`을 이용한다.



```java
public enum DispatcherType {
    FORWARD,
    INCLUDE,
    REQUEST,
    ASYNC,
    ERROR
}
```

- #### DispatcherType은 고객 요청시 REQUEST이며 오류 요청 시 ERROR이다. 즉, 고객이 요청한 것인지 서버 내부에서 오류 페이지를 요청하는 것인지 구분할 수 있다.

- #### `REQUEST` : 클라이언트 요청

- #### `ERROR` : 오류 요청

- #### `FORWARD` : MVC에서 배웠던 서블릿에서 다른 서블릿이나 JSP를 호출할 때

  `RequestDispatcher.forward(request, response);`

- #### `INCLUDE` : 서블릿에서 다른 서블릿이나 JSP의 결과를 포함할 때

  `RequestDispatcher.include(request, response);`

- #### `ASYNC` : 서블릿 비동기 호출



```java
@Slf4j
public class LogFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("log filter init");
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        String uuid = UUID.randomUUID().toString();
        try {
            log.info("REQUEST [{}][{}][{}]", uuid, request.getDispatcherType(), requestURI);
            chain.doFilter(request, response);
        } catch (Exception e) {
            throw e;
        } finally {
            log.info("RESPONSE [{}][{}][{}]", uuid, request.getDispatcherType(), requestURI);
        }
    }
    @Override
    public void destroy() {
        log.info("log filter destroy");
    }
}
```

```java
// WebConfig
@Bean
public FilterRegistrationBean logFilter(){
    FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new LogFilter());
    filterRegistrationBean.setOrder(1);
    filterRegistrationBean.addUrlPatterns("/*");
    filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
    return filterRegistrationBean;

}
```

```
// error-ex(RuntimeException) 호출 로그

// 최초 호출은 request로 처리
2022-03-07 18:25:19.584  INFO 10962 --- [nio-8080-exec-2] hello.exception.filter.LogFilter         : REQUEST [4991a52a-0897-4595-bd0c-22a4cd9ae855][REQUEST][/error-ex]
2022-03-07 18:25:19.585  INFO 10962 --- [nio-8080-exec-2] hello.exception.filter.LogFilter         : RESPONSE [4991a52a-0897-4595-bd0c-22a4cd9ae855][REQUEST][/error-ex]
2022-03-07 18:25:19.586 ERROR 10962 --- [nio-8080-exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is java.lang.RuntimeException: 예외 발생!] with root cause

// 다시 요청하는데 ERROR한다
2022-03-07 18:25:19.587  INFO 10962 --- [nio-8080-exec-2] hello.exception.filter.LogFilter         : REQUEST [f8b255c3-e933-4a91-998e-0f221c0a3574][ERROR][/error-page/500]
2022-03-07 18:25:19.588  INFO 10962 --- [nio-8080-exec-2] h.exception.servlet.ErrorPageController  : errorPage 500
2022-03-07 18:25:19.589  INFO 10962 --- [nio-8080-exec-2] h.exception.servlet.ErrorPageController  : ERROR_EXCEPTION: {}
```

- #### 필터에 DispatcherType을 적용한 것이며 등록할 때 어떤 DispatcherType 시 실행하는지 지정한다.

- #### `setDispatcherTypes()`에 허용할 타입을 작성한다. Default는 REQUEST이다. 즉, 클라이언트의 요청이 있는 경우에만 필터가 적용된다.

- #### REQUEST만 사용할 경우 예외 발생 시 필터를 거치지는 않지만 오류 페이지는 호출된다. 즉, 단지 이 필터만 거치지 않는다는 뜻이다.



#### Interceptor DispatcherType



```java
@Slf4j
public class LogInterceptor implements HandlerInterceptor {
    public static final String LOG_ID = "logId";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String uuid = UUID.randomUUID().toString();
        request.setAttribute(LOG_ID, uuid);
        log.info("REQUEST [{}][{}][{}][{}]", uuid,
                request.getDispatcherType(), requestURI, handler);
        return true;
    }
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("postHandle [{}]", modelAndView);
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse
            response, Object handler, Exception ex) throws Exception {
        String requestURI = request.getRequestURI();
        String logId = (String)request.getAttribute(LOG_ID);
        log.info("RESPONSE [{}][{}][{}]", logId, request.getDispatcherType(), requestURI);
        if (ex != null) {
            log.error("afterCompletion error!!", ex);
        }
    }
}
```

```java
// WebConfig
@Override
public void addInterceptors(InterceptorRegistry registry) {

    LogInterceptor logInterceptor = new LogInterceptor();
    registry.addInterceptor(new LogInterceptor())
        .order(1)
        .addPathPatterns("/**")
        .excludePathPatterns("/css/**", "*.ico", "/error", "/error-page/**");
}
```

```
// error-ex(RuntimeException) 호출 로그

// 최초 호출은 request로 처리
2022-03-07 19:00:10.510  INFO 12005 --- [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2022-03-07 19:00:10.511  INFO 12005 --- [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2022-03-07 19:00:10.511  INFO 12005 --- [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 0 ms
2022-03-07 19:00:10.521  INFO 12005 --- [nio-8080-exec-1] h.exception.interceptor.LogInterceptor   : REQUEST [332c3635-f7db-44d5-9992-ccddf8274c23][REQUEST][/error-ex][hello.exception.servlet.ServletExController#errorEx()]
2022-03-07 19:00:10.529  INFO 12005 --- [nio-8080-exec-1] h.exception.interceptor.LogInterceptor   : RESPONSE [332c3635-f7db-44d5-9992-ccddf8274c23][REQUEST][/error-ex]
2022-03-07 19:00:10.531 ERROR 12005 --- [nio-8080-exec-1] h.exception.interceptor.LogInterceptor   : afterCompletion error!!

// 다시 요청에는 인터셉터 적용 안하게 설정되어 있다.
```



- #### 필터는 서블릿 기능이어서 서블릿 전처리가 가능하지만 인터셉터는 스프링 기능이어서 설정을 다르게 한다. 즉, 필터는 DispatcherType에 따라 적용 여부를 판단하는 것이 가능하지만 인터셉터는 DispatcherType과 무관하게 항상 호출된다.

- #### 인터셉터는 오류 페이지 경로를 `excludePathPatterns`를 사용해서 빼면 된다.

- #### 이때 예외 발생 시 인터셉터가 호출되어도 `postHandle`은 호출되지 않는다.

- #### `/error-ex` 오류 요청

  - #### 인터셉터는 경로 정보로 중복 호출 제거(excludePathPatterns("/error-page/**"))

    1. #### WAS(/error-ex, dispatchType=REQUEST) -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러

    2. #### WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)

    3. #### WAS 오류 페이지 확인

    4. #### WAS(/error-page/500, dispatchType=ERROR) -> 필터(x) -> 서블릿 -> 인터셉터(x) -> 컨트롤러(/error-page/500) -> View



## Error Page



- #### 지금까지 ErrorPage 추가하고 컨트롤러를 만들었지만 스프링 부트는 이러한 과정을 모두 기본으로 제공한다.

- #### ErrorPage를 자동으로 등록하며 이때 `/error`라는 경로로 기본 오류 페이지를 설정한다. Default 오류 페이지로 만든 에러 페이지 없으면 이 경로로 보내는 것이다.

  - #### `new ErrorPage("/error")` 상태코드와 예외를 설정하지 않으면 기본 오류 페이지로 사용된다. URL이 /error인 것이다.

  - #### 서블릿 밖으로 예외가 발생하거나 `response.sendError()`가 호출되면 모든 오류는 /error를 호출하게 된다.

- #### `BasicErrorController`라는 스프링 컨트롤러를 자동으로 등록한다.

  - #### `ErrorPage`에서 등록한 /error를 매핑해서 처리하는 컨트롤러이다.

- #### `ErrorMvcAutoConfiguration`이라는 클래스가 오류 페이지를 자동으로 등록하는 역할을 한다.

- #### 개발자는 오류 페이지 화면만 `BasicErrorController`가 제공하는 룰과 우선순위에 따라서 등록하면 된다. 즉, 정적 HTML이면 정적 리소스, 뷰 템플릿을 사용해서 동적으로 오류 화면을 만들고 싶으면 뷰 템플릿 경로에 오류 페이지 파일을 만들어서 넣어두면 된다.

- #### `BasicErrorController` 처리 순서(뷰 선택 우선순위)

  1. #### 뷰 템플릿

     `resources/templates/error/500.html`

     `resources/templates/error/5xx.html`

  2. #### 정적 리소스( static , public )

    `resources/static/error/400.htmlresources/static/error/404.html`

    `resources/static/error/4xx.html`

  3. #### 적용 대상이 없을 때 뷰 이름( error )

    `resources/templates/error.html`

- #### 해당 경로 위치에 HTTP 상태 코드 이름의 뷰 파일을 넣어두면 된다.

- #### 뷰 템플릿이 정적 리소스보다 우선 순위가 빠르다.

- #### 5xx, 4xx는 500대, 400대 오류를 모두 처리하는 것이다.

- #### 현재 코드에서 `error-402`, `error-3` 요청 시 404.html이 출력되는 이유는 404 상태 코드 예외가 발생하기 때문이다.



```
* timestamp: Fri Feb 05 00:00:00 KST 2021
* status: 400
* error: Bad Request
* exception: org.springframework.validation.BindException
* trace: 예외 trace
* message: Validation failed for object='data'. Error count: 1
* errors: Errors(BindingResult)
* path: 클라이언트 요청 경로 (`/hello`)
```

```html
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="utf-8">
</head>
<body>
<div class="container" style="max-width: 600px">
    <div class="py-5 text-center">
        <h2>500 오류 화면 스프링 부트 제공</h2>
    </div>
    <div><p>오류 화면 입니다.</p>
    </div>

    <ul>
        <li>오류 정보</li>
        <ul>
            <li th:text="|timestamp: ${timestamp}|"></li>
            <li th:text="|path: ${path}|"></li>
            <li th:text="|status: ${status}|"></li>
            <li th:text="|message: ${message}|"></li>
            <li th:text="|error: ${error}|"></li>
            <li th:text="|exception: ${exception}|"></li>
            <li th:text="|errors: ${errors}|"></li>
            <li th:text="|trace: ${trace}|"></li>
        </ul>
        </li>
    </ul>

    <hr class="my-4">
</div> <!-- /container -->
</body>
</html>
```

- #### `BasicErrorController`은 여러 정보를 model에 담아서 뷰에 제공한다. 이 이유로 뷰 템플릿을 이용한다.



```properties
# BasicErrorController model input activate
server.error.include-exception=true
server.error.include-message=always
server.error.include-stacktrace=on_param
server.error.include-binding-errors=on_param
```

- #### 오류 관련 내부 정보들은 고객에게 노출하지 않는 것이 좋다. 혼란과 보안상 문제가 발생할 수 있다.

- #### 위 코드로 오류 정보를 model에 포함할지 여부를 선택할 수 있다.

- #### server.error.include-exception=false : exception 포함 여부( true , false )

- #### server.error.include-message=never : message 포함 여부

- #### server.error.include-stacktrace=never : trace 포함 여부

- #### server.error.include-binding-errors=never : errors 포함 여부

- #### Default가 never인 부분은 3가지 옵션을 사용할 수 있다.

  - #### `never`은 사용하지 않음

  - #### `always`은 항상 허용

  - #### `on_param`은 파라미터가 있을 때 사용

  - #### `on_param`은 파라미터가 있으면 해당 정보를 노출하며 디버그 시에는 사용하기 좋으나 운영 서버에는 권장하지 않는다.

  - #### `on_param`으로 설정 시 HTTP 요청 파라미터를 전달하면 해당 정보들이 model에 담겨서 뷰 템플릿에서 출력된다. 없으면 null이 담긴다.

    `http://localhost:8080/error-ex?message=&errors=&trace=`

  - #### 실무에서는 이것들을 노출하면 안되며 오류는 서버에 로그로 남겨서 확인한다.



```properties
# Spring Boot basic  exception page(Whitelabel Error Page)
server.error.whitelabel.enabled=false
# Error page path
server.error.path=/error
```

- #### 오류 처리 화면을 못 찾을 시 스프링 whitelabel 오류 페이지를 적용한다. Whitelabel 페이지는 스프링 default 페이지이다.

- #### 오류 페이지 경로를 설정할 수 있으며 스프링이 자동 등록하는 서블릿 글로벌 오류 페이지 경로와 `BasicErrorController` 오류 컨트롤러 경로에 함께 사용된다. Default 값은 /error 이다.

- #### 에러 공통 처리 컨트롤러의 기능을 변경하고 싶으면 `ErrorController` 인터페이스를 상속 받아서 구현하거나 `BasicErrorController` 상속 받아서 기능을 추가하면 된다. `BasicErrorController`을 확인하면 protected로 확장 가능하게 되어있지만 거의 사용하지 않는다.



# API Exception



```java
@Slf4j
@RestController
public class ApiExceptionController {

    @GetMapping("/api/members/{id}")
    public MemberDto getMember(@PathVariable("id") String id){
        if (id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }

        return new MemberDto(id, "hello " + id);
    }

    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String memberId;
        private String name;
    }

}
```

- #### `id`가 `ex`이면 RuntimeException 발생시키는 컨트롤러이다.

- #### 정상 요청의 경우 JSON 형식으로 응답하지만 오류 발생 시 미리 만든 HTML이 반환된다.

- #### HTML이 아닌 JSON으로 응답해야 한다. 



```java
// ErrorPageController
@RequestMapping(value = "/error-page/500", produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<Map<String, Object>> errorPage500Api(
    HttpServletRequest request, HttpServletResponse response){
    log.info("API errorPage 500");

    HashMap<String , Object> result = new HashMap<>();
    // exception 가져오기
    Exception ex = (Exception)request.getAttribute(ERROR_EXCEPTION);
    // 상태코드 가져오기
    result.put("status", request.getAttribute(ERROR_STATUS_CODE));
    // 메시지 가져오기 // ApiExceptionController getMember() 에서 메시지 넣었다
    result.put("message", ex.getMessage());

    return new ResponseEntity<>(result, HttpStatus.valueOf(statusCode));
}
```

- #### `produces = MediaType.APPLICATION_JSON_VALUE`는 클라이언트가 요청하는 HTTP 헤더의 `Accept` 값이 `application/json`일 때 해당 메소드가 호출된다는 것이다. 즉, 클라이언트가 받고 싶은 미디어타입이 json이면 이 컨트롤러가 실행되는 것이다.

- #### Jackson 라이브러리는 Map을 Json 구조로 변환할 수 있다.



## Basic Error



```java
@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {}

@RequestMapping
public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {}
```

```
// GET http://localhost:8080/api/members/ex 실행 시 로그
{
    "timestamp": "2021-04-28T00:00:00.000+00:00",
    "status": 500,
    "error": "Internal Server Error",
    "exception": "java.lang.RuntimeException",
    "trace": "java.lang.RuntimeException: 잘못된 사용자\n\tat
    hello.exception.web.api.ApiExceptionController.getMember(ApiExceptionController
    .java:19...,
    "message": "잘못된 사용자",
    "path": "/api/members/ex"
}
```

- #### API 예외 처리도 `BasicErrorController`을 활용할 수 있다.

- #### Accept가 application/json이면 기본 오류 메시지를 응답하며 text/html이면 /error의 뷰를 응답한다.

- #### `errorHtml()` 인 경우 Accept 헤더가 text/html인 경우이며 뷰를 제공한다.

- #### `error()`인 경우는 그외에 호출되며 ResponseEntity로 HTTP Body에  Json 데이터를 반환한다.

- #### 즉, Accept가 text/html이 아니면 모두 Json 응답을 한다.

- #### 기본으로 모두 /error URL로 오류 메시지를 받는다.

- #### `BasicErrorController`를 확장해서 Json 오류 메시지를 변경하는 것이 가능하다.

- #### HTML 오류 페이지와 다르게 API는 컨트롤러나 예외마다 서로 다른 응답 결과를 출력해야 한다. 이때 `BasicErrorController`이 아닌 `@ExceptionHandler`을 사용한다.



## HandlerExceptionResolver



![ExceptionResolver](/media/mwkang/Klevv/Spring 일지/MVC2/04.04/ExceptionResolver.png)

- #### 스프링 MVC는 컨트롤러 밖으로 예외가 던져진 경우 예외를 해결하고 동작을 새로 정의할 수 있는 방법을 제공한다. 컨트롤러 밖으로 던져진 예외를 해결하고 동작 방식을 변경하고 싶으면 HandlerExceptionResolver를 사용하면 되며 ExceptionResolver이라고 부른다.

- #### DispatcherServlet이 예외 해결 가능한지 물어본 후 ModelAndView 오면 정상 작동하고 아니면 default 예외 발생한다.



```java
// ApiExceptionController
@GetMapping("/api/members/{id}")
public MemberDto getMember(@PathVariable("id") String id){
    if (id.equals("ex")){
        throw new RuntimeException("잘못된 사용자");
    }
    if (id.equals("bad")){
        throw new IllegalArgumentException("잘못된 입력 값");
    }

    return new MemberDto(id, "hello " + id);
}
```

- #### IllegalArgumentException을 발생시켜서 400 오류로 처리하고자 한다.



```java
public interface HandlerExceptionResolver {
    ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);
}
```

- #### handler : 핸들러(컨트롤러) 정보

- #### Exception ex : 핸들러(컨트롤러)에서 발생한 발생한 예외



```java
@Slf4j
public class MyHandlerExceptionResolver implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        log.info("call resolver = ", ex);

        try{
            if (ex instanceof IllegalArgumentException) {
                log.info("IllegalArgumentException resolver to 400");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                return new ModelAndView();
            }
        }catch (IOException e) {
            e.printStackTrace();
            log.error("resolver ex", e);
        }
        return null;
    }
}
```

- #### 이 코드로 상태 코드와 응답을 변경할 수 있다. 이렇게 하면 Json의 경우 WAS가 변경된 상태 코드를 바탕으로 지정한 URL를 재요청하게 된다.

- #### ModelAndView를 반환하는 이유는 마치 try ~ catch를 하듯이 Exception을 처리해서 정상 흐름 처럼 변경하는 것이 목적이다.

- #### 여기서는 IllegalArgumentException 발생 시 400 상태 코드를 지정 후 빈  ModelAndView를 반환한다.

- #### 빈 ModelAndView : `new ModelAndView()`처럼 빈 ModelAndView를 반환하면 뷰를 렌더링 하지 않고 정상 흐름으로 서블릿이 리턴된다.

- #### ModelAndView 지정 : ModelAndView에 View, Model 등의 정보를 지정해서 반환하면 뷰를 렌더링 한다.

- #### null : null 반환 시 다음 ExceptionResolver을 찾아서 실행한다. 만약 처리할 수 있는 ExceptionResolver가 없으면 예외 처리가 안되고 기존에 발생한 예외를 서블릿 밖으로 던진다. 즉, 500 처리되며 예외가 터진 상태로 위로 전달하는 것이다.

- #### 예외 상태 코드 변환

  - #### 예외를 `response.sendError(xxx)` 호출로 변경하여 서블릿에서 상태 코드에 따른 오류를 처리하도록 위임한다.

  - #### 이후 WAS는 서블릿 오류 페이지를 찾아서 내부 호출한다.

- #### 뷰 템플릿 처리

  - #### ModelAndView에 값을 채워서 예외에 따른 새로운 오류 화면 뷰 렌더링 해서 고객에게 제공한다.

- #### API 응답 처리

  - #### `response.getWriter().println("hello");`처럼 HTTP 응답 바디에 직접 데이터를 넣어주는 것이 가능하다. 여기에 Json으로 응답하면 API 응답 처리를 할 수 있다.



```java
// WebConfig
// ExceptionResolver 등록
@Override
public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
    // 기본적인 ExceptionResolver
    resolvers.add(new MyHandlerExceptionResolver());
    // Json, HTML 별로 설정한 ExceptionResolver
    resolvers.add(new UserHandlerExceptionResolver());
}
```

- #### ExceptionResolver을 등록하는 것이다.

- #### `configureHandlerExceptionResolvers(..)`를 사용하면 스프링이 기본으로 등록하는 ExceptionResolver이 제거되기 때문에 `extendHandlerExceptionResolvers`을 사용하는 것이 좋다.



```java
public class UserException extends RuntimeException{
    public UserException() {
        super();
    }

    public UserException(String message) {
        super(message);
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserException(Throwable cause) {
        super(cause);
    }

    protected UserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
```

- #### 사용자 정의 예외를 추가한 것이다.

- #### RuntimeException을 정의한 예외이다.



```java
// ApiExceptionController
@GetMapping("/api/members/{id}")
public MemberDto getMember(@PathVariable("id") String id){
    if (id.equals("ex")){
        throw new RuntimeException("잘못된 사용자");
    }
    if (id.equals("bad")){
        throw new IllegalArgumentException("잘못된 입력 값");
    }
    if (id.equals("user-ex")){
        throw new UserException("사용자 오류");
    }

    return new MemberDto(id, "hello " + id);
}
```

- #### UserException이 발생하도록 추가하였다.



```java
@Slf4j
public class UserHandlerExceptionResolver implements HandlerExceptionResolver {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if(ex instanceof UserException){
                log.info("UserException resolver to 400");
                String acceptHeader = request.getHeader("accept");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                if ("application/json".equals(acceptHeader)){
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("ex", ex.getClass());
                    errorResult.put("message", ex.getMessage());

                    String result = objectMapper.writeValueAsString(errorResult);

                    response.setContentType("application/json");
                    response.setCharacterEncoding("utf-8");
                    response.getWriter().write(result);
                    return new ModelAndView();
                }else{
                    // TEXT/HTML
                    return new ModelAndView("error/500");
                }
            }
        }catch (IOException e){
            log.error("resolver ex", e);
        }
        return null;
    }
}
```

```java
// WebConfig
@Override
public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
    // 기본적인 ExceptionResolver
    resolvers.add(new MyHandlerExceptionResolver());
    // Json, HTML 별로 설정한 ExceptionResolver
    resolvers.add(new UserHandlerExceptionResolver());
}
```

```json
// http://localhost:8080/api/members/user-ex Accept: application/json 요청
{
	"ex": "hello.exception.exception.UserException",
	"message": "사용자 오류"
}
```

- #### `Accept`가 `application/json`인 경우 Json으로 응답하며 ModelAndView는 빈 값을 반환한다. `text/html`인 경우 뷰를 응답한다.

- #### `sendError()`을 호출하지 않고 상태 코드와 바디만 변경하였기 때문에 WAS는 정상 인식하여 재요청하지 않고 응답한다.

- #### response에 write()시 String으로 넣어야 하기 때문에 map을 ObjectMapper로 json에서 String으로 만들어서 사용한다.

- #### ExceptionResolver를 사용하면 컨트롤러에서 예외가 발생해도 ExceptionResolver에서 예외를 처리해버린다.

- #### 따라서 예외가 발생해도 서블릿 컨테이너까지 예외가 전달되지 않고 스프링 MVC에서 예외 처리는 끝나고 결과적으로 WAS 입장에서는 정상처리 된 것이다.

- #### 이렇듯 예외를 이곳에서 모두 처리할 수 있다.



## Spring ExceptionResolver



- #### 스프링이 제공하는 ExceptionResolver에서 `HandlerExceptionResolverComposite`에 다음 순서로 등록한다. 이 순서로 우선 순위 처리한다.

  1. #### `ExceptionHandlerExceptionResolver` : `@ExceptionHandler`을 처리하며 API 예외 처리는 대부분 이 기능으로 해결한다.

  2. #### `ResponseStatusExceptionResolver` : HTTP 상태 코드를 지정한다.

     `@ResponseStatus(value = HttpStatus.NOT_FOUND)`

  3. ####  `DefaultHandlerExceptionResolver` : 스프링 내부 기본 예외를 처리한다.



```java
//@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "잘못된 오류 요청")
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.bad")
public class BadRequestException extends RuntimeException{
}
```

```java
// ApiExceptionController
// BadRequestException용 핸들러
// @ResponseStatus
@GetMapping("/api/response-status-ex1")
public String responseStatusEx1(){
    throw new BadRequestException();
}
```

```json
// http://localhost:8080/api/response-status-ex1 요청
{
    "timestamp": "2022-04-04T18:03:12.729+00:00",
    "status": 400,
    "error": "Bad Request",
    "exception": "hello.exception.exception.BadRequestException",
    "message": "잘못된 요청 오류입니다. 메시지 사용",
    "path": "/api/response-status-ex1"
}
```

- #### `ResponseStatusExceptionResolver`은 예외에 따라 HTTP 상태 코드를 지정하는 역할을 하며 2가지 경우 처리한다.

  - #### `@ResponseStatus`가 달려있는 예외

  - #### `ResponseStatusException` 예외

- #### `BadRequestException`  예외가 컨트롤러 밖으로 넘어가면 `ResponseStatusExceptionResolver` 예외가 해당 어노테이션을 확인해서 오류 코드 400 으로 변경하고 메시지도 담는다.

- #### `ResponseStatusExceptionResolver` 코드를 확인하면 결국 `response.sendError(statusCode, resolvedReason)`을 호출하는 것을 확인할 수 있다. 즉, `sendError(400)`을 호출했기 때문에 WAS에서 다시 오류페이지를 내부 요청하는 것이다.

- #### MessageSource에서 찾아서 메시지로 사용할 수 있다.



```java
// ResponseStatusException
@GetMapping("/api/response-status-ex2")
public String responseStatusException(){
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "error.bad", new IllegalArgumentException());
}
```

```json
// http://localhost:8080/api/response-status-ex2 요청
{
    "timestamp": "2022-04-04T18:19:17.622+00:00",
    "status": 404,
    "error": "Not Found",
    "exception": "org.springframework.web.server.ResponseStatusException",
    "message": "잘못된 요청 오류입니다. 메시지 사용",
    "path": "/api/response-status-ex2"
}
```

- #### `@ResponseStatus`는 개발자가 직접 변경할수 없는 예외에는 적용할 수 없다. 어노테이션을 직접 넣어야 하기 때문이다. 추가로 어노테이션을 사용하기 때문에 조건에 따라 동적으로 변경하는 것도 어렵다. 이때 `ResponseStatusException` 예외를 사용한다.

- #### 현재 WebServerCustomizer의 영향으로 404.html 뷰가 출력된다. 즉, WebServerCustomizer에 설정한 것이 우선순위를 갖는다.

- #### MyHandlerExceptionResolver이 있는데 Json 응답이 도착한 것을 볼 때`ResponseStatusException`이 우선 순위를 가지고 ExceptionHandler을 무시하는 것 같다. `ResponseStatusException`이 아닌 `throw new IllegalArgumentException()`하면 MyHandlerExceptionResolver이 실행된다.



```java
// ApiExceptionController
@GetMapping("/api/default-handler-ex")
public String defaultException(@RequestParam Integer data){
    return "ok";
}
```

```json
// http://localhost:8080/api/default-handler-ex?data=hello&message=
{
    "status": 400,
    "error": "Bad Request",
    "exception":
    "org.springframework.web.method.annotation.MethodArgumentTypeMismatchException"
    ,
    "message": "Failed to convert value of type 'java.lang.String' to required
    type 'java.lang.Integer'; nested exception is java.lang.NumberFormatException:
    For input string: \"hello\"",
    "path": "/api/default-handler-ex"
}
```

- #### `DefaultHandlerExceptionResolver`는 스프링 내부에서 발생하는 스프링 예외를 해결하며 대표적으로 파라미터 바인딩 시점에 타입이 맞지 않으면 내부에서 TypeMismatchException이 발생한다. 이때 500 오류가 발생하는데 클라이언트 오류 이므로 400 오류를 발생해야 한다.

- #### `DefaultHandlerExceptionResolver.handleTypeismatch`를 보면 `response.sendError(HttpServletResponse.SC_BAD_REQUEST)`(400) 이 있으며 결국 `response.sendError()`를 통해 문제를 해결한다. 즉, WAS는 다시 오류 페이지(/error) URL을 요청한다.

- #### 위 요청은 Integer 타입 오류로 예외가 발생한다.



## @ExceptionHandler



- #### HandlerExceptionResolver의 불편한 사항을 해결하기 위해 탄생했다.

- #### HandlerExceptionResolver은 ModelAndView를 반환해야 하며 이것은 API 응답에는 필요하지 않다.

- #### HandlerExceptionResolver은 API 응답을 위해 HttpServletResponse에 직접 응답 데이터를 넣어주었으며 이것은 매우 불편하다.

- #### HandlerExceptionResolver은 특정 컨트롤러에서만 발생하는 예외를 별도로 처리하기 어렵다. 각 컨트롤러마다 예외 상황에 따라 다르게 처리해야 할 수 있다.

- #### @ExceptionHandler 어노테이션이 API 처리 문제를 모두 해결하며 이것이 ExceptionHandlerExceptionResolver이다. ExceptionResolver 중 우선 순위도 가장 높다.

- #### 실무에서 대부분 API처리는 이 기능을 사용한다.



```java
// @ExceptionHandler에서 반환할 json 객체
@Data
@AllArgsConstructor
public class ErrorResult {
    private String code;
    private String message;
}
```

```java
@Slf4j
@RestController
public class ApiExceptionV2Controller {

    // 여기서도 사용 가능하지만 컨트롤러와 예외 처리를 분리하기 위해 ExControllerAdvice 클래스로 보낸다
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult illegalExHandler(IllegalArgumentException e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("BAD", e.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResult> userExHandler(UserException e){
        log.error("[exceptionHandler] ex", e);
        ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
        return new ResponseEntity(errorResult, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public ErrorResult exHandler(Exception e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("EX", "내부 오류");
    }

    @GetMapping("/api2/members/{id}")
    public MemberDto getMember(@PathVariable("id") String id){
        if (id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }
        if (id.equals("bad")){
            throw new IllegalArgumentException("잘못된 입력 값");
        }
        if (id.equals("user-ex")){
            throw new UserException("사용자 오류");
        }

        return new MemberDto(id, "hello " + id);
    }

    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String memberId;
        private String name;
    }
}
```

```json
// http://localhost:8080/api2/members/bad 요청
{
    "code": "BAD",
    "message": "잘못된 입력 값"
}
```

- #### `illegalExHandle` 메소드는 이 컨트롤러 클래스에서 IllegalArgumentException 발생 시 실행하라는 것이다.

- #### @ExceptionHandler은 정상으로 인식되어 servlet container까지 다시 예외를 올리지 않고 클라이언트에게 정상 응답한다.

- #### 파라미터에 예외를 작성하면 @ExceptionHandler에는 예외를 생략해도 된다.

- #### 지정한 예외 또는 그 예외의 자식 클래스는 모두 포함할 수 있다.

- #### 우선 순위는 자세한 것이 우선권을 가진다. 즉, 자식 예외 처리가 부모 예외 처리보다 우선권을 가진다.

- #### 응답 시 `@ResponseStatus`는 어노테이션으로 HTTP 응답 코드를 동적으로 변경할 수 없다. 이때 ResponseEntity를 사용하여 HTTP 응답 코드를 동적으로 변경할 수 있다.

- #### 실행 흐름

  - #### 컨트롤러를 호출한 결과 `IllegalArgumentException` 예외가 컨트롤러 밖으로 던져진다.

  - #### 예외가 발생했으므로 `ExceptionResolver` 가 작동한다. 가장 우선순위가 높은 `ExceptionHandlerExceptionResolver` 가 실행된다.

  - #### `ExceptionHandlerExceptionResolver` 는 해당 컨트롤러에 `IllegalArgumentException` 을 처리할 수 있는 `@ExceptionHandler` 가 있는지 확인한다.

  - #### `illegalExHandle()` 를 실행한다. @RestController 이므로 `illegalExHandle()` 에도 `@ResponseBody`가 적용된다. 따라서 HTTP 컨버터가 사용되고, 응답이 다음과 같은 JSON으로 반환된다.

  - #### `@ResponseStatus(HttpStatus.BAD_REQUEST)` 를 지정했으므로 HTTP 상태 코드 400으로 응답한다.



```java
@ExceptionHandler({AException.class, BException.class})
public String ex(Exception e) {
	log.info("exception e", e);
}
```

- #### 다양한 예외를 한번에 처리할 수 있다.

- #### 예외 매개변수에 HttpRequest, response 등 컨트롤러와 비슷한 것은 모두 올 수 있다.

- #### @ExceptionHandler에는 마치 스프링의 컨트롤러의 파라미터 응답처럼 다양한 파라미터와 응답을 지정할 수 있다.

[@ExceptionHandler](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-
exceptionhandler-args)



```java
@ExceptionHandler(ViewException.class)
public ModelAndView ex(ViewException e) {
	log.info("exception e", e);
	return new ModelAndView("error");
}
```

- #### ModelAndView를 사용해서 오류 화면 HTML을 응답할 수 있다.

- #### ModelAndView 대신 String을 사용할 수도 있다.

- #### @Controller 상황이어야 한다.

- #### 이 방법은 거의 사용하지 않는다. HTML 오류 페이지 응답 시 `BasicErrorController`, `HanderExceptionResolver`을 사용한다.



## @ContollerAdvice



```java
// RestControllerAdvice로 annotation, package, class를 지정할 수 있다.
@Slf4j
@RestControllerAdvice(basePackages = "hello.exception.api")
public class ExControllerAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult illegalExHandler(IllegalArgumentException e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("BAD", e.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResult> userExHandler(UserException e){
        log.error("[exceptionHandler] ex", e);
        ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
        return new ResponseEntity(errorResult, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public ErrorResult exHandler(Exception e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("EX", "내부 오류");
    }
}
```

- #### 정상 코드와 예외처리 코드를 분리하는 @ControllerAdvice, @RestControllerAdvice를 사용한 것이다.

- #### 여러 컨트롤러에서 발생한 예외를 공통 처리할 수 있다. AOP를 컨트롤러에 적용하는 느낌이다.

- #### @ControllerAdvice 는 대상으로 지정한 여러 컨트롤러에 @ExceptionHandler , @InitBinder 기능을 부여해주는 역할을 한다.

- #### @ControllerAdvice 에 대상을 지정하지 않으면 모든 컨트롤러에 적용된다. (글로벌 적용)



```java
// Target all Controllers annotated with @RestController
@ControllerAdvice(annotations = RestController.class)
public class ExampleAdvice1 {}

// Target all Controllers within specific packages
@ControllerAdvice("org.example.controllers")
public class ExampleAdvice2 {}

// Target all Controllers assignable to specific classes
@ControllerAdvice(assignableTypes = {ControllerInterface.class, AbstractController.class})
public class ExampleAdvice3 {}
```

- #### 대상 컨트롤러를 지정하는 방법에는 여러가지가 있으며 패키지 지정 시 "basePackages = "로 지정할 수 있다. 패키지 지정시 해당 패키지와 하위에 있는 컨트롤러가 대상이 된다.

- #### 어노테이션, 패키지, 특정 클래스로 지정할 수 있다.

- #### 대상 컨트롤러 지정을 생략하면 모든 컨트롤러에 적용된다.

- #### AOP와 같은 원리이다. Advice 용어도 AOP에서 나온 것이다.

[@ControllerAdvice](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-
controller-advice)







# Keyboard Shortcut

- #### ctrl + ctrl : 커서를 여러개로 만들어서 빠른 작업을 할 수 있다.