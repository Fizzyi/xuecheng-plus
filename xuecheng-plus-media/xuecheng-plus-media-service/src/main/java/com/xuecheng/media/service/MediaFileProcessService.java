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
}
