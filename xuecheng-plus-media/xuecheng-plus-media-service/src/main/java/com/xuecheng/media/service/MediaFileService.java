package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface MediaFileService {

    /**
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @description 媒资文件查询方法
     * @author Mr.M
     * @date 2022/9/10 8:57
     */
    public PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);


    /**
     * 上传文件
     *
     * @param companyId           机构Id
     * @param uploadFileParamsDto 上传文件信息
     * @param file                文件磁盘路径
     * @param objectName
     * @return 文件信息
     */
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, MultipartFile file,String objectName);

    /**
     * 检查文件是否存在
     *
     * @param fileMd5 文件的md5
     * @return true:存在 false:不存在
     */
    public RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * 检查分块是否存在
     *
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return true:存在 false:不存在
     */
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    /**
     * 上传分块
     *
     * @param fileMd5   文件md5
     * @param chunk     分块序号
     * @param chunkFile 分块文件本地路径
     * @return RestResponse
     */
    public RestResponse<Boolean> uploadChunk(String fileMd5, int chunk, MultipartFile chunkFile);


    /**
     * 合并分块
     *
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param chunkTotal          分块总数
     * @param uploadFileParamsDto 上传文件信息
     * @return RestResponse
     */
    public RestResponse<String> mergeChunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);


    /**
     * 根据媒资id查询文件信息
     *
     * @param mediaId 媒资id
     * @return 媒资信息
     */
    MediaFiles getFileById(String mediaId);
}
