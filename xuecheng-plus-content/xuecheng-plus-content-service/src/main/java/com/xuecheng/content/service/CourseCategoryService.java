package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

public interface CourseCategoryService {

    /**
     * 课程分类树形结构查询
     *
     * @param id 课程Id
     * @return
     */
    public List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
