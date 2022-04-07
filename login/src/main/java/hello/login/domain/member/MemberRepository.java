package hello.login.domain.member;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;

@Slf4j
@Repository
public class MemberRepository {

    private static Map<Long, Member> store = new HashMap<>(); // static 사용
    private static long sequence = 0L; // static

    public Member save(Member member){
        member.setId(++sequence);
        log.info("save: member={}", member);
        store.put(member.getId(), member);
        return member;
    }

    public Member findById(Long id){
        return store.get(id);
    }

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

    public void clearStore(){
        store.clear();
    }
}
