package com.xuecheng.media.service.impl;

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
        MediaFiles mediaFiles = mediaFilesMapper.selectById(DigestUtils.md5Hex(fileMd5 + chunkIndex));
        if (mediaFiles != null) {
            // 判断文件是否存在
            return RestResponse.success(true);
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, MultipartFile file) {
//        //得到分块文件的目录路径
//        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
//        //得到分块文件的路径
//        String chunkFilePath = chunkFileFolderPath + chunk;
        //将文件存储至oss
        String fileKey = "test/" + fileMd5 + "/" + chunk;
        log.info("分块序号：{},路径：{}", chunk, fileKey);
        fileStorageService.fileUpload(file, fileKey);
        UploadFileParamsDto dto = new UploadFileParamsDto();
        dto.setFilename(fileKey);
        String chunkFileMd5 = DigestUtils.md5Hex(fileMd5 + chunk);
        // 将文件信息保存到数据库
        MediaFiles mediaFiles = addMediaFilesToDb(0L, chunkFileMd5, dto, "", fileKey);
        return RestResponse.success(true);
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
