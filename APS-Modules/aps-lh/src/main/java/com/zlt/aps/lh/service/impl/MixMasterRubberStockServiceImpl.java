package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixMasterRubberStockMapper;
import com.zlt.aps.lh.api.domain.entity.MixMasterRubberStock;
import com.zlt.aps.lh.service.MixMasterRubberStockService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 母炼胶库存Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixMasterRubberStockServiceImpl implements MixMasterRubberStockService
{
    @Autowired
    private MixMasterRubberStockMapper mixMasterRubberStockMapper;

    /**
     * 查询母炼胶库存
     * 
     * @param id 母炼胶库存ID
     * @return 母炼胶库存
     */
    @Override
    public MixMasterRubberStock selectMixMasterRubberStockById(Long id)
    {
        return mixMasterRubberStockMapper.selectMixMasterRubberStockById(id);
    }

    /**
     * 查询母炼胶库存列表
     * 
     * @param mixMasterRubberStock 母炼胶库存
     * @return 母炼胶库存
     */
    @Override
    public List<MixMasterRubberStock> selectMixMasterRubberStockList(MixMasterRubberStock mixMasterRubberStock)
    {
        return mixMasterRubberStockMapper.selectMixMasterRubberStockList(mixMasterRubberStock);
    }

    /**
     * 新增母炼胶库存
     * 
     * @param mixMasterRubberStock 母炼胶库存
     * @return 结果
     */
    @Override
    public int insertMixMasterRubberStock(MixMasterRubberStock mixMasterRubberStock)
    {
        mixMasterRubberStock.setBaseVale(null);
        return mixMasterRubberStockMapper.insertMixMasterRubberStock(mixMasterRubberStock);
    }

    /**
     * 修改母炼胶库存
     * 
     * @param mixMasterRubberStock 母炼胶库存
     * @return 结果
     */
    @Override
    public int updateMixMasterRubberStock(MixMasterRubberStock mixMasterRubberStock)
    {
        mixMasterRubberStock.setBaseVale(mixMasterRubberStock.getId());
        return mixMasterRubberStockMapper.updateMixMasterRubberStock(mixMasterRubberStock);
    }

    /**
     * 批量删除母炼胶库存
     * 
     * @param ids 需要删除的母炼胶库存ID
     * @return 结果
     */
    @Override
    public int deleteMixMasterRubberStockByIds(Long[] ids)
    {
        return mixMasterRubberStockMapper.deleteMixMasterRubberStockByIds(ids);
    }

    /**
     * 删除母炼胶库存信息
     * 
     * @param id 母炼胶库存ID
     * @return 结果
     */
    @Override
    public int deleteMixMasterRubberStockById(Long id)
    {
        return mixMasterRubberStockMapper.deleteMixMasterRubberStockById(id);
    }

    /**
     * 校验母炼胶库存唯一性
     */
    @Override
    public String checkMixMasterRubberStockUnique(MixMasterRubberStock mixMasterRubberStock) {
        if (mixMasterRubberStock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixMasterRubberStock> list = mixMasterRubberStockMapper.selectMixMasterRubberStockList(mixMasterRubberStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入母炼胶库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixMasterRubberStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixMasterRubberStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixMasterRubberStock mixMasterRubberStock = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixMasterRubberStock);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixMasterRubberStock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixMasterRubberStock.setBaseVale(null);
                importList.add(mixMasterRubberStock);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixMasterRubberStockMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixMasterRubberStock mixMasterRubberStock = list.get(i);
                    // 错误记录跳过
                    if (mixMasterRubberStock.getId() != null && mixMasterRubberStock.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixMasterRubberStockUnique(mixMasterRubberStock);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixMasterRubberStock(mixMasterRubberStock);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
                    }
                }
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
}
