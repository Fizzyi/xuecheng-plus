package com.xuecheng.content.service;


import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * 课程基本信息管理业务接口
 */
public interface TeachplanService {

    /**
     * 查询课程计划树形结构
     *
     * @param courseId 课程Id
     * @return 树形结构
     */
    public List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 保存课程计划，包括新增和修改
     * @param saveTeachplanDto 课程计划信息
     */
    void saveTeachplan(SaveTeachplanDto saveTeachplanDto);
}
