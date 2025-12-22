package com.zlt.sync.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.sync.configuration.QueueConfigDatas;
import com.zlt.sync.constants.ParamConstants;
import com.zlt.sync.handle.CustomHandle;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.handle.SyncScheduleTask;
import com.zlt.sync.povo.SyncParamsVO;

@RestController
@RequestMapping(value = "/schedule")
public class SyncScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(SyncScheduleController.class);

    @Autowired
    private QueueConfigDatas queueConfigDatas;

    @Autowired
    private SyncScheduleTask syncScheduleTask;

    /**
     * 执行定时任务入口
     * @param scheduleKey
     * @return
     */
    @PostMapping(value = "/runSyncData")
    public AjaxResult runSyncData(@RequestParam("scheduleKey") String scheduleKey) {
        logger.info("runSyncData-001 执行定时任务 ,scheduleKey: " + scheduleKey);

        if (ParamConstants.SYNC_SUCCESSED_METHOD.equals(scheduleKey)) {
            syncScheduleTask.feedbackSyncdSucssDatas(ParamConstants.SEAR_TYPE_SUCCESSED_METHOD);
        }
        else if (ParamConstants.SYNC_SUCCESSED_CNT_METHOD.equals(scheduleKey)) {
            syncScheduleTask.feedbackSyncdSucssDatas(ParamConstants.SEAR_TYPE_SUCCESSED_CNT_METHOD);
        } else {
            List<String> syncKeys = queueConfigDatas.getScheduleSyncKeys(scheduleKey);
            if (CollectionUtils.isEmpty(syncKeys)) {
                logger.error("runSyncData-002 执行定时任务 ,根据 scheduleKey: " + scheduleKey + " 找到对应的接口配置，请检查 sync-data-xxx-{active}.yml 配置");
                return AjaxResult.error("根据 scheduleKey: " + scheduleKey + " 找到对应的接口配置，请检查 sync-data-xxx-{active}.yml 配置");
            }

            String syncKeysStr = org.apache.commons.lang.StringUtils.join(syncKeys, ",");

            logger.info("runSyncData-001 执行定时任务 ,scheduleKey: " + scheduleKey + "; syncKeys: " + syncKeysStr);
            syncScheduleTask.commonSyncData(syncKeysStr);
        }
        return AjaxResult.success();
    }

    /**
     * 自定义请求方法
     * {"methodFrom":"syncKeys", syncKeys:"xxx,xxxx", bean: "sapHandle"}
     * - methodFrom: syncKeys 通过 syncKeys 获取需要请求或通知的接口
     * - methodFrom: custom, method: "methodName", 需要 bean参数
     *   需要实现 CustomHandle, bean 需要添加到 spring.factories
     *   类创建在 handle/custom 下
     * @param params
     * @return
     */
    @PostMapping(value = "/runSyncHandle")
    public AjaxResult runSyncHandle(@RequestParam("params") String params) {
        if (StringUtils.isEmpty(params)) {
            logger.error("requestNotice-001 请求数据入口, 传入请求或通知方法的参数为 null;");
            return AjaxResult.error("传入请求数据入口的参数为 null");
        }
        logger.info("requestNotice-002 请求数据入口, params:" + params);

        try {
            JSONObject jsonObject = JSONObject.parseObject(params);
            if (!jsonObject.containsKey(ParamConstants.CUSTOM_KEY_METHOD_FROM)) {
                logger.error("requestNotice-003 请求数据入口, 传入请求或通知方法的参数 不包括 methodFrom 字段;");
                return AjaxResult.error("传入请求数据入口方法的参数 不包括 methodFrom 字段;");
            }

            if (ParamConstants.CUSTOM_VALUE_METHOD_FROM_SYNCKEYS.equals(jsonObject.getString(ParamConstants.CUSTOM_KEY_METHOD_FROM))) {
                if (!jsonObject.containsKey(ParamConstants.CUSTOM_KEY_SYNC_KEYS)) {
                    logger.error("requestNotice-004 请求数据入口, 传入请求或通知方法的参数 不包括 syncKeys 字段;");
                    return AjaxResult.error("传入请求数据入口方法的参数 不包括 syncKeys 字段;");
                }
                String syncKeys = jsonObject.getString(ParamConstants.CUSTOM_KEY_SYNC_KEYS);

                logger.info("requestNotice-005 请求数据入口, 需要执行数据请求的接口有: " + syncKeys);
                recordSyncRequests(syncKeys);
            } else if (ParamConstants.CUSTOM_VALUE_METHOD_FROM_CUSTOM.equals(jsonObject.getString(ParamConstants.CUSTOM_KEY_METHOD_FROM))) {
                // 根据 method 字段指定的方法，转到指定方法执行
                customHandle(jsonObject);
            }
        } catch (Exception ex) {
            logger.error("requestNotice-006 请求数据入口, 发生异常, 信息: " + ex.getMessage(), ex);
            return AjaxResult.error("传入请求数据入口方法发生异常, 信息: " + ex.getMessage());
        }

        return AjaxResult.success();
    }

    /**
     * 执行自定义请求方法
     * {"methodFrom":"syncKeys", syncKeys:"xxx,xxxx", bean: "sapHandle"}
     *      * - methodFrom: syncKeys 通过 syncKeys 获取需要请求或通知的接口
     *      * - methodFrom: custom, method: "methodName", 需要 bean参数
     *      *   需要实现 CustomHandle, bean 需要添加到 spring.factories
     *      *   类创建在 handle/custom 下
     * @param jsonObject
     * @throws Exception
     */
    private void customHandle(JSONObject jsonObject) throws Exception {
        logger.info("customHandle-custom-001 执行自定义方法");
        if (!jsonObject.containsKey(ParamConstants.CUSTOM_KEY_BEAN) || !jsonObject.containsKey(ParamConstants.CUSTOM_KEY_METHOD)) {
            throw new Exception("缺少所需参数: bean, methodName");
        }

        logger.info("customHandle-custom-001 执行自定义方法, 参数: " + jsonObject.toJSONString());

        try {
            CustomHandle customHandle = SpringUtils.getBean(jsonObject.getString(ParamConstants.CUSTOM_KEY_BEAN));
            customHandle.handle(jsonObject);
        } catch (Exception ex) {
            logger.error("customHandle-custom-001 执行自定义方法, 发生异常: " + ex.getMessage(), ex);
        }
    }

    /**
     * 调用请求方法
     * @param syncKeys
     */
    private void recordSyncRequests(String syncKeys) {
        if (StringUtils.isEmpty(syncKeys)) {
            logger.error("recordSyncRequests-001 调用请求方法, 需要调用的接口为空");
            return;
        }

        String[] syncKeyArr = syncKeys.split(",");
        SyncDataHandle handle = SpringUtils.getBean(SyncDataHandle.class);
        for (String syncKey: syncKeyArr) {
            SyncParamsVO paramsVO = new SyncParamsVO();
            paramsVO.setSyncKey(syncKey);

            handle.syncRequest(paramsVO);
        }
    }

    /**
     * 单个接口请求，不带参数
     * @param syncKey
     * @return
     */
    @PostMapping(value = "/syncRequest")
    public  AjaxResult syncRequest(@RequestParam("syncKey") String syncKey) {
        SyncParamsVO paramsVO = new SyncParamsVO();
        paramsVO.setSyncKey(syncKey);

        SyncDataHandle handle = SpringUtils.getBean(SyncDataHandle.class);
        handle.syncRequest(paramsVO);

        return AjaxResult.success();
    }

    /**
     * 获取版本号
     * @param syncKey
     * @return
     */
    @GetMapping(value = "/getDataVersion")
    public AjaxResult getDataVersion(@RequestParam("syncKey") String syncKey) {
        logger.info("getDataVersion-001 获取 syncKey: " + syncKey + " 的版本号");
        SyncDataHandle handle = SpringUtils.getBean(SyncDataHandle.class);
        return AjaxResult.success(handle.getDataVersion(syncKey));
    }
}
