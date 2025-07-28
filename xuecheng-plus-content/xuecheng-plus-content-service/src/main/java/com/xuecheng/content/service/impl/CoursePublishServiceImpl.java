package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CoursePublishServiceImpl implements CoursePublishService {


    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Override
    public CoursePreviewDto getCoursePreview(Long courseId) {

        //课程基本信息，营销信息
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);
        //课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        //封装数据
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    @Override
    public void commitAudit(Long companyId, Long courseId) {
//      功能：
//      1. 查询课程基本信息、课程营销信息、课程计划信息等课程相关信息，整合为课程预发布信息。
//      2. 向课程预发布表course_publish_pre插入一条记录，如果已经存在则更新，审核状态为：已提交。
//      3. 更新课程基本表course_base课程审核状态为：已提交。
//      约束：
//      1. 对已提交审核的课程不允许提交审核。
//      2. 本机构只允许提交本机构的课程。
//      3. 没有上传图片不允许提交审核。
//      4. 没有添加课程计划不允许提交审核。
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        // 校验约束
        if (courseBase.getAuditStatus().equals("202003")) {
            XueChengPlusException.cast("当前为等待审核状态，审核完成才可以再次提交");
        }
        if (!Objects.equals(courseBase.getCompanyId(), companyId)) {
            XueChengPlusException.cast("当前课程不属于本机构，不能提交审核");
        }
        if (StringUtils.isBlank(courseBase.getPic())) {
            XueChengPlusException.cast("课程图片不能为空");
        }

        CoursePublishPre coursePublishPre = new CoursePublishPre();
        // 查询课程基本信息
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);
        BeanUtils.copyProperties(courseBaseInfoDto, coursePublishPre);
        // 课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        coursePublishPre.setMarket(JSON.toJSONString(courseMarket));
        // 课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if (teachplanTree.size() <= 0) {
            XueChengPlusException.cast("提交失败，课程计划不能为空");
        }
        coursePublishPre.setTeachplan(JSON.toJSONString(teachplanTree));
        // 更新其他字段
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCompanyId(companyId);
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreUpdate != null) {
            coursePublishPre.setId(coursePublishPreUpdate.getId());
            coursePublishPreMapper.updateById(coursePublishPre);
        } else {
            coursePublishPreMapper.insert(coursePublishPre);
        }
        // 更新课程基本表的审核状态
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);

    }
}
