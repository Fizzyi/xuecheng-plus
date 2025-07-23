package com.xuecheng.media.service;


import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

/**
 * 媒体资源处理业务方法
 */
public interface MediaFileProcessService {

    /**
     * 获取待处理任务
     *
     * @param shardTotal 分片总数
     * @param shardIndex 分片序号
     * @param count      任务数
     * @return 待处理任务列表
     */
    public List<MediaProcess> getMediaProcessList(int shardTotal, int shardIndex, int count);


    /**
     * 开启一个任务
     *
     * @param id 任务Id
     * @return true开启成功，false开启失败
     */
    public boolean startTask(Long id);

    /**
     * 保存任务结果
     *
     * @param taskId   任务Id
     * @param status   任务状态
     * @param fileId   文件Id
     * @param url      文件访问路径
     * @param errorMsg 错误信息
     */
    void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg);
}
