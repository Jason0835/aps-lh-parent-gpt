package com.zlt.aps.dj.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.model.DjAdjustScheduleContext;

import java.util.Date;
import java.util.List;

/**
 * 垫胶排程调整引擎接口
 * <p>
 * 定义设计文档「垫胶排程调整算法设计.md」中所有调整操作的对外方法。
 * 包括：插单(2.)、调整(3.)、删除(4.)、发布(5.)、导入(6.)。
 * </p>
 *
 * @author zlt
 */
public interface IDjScheduleAdjustService {

    // ==================== 2. 插单 ====================

    /**
     * 插单 — 整体入口
     *
     * @param insertVO 插单数据
     * @return 操作结果
     */
    AjaxResult insertOrder(DjScheduleResult insertVO);

    /**
     * 确认插单（用户在前端弹窗点击"坚持执行"后调用）
     *
     * @param insertVO 插单数据
     * @return 操作结果
     */
    AjaxResult confirmInsertOrder(DjScheduleResult insertVO);

    // ==================== 3. 调整 ====================

    /**
     * 3.3 调量（统一入口）
     * 通过比较前端传值与原数据库值的差异自动判断增量或减量。
     *
     * @param adjustVO 调整数据（传回目标班次的新总量）
     * @return 操作结果
     */
    AjaxResult changeQty(DjScheduleResult adjustVO);

    /**
     * 3.5 转机台
     *
     * @param adjustVO 调整数据
     * @return 操作结果
     */
    AjaxResult changeMachine(DjScheduleResult adjustVO);

    // ==================== 4. 删除 ====================

    /**
     * 4. 删除
     *
     * @param ids 待删除记录ID数组
     * @return 操作结果
     */
    AjaxResult deleteByIds(Long[] ids);

    // ==================== 5. 发布 ====================

    /**
     * 5. 发布
     *
     * @param publishVO 发布数据
     * @return 操作结果
     */
    AjaxResult publish(DjScheduleResult publishVO);

    // ==================== 6. 导入 ====================

    /**
     * 6. 导入数据校验
     *
     * @param importList  导入数据列表
     * @param scheduleDate 排产日期
     * @param factoryCode  工厂编码
     * @return 操作结果（含成功/失败统计）
     */
    AjaxResult validateImportData(List<DjScheduleResult> importList, Date scheduleDate, String factoryCode);
}
