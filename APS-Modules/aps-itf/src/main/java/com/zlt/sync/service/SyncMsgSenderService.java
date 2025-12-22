package com.zlt.sync.service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mq.rabbit.property.MQConnectInfo;
import com.zlt.sync.configuration.QueueConfigDatas;
import com.zlt.sync.povo.QueueDataVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SyncMsgSenderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private QueueConfigDatas queueConfigDatas;

    /**
     * 发送给 crm 等
     * @param msg
     */
    public void send(String msg, String dataSys, String dockSys) {
        if (!StringUtils.isEmpty(dockSys) && dockSys.contains("_")) {
            String[] dockSysEs = dockSys.split("_");

            for (String dockSysOne : dockSysEs) {
                sendDockSys(msg, dataSys, dockSysOne);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {

                }
            }
        } else {
            sendDockSys(msg, dataSys, dockSys);
        }
    }

    private void sendDockSys(String msg, String dataSys, String dockSys) {
        String key = dataSys + "_" + dockSys;
        Map<String, QueueDataVO> queueDataMap = queueConfigDatas.getSendQueues();

        if (queueDataMap != null && queueDataMap.size() > 0) {

            if (!queueDataMap.containsKey(key)) {
                return;
            }

            QueueDataVO queueDataVO = queueDataMap.get(key);

            this.rabbitTemplate.convertAndSend(queueDataVO.getExchange(), queueDataVO.getKey(), msg);
        }
    }
}
