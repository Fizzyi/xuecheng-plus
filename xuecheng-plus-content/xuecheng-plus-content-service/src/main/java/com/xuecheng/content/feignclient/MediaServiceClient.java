package com.xuecheng.content.feignclient;


import com.xuecheng.content.config.MultipartSupportConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒资管理服务远程接口
 */
@FeignClient(value = "media-api", configuration = MultipartSupportConfig.class)
public interface MediaServiceClient {

    @RequestMapping(value = "/media/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadFile(@RequestPart("file") MultipartFile upload, @RequestParam(value = "objectName", required = false) String objectName);
}
