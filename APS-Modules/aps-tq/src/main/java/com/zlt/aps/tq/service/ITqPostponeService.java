package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.dto.TqPostponeConfirmDTO;
import com.zlt.aps.tq.api.domain.dto.TqPostponeRequestDTO;
import com.zlt.aps.tq.api.domain.vo.TqPostponePreviewVO;
import com.zlt.aps.tq.engine.vo.RollingUpdateResult;

/**
 * 胎圈排程跨班次推迟Service接口
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>预览：计算推迟效果，返回前后对比信息，不持久化</li>
 *   <li>确认：根据预览批次号执行推迟，持久化变更并记录日志明细</li>
 *   <li>取消：根据预览批次号取消推迟（清除缓存）</li>
 * </ul>
 *
 * <p>推迟规则：</p>
 * <ul>
 *   <li>整体推迟：将源班次未完成的任务整体移到下一班次开头</li>
 *   <li>部分推迟：将未完成部分拆分，已完成部分保留，未完成部分推迟</li>
 *   <li>递归处理：若下一班次也满，则继续向后递归，最多到第6班次</li>
 *   <li>跨天处理：第6班次满则推迟到次日第1班次</li>
 * </ul>
 *
 * @author APS
 */
public interface ITqPostponeService {

    /**
     * 预览推迟效果
     *
     * <p>不持久化任何变更，仅计算并缓存预览结果，返回前后对比信息。</p>
     *
     * @param request 推迟请求参数
     * @return 推迟预览结果
     */
    TqPostponePreviewVO previewPostpone(TqPostponeRequestDTO request);

    /**
     * 确认执行推迟
     *
     * <p>根据预览批次号获取缓存的预览结果，执行推迟并持久化变更。</p>
     *
     * @param confirmDTO 确认参数（包含预览批次号和调整原因）
     * @return 滚动更新结果
     */
    RollingUpdateResult confirmPostpone(TqPostponeConfirmDTO confirmDTO);

    /**
     * 取消推迟
     *
     * <p>根据预览批次号清除缓存，不执行任何变更。</p>
     *
     * @param previewBatchNo 预览批次号
     */
    void cancelPostpone(String previewBatchNo);
}
