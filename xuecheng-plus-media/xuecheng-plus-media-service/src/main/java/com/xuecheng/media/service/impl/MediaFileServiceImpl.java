package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.IFileStorageService;
import com.xuecheng.media.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
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
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

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
        // 文件mimeType
        String mimeType = getMimeType(extension);
        // 文件的md5值
        String fileMd5 = getFileMd5(file);
        // 文件的默认路径
        String defaultFolderPath = getDefaultFolderPath();
        // 存储到minio的对象名
        String ObjectName = defaultFolderPath + fileMd5 + extension;
        // 将文件上传到minio
        Boolean b = fileStorageService.fileUpload(file, "test/");
        // 文件大小
        uploadFileParamsDto.setFileSize(file.getSize());
        // 将文件信息保存到数据库
        MediaFiles mediaFiles = addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, ObjectName);
        // 准备返回数据
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
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
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto dto, String objectName) {
        MediaFiles mediaFile = mediaFilesMapper.selectById(fileMd5);
        if (mediaFile == null) {
            mediaFile = new MediaFiles();
            BeanUtils.copyProperties(dto, mediaFile);
            mediaFile.setId(fileMd5);
            mediaFile.setFileId(fileMd5);
            mediaFile.setCompanyId(companyId);
            mediaFile.setUrl("/" + companyId + "/" + fileMd5 + "/" + objectName);
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
}
