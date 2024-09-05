package com.zlt.kettle.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.common.utils.WebClientUtils;
import com.zlt.kettle.api.domain.TaskInfo;
import com.zlt.kettle.mapper.KettleTaskInfoMapper;
import com.zlt.kettle.service.KettleTaskInfoService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.kettle.scheduler.common.povo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class KettleTaskInfoServiceImpl implements KettleTaskInfoService {

    @Autowired
    KettleTaskInfoMapper kettleTaskInfoMapper;

    @Autowired
    WebClientUtils webClientUtils;

    /**
     * 读取任务信息
     *
     * @param taskInfo
     * @return
     */
    @Override
    public List<TaskInfo> getTaskInfoList(TaskInfo taskInfo) {
        return kettleTaskInfoMapper.getTaskInfoList(taskInfo);
    }

    /**
     * 执行一个任务
     *
     * @param id       String jobUri = "sys/job/startJob.do?id=";
     *                 String transUri = "/sys/trans/startTrans.do?id=";
     * @param taskType
     * @return
     */
    @Override
    public Result runOneTaskInfo(Integer id, String taskType) {

        TaskInfo taskInfo = kettleTaskInfoMapper.selectOneRecord(id, taskType);
        String uri = taskInfo.getRemoteUrl();

        Optional<Result> result;
        result = webClientUtils.getRequest().getClient(uri, Result.class).blockOptional();

        if (!result.isPresent() ||
                !result.get().isSuccess()) {
            log.error(I18nUtil.getMessage("common.webclient.invoke.kettle.fail"), result.get());
        }
        Result output = result.get();
        return output;
    }
}
