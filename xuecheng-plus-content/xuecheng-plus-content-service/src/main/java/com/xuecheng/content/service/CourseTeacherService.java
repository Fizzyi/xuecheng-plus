package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.dto.EditCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

public interface CourseTeacherService {
    List<CourseTeacher> queryCourseTeacherList(Long courseId);

    CourseTeacher addCourseTeacher(AddCourseTeacherDto dto);

    CourseTeacher updateCourseTeacher(EditCourseTeacherDto dto);

    void deleteCourseTeacher(Long courseId, Long teacherId);
}
