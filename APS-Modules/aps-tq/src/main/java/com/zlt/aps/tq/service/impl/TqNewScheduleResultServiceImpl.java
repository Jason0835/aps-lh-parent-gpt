package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.mapper.TqNewScheduleResultMapper;
import com.zlt.aps.tq.service.ITqNewScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 胎圈排程结果Service实现类（新版）
 *
 * @author APS
 */
@Slf4j
@Service
public class TqNewScheduleResultServiceImpl extends AbstractDocService<TqNewScheduleResult> implements ITqNewScheduleResultService {

    @Autowired
    private TqNewScheduleResultMapper tqNewScheduleResultMapper;

    @Override
    public String getDocTypeCode() {
        return "TQ_NEW_SCHEDULE_RESULT";
    }

    /**
     * 插单
     *
     * @param entity 插单数据
     * @return 结果
     */
    @Override
    public AjaxResult insertOrder(TqNewScheduleResult entity) {
        // TODO 插单业务逻辑待实现
        log.info("胎圈排程插单，排程日期：{}，胎圈代码：{}", entity.getScheduleDate(), entity.getBeadCode());
        return AjaxResult.success();
    }

    /**
     * 转机台
     *
     * @param entity 转机台数据
     * @return 结果
     */
    @Override
    public AjaxResult changeMachine(TqNewScheduleResult entity) {
        // TODO 转机台业务逻辑待实现
        log.info("胎圈排程转机台，id：{}，新机台：{}", entity.getId(), entity.getMachineCode());
        return AjaxResult.success();
    }

    /**
     * 调量
     *
     * @param entity 调量数据
     * @return 结果
     */
    @Override
    public AjaxResult changeQty(TqNewScheduleResult entity) {
        // TODO 调量业务逻辑待实现
        log.info("胎圈排程调量，id：{}", entity.getId());
        return AjaxResult.success();
    }

    /**
     * 发布排程到MES
     *
     * @param queryVO 查询条件（含排程日期等）
     * @return 结果
     */
    @Override
    public AjaxResult publish(TqNewScheduleResult queryVO) {
        // TODO 发布业务逻辑待实现
        log.info("胎圈排程发布，排程日期：{}", queryVO.getScheduleDateQuery());
        return AjaxResult.success();
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        // TODO 待实现：查询该排程日期下是否所有记录都已发布
        return false;
    }

    /**
     * 唯一性校验（排程日期+胎圈代码+机台编号）
     *
     * @param entity 校验数据
     * @return 是否唯一
     */
//    @Override
//    public Boolean checkUnique(TqNewScheduleResult entity) {
//        LambdaQueryWrapper<TqNewScheduleResult> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(TqNewScheduleResult::getScheduleDate, entity.getScheduleDate());
//        wrapper.eq(TqNewScheduleResult::getBeadCode, entity.getBeadCode());
//        wrapper.eq(TqNewScheduleResult::getMachineCode, entity.getMachineCode());
//        wrapper.ne(entity.getId() != null, TqNewScheduleResult::getId, entity.getId());
//        return tqNewScheduleResultMapper.selectCount(wrapper) == 0;
//    }
}
