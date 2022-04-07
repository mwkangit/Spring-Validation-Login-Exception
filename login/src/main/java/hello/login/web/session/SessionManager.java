package hello.login.web.session;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
