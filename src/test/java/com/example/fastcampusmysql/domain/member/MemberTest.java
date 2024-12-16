package com.example.fastcampusmysql.domain.member;

import com.example.fastcampusmysql.domain.member.entity.Member;
import com.example.fastcampusmysql.utill.MemberFixtureFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.stream.LongStream.*;

public class MemberTest {

    @DisplayName("회원은 닉네임을 변경할 수 있다.")
    @Test
    public void testChangeName(){
        var member = MemberFixtureFactory.create();
        var toChangeName = "pnu";

        member.changeNickname(toChangeName);

        Assertions.assertEquals(toChangeName, member.getNickname());
    }

    @DisplayName("회원의 닉네임은 10자를 초과할 수 없다")
    @Test
    public void testNicknameMaxLength(){
        var member = MemberFixtureFactory.create();
        var overMaxLengthName = "pnupnupnupnu";

        Assertions.assertThrows(IllegalAccessError.class, ()-> member.changeNickname(overMaxLengthName));
        member.changeNickname(overMaxLengthName);
    }
}
