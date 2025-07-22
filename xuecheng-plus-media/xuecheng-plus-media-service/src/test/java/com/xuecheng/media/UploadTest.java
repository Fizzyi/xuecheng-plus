package com.xuecheng.media;


import com.xuecheng.media.service.IFileStorageService;
import com.xuecheng.media.service.MediaFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
public class UploadTest {


    IFileStorageService fileStorageService;


    public MediaFileService mediaFileService;

    @Test
    public void test() {
        MultipartFile file = null;
        Boolean b = fileStorageService.fileUpload(file, "/test/");
    }
}
