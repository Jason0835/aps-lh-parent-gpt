package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqRollingLogDetail;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 胎圈排程滚动更新日志明细Service接口
 *
 * @author APS
 */
public interface ITqRollingLogDetailService extends IDocService<TqRollingLogDetail> {

    /**
     * 根据主表ID查询日志明细列表
     *
     * @param logId 主表ID
     * @return 日志明细列表
     */
    List<TqRollingLogDetail> selectByLogId(Long logId);
}
