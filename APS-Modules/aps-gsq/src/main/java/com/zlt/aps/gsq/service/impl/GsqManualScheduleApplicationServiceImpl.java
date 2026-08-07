package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.vo.GsqInsertTaskRequestVo;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleBaseInfoVo;
import com.zlt.aps.gsq.service.GsqManualScheduleApplicationService;
import com.zlt.aps.gsq.service.impl.GsqManualOperationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 钢丝圈人工排程应用服务实现。
 *
 * <p>对齐胎侧 {@code TcManualScheduleApplicationServiceImpl}，作为异步执行器与人工操作门面之间的
 * 业务编排层。负责请求校验、模板构建（插单场景的批次号/工单号/施工字段回填）等纯业务逻辑，
 * 再统一委托 {@link GsqManualOperationFacade} 完成锁、行锁、短事务、滚动和审计闭环。</p>
 *
 * <p>该实现不持有数据库事务或分布式锁，保证职责单一，便于 {@code @Async} 线程复用。</p>
 *
 * @author APS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GsqManualScheduleApplicationServiceImpl implements GsqManualScheduleApplicationService {

    /** 插单数据来源标识 */
    private static final String INSERT_DATA_SOURCE = "1";

    /** 钢丝圈排程引擎服务（用于生成批次号、工单号及施工信息回填） */
    private final GsqEngineService gsqEngineService;

    /** 人工操作门面（统一锁、行锁、短事务、滚动和审计闭环） */
    private final GsqManualOperationFacade gsqManualOperationFacade;

    @Override
    public int insertTask(GsqInsertTaskRequestVo vo) {
        this.validateInsertRequest(vo);
        // 校验施工是否存在（查询施工表 T_PRODUCT_CONSTRUCTION_INFO）
        List<GsqScheduleBaseInfoVo> baseInfoList = gsqEngineService.listGsqScheduleBaseInfo(
                Collections.singletonList(vo.getSteelRingCode()));
        if (CollectionUtils.isEmpty(baseInfoList)) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.constructionNotFound"));
        }
        // 构建 GsqScheduleResult 模板，回填6个班次字段
        GsqScheduleResult template = this.buildInsertTemplate(vo, baseInfoList.get(0));
        // 委托门面执行插单滚动（锁、行锁、审计由门面统一处理）
        return gsqManualOperationFacade.insertTask(template);
    }

    @Override
    public int changeQty(GsqScheduleResult request) {
        return gsqManualOperationFacade.changeQty(request);
    }

    @Override
    public int changeQtyBatch(List<GsqScheduleResult> requestList) {
        return gsqManualOperationFacade.batchChangeQty(requestList);
    }

    @Override
    public int changeMachine(List<GsqScheduleResult> requestList) {
        return gsqManualOperationFacade.changeMachineBatch(requestList);
    }

    @Override
    public int remove(List<Long> idList) {
        return gsqManualOperationFacade.deleteTasks(idList);
    }

    // ==================== 内部方法 ====================

    /**
     * 校验插单请求必填字段。
     *
     * @param vo 插单请求
     * @throws ServiceException 必填字段为空时抛出
     */
    private void validateInsertRequest(GsqInsertTaskRequestVo vo) {
        if (vo == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.insertTask.empty"));
        }
        if (vo.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.insertTask.scheduleDateEmpty"));
        }
        if (StringUtils.isBlank(vo.getSteelRingCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.insertTask.steelRingCodeEmpty"));
        }
        if (StringUtils.isBlank(vo.getMachineCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.machineCode.empty"));
        }
    }

    /**
     * 根据插单请求和施工信息构建排程结果模板。
     *
     * <p>回填6班次计划量/顺序/原因分析、批次号、工单号、英寸尺寸、数据来源和发布状态。</p>
     *
     * @param vo       插单请求
     * @param baseInfo 施工基础信息
     * @return 排程结果模板
     */
    private GsqScheduleResult buildInsertTemplate(GsqInsertTaskRequestVo vo, GsqScheduleBaseInfoVo baseInfo) {
        GsqScheduleResult template = new GsqScheduleResult();
        template.setFactoryCode(vo.getFactoryCode());
        template.setScheduleDate(vo.getScheduleDate());
        template.setSteelRingCode(vo.getSteelRingCode());
        template.setProSize(vo.getProSize());
        template.setMachineCode(vo.getMachineCode());
        // 回填6班次计划量/顺序/原因分析
        template.setClass1PlanQty(vo.getClass1PlanQty());
        template.setClass1Sequence(vo.getClass1Sequence());
        template.setClass1Analysis(vo.getClass1Analysis());
        template.setClass2PlanQty(vo.getClass2PlanQty());
        template.setClass2Sequence(vo.getClass2Sequence());
        template.setClass2Analysis(vo.getClass2Analysis());
        template.setClass3PlanQty(vo.getClass3PlanQty());
        template.setClass3Sequence(vo.getClass3Sequence());
        template.setClass3Analysis(vo.getClass3Analysis());
        template.setClass4PlanQty(vo.getClass4PlanQty());
        template.setClass4Sequence(vo.getClass4Sequence());
        template.setClass4Analysis(vo.getClass4Analysis());
        template.setClass5PlanQty(vo.getClass5PlanQty());
        template.setClass5Sequence(vo.getClass5Sequence());
        template.setClass5Analysis(vo.getClass5Analysis());
        template.setClass6PlanQty(vo.getClass6PlanQty());
        template.setClass6Sequence(vo.getClass6Sequence());
        template.setClass6Analysis(vo.getClass6Analysis());
        template.setRemark(vo.getRemark());
        // 数据来源：1-插单，发布状态：0-未发布
        template.setDataSource(INSERT_DATA_SOURCE);
        template.setIsRelease(ApsConstant.NO_RELEASE);
        // 生成批次号、工单号（复用自动排程口径，不影响其他记录）
        String scheduleDateStr = DateUtil.formatDate(vo.getScheduleDate());
        String[] batchAndOrder = gsqEngineService.generateBatchNoAndOrderNo(scheduleDateStr);
        template.setBatchNo(batchAndOrder[0]);
        template.setOrderNo(batchAndOrder[1]);
        // 回显施工字段（英寸尺寸），用户未填时从施工表获取
        if (StringUtils.isBlank(template.getProSize())) {
            template.setProSize(baseInfo.getProSize());
        }
        return template;
    }
}
