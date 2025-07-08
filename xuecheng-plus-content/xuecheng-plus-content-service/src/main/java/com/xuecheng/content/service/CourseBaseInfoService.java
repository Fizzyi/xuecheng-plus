package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.base.model.*;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;

/**
 * 课程基本信息管理业务接口
 */
public interface CourseBaseInfoService {

    /**
     * 课程分页查询接口
     *
     * @param pageParams           分页参数
     * @param queryCourseParamsDto 条件查询参数
     * @return PageResult<CourseBase> 分页数据
     */
    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);
}
