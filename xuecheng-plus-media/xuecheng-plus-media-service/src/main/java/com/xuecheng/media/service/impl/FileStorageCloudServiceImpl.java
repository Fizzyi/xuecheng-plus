package com.xuecheng.media.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;


import com.amazonaws.services.s3.model.S3Object;
import com.xuecheng.media.config.FileCloudConfig;
import com.xuecheng.media.service.IFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static software.amazon.ion.impl.PrivateIonConstants.False;

/**
 * 云计算 实现
 *
 * @Author 1024创新实验室: 罗伊
 * @Date 2019年10月11日 15:34:47
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright 1024创新实验室 （ https://1024lab.net ）
 */
@Service
@Slf4j
public class FileStorageCloudServiceImpl implements IFileStorageService {

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private FileCloudConfig cloudConfig;

    /**
     * 自定义元数据 文件名称
     */
    private static final String USER_METADATA_FILE_NAME = "file-name";

    /**
     * 自定义元数据 文件格式
     */
    private static final String USER_METADATA_FILE_FORMAT = "file-format";

    /**
     * 自定义元数据 文件大小
     */
    private static final String USER_METADATA_FILE_SIZE = "file-size";

    @Override
    public Boolean fileUpload(MultipartFile file, String fileKey) {
        // 设置文件 key
        String originalFilename = file.getOriginalFilename();
        String fileType = FilenameUtils.getExtension(originalFilename);
        // 文件名称 URL 编码
        String urlEncoderFilename;
        try {
            urlEncoderFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            log.error("阿里云文件上传服务URL ENCODE-发生异常：", e);
            return false;
        }
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentEncoding(StandardCharsets.UTF_8.name());
        meta.setContentDisposition("attachment;filename=" + urlEncoderFilename);
        Map<String, String> userMetadata = new HashMap(10);
        userMetadata.put(USER_METADATA_FILE_NAME, urlEncoderFilename);
        userMetadata.put(USER_METADATA_FILE_FORMAT, fileType);
        userMetadata.put(USER_METADATA_FILE_SIZE, String.valueOf(file.getSize()));
        meta.setUserMetadata(userMetadata);
        meta.setContentLength(file.getSize());
        meta.setContentType(this.getContentType(fileType));
        try {
            log.info("上传日志：{},{},{}", cloudConfig.getBucketName(), fileKey, meta);
            amazonS3.putObject(cloudConfig.getBucketName(), fileKey, file.getInputStream(), meta);
        } catch (IOException e) {
            log.error("文件上传-发生异常：", e);
            return false;
        }
        return true;
    }

    @Override
    public Boolean fileUpload(File file, String path) {
        String originalFilename = file.getName();
        String fileType = FilenameUtils.getExtension(originalFilename);
        String urlEncoderFilename;
        try {
            urlEncoderFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            log.error("阿里云文件上传服务URL ENCODE-发生异常：", e);
            return false;
        }

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentEncoding(StandardCharsets.UTF_8.name());
        meta.setContentDisposition("attachment;filename=" + urlEncoderFilename);

        Map<String, String> userMetadata = new HashMap<>(10);
        userMetadata.put(USER_METADATA_FILE_NAME, urlEncoderFilename);
        userMetadata.put(USER_METADATA_FILE_FORMAT, fileType);
        userMetadata.put(USER_METADATA_FILE_SIZE, String.valueOf(file.length()));
        meta.setUserMetadata(userMetadata);
        meta.setContentLength(file.length());
        meta.setContentType(this.getContentType(fileType));

        try (FileInputStream fis = new FileInputStream(file)) {
            log.info("上传日志：{},{},{}", cloudConfig.getBucketName(), path, meta);
            amazonS3.putObject(cloudConfig.getBucketName(), path, fis, meta);
        } catch (IOException e) {
            log.error("文件上传-发生异常：", e);
            return false;
        }
        return true;
    }

    /**
     * 获取文件url
     *
     * @param fileKey
     * @return
     */
    @Override
    public String getFileUrl(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            return "";
        }
        return cloudConfig.getPublicUrl() + fileKey;

    }

    /**
     * 流式下载（名称为原文件）
     */
    @Override
    public File fileDownload(String key) {
        File file = null;
        //获取oss对象
        S3Object s3Object = amazonS3.getObject(cloudConfig.getBucketName(), key);
        // 获取文件 meta
//        FileMetadataVO metadataDTO = null;
//        if (userMetadata != null) {
//            metadataDTO = new FileMetadataVO();
//            metadataDTO.setFileFormat(userMetadata.get(USER_METADATA_FILE_FORMAT));
//            metadataDTO.setFileName(userMetadata.get(USER_METADATA_FILE_NAME));
//            String fileSizeStr = userMetadata.get(USER_METADATA_FILE_SIZE);
//            Long fileSize = StringUtils.isBlank(fileSizeStr) ? null : Long.valueOf(fileSizeStr);
//            metadataDTO.setFileSize(fileSize);
//        }
        // 获得输入流
        InputStream stream = s3Object.getObjectContent();
        try {
            file = File.createTempFile("minio", "temp");
            FileOutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(stream, outputStream);
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                stream.close();
                s3Object.close();
            } catch (IOException e) {
                log.error("文件下载-发生异常：", e);
            }
        }
    }

//        try {
//            // 输入流转换为字节流
//            byte[] buffer = FileCopyUtils.copyToByteArray(objectContent);
//
//            FileDownloadVO fileDownloadVO = new FileDownloadVO();
//            fileDownloadVO.setData(buffer);
//            fileDownloadVO.setMetadata(metadataDTO);
//            return ResponseDTO.ok(fileDownloadVO);
//        } catch (IOException e) {
//            log.error("文件下载-发生异常：", e);
//            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "下载失败");
//        } finally {
//            try {
//                // 关闭输入流
//                objectContent.close();
//                s3Object.close();
//            } catch (IOException e) {
//                log.error("文件下载-发生异常：", e);
//            }
//        }


/**
 * 根据文件夹路径 返回对应的访问权限
 *
 * @param fileKey
 * @return
 */
//    private CannedAccessControlList getACL(String fileKey) {
//        // 公用读
//        if (fileKey.contains(FileFolderTypeEnum.FOLDER_PUBLIC)) {
//            return CannedAccessControlList.PublicRead;
//        }
//        // 其他默认私有读写
//        return CannedAccessControlList.Private;
//    }

//    /**
//     * 单个删除文件
//     * 根据 file key 删除文件
//     * ps：不能删除fileKey不为空的文件夹
//     *
//     * @param fileKey 文件or文件夹
//     * @return
//     */
//    @Override
//    public ResponseDTO<String> delete(String fileKey) {
//        amazonS3.deleteObject(cloudConfig.getBucketName(), fileKey);
//        return ResponseDTO.ok();
//    }
    @Override
    public Long cacheExpireSecond() {
        return cloudConfig.getUrlExpire() - 1800;
    }

}