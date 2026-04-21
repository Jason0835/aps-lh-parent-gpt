package com.zlt.sync.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.zlt.aps.itf.vo.SyncDataLogs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 同步日志 远程同步接口
 *
 * @author zlt
 */
@FeignClient(contextId = "ISyncDataLogsApiService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface ISyncDataLogsApiService {
	
	/**
	 * 获取数据版本
	 * @param syncKey
	 * @return
	 */
    @PostMapping("/syncDataLogs/getDataVersion/{syncKey}")
	String getDataVersion(@PathVariable("syncKey") String syncKey);

    /**
     * 获取同步日志的反馈状态
     *
     * @param dataVersion 数据版本
     * @return 结果
     */
    @PostMapping("/syncDataLogs/getSyncDataResult/{dataVersion}")
    public SyncDataLogs getSyncDataResult(@PathVariable("dataVersion") String dataVersion);

    /**
     * 获取请求日志的反馈状态
     *
     * @param dataVersion 数据版本
     * @return 结果
     */
    @PostMapping("/syncDataLogs/getReqDataResult/{dataVersion}")
    public SyncDataLogs getReqDataResult(@PathVariable("dataVersion") String dataVersion);

    /**
     * 检查待发布排程记录是否已被锁定
     *
     * @param lockKey    锁key
     * @param publishIds 待发布记录ID
     * @return 结果
     */
    @PostMapping("/syncDataLogs/getReqDataResult/{lockKey}/{publishIds}")
    public boolean checkPublishLocking(@PathVariable("lockKey") String lockKey, @PathVariable("publishIds") Long[] publishIds);
}
