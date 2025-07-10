package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    private TeachplanMapper teachplanMapper;

    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;


    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto dto) {
        if (dto.getId() != null) {
            // 修改
            Teachplan teachplan = teachplanMapper.selectById(dto.getId());
            BeanUtils.copyProperties(dto, teachplan);
            teachplan.setChangeDate(LocalDateTime.now());
            teachplanMapper.updateById(teachplan);
        } else {
            // 新增
            // 取出同父同级别的课程计划数量 用于设置排序号
            int count = getTeachplanCount(dto.getCourseId(), dto.getParentid());
            Teachplan teachplan = new Teachplan();
            // 设置排序号
            teachplan.setOrderby(count + 1);
            BeanUtils.copyProperties(dto, teachplan);
            teachplan.setCreateDate(LocalDateTime.now());
            teachplanMapper.insert(teachplan);
        }
    }

    @Transactional
    @Override
    public void deleteTeachplan(Long id) {
        // 1、先判断是大章节还是小章节
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (teachplan.getParentid().equals(0L)) {
            // 大章节
            // 2、判断是否有小章节
            int teachplanCount = getTeachplanCount(teachplan.getCourseId(), id);
            if (teachplanCount > 0) {
                throw new XueChengPlusException("该大章节下有小章节，不允许删除");
            }
            teachplanMapper.deleteById(id);
        } else {
            // 小章节 同时需要删除关联的信息
            LambdaQueryWrapper<TeachplanMedia> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(TeachplanMedia::getTeachplanId, teachplan.getId());
            teachplanMediaMapper.delete(lambdaQueryWrapper);
            teachplanMapper.deleteById(id);
        }
    }

    @Override
    public void updateTeachplanMove(String moveWay, Long id) {
        // 取出当前课程计划
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (teachplan == null) {
            throw new XueChengPlusException("课程计划不存在");
        }
        Teachplan teachplan1 = null;
        int orderby = 0;
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, teachplan.getCourseId());
        queryWrapper.eq(Teachplan::getParentid, teachplan.getParentid());
        if (moveWay.equals("movedown")) {
            // 向下移动
            // 找到当前课程计划的下一个课程计划
            queryWrapper.gt(Teachplan::getOrderby, teachplan.getOrderby()).orderByAsc(Teachplan::getOrderby).last("LIMIT 1");
            teachplan1 = teachplanMapper.selectOne(queryWrapper);
            if (teachplan1 == null) {
                throw new XueChengPlusException("下面没有课程计划了");
            }
            // 交换两个课程计划的排序号
            orderby = teachplan.getOrderby();
        } else {
            // 向上移动
            // 找到当前课程计划的上一个课程计划
            queryWrapper.lt(Teachplan::getOrderby, teachplan.getOrderby()).orderByDesc(Teachplan::getOrderby).last("LIMIT 1");
            teachplan1 = teachplanMapper.selectOne(queryWrapper);
            if (teachplan1 == null) {
                throw new XueChengPlusException("上面没有课程计划了");
            }
            orderby = teachplan.getOrderby();
        }
        teachplan.setOrderby(teachplan1.getOrderby());
        teachplan1.setOrderby(orderby);
        teachplanMapper.updateById(teachplan1);
        teachplanMapper.updateById(teachplan);
    }

    /**
     * 取出同父同级别的课程计划数量 用于设置排序号
     *
     * @param courseId 课程Id
     * @param parentId 父课程计划Id
     * @return 最新排序号
     */
    private int getTeachplanCount(Long courseId, Long parentId) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        queryWrapper.eq(Teachplan::getParentid, parentId);
        return teachplanMapper.selectCount(queryWrapper);
    }
}
