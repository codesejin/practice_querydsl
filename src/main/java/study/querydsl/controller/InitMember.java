package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {
        @PersistenceContext
        private EntityManager em;

        @Transactional // 데이터 초기화하는 로직
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member"+i, i, selectedTeam));
            }
        }
    }
}


/**
 * 데이터베이스에 초기 데이터를 입력하는 기능을 하는 코드입니다. 스프링 애플리케이션을 시작할 때 @PostConstruct 어노테이션이 붙은 init() 메소드가 실행되면서 초기 데이터를 입력합니다.
 *
 * InitMember 클래스는 InitMemberService 클래스를 주입받아 init() 메소드를 호출하는 역할을 합니다.
 *
 * InitMemberService 클래스에서는 EntityManager를 사용하여 데이터베이스에 초기 데이터를 입력합니다.
 *
 * 먼저, init() 메소드에서는 Team 엔티티를 생성하고, EntityManager의 persist() 메소드를 사용하여 데이터베이스에 저장합니다.
 *
 * Team 엔티티를 생성할 때, teamA와 teamB 두 개의 팀을 생성합니다.
 *
 * 그리고 for 문을 사용하여 0부터 99까지의 100명의 회원을 생성하고, selectedTeam 변수를 사용하여 회원이 속한 팀을 랜덤하게 지정하여 데이터베이스에 저장합니다.
 *
 * Member 엔티티를 생성할 때, 이름(memberX)과 나이(X) 그리고 팀(teamA 또는 teamB) 정보를 포함하여 생성합니다.
 *
 * 마지막으로 @Transactional 어노테이션을 사용하여 데이터베이스 트랜잭션을 처리합니다. @Transactional 어노테이션이 붙은 메소드는 스프링 프레임워크가 제공하는 트랜잭션 기능을 사용하여 데이터베이스 트랜잭션을 처리합니다.
 *
 * 따라서, 위 코드는 애플리케이션 시작 시 데이터베이스에 초기 데이터를 입력하는 기능을 하는 코드입니다.
 */