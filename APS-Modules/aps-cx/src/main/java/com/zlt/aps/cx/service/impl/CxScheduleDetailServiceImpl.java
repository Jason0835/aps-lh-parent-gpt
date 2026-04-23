package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.mapper.CxScheduleDetailMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程明细服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxScheduleDetailServiceImpl extends ServiceImpl<CxScheduleDetailMapper, CxScheduleDetail>
        implements CxScheduleDetailService {

    @Override
    public List<CxScheduleDetail> listByMainId(Long mainId) {
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .orderByAsc(CxScheduleDetail::getShiftCode));
    }

    @Override
    public List<CxScheduleDetail> listByMachineAndDate(String cxMachineCode, LocalDate scheduleDate) {
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getCxMachineCode, cxMachineCode)
                .eq(CxScheduleDetail::getScheduleDate, scheduleDate)
                .orderByAsc(CxScheduleDetail::getShiftCode));
    }

    @Override
    public List<CxScheduleDetail> listByShift(Long mainId, String shiftCode) {
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .eq(CxScheduleDetail::getShiftCode, shiftCode)
                .orderByAsc(CxScheduleDetail::getShiftCode));
    }

    /**
     * 更新完成量
     *
     * @param detailId          明细ID
     * @param completedQuantity 完成量
     * @return 是否成功
     */
    @Override
    public boolean updateCompletedQuantity(Long detailId, Integer completedQuantity) {
        return false;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTripStatus(Long detailId, String tripStatus) {
        // 这里可以根据业务需要扩展状态字段
        // 当前实体类中通过 tripActualQty 与 tripCapacity 的比较来判断状态
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(List<CxScheduleDetail> details) {
        if (details == null || details.isEmpty()) {
            return false;
        }
        // 清除所有明细记录的ID,避免主键冲突,让数据库自动生成新ID
        for (CxScheduleDetail detail : details) {
            detail.setId(null);
        }
        return saveBatch(details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMainId(Long mainId) {
        return remove(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMainIds(List<Long> mainIds) {
        if (mainIds == null || mainIds.isEmpty()) {
            return true;
        }
        return remove(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds));
    }
}
