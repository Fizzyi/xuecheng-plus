package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseBaseInfoServerImpl implements CourseBaseInfoService {


    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Autowired
    private TeachplanMapper teachplanMapper;


    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        //构建查询条件对象
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根据课程名称查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        //根据课程审核状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        //根据课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());
        //分页对象
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<CourseBase> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        return new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBaseInfo(Long companyId, AddCourseDto dto) {
        // 合规性校验
        if (StringUtils.isBlank(dto.getName())) {
            throw new XueChengPlusException("课程名称为空");
        }

        if (StringUtils.isBlank(dto.getMt())) {
            throw new XueChengPlusException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getSt())) {
            throw new XueChengPlusException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getGrade())) {
            throw new XueChengPlusException("课程等级为空");
        }

        if (StringUtils.isBlank(dto.getTeachmode())) {
            throw new XueChengPlusException("教育模式为空");
        }

        if (StringUtils.isBlank(dto.getUsers())) {
            throw new XueChengPlusException("适应人群为空");
        }

        if (StringUtils.isBlank(dto.getCharge())) {
            throw new XueChengPlusException("收费规则为空");
        }
        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(dto, courseBase);
        courseBase.setAuditStatus("202002");
        courseBase.setStatus("203001");
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        int insert = courseBaseMapper.insert(courseBase);
        if (insert <= 0) {
            throw new XueChengPlusException("新增课程基本信息失败");
        }
        // 向课程营销表保存课程营销信息
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto, courseMarket);
        courseMarket.setId(courseBase.getId());
        int i = saveCourseMarket(courseMarket);
        if (i <= 0) {
            throw new XueChengPlusException("新增课程营销信息失败");
        }
        return getCourseBaseInfo(courseBase.getId());
    }

    // 保存课程营销信息
    private int saveCourseMarket(CourseMarket courseMarket) {
        //收费规则
        String charge = courseMarket.getCharge();
        if (StringUtils.isBlank(charge)) {
            throw new XueChengPlusException("收费规则为空");
        }
        // 收费规则为收费检测价格
        if (charge.equals("201001")) {
            if (courseMarket.getPrice() == null || courseMarket.getPrice() <= 0) {
                throw new XueChengPlusException("课程的价格为空或小于0");
            }
        }
        CourseMarket courseMarketObj = courseMarketMapper.selectById(courseMarket.getId());
        if (courseMarketObj == null) {
            return courseMarketMapper.insert(courseMarket);
        } else {
            return courseMarketMapper.updateById(courseMarket);
        }
    }

    // 根据课程id查询课程信息，包括基本信息和和营销信息
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        // 查询分类名称
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());
        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        CourseBase courseBase = courseBaseMapper.selectById(editCourseDto.getId());
        if (courseBase == null) {
            throw new XueChengPlusException("课程不存在");
        }
        // 校验是否是本机构的课程
        if (!courseBase.getCompanyId().equals(companyId)) {
            throw new XueChengPlusException("课程不属于本机构");
        }
        BeanUtils.copyProperties(editCourseDto, courseBase);
        // 更新课程信息
        courseBase.setChangeDate(LocalDateTime.now());
        int update = courseBaseMapper.updateById(courseBase);
        // 更新营销信息
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        saveCourseMarket(courseMarket);
        // 查询最新的课程信息
        return getCourseBaseInfo(editCourseDto.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteCourse(Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            throw new XueChengPlusException("课程不存在");
        }
        if (courseBase.getStatus().equals("203001")) {
            throw new XueChengPlusException("课程的审核状态为非提交，不能删除");
        }
        // 删除课程基本信息
        courseBaseMapper.deleteById(courseId);
        // 删除课程营销信息
        courseMarketMapper.deleteById(courseId);
        // 删除课程计划
        teachplanMapper.delete(new QueryWrapper<Teachplan>().eq("course_id", courseId));
        // 删除课程计划关联信息
        // 删除课程师资
        courseTeacherMapper.delete(new QueryWrapper<CourseTeacher>().eq("course_id", courseId));
    }
}
