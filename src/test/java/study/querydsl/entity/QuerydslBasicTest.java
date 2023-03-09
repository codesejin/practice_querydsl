package study.querydsl.entity;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceUnits;
import javax.transaction.Transactional;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화
        em.flush(); // 영속성 컨텍스트에 있는 오브젝트들을 실제 쿼리를 만들어서 디비에 날리게 되고
        em.clear(); // 영속성 컨텍스트를 초기화해서 캐시같은게 다 날라감

        //확인  - Jpql
        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println("member = " + member);
            System.out.println("-> member.team" + member.getTeam());

        }
    }

    @Test
    public void startJPQL() {
        // member1.을 찾아라.
//        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        //QMember m = QMember.member;

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }


    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                // 이 방법으로 할 경우 중간에 NULL이 들어갈때 NULL을 무시해서, 동적쿼리 만들때 기가 막힘
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
////                .limit(1).fetchOne();
//                .fetchFirst();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal(); // 어디까지 페이지가 있는지
//        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(Desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
        Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
        Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
        Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation() {
        // 튜플을 여러개가 있을때 꺼내오는 것
        // 튜플로 사용하는 이유 :  데이터타입이 여러개가 들어올 때 사용, 하지만 실무에서는 튜플 많이 사용 안하고 DTO사용
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     *
     * @throws Exception
     */
    @Test
    public void group() throws Exception {

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) // 내부조인
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15); // ( 10 + 20 ) / 2
        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35); // ( 30 + 40 ) / 2
    }

    /**
     * 팀 A에 소속된 모든 회원
     *
     * @throws Exception
     */
    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 히원 조회 ( 연관관계 없는 조인을 해보려고 함 )
     * left outer join 이나 right outer join 과 같은 외부 조인 불가능 -> hibernate 최신버저 들어가면서 조인 ON을 사용하면 외부 조인 가능
     *
     * @throws Exception
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // 멤버랑 팀 테이블 전부 다 조인
                .where(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     *
     * @throws Exception
     */
    @Test
    public void join_on_filtering() throws Exception {

        // select 가 여러가지 타입이라 튜플로 나옴
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                //.where(team.name.eq("teamA")) 이너 조인일 경우 이렇게 where절 하나 ON절 하나 똑같음
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        /*
            출력값 : left join 이라 member를 다 가져오는데, teamA와 동일하다는 ON절 조건을 해서 나머지 밑 2개나는 NULL
            // left join이 아닌 그냥 join 으로 할 경우, 내부 조인이라서  2개만 가져옴
            tuple = [Member{id=3, username='member1', age=10}, study.querydsl.entity.Team@1a53ac0c]
            tuple = [Member{id=4, username='member2', age=20}, study.querydsl.entity.Team@1a53ac0c]
            tuple = [Member{id=5, username='member3', age=30}, null]
            tuple = [Member{id=6, username='member4', age=40}, null]
         */

    }

    /**
     * 연관관계 없는 엔티티 외부조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     *
     * @throws Exception
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member) // 멤버랑 팀 테이블 전부 다 조인
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test // fetchJoin 테스트할때는 영속성 컨텍스트에 남아있는 캐시를 지워줘야 제대로 값을 확인할 수 있다
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test // fetchJoin 테스트할때는 영속성 컨텍스트에 남아있는 캐시를 지워줘야 제대로 값을 확인할 수 있다
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     *
     * @throws Exception
     */
    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     *
     * @throws Exception
     */
    @Test
    public void subQueryGoe() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }


    /**
     * (효율적이지 않지만 예제 상 만듬)
     *
     * @throws Exception
     */
    @Test
    public void subQueryIn() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * select 절에서 SUBQUERY 사용 예
     *
     * @throws Exception
     */
    @Test
    public void selectSubquery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
//                        JPAExpressions
//                                .select(memberSub.age.avg())
//                                .from(memberSub))
                        select(memberSub.age.avg()) // <- static import
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        /** JPA는 From절에서 서브쿼리가 안된다. 당연히 querydsl 도 지원하지 않는다 (select절이나 Where절에서만 가능)
         * 하이버네이트 구현체를 사용하면 Select절의 서브쿼리는 지원한다
         * QUERYDSL도 하이버네이트 구현체를 사용하면 Select절의 서브쿼리를 지원한다
         *
         *  from절의 서브쿼리 해결방안
         *  1, 서브쿼리를 JOIN으로 변경한다(가능한 상황도있고, 불가능한 상황도 있다
         *  2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
         *  3. Native SQL을 사용한다
         *
         *  from절에서 서브쿼리를 쓰는 안좋은 이유?
         *  화면에서 이쁘게 랜더링된 데이터 포맷을 만들기 위해 from절안에 from절이 들어가는 경우
         *
         *  한방 쿼리를 짜기위해 복잡하게 쿼리를 짜는것보다 2,3번 나눠서 쿼리를 날리는게 낫다
         *  SQl을 집합적으로 생각해서 로직을 만들어야하는데, 시퀀스로 풀어서 만들 수 있는데..
         *  정말 복잡한 쿼리 1000줄 짜리를 나눠서 100줄짜리로 ..
         */

    }

    @Test
    public void basicCase() throws Exception {

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("얄실")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s =" + s);
        }
    }

    /**
     * 이걸 정말 사용해야하는지 고민해야함
     * 살다보면 꼭 필요할때도 있을텐데,가급적이면 DB에서 이런ㅂ문제를 해결 안한다
     * DB는 Raw데이터를 최소한으로 필터링,그룹핑,계산하는데
     * 열살인지 스무살인지 전환하고 바꿔서 보여주는건 DB에서 하면 안된다
     * 애플리케이션 로직이나 화면 프레젠테이션 레이어에서 해결해야한다
     *
     * @throws Exception
     */

    @Test
    public void complexCase() throws Exception {

        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 31)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception {

        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {

        // {username}_{age}
        List<String> result = queryFactory
//                .select(member.username.concat("_").concat(member.age)) concat은 문자만 되고, 데이터타입이 서로 달라서 안됨
                .select(member.username.concat("_").concat(member.age.stringValue())) // enum타입도 값이 ENUM이라 제대로 안나오는데 StringValue쓰면 된다
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() throws Exception {

        // select절 대상 지정
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() throws Exception {

        /**
         * 튜플의 패키지는 querydsl
         * repository 계층에서 사용하는건 괜찮지만, service 계층, controller 계층까지 넘어가는건 좋은 설계가 아니다
         * 하부 구현 기술인 jpa나 querydsl을 쓴다는걸 앞단인 비즈니스 로직에서 알면 좋지 않다
         * JDBC같은거 쓸 때는 걔네가 반환해주는 ResultSet 이런거를 Repository 나 DAO 계층 안에서 쓰도록 하고
         * 나머지 계층에서는 그런거에 대한 의존이 없게 설계하는게 좋은 설계이다
         * 그래야 나중에 하부 기술을 querydsl에서 다른 기술로 바꾸더라도 앞단인 CONTROLLER 나 Service를 바꿀 필요가 없다
         * 결론은 튜플 바깥으로 던질때는 DTO로 바꿔서 반환해라
         */

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * 순수 JPA 에서 DTO를 조회할 때는 NEW 명령어를 사용해야함
     * DTO 의 package 이름을 다 적어줘야 해서 지저분함
     * 생성자 방식마 지원함
     *
     * @throws Exception
     */
    @Test
    public void findDtoByJPQL() throws Exception {
        // new operation을 활용하는 법 (생성자를 호출하는 것 처럼 생김)
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQueryDslSetter() throws Exception {

        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQueryDslField() throws Exception {

        List<MemberDto> result = queryFactory
                // setter 무시하고 바로 값이 MemberDto안에 있는 필드에 촥촥 꽂힌다
                // 앗, private필드인데 어떻게 꽂히나요? java reflection
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByQueryDslField() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        /** 억지 쿼리긴 하지만  subQuery를 쓰기 위해
                         * 프로퍼티나 필드 접근 생성 방식에서 이름이 다를 때 해결방안
                         * ExpressionUtils.as(source, alias) 필드나 서브 쿼리에 별칭 적용
                         * 이 방법은 지저분하게 보이기 때문에 .as로 할 수 있으면 그걸로 가라
                         * 하지만 서브쿼리 같은 경우에는 방법이 없기에 ExpressionUtils 사용
                         */
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
        /**
         *         List<UserDto> result = queryFactory
         *                 .select(Projections.fields(UserDto.class,
         *                         member.username.as("name"),
         *                         member.age))
         *                 .from(member)
         *                 .fetch();
         *
         *         for (UserDto userDto : result) {
         *             System.out.println("userDto = " + userDto);
         *         }
         *
         * .as("name")이 없을 경우
         * 필드 이름이 동일한게 없어서 매치가 안되서 무시가 되버림
         * userDto = UserDto(name=null, age=10)
         * userDto = UserDto(name=null, age=20)
         * userDto = UserDto(name=null, age=30)
         * userDto = UserDto(name=null, age=40)
         */
    }

    @Test
    public void findDtoByQueryDslConstructor() throws Exception {

        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        // MemberDto안에 있는 타입이랑 일치해야한다
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByQueryDslConstructor() throws Exception {

        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * findUserDtoByQueryDslConstructor 랑 비슷한 방식이긴 한데,
     * findDtoByQueryProjection를 사용하면 컴파일 에러를 잡을 수 있어서 안전하다
     *
     * 하지만 단점은
     * MemberDto에 대한 Q파일을 생성하려면 DTO에 @QueryProjection을 추가해야한다
     * 또한 아키텍처적인 의존관계 문제가 있는데, Dto에 querydsl 관련 라이브러리 의존성을 가지게 된다
     * 그래서 querydsl라이브러리를 뺄때, Dto에 빨간불이 많이 들어올 것이다
     *그리고 DTO는 repository, service, controller 로도 쓰고 API로 바로 반환하기도 한다
     * 여러 레이어에 걸쳐서 돌아다니느데, 아키텍처 설계적으로 쓰기 애매하다
     * @throws Exception
     */
    @Test
    public void findDtoByQueryProjection() throws Exception {

        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    /**
     * 동적 쿼리로 조건을 갖고 회원 조회
     */
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParma = 10;

        List<Member> result = searchMember1(usernameParam, ageParma);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * BooleanBuilder 보다 훨씬 깔끔한 쿼리
     * where 조건에 Null 값은 무시된다
     * 메서드를 다른 쿼리에서도 재활용 할 수 있다
     * 쿼리 자체의 가독성이 높아진다
     * @throws Exception
     */
    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParma = 10;

        List<Member> result = searchMember2(usernameParam, ageParma);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        //Predicate vs BooleanExpression
//        if (usernameCond == null) {
//            return null;
//        }
//        return member.username.eq(usernameCond);
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if (ageCond == null) {
            return null;
        }
        return member.age.eq(ageCond);
    }

    // 광고 상태 isValid, 날짜가 IN : isServicable
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 수정 , 삭제 배치 쿼리 (벌크 연산)
     * 쿼리 한번으로 대량 데이터 수정
     * jpa는 기본적으로 엔티티를 가져와서, 엔티티의 값만 바꾸면
     * Transaction commit할때 flush가 에러가 나면서, 영어로는 dirty checking , 한글로는 변경감지가 일어나면서
     * 이 엔티티가 바뀌었네 파악하고 업데이트 쿼리가 만들어지고 실행된다
     * 그런데 이 변경감지는 개별 엔티티 건건이 일어나는거라 쿼리가 많이 나가게 된다
     * 그런데 한번에 한방쿼리로 처리해야할 경우 성능이 더 낫다.
     * 예를 들어 모든 개발자의 연봉을 50%인상해라고 하는 기능은 개별 건건이 날리는것 보다
     * 쿼리 한번으로 트랜잭션 커밋하는게 낫다 이런걸 jpa에서 벌크연산이라고 한다
     *
     */

    @Test
    //@Commit // 트랜잭션이 커밋을 안하니까 안보여서 추가함.
    /**
     * 테스트에서 spring @transactional 이 되어있으면 트랜잭션을 시작하고 테스트를 시작하는데
     * 끝나면 롤백을 해버린다. 그래야 다음에 다시 테스트를 해도 정상적으로 될 수 있으니까.
     *
     */
    public void bulkUpdate() throws Exception {
        /**
         * 벌크 연산에서 조심해야할 것
         * jpa는 기본적으로 영속성 컨텍스트라는 곳에 엔티티들이 다 올라가 있다.
         * 이미 영속석 컨텍스트에 member1, member2, member3, member4 가 올라가 있다
         *
         * 모든 벌크연산은 영속성 컨텍스트(1차 캐시)를 무시하고 DB에 바로 쿼리를 날려버린다
         * 그래서 영속성 컨텍스트와 DB의 상태가 달라진다
         */

        // member1 = 10 -> member1
        // member2 = 20 -> member2
        // member3 = 30 -> member3
        // member4 = 40 -> member4

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // member1 = 10 -> 비회원
        // member2 = 20 -> 비회원
        // member3 = 30 -> member3
        // member4 = 40 -> member4
        em.flush();
        em.clear();

        /**
         * 이 상태에서 멤버를 조회해서 가져오면 원하는 결과가 안나온다
         * jpa 기본적으로 Db에서 가져온 결과를 다시 영속성 컨텍스트 안에 넣어줘야 하는데
         * 이미 같은 ID값이 있으면 db에서 가져온걸 버린다
         * 영속성 컨텍스트가 항상 우선권을 가진다.
         * 그래서 UPDATE된 db결과를 가져오려면 영속성 컨텍스트에 있는 걸 다 보내서 데이터를 맞추고,영속성 컨텍스트의 데이터를 다 초기화해버리는것이다
         * 벌크연산이 나가면 이미  영속성 컨텍스트랑 Db랑 안맞기 때문에 초기화해버리는게 낫다.
         * 하지만 비즈니스 로직상 큰 버그가 일어날 수 있기 때문에 잘 확인해라
         */

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }
    
    @Test
    public void bulkAdd() throws Exception {

        queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) // minus는 없기 때문에 .add(-1)해라, 곱하기는 MULTIPLY
                .execute();
    }
    
    @Test
    public void bulkDelete() throws Exception {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() throws Exception {

        /**
         * function('replace', {0}, {1}, {2}) -> Replace함수를 사용하여 문자열을 치환하는 함수를 나타내는 문자열 탬플릿
         * {0}, {1}, {2 는 각각 함수의 첫번째, 두번째, 세번째 파라미터를 나타내며 쉽게 말해 문자열 템플릿 ㅏㄴ에 들어갈 파라미터의 위치를 표시하는 기호이다
         * Expressions.stringTemplate() 메소드를 사용하여 문자열 템플릿을 만들고 있습니다. 첫 번째 파라미터로 "function('replace', {0}, {1}, {2})" 문자열 템플릿을 전달하고, 두 번째 파라미터부터는 치환할 값을 전달합니다.
         * 즉, member.username 값을 {0} 자리에, "member" 값을 {1} 자리에, "M" 값을 {2} 자리에 삽입하여 문자열 템플릿을 완성합니다. 이렇게 완성된 문자열 템플릿은 select() 메소드에서 사용되어 replace 함수를 적용한 결과를 조회하게 됩니다.
         * 따라서, 위 코드는 replace 함수를 사용하여 member 문자열을 M 문자열로 치환한 결과를 조회하는 코드입니다.
         *
         * 이처럼 sqlFunction을 쓸 수 있는데, H2 Database를 쓰고 있으니까, H2Dialect에 Function이 등록되어있어야 한다
         * 만일 임의로 DB에서 펑션을 만들ㅇ고 싶으면 H2Dialect를 상속받은걸 만들어서, 그걸 설정(yml, properties)에 넣어서 사용해야 한다.
         */
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower())) // 똑같은 기능. 기본적으로 일반적인 DB에서 제공하는 난시 표준에 있는건 다 제공이 된다
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}


