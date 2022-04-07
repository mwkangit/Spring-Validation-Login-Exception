# Practice Setting



**Project : login**

```groovy
plugins {
	id 'org.springframework.boot' version '2.4.4'
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
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
	useJUnitPlatform()
}
```

- #### Cookie, Session을 다루게 된다.



# Cookie & Session



- #### 도메인은 화면, UI, 기술 인프라 등등의 영역을 제외한 시스템이 구현해야 하는 핵심 비즈니스 업무 영역이다.

- #### 향 후 web을 다른 기술로 바꾸어도 도메인은 그대로 유지할 수 있어야 한다.

- #### Web은 domain을 알고 있지만 domain은 web을 모르도록 설계해야 하며 이것을 web은 domain을 의존하지만 domain은 web을 의존하지 않는다고 표현한다. 즉, domain은 web을 참조하면 안된다.



```java
// MemberRepository
public Optional<Member> findByLoginId(String loginId){
    // 일반 적인 코드
    //        List<Member> all = findAll();
    //        for (Member m : all) {
    //            if(m.getLoginId().equals(loginId)){
    //                return Optional.of(m);
    //            }
    //        }
    //        return null; // 아이디에 해당하는 멤버가 없는 경우 // 이거 때문에 optional로 감싸서 null 표현한다.
    //        return Optional.empty(); // 위 코드와 같은 로직이다.

    // 람다 코드
    return findAll().stream()
        .filter(m -> m.getLoginId().equals(loginId))
        .findFirst();
}

public List<Member> findAll(){
    return new ArrayList<>(store.values());
}
```

- #### MemberRepository의 코드로 findAll()로 리스트를 가져온 후 stream()으로 리스트를 스트림으로 바꾼다. 이때 loop를 도는 걸로 볼 수 있다.

- #### Filter()은 내부 조건을 만족하는 것만 다음 단계로 넘어가게 하는 것이다. 즉, 여기서는 리스트의 MemberId와 LoginId 같은 것이 있으면 다음 메소드로 넘어가는 것이다. 한번도 다음으로 넘어가지 않을 경우 null을 반환한다. Optional 객체는 stream()없이 filter()를 바로 적용하는 것이 가능하다.

- #### Null이 반환되는 경우를 위해서 Optional로 감쌌다.



```java
@Controller
@RequiredArgsConstructor
public class LoginController {
	private final LoginService loginService;
	@GetMapping("/login")
	public String loginForm(@ModelAttribute("loginForm") LoginForm form) {
		return "login/loginForm";
	}
	@PostMapping("/login")
	public String login(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return "login/loginForm";
		}
		
       	Member loginMember = loginService.login(form.getLoginId(), form.getPassword());log.info("login? {}", loginMember);
		if (loginMember == null) {
            bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
            return "login/loginForm";
        }
        
        //로그인 성공 처리 TODO
        return "redirect:/";
    }
}
```

- #### `if(loginMember == null)`부분은 보통 데이터베이스를 접근하는 로직이므로 @Script로 해결하기 어렵다. 즉, 직접 로직을 만드는 것이 좋다.

- #### 여기서는 로그인시 아이디 오류인지 패스워드 오류인지 알려주지 않고 오류 발생 시 글로벌 오류 1개로 처리한다.



## Cookie



![Client Cookie](/media/mwkang/Klevv/Spring 일지/MVC2/03.13/Client Cookie.png)

- #### 쿠키에는 영속 쿠키와 세션 쿠키가 있다.

  - #### 영속 쿠키는 만료 날짜를 입력하면 해당 날짜까지 유지하는 것이다. 즉, 컴퓨터를 껐다 켜도 남아있다.

  - #### 세션 쿠키는 만료 날짜를 생략하면 브라우저 종료 시 까지만 유지한다.



```java
// LoginController
@PostMapping("/login")
public String login(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult, HttpServletResponse response){
    if(bindingResult.hasErrors()){
        return "login/loginForm";
    }

    Member loginMember = loginService.login(form.getLoginId(), form.getPassword());

    if(loginMember == null){
        bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
        return "login/loginForm";
    }

    // 로그인 성공 처리

    // 쿠키에 시간 정보를 주지 않으면 세션 쿠키(브라우저 종료시 모두 종료)
    Cookie idCookie = new Cookie("memberId", String.valueOf(loginMember.getId()));
    response.addCookie(idCookie);

    return "redirect:/";
}
```

- #### 쿠키를 생성 후 사용자에게 응답했다. 쿠키 이름은 "memberId"이며 값은 id이다.



```java
// HomeController
@GetMapping("/")
public String homeLogin(@CookieValue(name = "memberId", required = false) Long memberId, Model model){

    if(memberId == null){
        return "home";
    }

    // 로그인
    Member loginMember = memberRepository.findById(memberId);
    if(loginMember == null){
        return "home";
    }

    model.addAttribute("member", loginMember);
    return "loginHome";
}
```

- #### 로그인 하지 않은 사용자도 홈에 접근할 수 있기 때문에 required = false를 사용한다.

- #### 쿠키가 오래전에 만들어져서 데이터베이스에 멤버가 없을 수 있다. 이때 다시 로그인 하도록 한다.



```java
@PostMapping("/logout")
public String logout(HttpServletResponse response){
    expireCookie(response, "memberId");
    return "redirect:/";
}

private void expireCookie(HttpServletResponse response, String cookieName) {
    Cookie cookie = new Cookie(cookieName, null);
    cookie.setMaxAge(0);
    response.addCookie(cookie);
}
```

- #### 세션 쿠키이므로 웹 브라우저 종료 시 로그아웃한다.

- #### 로그아웃하기 위해 서버에서 해당 쿠키의 종료 날짜를 0으로 지정한다.

- #### 로그아웃 시 응답 쿠키가 Max-age=0 이되어 해당 쿠키는 즉시 종료된다.



## Cookie Security Problem

- #### 문제

  - #### 쿠키 값은 임의로 변경할 수 있다.

    - #### 클라이언트가 쿠키를 강제로 변경하면 다른 사용자가 된다.

    - #### 실제 웹 브라우저 개발자모드 -> Application -> Cookie 변경으로 쿠키를 변경할 수 있다.

    - #### 쿠키에 보관된 정보는 훔쳐갈 수 있다.

      - #### 쿠키에 개인정보, 신용카드 정보와 같은 중요한 정보가 있을 때 이 정보는 웹 브라우저에도 보관되고 네트워크 요청마다 계속 클라이언트에서 서버로 전달된다.

      - #### 쿠키의 정보가 로컬 PC가 털릴 수 있고 네트워크 전송 구간에서 털릴 수도 있다.

  - #### 해커가 쿠키를 한번 훔치면 평생 사용할 수 있다.

    - #### 해커가 쿠키를 훔쳐가서 그 쿠키로 악의적인 요청을 계속 시도할 수 있다.

- #### 대안

  - #### 쿠키에 중요한 값을 노출하지 않고 상ㅇ자 별로 예측 불가능한 임의의 토큰(랜덤 값)을 노출하고 서버에서 토큰과 사용자 id를 매핑해서 인식한다. 그리고 서버에서 토큰을 관리해야 한다.

  - #### 토큰은 해커가 임의의 값을 넣어도 찾을 수 없도록 예상 불가능 해야 한다.

  - #### 해커가 토큰을 털어가도 시간이 지나면 사용할 수 없도록 서버에서 해당 토큰의 만료시간을 짧게(30분) 유지한다. 또는 해킹이 의심되는 경우 서버에서 해당 토큰을 강제로 제거한다.

  - #### 이러한 방법은 서버, 세션으로 해결한다.



## Session



![Client Session](/media/mwkang/Klevv/Spring 일지/MVC2/03.13/Client Session.png)

- #### 세션은 서버에 중요한 정보를 보관하고 연결을 유지하는 방법이다.

- #### 사용자가 loginId, password 정보를 전달하면 서버에서 해당 사용자가 맞는지 검사한다.

- #### 세션 ID는 추정 불가능해야하므로 UUID를 사용한다.

  `Cookie: mySessionId=zz0101xx-bab9-4b92-9b32-dadb280f4b61`

- #### 생선된 세션 ID와 세션에 보관할 값을 서버의 세션 저장소에 보관한다.

- #### 클라이언트와 서버는 결국 쿠키로 연결되어야 한다.

  - #### 서버는 클라이언트에 mySessionId라는 이름으로 세션ID 만 쿠키에 담아서 전달한다.

  - #### 클라이언트는 쿠키 저장소에 mySessionId 쿠키를 보관한다.

- #### 여기서 중요한 포인트는 회원과 관련된 정보는 전혀 클라이언트에 전달하지 않았다는 것으로 오직 추정 불가능한 세션ID만 쿠키를 통해 클라이언트에 전달했다.

- #### 클라이언트는 요청 시 항상 mySessionId 쿠키를 전송한다.

- #### 서버에서는 클라이언트가 전달한 mySessionId 쿠키 정보로 세션 저장소를 조회해서 로그인 시 보관한 세션 정보(value)를 사용한다.

- #### 이것으로 Cookie에 발생한 문제를 모두 해결하였다.



### SessionManager



```java
/**
 * 세션 관리
 */
@Component
public class SessionManager {

    public static final String SESSION_COOKIE_NAME = "mySessionId";
    private Map<String, Object> sessionStore = new ConcurrentHashMap<>();

    /**
     * 세션 생성
     * sessionID 생성 (임의의 추정 불가능한 랜덤 값)
     * 세션 저장소에 sessionId와 보관할 값 저장
     * sessionId로 응답 쿠키를 생성해서 클라이언트에 전달
     */
    public void createSession(Object value, HttpServletResponse response ){

        // 세션 id를 생성하고, 값을 세션에 저장
        String sessionId = UUID.randomUUID().toString();
        sessionStore.put(sessionId, value);

        // 쿠키 생성
        Cookie mySessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        response.addCookie(mySessionCookie);
    }

    /**
     * 세션 조회
     */
    public Object getSession(HttpServletRequest request){
        // findCookie 메소드로 떼어냈다
//        Cookie[] cookies = request.getCookies();
//        // 쿠키가 존재하지 않을 시
//        if(cookies == null){
//            return null;
//        }
//        // 쿠키가 있을 시
//        for (Cookie cookie : cookies) {
//            if(cookie.getName().equals(SESSION_COOKIE_NAME)){
//                return sessionStore.get(cookie.getValue());
//            }
//        }
//        // 못 찾을 시
//        return null;

        Cookie sessionCookie = findCookie(request, SESSION_COOKIE_NAME);
        // 세션 저장소에 등록된 value가 없으면
        if(sessionCookie == null){
            return null;
        }

        // 세션 저장소에 등록된 value가 있으면
        return sessionStore.get(sessionCookie.getValue());

    }

    /**
     * 세션 만료
     */
    public void expire(HttpServletRequest request){
        Cookie sessionCookie = findCookie(request, SESSION_COOKIE_NAME);
        // 세션 저장소에 등록된 value가 있으면
        if(sessionCookie != null){
            sessionStore.remove(sessionCookie.getValue());
        }
    }

    public Cookie findCookie(HttpServletRequest request, String cookieName){
        // 쿠키가 존재하지 않을 시
        if(request.getCookies() == null){
            return null;
        }

        // 쿠키 uuid에 대응하는 value가 있는지 검사
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(cookieName))
                .findAny()
                .orElse(null);
    }


}
```

- #### ConcurrentHashMap으로 동시성 문제를 해결한다.

- #### Cookie는 배열 형식으로 반환되어 stream() 적용한다.

- #### 쿠키들 중 세션과 관련된(mySessionId) 쿠키 발견 시 그 쿠키를 findCookie() 메소드에서 반환한다.

- #### getSession() 메소드에서 쿠키를 통해 멤버 객체를 반환한다.

- #### expire() 메소드를 통해 세션Id를 제거하여 다음에 세션Id로 요청이 와도 로그아웃 된 것으로 인식하게 한다.



```java
public class SessionManagerTest {

    SessionManager sessionManager = new SessionManager();

    @Test
    void sessionTest(){

        // 세션
        // sessionId 생성 완료
        // 웹 브라우저로 응답한 것이라고 가정
        MockHttpServletResponse response = new MockHttpServletResponse();
        Member member = new Member();
        sessionManager.createSession(member, response);

        // 요청에 응답 쿠키 저장
        // response에 저장한 값을 request에 넣는다
        // 웹 브라우저에서 쿠키를 담아서 요청
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(response.getCookies());

        // 세션 조회
        Object result = sessionManager.getSession(request);
        assertThat(result).isEqualTo(member);

        // 세션 만료
        sessionManager.expire(request);
        Object expired = sessionManager.getSession(request);
        assertThat(expired).isNull();

    }
}
```

- #### 직접 생성한 SessionManger을 테스트하는 것이다.

- #### 테스트에서는 HttpServletRequest, HttpServletResponse 객체를 직접 사용할 수 없기 때문에 테스트에서 비슷한 역할을 해주는 가짜 MockHttpServletRequest, MockHttpServletResponse를 사용한다.



```java
// LoginController
@PostMapping("/login")
public String loginV2(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult, HttpServletResponse response){
    if(bindingResult.hasErrors()){
        return "login/loginForm";
    }

    Member loginMember = loginService.login(form.getLoginId(), form.getPassword());

    if(loginMember == null){
        bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
        return "login/loginForm";
    }

    // 로그인 성공 처리

    // 세션 관리자를 통해 세션을 생성하고, 회원 데이터 보관
    sessionManager.createSession(loginMember, response);

    return "redirect:/";
}

@PostMapping("/logout")
public String logoutV2(HttpServletRequest request){
    sessionManager.expire(request);
    return "redirect:/";
}
```

- #### 로그인 성공 시 세션을 등록하며 세션에 멤버를 저장하고 쿠키도 발행한다.

- #### 로그아웃 시 간단하게 해당 세션의 정보를 제거하여 요청시 로그아웃 된 것으로 간주하게 한다.



```java
// HomeController
@GetMapping("/")
public String homeLoginV2(HttpServletRequest request, Model model){

    // 세션 관리자에 저장된 회원 정보 조회
    Member member = (Member) sessionManager.getSession(request);

    // 로그인
    if(member == null){
        return "home";
    }

    model.addAttribute("member", member);
    return "loginHome";
}
```

- #### 세션 관리자에서 저장된 회원 정보를 조회하며 만약 회원 정보가 없으면 쿠키나 세션이 없는 것 이므로 로그인 되지 않은 것으로 처리한다.

- #### 세션은 쿠키를 사용하는데 서버에서 데이터를 유지하는 방법일 뿐이다.

- #### 서블릿도 세션 개념을 지원한다. 서블릿이 제공하는 세션은 직접 구현한 세션과 비슷하며 일정시간 사용하지 않으면 해당 세션을 삭제하는 기능을 제공한다.

- #### 현재 로그인 하면 세션Id 가지고 "/"로 가며 세션Id 있으면 "loginHome"으로 간다.

- #### 세션Id 임의로 변경 후 새로고침 시 "/" 로 간다.

- #### 로그아웃 시 세션Id 없어서 "/" 로 간다.

- #### 현재 세션Id를 바꿔도 ItemList 페이지로 넘어갈 수 있다. 이것은 그 페이지에 세션에 대한 검증을 적용하지 않았기 때문이다.



### Servlet HttpSession



- #### HttpSession을 생성하면 쿠키 이름이 JSESSIONID이고 값은 추정 불가능한 랜덤 값이 저장된 쿠키가 생성된다.

  `Cookie: JSESSIONID=5B78E23B513F50164D6FDD8C97B0AD05`



```java
public class SessionConst {
    public static final String LOGIN_MEMBER = "loginMember";
}
```

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



    // 세션 관리자를 통해 세션을 생성하고, 회원 데이터 보관
    //        sessionManager.createSession(loginMember, response);

    return "redirect:/";
}

@PostMapping("/logout")
public String logoutV3(HttpServletRequest request){
    HttpSession session = request.getSession(false);

    if(session != null){
        session.invalidate();
    }

    return "redirect:/";
}
```

- #### HttpSession에 데이터를 보관하고 조회할 때 같은 이름이 중복되어 사용되므로 상수를 하나 정의한다.

- #### 세션을 생성하려면 request.getSession(true)를 사용하며 매개변수는 default가 true이다.

  `public HttpSession getSession(boolean create);`

- #### request.getSession(true)

  - #### 세션이 있으면 기존 세션을 반환한다.

  - #### 세션이 없으면 새로운 세션을 생성해서 반환한다.

- #### request.getSession(false)

  - #### 세션이 있으면 기존 세션을 반환한다.

  - #### 세션이 없으면 새로운 세션을 생성하지 않고 null을 반환한다.

- #### session.setAttribute()

  - #### 세션에 데이터를 보관하는 방법은  request.setAttribute()와 비슷하다.

  - #### 하나의 세션에 여러 값을 저장할 수 있다.

- #### 로그아웃 시 session.invalidate()로 세션을 제거할 수 있다.



```java
@GetMapping("/")
public String homeLoginV3(HttpServletRequest request, Model model){

    HttpSession session = request.getSession(false);

    // 요청에 세션이 없다면 (지금 막 로그인 화면에 왔다면)
    if(session == null){
        return "home";
    }

    // 세션이 있는 상태라면
    Member loginMember = (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);

    // 세션에 회원 데이터가 없으면 home
    if(loginMember == null){
        return "home";
    }

    // 세션이 유지되면 로그인으로 이동
    model.addAttribute("member", loginMember);
    return "loginHome";
}
```

- #### 세션을 찾아서 사용하는 시점에는 false 매개변수를 사용하여 세션을 생성하지 않아야 한다.

- #### 세션은 메모리를 사용하므로 반드시 필요한 시점에만 사용한다.

- #### session.getAttribute()는 로그인 시점에 세션에 보관한 회원 객체를 찾는다.

- #### 보통 실무에서 V3 버전을 사용한다.

- #### 현재 세션값을 변경해도 로그인할 때 세션을 부여받는다. 즉, 이전의 세션값은 그대로 메모리에 남아있으며 삭제되지 않았다.



```java
@GetMapping("/")
public String homeLoginV3Spring(
    @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) Member loginMember
    , Model model){


    // 세션에 회원 데이터가 없으면 home
    if(loginMember == null){
        return "home";
    }

    // 세션이 유지되면 로그인으로 이동
    model.addAttribute("member", loginMember);
    return "loginHome";
}
```

- #### @SessionAttribute는 이미 로그인 된 사용자를 찾을 때 사용하며 이 기능은 세션을 생성하지 않는다. V3에서 getSession() 일일이 작성하는 불편함을 해소한다.

- #### 저장된 세션이 있으면 멤버 객체를 반환한다.



#### TrackingModes



- #### TrackingModes로 로그인을 처음 시도하면 URL이 jsessionid를 포함하는 것을 확인할 수 있다.

  `http://localhost:8080/;jsessionid=F59911518B921DF62D09F0DF8F83F872`

  - #### 이것은 웹 브라우저가 쿠키를 지원하지 않을 때 쿠키 대신 URL을 통해서 세션을 유지하는 방법이다.

  - #### 이방법을 이용하려면 인위적으로 세션을 URL에 붙여줘야 한다. 첫 로그인 시 URL 이후로는 자동으로 URL에 세션을 붙여주는 기능이 사라지기 때문이다.

  - #### 타임리프 같은 템플릿은 엔진을 통해서 링크를 걸면 jsessionid를 URL에 자동으로 포함해준다.

  ```properties
  #url cookie exposure delete
  server.servlet.session.tracking-modes=cookie
  ```

  - #### URL 전달 방식을 끄려면 위 옵션을 추가한다.



#### Session Information



```java
@RestController
public class SessionInfoController {

    @GetMapping("/session-info")
    public String sessionInfo(HttpServletRequest request){
        HttpSession session = request.getSession(false);
        if(session == null){
            return "세션이 없습니다.";
        }

        // 세션 데이터 출력
        session.getAttributeNames().asIterator()
                .forEachRemaining(name -> log.info("session name={}, value={}", name, session.getAttribute(name)));

        log.info("sessionId={}", session.getId());
        log.info("getMaxInactiveInterval={}", session.getMaxInactiveInterval());
        log.info("getCreationTime={}", new Date(session.getCreationTime()));
        log.info("lastAccessedTime={}", new Date(session.getLastAccessedTime()));
        log.info("isNew={}", session.isNew());

        return "세션 출력";
    }

}
```

- #### `sessionId`는 세션Id, JSESSIONID의 값이다. 즉, 추정 불가능한 값이다.

- #### `maxInactiveInterval`은 세션의 유효 시간이다. 초 단위이다.

- #### `creationTime`은 세션 생성일시이다.

- #### `lastAccessedTime`은 세션과 연결된 사용자가 최근에 서버에 접근한 시간, 클리이언트에서 서버로 sessionId(JSESSIONID)를 요청한 경우에 갱신된다.

- #### `isNew`는 새로 생성된 세션인지 아니면 이미 과거에 만들어졌고 클라이언트에서 서버로 sessionId(JSESSIONID)를 요청해서 조회된 세션인지 여부이다.



#### Timeout



- #### 사용자가 웹 브라우저를 종료할 경우 HTTP는 비연결성(connectionless)이므로 서버 입장에서는 해당 사용자가 웹 브라우저를 종료한 것인지 아닌지를 인식할 수 없다.

- #### 이 경우 세션이 삭제되지 않고 무한정 보관하면 다음과 같은 문제가 발생할 수 있다.

  - #### 세션과 관련된 쿠키(JSESSSIONID)를 탈취 당했을 경우 오랜 시간이 지나도 해당 쿠키로 악의적인 요청을 할 수 있다.

  - #### 세션은 기본적으로 메모리에 생성된다. 메모리의 크기가 무한하지 않기 때문에 꼭 필요한 경우만 생성해서 사용해야 한다.

- #### 세션 종료 시점

  - #### 30분으로 설정하면 사용 도중 세션이 사라지는 경우가 발생한다.

  - #### 사용자가 서버에 최근 요청한 시간을 기준으로 30분 정도를 유지하는 것이 좋다. HttpSession이 이 방식을 지원한다.

- #### 세션 타임아웃 설정

  - #### 글로벌 설정

  ```properties
  # timeout
  server.servlet.session.timeout=1800
  ```

  - #### 글로벌 설정은 분 단위로 설정해야하는 규칙이 있다. 즉, 60(1분), 120(2분)... 로 설정한다.

  - #### 특정 세션 단위로 시간 설정

  ```java
  session.setMaxInactiveInterval(1800); // 1800초
  ```

- #### 세션의 타임아웃 시간은 해당 세션과 관련된 JSESSIONID를 전달하는 HTTP 요청이 있으면 현재 시간으로 초기화된다. 이렇게 초기화하면 세션 타임아웃으로 설정한 시간동안 세션을 추가로 이용할 수 있다.

- #### session.getLastAccessTime()으로 최근 세션 접근 시간을 알 수 있으며 이시간 이후로 타임아웃 시간이 지나면 WAS 내부에서 해당 세션을 자동으로 제거한다.

- #### 세션에는 최소한의 데이터만 보관해야 한다. 즉, 보관한 데이터 용량 * 사용자 수로 메모리를 사용하기 때문에 멤버 객체 대신 id와 필수 사용할 정보만 있는 객체를 따로 생성하여 세션에 보관해야 한다. 이러한 점 때문에 세션 시간을 너무 길게 가져가는 것도 좋지 않다. Default가 30분이라는 것을 기준으로 고민해야 한다.



# Filter & Interceptor



- #### 로그인 여부 체크 로직을 공통으로 처리해야 유지보수에 좋다.

- #### 공통 처리는 AOP로 해결할 수 있지만 웹 관련 공통 관심사는 서블릿 필터 또는 스프링 인터셉터를 사용하는 것이 좋다.

- #### 웹 관련 공통 관심사를 처리할 때는 HTTP의 헤더나 URL 정보들이 필요한데 서블릿 필터나 스프링 인터셉터는 HttpServletRequest를 제공한다.



## Filter



- #### 필터 흐름

  - #### HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 컨트롤러

  - #### 필터가 호출된 후 서블릿이 호출된다.

  - #### 고객의 요구사항은 필터를 사용하며 특정 URL패턴에 사용 가능하다. 즉, URL 마다 다른 필터 적용을 WAS에 등록하여 구현 가능하다.

  - #### `/*`이라고 하면 모든 요청에 필터가 적용된다.

  - #### 여기서 서블릿은 디스패처 서블릿이다.

- #### 필터 제한

  - #### HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 컨트롤러 //로그인 사용자

  - #### HTTP 요청 -> WAS -> 필터(적절하지 않은 요청이라 판단, 서블릿 호출X) //비 로그인 사용자

- #### 필터 체인

  - #### HTTP 요청 -> WAS -> 필터1 -> 필터2 -> 필터3 -> 서블릿 -> 컨트롤러

  - #### 필터는 체인으로 구성되며 자유롭게 추가 가능하다.

  - #### 로그를 남기는 필터를 먼저 적용하고 그 다음에 로그인 여부를 체크하는 필터를 만들 수 있다.



```java
public interface Filter {
    public default void init(FilterConfig filterConfig) throws ServletException {}
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException;
    
    public default void destroy() {}
}
```

- #### 필터 인터페이스를 구현하고 등록하면 서블릿 컨테이너가 필터를 싱글톤 객체로 생성하고 관리한다.

- #### `init()`은 필터 초기화 메서드이며 서블릿 컨테이너가 생성될 때 호출된다.

- #### `doFilter()`은 고객의 요청이 올 때 마다 해당 메서드가 호출된다. 즉, 필터의 로직을 구현하면 된다.

- #### `destroy()`는 필터 종료 메서드이며 서블릿 컨테이너가 종료될 때 호출된다.



```java
@Slf4j
public class LogFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("log filter init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.info("log filter doFilter");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        String uuid = UUID.randomUUID().toString();

        try{
            log.info("REQUEST [{}][{}]", uuid, requestURI);
            chain.doFilter(request, response);
        }catch (Exception e){
            throw e;
        }finally {
            log.info("RESPONSE [{}][{}]", uuid, requestURI);
        }
    }

    @Override
    public void destroy() {
        log.info("log filter destroy");
    }
}
```

- #### 모든 요청에 대해 로그를 남기는 필터이다.

- #### HTTP 요청이 오면 `dofilter`이 호출된다.

- #### ServletRequest는 HTTP 요청이 아닌 경우도 고려한 것이므로 다운 케스팅 해야 한다.

- #### HTTP 요청을 구분하기 위해 요청당 uuid를 생성한다.

- #### `chain.doFilter(request, response)`는 다음 필터가 있으면 필터를 호출하고 필터가 없으면 서블릿을 호출한다. 이 로직이 없다면 다음 단계로 진행되지 않으니 필터에 필수적인 로직이다.



```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Bean
    public FilterRegistrationBean logFilter(){
        FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
        filterFilterRegistrationBean.setFilter(new LogFilter());
        filterFilterRegistrationBean.setOrder(1);
        filterFilterRegistrationBean.addUrlPatterns("/*");

        return filterFilterRegistrationBean;
    }

}
```

- #### 스프링 부트 이용 시 `FilterRegistrationBean`을 사용하여 필터를 등록한다.

- #### `setFilter(new LogFilter())`은 등록할 필터를 지정한다.

- #### `setOrder(1)`는 필터의 체인을 관리한다. 순서를 지정하는 것으로 낮을 수록 먼저 동작한다.

- #### `addUrlPatterns("/*")`는 필터를 적용할 URL 패턴을 지정한다. 한번에 여러 패턴을 지정할 수 있다. `/*`는 모든 URL에 적용한다는 뜻이다.

- #### `@ServletComponentScan` `@WebFilter(filterName = "logFilter", urlPatterns = "/*")`로 필터를 등록할 수 있지만 필터 순서 조절이 안되기 때문에 사용하지 않는다.

- #### 실무에서  HTTP 요청 시 같은 요청의 로그에 모두 같은 식별자를 자동으로 남기는 방법은 logback mdc로 한다.



```java
@Slf4j
public class LoginCheckFilter implements Filter {

    private static final String[] whiteList = {"/", "/members/add", "/login", "/logout", "/css/*"};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try{
            log.info("인증 체크 필터 시작 {}", requestURI);

            if(isLoginCheckPath(requestURI)){
                log.info("인증 체크 로직 실행 {}", requestURI);
                HttpSession session = httpRequest.getSession(false);
                // 로그인 안된 경우
                if(session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null){

                    log.info("미인증 사용자 요청 {}", requestURI);
                    // 로그인 페이지로 redirect
                    httpResponse.sendRedirect("/login?redirectURL=" + requestURI);
                    return;
                }
            }
            // 화이트 리스트에 포함된 URL이 아닌 경우
            chain.doFilter(request, response);


        } catch (Exception e){
            throw e; // 예외 로깅 가능하지만 톰캣까지 예외를 보내주어야 한다.
        } finally {
            log.info("인증 체크 필터 종료 {}", requestURI);
        }

    }

    /**
     * 화이트 리스트의 경우 인증 체크X
     */
    private boolean isLoginCheckPath(String resultURI){
        return !PatternMatchUtils.simpleMatch(whiteList, resultURI);
    }


}
```

- #### 로그인을 확인하는 필터로 로그인 되지 않은 사용자는 상품 관리 뿐만 아니라 미래에 개발될 페이지에도 접근하지 못 한다.

- #### 인터페이스에 default 키워드의 추가로 구현하지 않아도 되는 메소드를 만들 수 있다. 위 코드에서는 이러한 이유로 `init()`, `destroy()` 메소드를 뺐다. `doFilter()`은 필수이다. Default 키워드는 Java 8 부터 적용되었다.

- #### 화이트 리스트는 인증과 무관하게 항상 허용하는 경로이다. 즉, 화이트 리스트를 제외한 나머지 모든 경로에는 인증 체크 로직을 적용한다.

- #### `httpResponse.sendRedirect("/login?redirectURL=" + requestURI);`는 미인증 사용자를 로그인 화면으로 리다이렉트하고 로그인 후 다시 기존에 있던 페이지로 가는 것이다. 현재 요청한 경로인 requestURI를 /login에 쿼리 파라미터로 함께 전달한다. 물론 /login 컨트롤러에서 로그인 성공시 해당 경로로 이동하는 기능은 추가로 개발해야 한다. 즉, 쿼리 파라미터로 받은 후 그곳으로 가도록 /login에서 다시 리다이렉트 해야 한다.

- #### `return;`은 필터를 더 진행하지 않게 한다. 즉, 앞서 redirect를 사용했기 때문에 redirect가 응답으로 적용되고 요청이 끝난다. Redirect 시 response를 사용한다.

- #### `throw e;`로 구현하면 이 다음 로직이 정상 동작하므로 WAS까지 예외를 올리게 된다.

- #### `PatternMatchUtils`는 스프링이 제공하는 기능이며 로그인 체크를 해야하는 URL인지 찾는 것이므로 `!`를 붙여준다. `simpleMatch`는 단순하게 패턴이 일치하는지 검증한다.

- #### 필터를 @Component로 빈으로 등록하여 WebConfig에서 DI로 이용하는 방법도 있다.



```java
// WebConfig
@Bean
public FilterRegistrationBean loginCheckFilter(){
    FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
    filterFilterRegistrationBean.setFilter(new LoginCheckFilter());
    filterFilterRegistrationBean.setOrder(2);
    filterFilterRegistrationBean.addUrlPatterns("/*");

    return filterFilterRegistrationBean;
}
```

- #### 로그인 체크 필터를 등록하는 것으로 전체 URL에 적용하는 것으로 하며 필터 내부 로직에서 화이트 리스트로 걸러주는 것이 좋다.

- #### 이렇게 모든 URL에 적용한다고 해서 성능 저하는 거의 없다. 



```java
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



    // 세션 관리자를 통해 세션을 생성하고, 회원 데이터 보관
    //        sessionManager.createSession(loginMember, response);

    return "redirect:" + redirectURL;
}
```

- #### 로그인 성공 시 기존의 URL로 다시 이동하는 로직이 추가되었다.

- #### 쿼리 파라미터로 URL을 받으며 default를 `/`로 설정하여 로직을 완성하였다.

- #### 필터는 doFilter로 다음 필터 또는 서블릿을 호출할 때 request, response 객체를 바꿀 수 있다. 즉, ServletRequest, ServletResponse를 구현한 다른 객체를 넘기면 되고 다음 필터 또는 서블릿은 그 객체를 이용하게 된다. 잘 사용하는 기능은 아니다.

- #### Spring Security도 필터 형식으로 작동한다. Java 코드로 되어있으며 보안이 더 복잡하지만 기본적으로 필터 형식으로 서블릿 가기 전에 거르는 작업은 같다.



## Interceptor



- #### 필터는 서블릿이 제공하는 기술이지만 인터셉터는 스프링 MVC가 제공하는 기술이다.

- #### 인터셉터 흐름

  - #### HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 스프링 인터셉터 -> 컨트롤러

  - #### 인터셉터는 디스패처 서블릿과 컨트롤러 사이에서 컨트롤러 호출 직전에 호출된다.

  - #### 인터셉터에도 URL 패턴을 적용할 수 있다.

- #### 인터셉터 제한

  - #### HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 스프링 인터셉터 -> 컨트롤러 //로그인 사용자

  - #### HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 스프링 인터셉터(적절하지 않은 요청이라 판단, 컨트롤러 호출 X) // 비 로그인 사용자

- #### 인터셉터 체인

  - #### HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 인터셉터1 -> 인터셉터2 -> 컨트롤러



```java
public interface HandlerInterceptor {
	default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {}

    default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {}

    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {}
}
```

- #### 인터셉터는 컨트롤러 호출 전(preHandle), 호출 후(postHandle), 요청 완료 이후(afterCompletion)와 같이 세분화 되어 있다.

- #### 필터는 단순히 request, response만 제공했지만 인터셉터는 어떤 컨트롤러(handler)가 호출되는지 호출 정보도 받을 수 있다. 그리고 어떤 modelAndView가 반환되는지 응답 정보도 받을 수 있다.



### Interceptor Flow



![인터셉터 흐름](/media/mwkang/Klevv/Spring 일지/MVC2/03.13/인터셉터 흐름.png)

- #### `preHandle`은 컨트롤러 호출 전에 호출된다. 정확히는 핸들러 어댁터 호출 전에 호출된다. 응답값이 true이면 다음으로 진행하고 false이면 더는 진행하지 않는다. False인 경우 나머지 인터셉터는 물론이고 핸들러 어댑터도 호출되지 않는다.

- #### `postHandle`는 컨트롤러 호출 후에 호출된다. 정확히는 핸들러 어댑터 호출 후에 호출된다.

- #### `afterCompletion`은 뷰가 렌더링 된 이후에 호출된다.



### Interceptor Exception



![인터셉터 예외](/media/mwkang/Klevv/Spring 일지/MVC2/03.13/인터셉터 예외.png)

- #### `postHandle`은 호출되지 않는다.

- #### `preHandle`과 `afterCompletion`은 항상 호출되며 `afterCompletion`의 경우 예외를 파라미터로 받아서 어떤 예외가 발생했는지 로그로 출력할 수 있다.

- #### 인터셉터는 스프링 MVC 구조에 특화된 필터 기능을 제공한다. 즉, 스프링 MVC를 사용하고 특별히 필터를 사용해야 하는 상황이 아니라면 인터셉터를 사용하는 것이 더 편리하다.



### Interceptor Application



```java
@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    public static final String LOG_ID = "logId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        String uuid = UUID.randomUUID().toString();


        request.setAttribute(LOG_ID, uuid);

        // @RequestMapping: HandlerMethod

        if (handler instanceof HandlerMethod){
            HandlerMethod hm = (HandlerMethod) handler;// 호출할 컨트롤러 메서드의 모든 정보가 포함되어 있다.
        }

        log.info("REQUEST [{}][{}][{}]", uuid, requestURI, handler);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("postHandle [{}]", modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestURI = request.getRequestURI();
        Object uuid = (String) request.getAttribute(LOG_ID);
        log.info("RESPONSE [{}][{}][{}]", uuid, requestURI, handler);
        // 에러 발생한 경우
        if(ex != null){
            log.error("afterCompletion error!!", ex);
        }

    }
}
```

- #### uuid, URL, 핸들러 로그를 출력하는 인터셉터이다.

- #### `preHandle`에서 `afterCompletion`으로 uuid를 공유하고 싶을 때 인터셉터가 싱글톤으로 되어서 전역변수 사용 시 다른 사용자가 전역변수 변경가능하다. 즉, 전역변수는 사용하면 안된다.

- #### `request.setAttribute(LOG_ID, uuid)`는 request에 정보를 담는 것이다. 필터는 지역변수로 해결 가능하지만 인터셉터는 호출 시점이 완전히 분리되어 있어서 저장할 곳으로 request를 사용한다. 사용할 때에는 `request.getAttribute(LOG_ID)`를 이용한다.

- #### `return true;`일 때 true이면 정상 호출이다. 다음 인터셉터나 컨트롤러가 호출되게 된다.

- #### `HandlerMethod`는 행들러 정보를 가지고 있다. 핸들러 정보는 어떤 핸들러 매핑을 사용하는가에 따라 달라진다. 스프링을 사용하면 일반적으로 @Controller, @RequestMapping을 활용한 핸들러 매핑을 사용한다.

- #### `ResourceHttpRequestHandler`은 @Controller가 아니라 /resource/static와 같은 정적 리소스가 호출되는 경우에 넘어오는 것이다. 이때 타입에 따라서 처리가 필요하다.



```java
// WebConfig
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new LogInterceptor())
        .order(1)
        .addPathPatterns("/**")
        .excludePathPatterns("/css/**", "/*.ico", "/error");
}
```

- #### `registry.addInterceptor`은 인터셉터를 등록한다.

- #### `order(1)`은 인터셉터의 호출 순서를 지정하며 낮을 수록 먼저 호출된다.

- #### `addPathPatterns("/**")`은 인터셉터를 적용할  URL 패턴을 지정한다. `/**`는 전체 URL을 뜻한다.

- #### `excludePathPatterns("/css/**", "/*.ico", "/error")`는 인터셉터에서 제외할 패턴을 지정하는 것으로 화이트 리스트와 유사하다.

- #### 인터셉터는 정밀하게 URL 패턴을 지정할 수 있어서 유용하다.



```
? 한 문자 일치
* 경로(/) 안에서 0개 이상의 문자 일치
** 경로 끝까지 0개 이상의 경로(/) 일치
{spring} 경로(/)와 일치하고 spring이라는 변수로 캡처
{spring:[a-z]+} matches the regexp [a-z]+ as a path variable named "spring"
{spring:[a-z]+} regexp [a-z]+ 와 일치하고, "spring" 경로 변수로 캡처
{*spring} 경로가 끝날 때 까지 0개 이상의 경로(/)와 일치하고 spring이라는 변수로 캡처

/pages/t?st.html — matches /pages/test.html, /pages/tXst.html but not /pages/ toast.html
/resources/*.png — matches all .png files in the resources directory
/resources/** — matches all files underneath the /resources/ path, including / resources/image.png and /resources/css/spring.css
/resources/{*path} — matches all files underneath the /resources/ path and captures their relative path in a variable named "path"; /resources/image.png will match with "path" → "/image.png", and /resources/css/spring.css will match with "path" → "/css/spring.css"
/resources/{filename:\\w+}.dat will match /resources/spring.dat and assign the value "spring" to the filename variable
```

- #### 스프링이 제공하는 URL 경로는 서블릿이 제공하는 URL 경로와 완전히 다르며 더욱 자세하고 세밀하게 설정할 수 있다.

[Spring URL Pattern](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/
springframework/web/util/pattern/PathPattern.html)



```java
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

- #### 로그인 체크하는 인터셉터이다.

- #### 로그인 체크는 컨트롤러 호출 전에 실행되어야  하기 때문에 `preHandle`만 다룬다.

- #### 인터셉터 등록 시 화이트 리스트를 반영하는 것이 가능하기 때문에 인터셉터 내부 로직에 화이트 리스트는 추가하지 않는다.



```java
// WebConfig
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new LoginCheckInterceptor())
        .order(2)
        .addPathPatterns("/**")
        .excludePathPatterns("/", "/members/add", "/login", "/css/**", "/*.ico", "/error");
}
```

- #### 기본으로 모든 URL 패턴에 적용하는 것으로 하며 제외할 패턴은 `excludePathPatterns()`에 작성한다.

- #### 화이트 리스트의 경우 등록시에 변경하면 되기 때문에 관리하기 쉽다.



### ArgumentResolver Application



- #### 로그인 시 @SessionAttribute() 없이 직접 개발한 어노테이션을 사용하여 객체를 주입할 수 있다.



```java
// HomeController
@GetMapping("/")
public String homeLoginV4ArgumentResolver(@Login Member loginMember, Model model){}
```

- #### @Login 어노테이션을 직접 만든 후 검사 로직을 거쳐서 Member 객첼을 반환하게 할 수 있다.

- #### 세션이 있으며 등록된 멤버면 멤버가 반환되지만 아닌 경우 null이 반환된다.



```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Login {
}
```

- #### `@Target(ElementType.Parameter)`은 파라미터에만 사용할 것이라는 뜻이다.

- #### `@Retention(RetentionPolicy.RUNTIME)`은 리플렉션 등을 활용할 수 있도록 런타임까지 어노테이션 정보가 남아있게 하는 것이다. 즉, 실제 동작할 때까지 어노테이션이 남아있어야 하므로 RUNTIME으로 한 것이다.

- #### 현재는 ModelAttribute처럼 동작한다. 즉, 어떤 객체로 어떻게 넣어줘야 하는지 인식하지 못하고 있다.

- #### 컨트롤러의 파라미터 조거이 만족되면 ModelAttribute가 아닌 직접 개발한  ArgumentResolver이 작동되게 해야 한다.



```java
@Slf4j
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        log.info("supportsParameter 실행");

        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);
        boolean hasMemberType = Member.class.isAssignableFrom(parameter.getParameterType());

        return hasLoginAnnotation && hasMemberType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        log.info("resolveArgument 실행");

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);
        if(session == null){
            return null;
        }

        return session.getAttribute(SessionConst.LOGIN_MEMBER);
    }
}
```

- #### ArgumentResolver이 작동되게 검사하고 내부 로직을 구성한 것이다.

- #### `supportsParameter()`은 @Login 어노테이션이 있으면서 Member 타입이면 해당 ArgumentResolver이 호출되게 한다.

- #### `resolveArgument()`는컨트롤러 호출 직전에 호출된다. 여기서는 세션에 있는 로그인 회원 정보인 Member 객체를 반환한다.

- #### `boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);`는 @Login 이 파라미터에 있는지 검사하는 것이다.

- #### `boolean hasMemberType = Member.class.isAssignableFrom(parameter.getParameterType());`는 파라미터에 Member 객체가 있는지 검사하는 것이다.

- #### `webRequest.getNativeRequest()`에서 HttpServletRequest를 반환하여 사용할 수 있다.



```java
@Override
public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new LoginMemberArgumentResolver());
}
```

- #### ArgumentResolver를 등록하는 것이다.

- #### `supportsParameter()`은 스프링 내부에 캐싱되어 두 번째 실행 시 실행되지 않고 바로 `resolveArgument`가 실행된다.



# Keyboard Shortcut

- #### Ctrl + alt + c : 해당 값을 상수로 만든다.



