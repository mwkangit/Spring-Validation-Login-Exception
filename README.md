# Spring-Validation-Login-Exception



## Description

본 프로젝트는 3가지 프로젝트로 구성 된다. 1번째 프로젝트는 `validation`이며 클라이언트 입력에 대한 validation을 구현하여 상세하게 알아본다. 2번째 프로젝트는 `login`이며 Cookie, Session을 이용하여 로그인에 대한 권한을 부여, 유지를 구현하여 알아본다. 3번째 프로젝트는 `exception`이며 오류 페이지, API 오류 메시지에 대해 구현하여 상세하게 알아본다.



-----



## Environment

![framework](https://img.shields.io/badge/Framework-SpringBoot-green)![framework](https://img.shields.io/badge/Language-java-b07219) 

Framework: `Spring Boot` 2.4.4

Project: `Gradle`

Packaging: `Jar`

IDE: `Intellij`

Template Engine: `Thymeleaf`

Dependencies: `Spring Web`, `Lombok`, `Validation`



-----



## Installation



![Linux](https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black) 

```
./gradlew build
cd build/lib
java -jar hello-spring-0.0.1-SNAPSHOT.jar
```



![Windows](https://img.shields.io/badge/Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white) 

```
gradlew build
cd build/lib
java -jar hello-spring-0.0.1-SNAPSHOT.jar
```



------



## Core Feature



**Project : validation**

```java
// ValidationItemControllerV4
@PostMapping("/add")
public String addItem(@Validated @ModelAttribute("item") ItemSaveForm form, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {}
```

```java
@Data
public class ItemSaveForm {
    @NotBlank
    private String itemName;

    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;

    @NotNull
    @Max(value = 9999)
    private Integer quantity;
}
```

`Bean Validation` 을 이용하여 validation을 수행하는 코드이다. 사용하기 전 `Item` 객체가 아닌 `ItemSaveForm` 객체에 저장하여 각 로직마다 사용가능한 폼에 저장 후 이용한다.



**Project : login**

```java
// LoginController
@PostMapping("/login")
public String loginV3(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult, HttpServletRequest request){
    if(bindingResult.hasErrors()){
        return "login/loginForm";
    }

    Member loginMember = loginService.login(form.getLoginId(), form.getPassword());

    if(loginMember == null){
        bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
        return "login/loginForm";
    }

    // 로그인 성공 처리
    // 세션이 있으면 있는 세션 반환, 없으면 신규 세션을 생성
    HttpSession session = request.getSession(true);
    // 세션에 로그인 회원 정보를 보관
    session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);

    return "redirect:/";
}
```

```java
// HomeController
@PostMapping("/login")
public String loginV4(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult,
                      @RequestParam(defaultValue = "/") String redirectURL,
                      HttpServletRequest request){
    if(bindingResult.hasErrors()){
        return "login/loginForm";
    }

    Member loginMember = loginService.login(form.getLoginId(), form.getPassword());

    if(loginMember == null){
        bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
        return "login/loginForm";
    }

    // 로그인 성공 처리
    // 세션이 있으면 있는 세션 반환, 없으면 신규 세션을 생성
    HttpSession session = request.getSession(true);
    // 세션에 로그인 회원 정보를 보관
    session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);

    return "redirect:" + redirectURL;
}
```

`Cookie`, `Session` 을 통해 로그인 기능을 구현한 것이다. `HttpSession` 을 통해 `Session` 을 생성 및 관리하며 세션 유무에 따라 화면을 출력한다. 로그인 시 쿼리 파라미터로의 redirect 경로를 요청하게 된다.



```java
// LoginCheckInterceptor
@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();

        log.info("인증 체크 인터셉터 실행 {}", requestURI);

        HttpSession session = request.getSession();

        if(session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null){
            log.info("미인증 사용자 요청");
            // 로그인으로 redirect
            response.sendRedirect("/login?redirectURL=" + requestURI);
            return false;
        }

        return true;
    }
}
```

로그인 유무를 판별하는 `Interceptor` 을 구현한 것이다. `Session` 값을 확인하여 로그인 유무를 검사한 후 미인증 사용자의 경우 현재 페이지를 저장 후 로그인 페이지를 출력한다.



**Project : Exception** 



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

`RestControllerAdvice` 와 `@ExceptionHandler` 을 이용하여 예외 처리를 구현한 것이다. 클래스를 패키지, 클래스, 어노테이션 범위로 적용하여 컨트롤러마다 다양한 예외 API를 설정할 수 있다.



-----



## Demonstration Video



**Project : validation**

![Spring-Validation-Login-Exception1](/home/mwkang/Downloads/Spring-Validation-Login-Exception1.gif)



**Project : login**

![Spring-Validation-Login-Exception2](/home/mwkang/Downloads/Spring-Validation-Login-Exception2.gif)



**Project : exception**

![Spring-Validation-Login-Exception3](/home/mwkang/Downloads/Spring-Validation-Login-Exception3.gif)



------



## More Explanation



