package com.zlt.aps.gdyy.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyQuotaSettingDto;
import com.zlt.aps.gdyy.entity.GdyyQuotaSetting;
import com.zlt.aps.gdyy.mapper.GdyyQuotaSettingMapper;
import com.zlt.aps.gdyy.service.GdyyQuotaSettingService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 钢带压延定额设定Service业务层处理
 *
 * @author chen
 * @date 2021-06-30
 */
@Service
public class GdyyQuotaSettingServiceImpl extends ServiceImpl<GdyyQuotaSettingMapper, GdyyQuotaSetting> implements GdyyQuotaSettingService {
    @Autowired
    private GdyyQuotaSettingMapper gdyyQuotaSettingMapper;

    /**
     * 查询钢带压延定额设定列表
     *
     * @param quotaSetting 钢带压延定额设定
     * @return 钢带压延定额设定集合
     */
    @Override
    public List<GdyyQuotaSettingDto> selectQuotaSettingList(GdyyQuotaSetting quotaSetting) {
        return gdyyQuotaSettingMapper.selectQuotaSettingList(quotaSetting);
    }

    /**
     * 查询钢带压延定额设定
     *
     * @param id 钢带压延定额设定ID
     * @return 钢带压延定额设定
     */
    @Override
    public GdyyQuotaSetting selectQuotaSettingById(Long id) {
        LambdaQueryWrapper<GdyyQuotaSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GdyyQuotaSetting::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(GdyyQuotaSetting::getId, id);
        return gdyyQuotaSettingMapper.selectOne(wrapper);
    }

    /**
     * 修改钢带压延定额设定
     *
     * @param quotaSetting 钢带压延定额设定
     */
    @Override
    public AjaxResult saveQuotaSetting(GdyyQuotaSetting quotaSetting) {
        // 定额设定记录唯一性校验
        String unique = checkUnique(quotaSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
        quotaSetting.setBaseVale(quotaSetting.getId());
        saveOrUpdate(quotaSetting);
        return AjaxResult.success();
    }

    /**
     * 批量删除钢带压延定额设定
     *
     * @param ids 需要删除的钢带压延定额设定ID
     */
    @Override
    public void deleteQuotaSettingByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        gdyyQuotaSettingMapper.deleteQuotaSettingByIds(ids);
    }

    /**
     * 验证定额设定信息唯一性
     *
     * @param quotaSetting 要校验的记录
     */
    @Override
    public String checkUnique(GdyyQuotaSetting quotaSetting) {
        List<GdyyQuotaSetting> list = gdyyQuotaSettingMapper.checkUnique(quotaSetting);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GdyyQuotaSettingDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GdyyQuotaSettingDto> importList = new ArrayList<>();
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(GdyyQuotaSettingDto::getBigRollCode, Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            GdyyQuotaSettingDto entity = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(entity.getBigRollCode());
			if (hasValue > 1) {
				entity.setId(-999L);
				String columnName = I18nUtil.getMessage("ui.data.column.gdyy.quota.bigRollCode");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), columnName),
						importErrorLogs);
                failureNum++;
				continue;
			}
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            if (CollectionUtils.isNotEmpty(validated)) {
				entity.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                entity.setBaseVale(null);
                importList.add(entity);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    gdyyQuotaSettingMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        GdyyQuotaSettingDto excelItem = list.get(i);
                        //过滤错误的记录
                        if (excelItem.getId() != null && excelItem.getId() == -999L) {
                            continue;
                        }
                        // 唯一性校验
                        GdyyQuotaSetting gdyyQuotaSetting = new GdyyQuotaSetting();
                        BeanUtils.copyProperties(excelItem, gdyyQuotaSetting);
                        List<GdyyQuotaSetting> unic = gdyyQuotaSettingMapper.checkUnique(gdyyQuotaSetting);
                        if (CollectionUtils.isEmpty(unic)) {
                            //不存在插入
                            successNum++;
                            gdyyQuotaSettingMapper.insert(gdyyQuotaSetting);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 执行sql失败，插入导入失败记录
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
