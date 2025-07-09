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
     *
     * @param saveTeachplanDto 课程计划信息
     */
    void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    /**
     * 删除课程计划
     *
     * @param id 课程计划id
     */
    void deleteTeachplan(Long id);

    /**
     * 课程计划上下移动
     *
     * @param moveWay 向上或者向下 movedown(向下移动) 或 moveup(向上移动)
     * @param id      课程计划id
     */
    void updateTeachplanMove(String moveWay, Long id);
}
