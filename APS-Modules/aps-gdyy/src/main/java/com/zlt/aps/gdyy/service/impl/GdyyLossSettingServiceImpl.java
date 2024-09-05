package com.zlt.aps.gdyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyLossSettingDto;
import com.zlt.aps.gdyy.entity.GdyyLossSetting;
import com.zlt.aps.gdyy.mapper.GdyyLossSettingMapper;
import com.zlt.aps.gdyy.service.GdyyLossSettingService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 钢带压延损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-19
 */
@Service
public class GdyyLossSettingServiceImpl extends ServiceImpl<GdyyLossSettingMapper, GdyyLossSetting> implements GdyyLossSettingService {
    @Autowired
    private GdyyLossSettingMapper gdyyLossSettingMapper;

    /**
     * 查询钢带压延损耗率设定
     *
     * @param id 钢带压延损耗率设定ID
     * @return 钢带压延损耗率设定
     */
    @Override
    public GdyyLossSettingDto selectGdyyLossSettingById(Long id) {
        return gdyyLossSettingMapper.selectGdyyLossSettingById(id);
    }

    /**
     * 查询钢带压延损耗率设定列表
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 钢带压延损耗率设定
     */
    @Override
    public List<GdyyLossSettingDto> selectGdyyLossSettingList(GdyyLossSetting gdyyLossSetting) {
        return gdyyLossSettingMapper.selectGdyyLossSettingList(gdyyLossSetting);
    }

    /**
     * 新增钢带压延损耗率设定
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 结果
     */
    @Override
    public int insertGdyyLossSetting(GdyyLossSetting gdyyLossSetting) {
        checkParamAndUnique(gdyyLossSetting);
        gdyyLossSetting.setBaseVale(null);
        return gdyyLossSettingMapper.insertGdyyLossSetting(gdyyLossSetting);
    }

    /**
     * 修改钢带压延损耗率设定
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 结果
     */
    @Override
    public int updateGdyyLossSetting(GdyyLossSetting gdyyLossSetting) {
        checkParamAndUnique(gdyyLossSetting);
        gdyyLossSetting.setBaseVale(gdyyLossSetting.getId());
        return gdyyLossSettingMapper.updateGdyyLossSetting(gdyyLossSetting);
    }

    /**
     * 批量删除钢带压延损耗率设定
     *
     * @param ids 需要删除的钢带压延损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteGdyyLossSettingByIds(Long[] ids) {
        return gdyyLossSettingMapper.deleteGdyyLossSettingByIds(ids);
    }

    /**
     * 删除钢带压延损耗率设定信息
     *
     * @param id 钢带压延损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteGdyyLossSettingById(Long id) {
        return gdyyLossSettingMapper.deleteGdyyLossSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkGdyyLossSettingUnique(GdyyLossSetting gdyyLossSetting) {
        if (gdyyLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = gdyyLossSettingMapper.checkGdyyLossSettingUnique(gdyyLossSetting);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param gdyyLossSetting 要检查记录
     */
    private void checkParamAndUnique(GdyyLossSetting gdyyLossSetting) {
        if (StringUtils.isEmpty(gdyyLossSetting.getBigRollCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkGdyyLossSettingUnique(gdyyLossSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
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
    public AjaxResult importData(List<GdyyLossSettingDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GdyyLossSettingDto> importList = new ArrayList<>();
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(GdyyLossSettingDto::getBigRollCode, Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            GdyyLossSettingDto entity = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(entity.getBigRollCode());
			if (hasValue > 1) {
				entity.setId(-999L);
				String columnName = I18nUtil.getMessage("ui.data.column.loss.gdyy.bigRollCode");
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
                    gdyyLossSettingMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        GdyyLossSettingDto excelItem = list.get(i);
                        //过滤错误的记录
                        if (excelItem.getId() != null && excelItem.getId() == -999L) {
                            continue;
                        }
                        // 唯一性校验
                        GdyyLossSetting gdyyLossSetting = new GdyyLossSetting();
                        BeanUtils.copyProperties(excelItem, gdyyLossSetting);
                        String unic = checkGdyyLossSettingUnique(gdyyLossSetting);
                        if (unic.equals(UserConstants.UNIQUE) && StringUtils.isNotBlank(excelItem.getBigRollCode())) {
                            //不存在插入
                            successNum++;
                            gdyyLossSettingMapper.insert(gdyyLossSetting);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.error.message.loss.unique"), importErrorLogs);
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

    @Override
    public void deleteAll() {
        this.gdyyLossSettingMapper.deleteAll();
    }
}
