package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;
import java.io.IOException;

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

    void commitAudit(Long companyId, Long courseId);

    void coursePublish(Long companyId, Long courseId);

    /**
     * 课程静态化
     *
     * @param courseId 课程Id
     * @return 静态化文件
     */
    public File generateCourseHtml(Long courseId);

    /**
     * 上传课程静态化页面
     *
     * @param courseId 课程Id
     * @param courseHtml 静态化文件
     */
    public void uploadCourseHtml(Long courseId, File courseHtml);
}
