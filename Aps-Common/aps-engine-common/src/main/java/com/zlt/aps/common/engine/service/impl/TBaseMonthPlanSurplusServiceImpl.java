package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.MonthPlanSurplusBaseEntity;
import com.zlt.aps.common.engine.domain.TGdyyMonthPlanSurplus;
import com.zlt.aps.common.engine.domain.TXwyyMonthPlanSurplus;
import com.zlt.aps.common.engine.mapper.TBaseMonthPlanSurplusMapper;
import com.zlt.aps.common.engine.service.TBaseMonthPlanSurplusService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class TBaseMonthPlanSurplusServiceImpl implements TBaseMonthPlanSurplusService {

    @Resource
    private TBaseMonthPlanSurplusMapper mapper;

    @Override
    public <K extends MonthPlanSurplusBaseEntity> void mergeTm(List<K> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeTm(list);
    }

    @Override
    public <K extends MonthPlanSurplusBaseEntity> void mergeTc(List<K> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeTc(list);
    }

    @Override
    public <K extends MonthPlanSurplusBaseEntity> void mergeNc(List<K> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeNc(list);
    }

    @Override
    public <K extends MonthPlanSurplusBaseEntity> void mergeTq(List<K> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeTq(list);
    }

    @Override
    public <K extends MonthPlanSurplusBaseEntity> void mergeGsq(List<K> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeGsq(list);
    }

    @Override
    public <K extends MonthPlanSurplusBaseEntity> void mergeCd15(List<K> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeCd15(list);
    }

    @Override
    public <K extends MonthPlanSurplusBaseEntity> void mergeCd90(List<K> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeCd90(list);
    }

    @Override
    public void mergeXwyy(List<TXwyyMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeXwyy(list);
    }

    @Override
    public void mergeGdyy(List<TGdyyMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeGdyy(list);
    }
    

	/**
	 * 通过年月获取月度计划版本号
	 * 
	 * @param year  年
	 * @param month 月
	 * @return 版本号
	 */
    @Override
	public String selectMonthPlanApsVersion(String year, String month) {
		return mapper.selectMonthPlanApsVersion(year, month);
	}
}
