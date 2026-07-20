package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;

/** 斜裁排程结果发布业务编排服务。 */
public interface Cd15ScheduleResultPublishService {

    /**
     * 发布选中的斜裁排程结果到 MES。
     *
     * @param request 发布条件
     * @param ids 选中主键，逗号分隔
     * @return 发布结果
     */
    AjaxResult publish(Cd15ScheduleResult request, String ids);
}
