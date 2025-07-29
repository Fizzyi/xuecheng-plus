package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
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
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
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

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    private MediaServiceClient mediaServiceClient;

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

    @Transactional
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

    @Transactional
    @Override
    public void coursePublish(Long companyId, Long courseId) {
        /*
        功能：
        1. 向课程发布表course_publish插入一条记录,记录来源于课程预发布表，如果存在则更新，发布状态为：已发布。
        2. 更新course_base表的课程发布状态为：已发布
        3. 删除课程预发布表的对应记录。
        4. 向mq_message消息表插入一条消息，消息类型为：course_publish
        约束：
        1. 课程审核通过方可发布。
        2. 本机构只允许发布本机构的课程。
         */
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程预发布不存在");
        }
        if (!coursePublishPre.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许提交其他机构的课程");
        }
        if (!coursePublishPre.getStatus().equals("202004")) {
            XueChengPlusException.cast("课程审核未通过，不能发布");
        }
        // 保存课程发布信息
        saveCoursePublish(coursePublishPre);
        // 保存消息表
        saveCoursePublishMessage(courseId);
        // 删除课程预发布表对应记录
        coursePublishPreMapper.deleteById(courseId);
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        File htmlFile = null;
        try {
            Configuration configuration = new Configuration(Configuration.getVersion());
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            configuration.setDefaultEncoding("UTF-8");
            // 指定模板文件
            Template template = configuration.getTemplate("course_template.ftl");
            //准备数据
            CoursePreviewDto coursePreviewDto = getCoursePreview(courseId);
            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewDto);
            //静态化
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            // 将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);
            // 创建静态化文件
            htmlFile = File.createTempFile("course", "html");
            log.info("课程静态化，生成静态化文件：{}", htmlFile.getAbsolutePath());
            // 输出流
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            log.info("生成课程静态化页面失败", e);
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
        return htmlFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.uploadFile(multipartFile, "course/" + courseId + ".html");
        if (StringUtils.isEmpty(course)) {
            XueChengPlusException.cast("上传课程静态化页面失败");
        }
    }

    /**
     * 保存消息表
     *
     * @param courseId 课程Id
     */
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage coursePublish = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (coursePublish == null) {
            XueChengPlusException.cast("保存消息表失败");
        }
    }

    /**
     * 保存课程发布信息
     *
     * @param coursePublishPre 预检测
     */
    private void saveCoursePublish(CoursePublishPre coursePublishPre) {
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        coursePublish.setStatus("203002");
        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(coursePublishPre.getId());
        if (coursePublishUpdate != null) {
            coursePublishMapper.updateById(coursePublish);
        } else {
            coursePublishMapper.insert(coursePublish);
        }
        // 更新课程基本信息表的发布状态
        CourseBase courseBase = courseBaseMapper.selectById(coursePublishPre.getId());
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);
    }
}
