package com.zlt.aps.xwyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.dto.XwyyReserveStockDto;
import com.zlt.aps.xwyy.entity.XwyyReserveStock;
import com.zlt.aps.xwyy.mapper.XwyyReserveStockMapper;
import com.zlt.aps.xwyy.service.XwyyReserveStockService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 纤维压延预生产库存倍数设定Service业务层处理
 *
 * @author hak
 * @date 2025-02-11
 */
@Service
public class XwyyReserveStockServiceImpl extends ServiceImpl<XwyyReserveStockMapper, XwyyReserveStock> implements XwyyReserveStockService {
    @Autowired
    private XwyyReserveStockMapper xwyyReserveStockMapper;

    /**
     * 查询纤维压延预生产库存倍数设定列表
     *
     * @param reserveStock 纤维压延预生产库存倍数设定
     * @return 纤维压延预生产库存倍数设定集合
     */
    @Override
    public List<XwyyReserveStockDto> selectReserveStockList(XwyyReserveStock reserveStock) {
        return xwyyReserveStockMapper.selectReserveStockList(reserveStock);
    }

    /**
     * 查询纤维压延预生产库存倍数设定
     *
     * @param id 纤维压延预生产库存倍数设定ID
     * @return 纤维压延预生产库存倍数设定
     */
    @Override
    public XwyyReserveStock selectReserveStockById(Long id) {
        LambdaQueryWrapper<XwyyReserveStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XwyyReserveStock::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(XwyyReserveStock::getId, id);
        return xwyyReserveStockMapper.selectOne(wrapper);
    }

    /**
     * 修改纤维压延预生产库存倍数设定
     *
     * @param reserveStock 纤维压延预生产库存倍数设定
     */
    @Override
    public AjaxResult saveReserveStock(XwyyReserveStock reserveStock) {
        // 预生产库存倍数设定记录唯一性校验
        String unique = checkUnique(reserveStock);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
        reserveStock.setBaseVale(reserveStock.getId());
        saveOrUpdate(reserveStock);
        return AjaxResult.success();
    }

    /**
     * 批量删除纤维压延预生产库存倍数设定
     *
     * @param ids 需要删除的纤维压延预生产库存倍数设定ID
     */
    @Override
    public void deleteReserveStockByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        xwyyReserveStockMapper.deleteReserveStockByIds(ids);
    }

    /**
     * 验证预生产库存倍数设定信息唯一性
     *
     * @param reserveStock 要校验的记录
     */
    @Override
    public String checkUnique(XwyyReserveStock reserveStock) {
        List<XwyyReserveStock> list = xwyyReserveStockMapper.checkUnique(reserveStock);
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
    public AjaxResult importData(List<XwyyReserveStockDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<XwyyReserveStockDto> importList = new ArrayList<>();
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(XwyyReserveStockDto::getBigRollCode, Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            XwyyReserveStockDto entity = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(entity.getBigRollCode());
			if (hasValue > 1) {
				entity.setId(-999L);
				String columnName = I18nUtil.getMessage("ui.data.column.xwyy.reserveStock.bigRollCode");
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
                    xwyyReserveStockMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        XwyyReserveStockDto excelItem = list.get(i);
                        //过滤错误的记录
                        if (excelItem.getId() != null && excelItem.getId() == -999L) {
                            continue;
                        }
                        // 唯一性校验
                        XwyyReserveStock xwyyReserveStock = new XwyyReserveStock();
                        BeanUtils.copyProperties(excelItem, xwyyReserveStock);
                        List<XwyyReserveStock> unic = xwyyReserveStockMapper.checkUnique(xwyyReserveStock);
                        if (CollectionUtils.isEmpty(unic)) {
                            //不存在插入
                            successNum++;
                            xwyyReserveStockMapper.insert(xwyyReserveStock);
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
