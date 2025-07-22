package com.xuecheng.media.service;


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
}
