package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    /**
     *
     * http://localhost:8080/v1/members?teamName=teamB&ageGoe=31&ageLoe=35&username=member31
     *
     * [
     *     {
     *         "memberId": 34,
     *         "username": "member31",
     *         "age": 31,
     *         "teamId": 2,
     *         "teamName": "teamB"
     *     }
     * ]
     *
     * @param condition
     * @return
     */
    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.search(condition);
    }
}
