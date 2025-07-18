package com.xuecheng.media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {
    @Value("${file.storage.cloud.endpoint}")
    private String ALIYUN_OSS_ENDPOINT;
    @Value("${file.storage.cloud.access-key}")
    private String ALIYUN_OSS_ACCESSKEYID;
    @Value("${file.storage.cloud.secret-key}")
    private String ALIYUN_OSS_ACCESSKEYSECRET;

}
