package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

/**
 * 课程计划树形结构dto
 */
@Data
public class TeachplanDto extends Teachplan {

    /**
     * 课程计划关联的媒资信息
     */
    TeachplanMedia teachplanMedia;

    /**
     * 子节点
     */
    List<TeachplanDto> teachPlanTreeNodes;
}
