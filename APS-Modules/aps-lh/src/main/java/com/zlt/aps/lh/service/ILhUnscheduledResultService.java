package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;

/**
 * 硫化未排产结果服务接口
 *
 * @author APS
 */
public interface ILhUnscheduledResultService extends IDocService<LhUnscheduledResult> {

    /**
     * 根据排程日期和工厂删除未排产结果
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编号
     * @return 删除记录数
     */
    int deleteByDateAndFactory(Date scheduleDate, String factoryCode);
}
