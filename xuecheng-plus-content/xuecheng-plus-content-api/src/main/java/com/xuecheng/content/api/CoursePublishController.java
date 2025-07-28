package com.xuecheng.content.api;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;


/**
 * 课程预览发布
 */
@Controller
@Slf4j
public class CoursePublishController {

    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView coursePreview(@PathVariable("courseId") String courseId) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("model", null);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }
}