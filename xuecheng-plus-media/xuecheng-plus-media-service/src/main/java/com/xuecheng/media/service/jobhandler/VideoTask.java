package com.xuecheng.media.service.jobhandler;


import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.IFileStorageService;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VideoTask {

    @Autowired
    private MediaFileProcessService mediaFileProcessService;

    @Autowired
    private IFileStorageService fileStorageService;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardTotal = XxlJobHelper.getShardTotal();
        int shardIndex = XxlJobHelper.getShardIndex();
        List<MediaProcess> mediaProcessList;
        int size = 0;
        try {
            //取出cpu核心数作为一次处理数据的条数
            int processors = Runtime.getRuntime().availableProcessors();
            //一次处理视频数量不要超过cpu数
            mediaProcessList = mediaFileProcessService.getMediaProcessList(shardTotal, shardIndex, processors);
            size = mediaProcessList.size();
            log.info("取出待处理视频任务{}条", size);
            if (size <= 0) {
                return;
            }
        } catch (Exception e) {
            log.error("查询视频处理列表失败");
            e.printStackTrace();
            throw e;
        }
        //启动size个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将处理任务加入到线程池
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(
                    () -> {
                        try {
                            // 任务Id
                            Long taskId = mediaProcess.getId();
                            // 抢占任务
                            boolean b = mediaFileProcessService.startTask(taskId);
                            if (!b) {
                                log.error("抢占任务失败");
                                return;
                            }
                            log.info("开始执行任务：{}", mediaProcess);

                            // 下边是处理逻辑
                            // (1) 文件下载到本地
                            File originalFile = fileStorageService.fileDownload(mediaProcess.getUrl());
                            if (originalFile == null) {
                                log.error("待处理文件下载失败，originalFile:{}", mediaProcess.getUrl());
                                mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "下载待处理文件失败");
                                return;
                            }

                            // (2) 处理下载的文件
                            File mp4File = null;
                            try {
                                mp4File = File.createTempFile("mp4", ".mp4");
                            } catch (Exception e) {
                                log.error("创建mp4临时文件失败");
                                mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "创建mp4临时文件失败");
                                return;
                            }

                            // (3) 视频处理结果
                            String result = null;
                            try {
                                //开始处理视频
                                Mp4VideoUtil videoUtil = new Mp4VideoUtil("/opt/homebrew/bin/ffmpeg", originalFile.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
                                result = videoUtil.generateMp4();
                            } catch (Exception e) {
                                log.info("处理视频文件：{},出错：{}", mediaProcess.getUrl(), e);
                            }
                            if (!result.equals("success")) {
                                //记录错误信息
                                log.info("处理视频失败，视频地址：{},错误信息：{}", mediaProcess.getUrl(), result);
                                mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, result);
                                return;
                            }
                            // (4) 上传视频
                            Boolean b1 = fileStorageService.fileUpload(mp4File, "test/" + mediaProcess.getFileId() + ".mp4");
                            if (b1) {
                                log.info("上传视频成功，视频地址：{}", mediaProcess.getUrl());
                                mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", mediaProcess.getFileId(), "test/" + mediaProcess.getFileId() + ".mp4", null);
                            } else {
                                log.info("上传视频失败，视频地址：{}", mediaProcess.getUrl());
                                mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "上传视频失败");
                            }
                        } catch (Exception e) {
                            log.error("处理视频失败");
                            e.printStackTrace();
                        } finally {
                            countDownLatch.countDown();
                        }
                    }
            );
        });
        countDownLatch.await(30, TimeUnit.MINUTES);
    }
}
