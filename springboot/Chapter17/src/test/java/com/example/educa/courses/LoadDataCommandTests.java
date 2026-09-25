package com.example.educa.courses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.example.educa.EducaTest;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;

@EducaTest
class LoadDataCommandTests {

    @Autowired
    LoadDataCommand loadData;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    UserRepository users;

    @Autowired
    MockMvc mvc;

    @Test
    void loaddataIsIdempotentAndResetsTheSequence() throws Exception {
        assertThat(loadData.load("subjects")).isEqualTo(4);
        assertThat(loadData.load("subjects")).isEqualTo(4);   // 再导入一次：按主键更新，不会重复
        assertThat(subjects.findAllByOrderByTitleAsc()).extracting(Subject::getTitle)
                .containsExactly("Mathematics", "Music", "Physics", "Programming");

        // 显式指定了主键之后，普通插入仍然能拿到不冲突的新主键
        Subject chemistry = subjects.save(new Subject("Chemistry", "chemistry"));
        assertThat(chemistry.getId()).isGreaterThan(4L);
        subjects.delete(chemistry);
    }

    @Test
    void loginPage() throws Exception {
        users.save(new User("teacher2", "{noop}secret", ""));
        mvc.perform(get("/accounts/login/")).andExpect(content().string(Matchers.containsString("Log-in")));
        mvc.perform(get("/accounts/login/").param("error", ""))
                .andExpect(content().string(Matchers.containsString("didn't match")));
        mvc.perform(formLogin("/accounts/login/").user("teacher2").password("secret")).andExpect(authenticated());
    }
}
