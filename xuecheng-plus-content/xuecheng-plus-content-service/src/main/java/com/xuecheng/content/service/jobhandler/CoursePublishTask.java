package com.xuecheng.content.service.jobhandler;

import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {


    @Autowired
    CoursePublishService coursePublishService;


    // 任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("分片序号：{},分片总数：{},消息类型：course_publish", shardIndex, shardTotal);
        // 30 》 一次最多渠道的任务数量，60 》 一次任务调度执行的超时时间
        process(shardIndex, shardTotal, "course_publish", 30, 60);
    }


    /**
     * 课程发布任务处理
     *
     * @param mqMessage 消息
     * @return boolean true:处理成功，false处理失败
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        // 获取消息相关的业务信息
        String businessKey1 = mqMessage.getBusinessKey1();
        Long courseId = (long) Integer.parseInt(businessKey1);
        // 课程静态化
        generateCourseHtml(mqMessage, courseId);
        //课程索引
        saveCourseIndex(mqMessage, courseId);
        //课程缓存
        saveCourseCache(mqMessage, courseId);
        return false;
    }

    private void saveCourseCache(MqMessage mqMessage, Long courseId) {
        log.info("开始进行课程缓存，课程Id：{}", courseId);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveCourseIndex(MqMessage mqMessage, Long courseId) {
        log.info("保存课程索引信息，课程Id：{}", courseId);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 生成课程静态化页面并上传至文件系统
    private void generateCourseHtml(MqMessage mqMessage, Long courseId) {
        log.info("开始进行课程静态化，课程Id：{}", courseId);
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageOne = mqMessageService.getStageOne(mqMessage.getId());
        if (stageOne > 0) {
            log.info("课程静态化已处理直接返回，课程Id：{}", courseId);
            return;
        }
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file != null) {
            coursePublishService.uploadCourseHtml(courseId, file);
        }
        // 保存第一阶段状态
        mqMessageService.completedStageOne(mqMessage.getId());
    }
}
