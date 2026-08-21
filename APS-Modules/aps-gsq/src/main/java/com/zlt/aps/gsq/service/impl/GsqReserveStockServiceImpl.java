package com.zlt.aps.gsq.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqReserveStockDto;
import com.zlt.aps.gsq.entity.GsqReserveStock;
import com.zlt.aps.gsq.mapper.GsqReserveStockMapper;
import com.zlt.aps.gsq.service.GsqReserveStockService;


/**
 * 钢丝圈定额设定Service业务层处理
 *
 * @author hak
 * @date 2025-02-11
 */
@Service
public class GsqReserveStockServiceImpl extends ServiceImpl<GsqReserveStockMapper, GsqReserveStock> implements GsqReserveStockService {
    @Autowired
    private GsqReserveStockMapper gsqReserveStockMapper;

    /**
     * 查询钢丝圈定额设定列表
     *
     * @param reserveStock 钢丝圈定额设定
     * @return 钢丝圈定额设定集合
     */
    @Override
    public List<GsqReserveStockDto> selectReserveStockList(GsqReserveStock reserveStock) {
        return gsqReserveStockMapper.selectReserveStockList(reserveStock);
    }

    /**
     * 查询钢丝圈定额设定
     *
     * @param id 钢丝圈定额设定ID
     * @return 钢丝圈定额设定
     */
    @Override
    public GsqReserveStock selectReserveStockById(Long id) {
        LambdaQueryWrapper<GsqReserveStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqReserveStock::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(GsqReserveStock::getId, id);
        return gsqReserveStockMapper.selectOne(wrapper);
    }

    /**
     * 修改钢丝圈定额设定
     *
     * @param reserveStock 钢丝圈定额设定
     */
    @Override
    public AjaxResult saveReserveStock(GsqReserveStock reserveStock) {
        // 定额设定记录唯一性校验
        String unique = checkUnique(reserveStock);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
        reserveStock.setBaseVale(reserveStock.getId());
        saveOrUpdate(reserveStock);
        return AjaxResult.success();
    }

    /**
     * 批量删除钢丝圈定额设定
     *
     * @param ids 需要删除的钢丝圈定额设定ID
     */
    @Override
    public void deleteReserveStockByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        gsqReserveStockMapper.deleteReserveStockByIds(ids);
    }

    /**
     * 验证定额设定信息唯一性
     *
     * @param reserveStock 要校验的记录
     */
    @Override
    public String checkUnique(GsqReserveStock reserveStock) {
        List<GsqReserveStock> list = gsqReserveStockMapper.checkUnique(reserveStock);
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
    public AjaxResult importData(List<GsqReserveStockDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqReserveStockDto> importList = new ArrayList<>();
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(GsqReserveStockDto::getSteelRingCode, Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            GsqReserveStockDto entity = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(entity.getSteelRingCode());
			if (hasValue > 1) {
				entity.setId(-999L);
				String columnName = I18nUtil.getMessage("ui.data.column.gsq.reserveStock.steelRingCode");
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
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    // 勾选更新：批量预取已存在备库（按钢丝圈代码匹配），存在则更新原记录，不存在则新增
                    Map<String, GsqReserveStock> existingMap = this.loadExistingReserveStockMap(importList);
                    for (GsqReserveStockDto excelItem : importList) {
                        GsqReserveStock reserveStock = new GsqReserveStock();
                        BeanUtils.copyProperties(excelItem, reserveStock);
                        GsqReserveStock existing = existingMap.get(excelItem.getSteelRingCode());
                        if (existing != null) {
                            // 已存在：回填主键ID，清空新增审计字段避免覆盖原记录创建信息，setBaseVale补齐更新审计字段后更新
                            reserveStock.setId(existing.getId());
                            reserveStock.setCreateBy(null);
                            reserveStock.setCreateTime(null);
                            reserveStock.setBaseVale(existing.getId());
                            gsqReserveStockMapper.updateById(reserveStock);
                        } else {
                            // 不存在：setBaseVale(null)自动补齐delFlag/createBy/createTime后插入
                            reserveStock.setBaseVale(null);
                            gsqReserveStockMapper.insert(reserveStock);
                        }
                        successNum++;
                    }
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        GsqReserveStockDto excelItem = list.get(i);
                        //过滤错误的记录
                        if (excelItem.getId() != null && excelItem.getId() == -999L) {
                            continue;
                        }
                        // 唯一性校验
                        GsqReserveStock gsqReserveStock = new GsqReserveStock();
                        BeanUtils.copyProperties(excelItem, gsqReserveStock);
                        List<GsqReserveStock> unic = gsqReserveStockMapper.checkUnique(gsqReserveStock);
                        if (CollectionUtils.isEmpty(unic)) {
                            //不存在插入
                            successNum++;
                            gsqReserveStockMapper.insert(gsqReserveStock);
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

    /**
     * 批量预取已存在的备库数据（导入更新模式使用）
     * 按钢丝圈代码批量查询数据库未删除的已有记录
     *
     * @param importList 导入数据列表
     * @return 钢丝圈代码 -> 已存在备库记录 的映射
     */
    private Map<String, GsqReserveStock> loadExistingReserveStockMap(List<GsqReserveStockDto> importList) {
        // 提取非空钢丝圈代码并去重
        List<String> codeList = importList.stream()
                .map(GsqReserveStockDto::getSteelRingCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(codeList)) {
            return new HashMap<>();
        }
        // 按1000条一批查询（过滤逻辑删除），避免in条件超长；同编码多条时保留首条
        return CollUtil.split(codeList, 1000).stream()
                .flatMap(batch -> gsqReserveStockMapper.selectList(new LambdaQueryWrapper<GsqReserveStock>()
                        .eq(GsqReserveStock::getDelFlag, ApsConstant.DEL_FLAG_NORMAL)
                        .in(GsqReserveStock::getSteelRingCode, batch)).stream())
                .collect(Collectors.toMap(GsqReserveStock::getSteelRingCode, Function.identity(), (v1, v2) -> v1));
    }
}
