package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;

/**
 * 直裁排程结果发布业务编排服务。
 *
 * <p>负责：参数校验 → 预过滤 → 置 RELEASING → 抢锁 → MES 下发（指数退避重试）→ 状态回写 → 失败明细落日志。
 * Controller 仅做 HTTP 层解析，全部业务下沉到本服务。</p>
 *
 * @author APS
 */
public interface Cd90ScheduleResultPublishService {

    /**
     * 发布直裁排程结果到 MES。
     *
     * @param dto 包含 scheduleDate、factoryCode 的请求体
     * @param ids 选中记录 ID 列表（逗号分隔），为空时按日期全量发布
     * @return 发布结果
     */
    AjaxResult publish(Cd90ScheduleResult dto, String ids);
}
