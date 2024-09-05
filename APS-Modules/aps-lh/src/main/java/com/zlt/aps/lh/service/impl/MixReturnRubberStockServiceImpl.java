package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixReturnRubberStockMapper;
import com.zlt.aps.lh.api.domain.entity.MixReturnRubberStock;
import com.zlt.aps.lh.service.MixReturnRubberStockService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 返回胶库存Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixReturnRubberStockServiceImpl implements MixReturnRubberStockService
{
    @Autowired
    private MixReturnRubberStockMapper mixReturnRubberStockMapper;

    /**
     * 查询返回胶库存
     * 
     * @param id 返回胶库存ID
     * @return 返回胶库存
     */
    @Override
    public MixReturnRubberStock selectMixReturnRubberStockById(Long id)
    {
        return mixReturnRubberStockMapper.selectMixReturnRubberStockById(id);
    }

    /**
     * 查询返回胶库存列表
     * 
     * @param mixReturnRubberStock 返回胶库存
     * @return 返回胶库存
     */
    @Override
    public List<MixReturnRubberStock> selectMixReturnRubberStockList(MixReturnRubberStock mixReturnRubberStock)
    {
        return mixReturnRubberStockMapper.selectMixReturnRubberStockList(mixReturnRubberStock);
    }

    /**
     * 新增返回胶库存
     * 
     * @param mixReturnRubberStock 返回胶库存
     * @return 结果
     */
    @Override
    public int insertMixReturnRubberStock(MixReturnRubberStock mixReturnRubberStock)
    {
        mixReturnRubberStock.setBaseVale(null);
        return mixReturnRubberStockMapper.insertMixReturnRubberStock(mixReturnRubberStock);
    }

    /**
     * 修改返回胶库存
     * 
     * @param mixReturnRubberStock 返回胶库存
     * @return 结果
     */
    @Override
    public int updateMixReturnRubberStock(MixReturnRubberStock mixReturnRubberStock)
    {
        mixReturnRubberStock.setBaseVale(mixReturnRubberStock.getId());
        return mixReturnRubberStockMapper.updateMixReturnRubberStock(mixReturnRubberStock);
    }

    /**
     * 批量删除返回胶库存
     * 
     * @param ids 需要删除的返回胶库存ID
     * @return 结果
     */
    @Override
    public int deleteMixReturnRubberStockByIds(Long[] ids)
    {
        return mixReturnRubberStockMapper.deleteMixReturnRubberStockByIds(ids);
    }

    /**
     * 删除返回胶库存信息
     * 
     * @param id 返回胶库存ID
     * @return 结果
     */
    @Override
    public int deleteMixReturnRubberStockById(Long id)
    {
        return mixReturnRubberStockMapper.deleteMixReturnRubberStockById(id);
    }

    /**
     * 校验返回胶库存唯一性
     */
    @Override
    public String checkMixReturnRubberStockUnique(MixReturnRubberStock mixReturnRubberStock) {
        if (mixReturnRubberStock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixReturnRubberStock> list = mixReturnRubberStockMapper.selectMixReturnRubberStockList(mixReturnRubberStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入返回胶库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixReturnRubberStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixReturnRubberStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixReturnRubberStock mixReturnRubberStock = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixReturnRubberStock);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixReturnRubberStock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixReturnRubberStock.setBaseVale(null);
                importList.add(mixReturnRubberStock);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixReturnRubberStockMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixReturnRubberStock mixReturnRubberStock = list.get(i);
                    // 错误记录跳过
                    if (mixReturnRubberStock.getId() != null && mixReturnRubberStock.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixReturnRubberStockUnique(mixReturnRubberStock);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixReturnRubberStock(mixReturnRubberStock);
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
