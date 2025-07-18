package com.xuecheng.media.service.impl;
import org.springframework.mock.web.MockMultipartFile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.IFileStorageService;
import com.xuecheng.media.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    IFileStorageService fileStorageService;

    @Override
    public PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, MultipartFile file) {
        // 文件名称
        String filename = uploadFileParamsDto.getFilename();
        // 文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        // 文件的md5值
        String fileMd5 = getFileMd5(file);
        // 文件的默认路径
        String defaultFolderPath = getDefaultFolderPath();
        // 存储到minio的对象名
        String ObjectName = defaultFolderPath + fileMd5 + extension;
        // 将文件上传到minio
        String fileKey = "test/" + generateFileName(filename);
        fileStorageService.fileUpload(file, fileKey);
        // 文件大小
        uploadFileParamsDto.setFileSize(file.getSize());
        // 将文件信息保存到数据库
        MediaFiles mediaFiles = addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, ObjectName, fileKey);
        // 准备返回数据
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }

    /**
     * 检查文件是否存在
     *
     * @param fileMd5 文件的md5
     * @return
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        // 查询文件信息
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            // 判断文件是否存在
            return RestResponse.success(true);
        }
        return RestResponse.success(false);
    }

    /**
     * 检查块是否存在
     *
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        String uploadDir = "uploads/" + fileMd5 + "/" + chunkIndex;
        File file = new File(uploadDir);
        return RestResponse.success(file.exists());

    }

    @Override
    public RestResponse<Boolean> uploadChunk(String fileMd5, int chunk, MultipartFile file) {
        // 构建保存文件的路径，这里假设以文件MD5值和分块序号构建路径
        String uploadDir = "uploads/" + fileMd5 + "/" + chunk;
        Path uploadPath = Paths.get(uploadDir);

        try {
            // 创建目录，如果父目录不存在也一同创建
            Files.createDirectories(uploadPath);

            // 构建完整的文件路径
            Path filePath = uploadPath;

            // 将MultipartFile保存到指定路径
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件块 {} 保存成功，路径为: {}", chunk, filePath);
            return RestResponse.success(true);

        } catch (IOException e) {
            log.error("保存文件块 {} 时出错", chunk, e);
            return RestResponse.validfail("文件块保存失败");
        }
    }

    @Override
    public RestResponse<String> mergeChunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        // 分块文件存储路径
        String chunkFileFolderPath = "uploads/" + fileMd5 + "/";
        // 合并后的文件名
        String fileName = uploadFileParamsDto.getFilename();
        String extName = fileName.substring(fileName.lastIndexOf("."));
        // 合并后的文件路径
        String mergeFilePath = "uploads/" + fileMd5 + extName;

        // 1. 创建合并后的文件
        File mergeFile = new File(mergeFilePath);
        try (RandomAccessFile rafWrite = new RandomAccessFile(mergeFile, "rw")) {
            // 2. 依次读取每个分块文件，写入合并文件
            for (int i = 0; i < chunkTotal; i++) {
                File chunkFile = new File(chunkFileFolderPath + i);
                if (!chunkFile.exists()) {
                    return RestResponse.validfail("分块文件缺失：" + i);
                }
                try (RandomAccessFile rafRead = new RandomAccessFile(chunkFile, "r")) {
                    byte[] buffer = new byte[1024 * 1024];
                    int len;
                    while ((len = rafRead.read(buffer)) != -1) {
                        rafWrite.write(buffer, 0, len);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RestResponse.validfail("合并文件失败：" + e.getMessage());
        }
//        // 3.上传到oss
        try (FileInputStream fis = new FileInputStream(mergeFile)) {
            MultipartFile multipartFile = new MockMultipartFile(
                    mergeFile.getName(), // 文件名
                    mergeFile.getName(), // 原始文件名
                    null,                // contentType
                    fis                  // 文件流
            );
            uploadFile(companyId, uploadFileParamsDto, multipartFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        4. 删除分块文件（可选）
        for (int i = 0; i < chunkTotal; i++) {
            File chunkFile = new File(chunkFileFolderPath + i);
            chunkFile.delete();
        }

        // 5. 返回成功
        return RestResponse.success("合并成功");
    }

    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/chunk/";
    }

    /**
     * 保存文件信息到数据库
     *
     * @param companyId
     * @param fileMd5
     * @param dto
     * @param objectName
     * @return
     */
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto dto, String objectName, String fileKey) {
        MediaFiles mediaFile = mediaFilesMapper.selectById(fileMd5);
        if (mediaFile == null) {
            mediaFile = new MediaFiles();
            BeanUtils.copyProperties(dto, mediaFile);
            mediaFile.setId(fileMd5);
            mediaFile.setFileId(fileMd5);
            mediaFile.setCompanyId(companyId);
            mediaFile.setUrl(fileKey);
            mediaFile.setFilePath(objectName);
            mediaFile.setCreateDate(LocalDateTime.now());
            mediaFile.setAuditStatus("002003");
            mediaFile.setStatus("1");
            int insert = mediaFilesMapper.insert(mediaFile);
            if (insert != 1) {
                throw new XueChengPlusException("保存文件信息失败");
            }
            log.info("保存文件信息成功，文件id：{}", mediaFile.getId());
        }
        return mediaFile;
    }


    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date()).replace("-", "/") + "/";
    }

    private String getFileMd5(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return DigestUtils.md5Hex(inputStream);
        } catch (IOException e) {
            return null;
        }
    }


    private String getMimeType(String extension) {
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }


    private String generateFileName(String originalFileName) {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return uuid + "." + FilenameUtils.getExtension(originalFileName);
    }
}
