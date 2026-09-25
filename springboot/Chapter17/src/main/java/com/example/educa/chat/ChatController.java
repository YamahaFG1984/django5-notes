package com.example.educa.chat;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.example.educa.account.CurrentUser;
import com.example.educa.courses.Course;

/** chat/views.py 的 course_chat_room（@login_required 在 SecurityConfig 里配置） */
@Controller
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping("/chat/room/{courseId}/")
    public String room(@PathVariable Long courseId, @AuthenticationPrincipal CurrentUser user, Model model) {
        // 课程不存在或没有选这门课：HttpResponseForbidden()
        Course course = chat.joinedCourse(courseId, user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        model.addAttribute("course", course);
        model.addAttribute("latestMessages", chat.latestMessages(courseId));
        return "chat/room";
    }
}
