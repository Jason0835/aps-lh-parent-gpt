package com.zlt.aps.monthplan.adjust.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.monthplan.api.annotation.WeekAdjustType;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import com.zlt.common.utils.PubUtil;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 结构内调整策略
 * @author wengpc
 */
@Slf4j
@Service
@WeekAdjustType(adjustType = WeekAdjustTypeEnum.STRUCTURE_IN)
public class MpAdjustStructureInStrategy extends AbstractBaseWeekAdjustService {

    @Override
    public void doGenerateAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        // 1、构建结构内调整明细
        List<MpAdjustDetailVo> adjustDetailList = buildAdjustDetailList(contextDTO);
        contextDTO.setAdjustDetailList(adjustDetailList);
        // 未获取到调整记录，抛出异常
        Assert.isFalse(PubUtil.isEmpty(adjustDetailList), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindAdjustDetailList"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 2、设置净需求
        setCurrentNetQty(contextDTO);
        // 3、设置计划剩余排产量、计划已排产量
        setMonthUnScheduledQty(contextDTO);
        // 4、筛选：净需求 - 计划剩余排产量 > 0的数据
        filterAdjustList(contextDTO.getAdjustDetailList());
        // 筛选后数据为空，抛出异常
        Assert.isFalse(PubUtil.isEmpty(contextDTO.getAdjustDetailList()), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notMatchAdjustDetailList"), contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 5、设置其他字段
        setOtherField(contextDTO);
    }

    @Override
    public void doAutoAdjust(MpRollAdjustContextDTO contextDTO) {

    }

    @Override
    public void doConfirmAdjust(MpRollAdjustContextDTO contextDTO) {

    }





    /**
     * 筛选：净需求 - 计划剩余排产量 > 0的数据
     * @param adjustList
     */
    private void filterAdjustList(List<MpAdjustDetailVo> adjustList) {
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        adjustList.removeIf(adjust -> {
            Integer currentNetQty = Convert.toInt(adjust.getCurrentNetQty(),0);
            Integer monthUnScheduledQty = Convert.toInt(adjust.getMonthUnScheduledQty(),0);
            return (currentNetQty - monthUnScheduledQty) <= 0;
        });
    }

}
