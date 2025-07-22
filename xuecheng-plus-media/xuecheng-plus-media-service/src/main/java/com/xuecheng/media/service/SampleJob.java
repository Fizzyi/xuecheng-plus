package com.xuecheng.media.service;


import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SampleJob {


    @XxlJob("testJob")
    public void testJob() throws Exception {
        log.info("开始执行.....");
    }

    /**
     * 分片参数
     *
     * @throws Exception
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        log.info("当前分片参数：当前分片索引={}, 总分片数={}", shardIndex, shardTotal);
        log.info("开始执行第" + shardIndex + "批任务");
    }
}
