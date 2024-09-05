package com.zlt.aps.xwyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistRequirement;
import com.zlt.aps.xwyy.mapper.XwyyAssistRequirementMapper;
import com.zlt.aps.xwyy.service.XwyyAssistRequirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 纤维压延外厂需求Service业务层处理
 *
 * @author chen
 * @date 2022-03-14
 */
@Service
public class XwyyAssistRequirementServiceImpl extends ServiceImpl<XwyyAssistRequirementMapper, XwyyAssistRequirement> implements XwyyAssistRequirementService {
    @Autowired
    private XwyyAssistRequirementMapper xwyyAssistRequirementMapper;

    /**
     * 查询纤维压延外厂需求
     *
     * @param id 纤维压延外厂需求ID
     * @return 纤维压延外厂需求
     */
    @Override
    public XwyyAssistRequirement selectXwyyAssistRequirementById(Long id) {
        return xwyyAssistRequirementMapper.selectXwyyAssistRequirementById(id);
    }

    /**
     * 查询纤维压延外厂需求列表
     *
     * @param xwyyAssistRequirement 纤维压延外厂需求
     * @return 纤维压延外厂需求
     */
    @Override
    public List<XwyyAssistRequirement> selectXwyyAssistRequirementList(XwyyAssistRequirement xwyyAssistRequirement) {
        return xwyyAssistRequirementMapper.selectXwyyAssistRequirementList(xwyyAssistRequirement);
    }

    /**
     * 新增纤维压延外厂需求
     *
     * @param xwyyAssistRequirement 纤维压延外厂需求
     * @return 结果
     */
    @Override
    public int insertXwyyAssistRequirement(XwyyAssistRequirement xwyyAssistRequirement) {
        xwyyAssistRequirement.setBaseVale(null);
        return xwyyAssistRequirementMapper.insertXwyyAssistRequirement(xwyyAssistRequirement);
    }

    /**
     * 修改纤维压延外厂需求
     *
     * @param xwyyAssistRequirement 纤维压延外厂需求
     * @return 结果
     */
    @Override
    public int updateXwyyAssistRequirement(XwyyAssistRequirement xwyyAssistRequirement) {
        xwyyAssistRequirement.setBaseVale(xwyyAssistRequirement.getId());
        return xwyyAssistRequirementMapper.updateXwyyAssistRequirement(xwyyAssistRequirement);
    }

    /**
     * 批量删除纤维压延外厂需求
     *
     * @param ids 需要删除的纤维压延外厂需求ID
     * @return 结果
     */
    @Override
    public int deleteXwyyAssistRequirementByIds(Long[] ids) {
        return xwyyAssistRequirementMapper.deleteXwyyAssistRequirementByIds(ids);
    }

    /**
     * 删除纤维压延外厂需求信息
     *
     * @param id 纤维压延外厂需求ID
     * @return 结果
     */
    @Override
    public int deleteXwyyAssistRequirementById(Long id) {
        return xwyyAssistRequirementMapper.deleteXwyyAssistRequirementById(id);
    }

    /**
     * 校验纤维压延外厂需求唯一性
     */
    @Override
    public String checkXwyyAssistRequirementUnique(XwyyAssistRequirement xwyyAssistRequirement) {
        if (xwyyAssistRequirement == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int unique = xwyyAssistRequirementMapper.checkUnique(xwyyAssistRequirement);
        if (unique > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入纤维压延外厂需求数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<XwyyAssistRequirement> list, Long importLogId, Date scheduleDate) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<XwyyAssistRequirement> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> item.getScheduleDate() + item.getBigRollCode(), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 4;
            XwyyAssistRequirement assistRequirement = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(assistRequirement.getScheduleDate() + assistRequirement.getBigRollCode());
            if (hasValue > 1) {
                failureNum++;
                assistRequirement.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName1 = I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDate");
                String columnName2 = I18nUtil.getMessage("ui.data.column.xwyy.scheduleResult.bigRollCode");
                message = String.format(message, columnName1 + "+" + columnName2);
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            }
            
			// 校验所有业务字段
			BigDecimal dayPlanQty = assistRequirement.getDayPlanQty();
			BigDecimal nightPlanQty = assistRequirement.getNightPlanQty();
			BigDecimal todayStock = assistRequirement.getTodayStock();
			BigDecimal dayOut = assistRequirement.getDayOut();
			BigDecimal fac5Class1Plan = assistRequirement.getFac5Class1Plan();
			BigDecimal fac5Class2Plan = assistRequirement.getFac5Class2Plan();
			BigDecimal fac5Class3Plan = assistRequirement.getFac5Class3Plan();
			// 全都为0的记录直接忽略掉
			if (this.isAllZero(dayPlanQty, nightPlanQty, todayStock, dayOut, fac5Class1Plan, fac5Class2Plan,
					fac5Class3Plan)) {
				continue;
			}

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, assistRequirement);
            
			if (CollectionUtils.isNotEmpty(validated)) {
				assistRequirement.setId(-999L);
				failureNum++;
				importErrorLogs.addAll(validated);
			} else {
				assistRequirement.setBaseVale(null);
				importList.add(assistRequirement);
			}
        }

        try {
            if (CollectionUtils.isNotEmpty(importList)) {
                // 默认覆盖当天所有数据
                xwyyAssistRequirementMapper.deleteAll(scheduleDate);
                this.saveBatch(importList);
                successNum = importList.size();
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
    
    /**
     * 判断参数是否全部都等于0
     * @param value
     * @return
     */
    private boolean isAllZero(BigDecimal... value) {
    	for (int i = 0, len = value.length; i < len; i ++) {
    		BigDecimal val = value[i];
    		if (val == null || val.compareTo(BigDecimal.ZERO) != 0) {
    			return false;
    		}
    	}
    	return true;
    }
}
