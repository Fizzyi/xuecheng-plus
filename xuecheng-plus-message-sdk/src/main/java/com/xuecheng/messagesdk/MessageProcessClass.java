package com.xuecheng.messagesdk;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class MessageProcessClass extends MessageProcessAbstract {


    @Autowired
    MqMessageService mqMessageService;

    // 执行任务
    @Override
    public boolean execute(MqMessage mqMessage) {
        Long id = mqMessage.getId();
        log.info("执行任务，消息id：{}", id);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //取出阶段状态
        int stageOne = mqMessageService.getStageOne(id);
        if (stageOne < 1) {
            log.info("消息id：{}，阶段状态：{}，开始执行第一阶段任务", id, stageOne);
            int i = mqMessageService.completedStageOne(id);
            if (i > 0) {
                log.info("消息id：{}，阶段状态：{}，第一阶段任务完成", id, stageOne);
            }
        } else {
            log.info("消息id：{}，阶段状态：{}，无需执行第一阶段任务", id, stageOne);
        }
        return true;
    }
}
