package com.xuecheng.media;


import com.xuecheng.media.service.IFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 大文件处理测试
 */
@SpringBootTest
@Slf4j
public class BigFileTest {

    @Autowired
    IFileStorageService fileStorageService;

    // 测试文件分块方法
    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("/Users/zhaohangyi/Documents/Code/JavaProject/xuecheng-plus/file/original.mov");
        String chunkPath = "/Users/zhaohangyi/Documents/Code/JavaProject/xuecheng-plus/file/chunk/"; //分块的保存路径？
        File chunkFolder = new File(chunkPath);
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }
        // 分块大小
        long chunkSize = 1024 * 1024 * 10;
        // 分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        log.info("分块数量为：{}", chunkNum);
        // 缓冲区大小
        byte[] b = new byte[1024];
        // 使用RandomAccessFile读写
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "rw");
        // 分块
        for (int i = 0; i < chunkNum; i++) {
            // 分块文件
            File file = new File(chunkPath + i);
            // 判断是否存在，存在则删除
            if (file.exists()) {
                file.delete();
            }
            boolean newFile = file.createNewFile();
            if (newFile) {
                // 向分块中写数据
                RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
                int len = -1;
                while ((len = raf_read.read(b)) != -1) {
                    raf_write.write(b, 0, len);
                    if (file.length() >= chunkSize) {
                        break;
                    }
                }
                raf_write.close();
                log.info("第{}个分块完成", i);
            }
        }
        raf_read.close();
    }

    // 测试文件合并方法
    @Test
    public void testMerge() throws IOException {
        // 块文件所在目录
        File chunkFolder = new File("/Users/zhaohangyi/Documents/Code/JavaProject/xuecheng-plus/file/chunk/");
        // 原始文件
        File orginalFile = new File("/Users/zhaohangyi/Documents/Code/JavaProject/xuecheng-plus/file/original.mov");
        // 合并文件
        File mergeFile = new File("/Users/zhaohangyi/Documents/Code/JavaProject/xuecheng-plus/file/merge.mov");
        // 判断合并文件是否存在，如果存在则删除
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        // 创建新的合并文件
        mergeFile.createNewFile();
        // 用于写文件
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
        // 指针指向文件顶端
        raf_write.seek(0);
        // 缓冲区
        byte[] b = new byte[1024];
        // 分块文件列表
        File[] fileArray = chunkFolder.listFiles();
        // 转成集合，便于排序
        List<File> fileList = Arrays.asList(fileArray);
        // 从小到大的排序
        fileList.sort((file1, file2) -> {
            if (Integer.parseInt(file1.getName()) < Integer.parseInt(file2.getName())) {
                return -1;
            }
            return 1;
        });
        // 合并文件
        for (File chunkFile : fileList) {
            RandomAccessFile raf_read = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_read.read(b)) != -1) {
                raf_write.write(b, 0, len);
            }
            raf_read.close();
        }
        raf_write.close();
        // 校验文件
        try (
                FileInputStream fileInputStream = new FileInputStream(orginalFile);
                FileInputStream mergeFileStream = new FileInputStream(mergeFile);
        ) {
            // 取出原始文件的md5
            String originalMd5 = DigestUtils.md5Hex(fileInputStream);
            // 取出合并文件的md5进行比较
            String mergeFileMd5 = DigestUtils.md5Hex(mergeFileStream);
            if (originalMd5.equals(mergeFileMd5)) {
                log.info("文件合并成功");
            } else {
                log.info("文件合并失败");
            }
        }

    }

    //将分块文件上传至oss
    @Test
    public void testUploadChunk() throws IOException {
        // 块文件所在目录
        File chunkFolder = new File("/Users/zhaohangyi/Documents/Code/JavaProject/xuecheng-plus/file/chunk/");
        // 分块文件列表
        File[] fileArray = chunkFolder.listFiles();
        // 将分块文件上传
        for (int i = 0; i < fileArray.length; i++) {
            try {
                File file = fileArray[i];
                Boolean b = fileStorageService.fileUpload((MultipartFile) file, "test/chunk/" + i);
                log.info("第{}个文件上传结果：{}", i, b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

