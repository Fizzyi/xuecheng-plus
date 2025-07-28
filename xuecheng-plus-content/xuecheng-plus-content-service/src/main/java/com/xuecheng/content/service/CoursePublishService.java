package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

/**
 * 课程预览、发布接口
 */
public interface CoursePublishService {

    /**
     * 获取课程预览信息
     *
     * @param courseId
     * @return
     */
    public CoursePreviewDto getCoursePreview(Long courseId);
}
