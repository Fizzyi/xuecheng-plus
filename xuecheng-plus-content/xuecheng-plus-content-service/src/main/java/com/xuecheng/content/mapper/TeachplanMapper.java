package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;

import java.util.List;

/**
 * <p>
 * 课程计划 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface TeachplanMapper extends BaseMapper<Teachplan> {

    /**
     * 查询某课程的课程计划，组成树形结构
     *
     * @param courseId 课程Id
     * @return 课程计划树型结构
     */
    public List<TeachplanDto> selectTreeNodes(Long courseId);
}
