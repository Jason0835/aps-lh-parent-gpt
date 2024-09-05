package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.domain.TSapEmbryoBadNumber;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import com.zlt.aps.common.engine.service.TSapEmbryoBadNumberService;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.SapImportBadNumber;
import com.zlt.aps.cx.engine.mapper.CxLhEngineCommonMapper;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import com.zlt.aps.cx.mapper.SapImportBadNumberMapper;
import com.zlt.aps.cx.service.SapImportBadNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * SAP导入不良数Service业务层处理
 *
 * @author Joran.zhang
 * @date 2022-01-15
 */
@Service
public class SapImportBadNumberServiceImpl extends ServiceImpl<SapImportBadNumberMapper, SapImportBadNumber> implements SapImportBadNumberService {
    @Autowired
    private SapImportBadNumberMapper sapImportBadNumberMapper;

    @Autowired
    private TSapEmbryoBadNumberService sapEmbryoBadNumberService;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    @Autowired
    private MdmMonthPlanMainService mdmMonthPlanMainService;

    @Autowired
    private MdmMonthPlanAmountSumService mdmMonthPlanAmountSumService;
    @Autowired
    private CxLhEngineCommonMapper cxLhEngineCommonMapper;

    /**
     * 查询SAP导入不良数列表
     *
     * @param sapImportBadNumber SAP导入不良数
     * @return SAP导入不良数
     */
    @Override
    public List<SapImportBadNumber> selectSapImportBadNumberList(SapImportBadNumber sapImportBadNumber) {
        return sapImportBadNumberMapper.selectSapImportBadNumberList(sapImportBadNumber);
    }

    /**
     * 导入SAP导入不良数数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<SapImportBadNumber> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<SapImportBadNumber> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(SapImportBadNumber::getSapCode, Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            SapImportBadNumber sapImportBadNumber = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(sapImportBadNumber.getSapCode());
            if (hasValue > 1) {
                failureNum++;
                sapImportBadNumber.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.badNumber.sapCode");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, sapImportBadNumber);
            if (CollectionUtils.isNotEmpty(validated)) {
                sapImportBadNumber.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                sapImportBadNumber.setBaseVale(null);
                importList.add(sapImportBadNumber);
            }
        }

        try {
            if (CollectionUtils.isNotEmpty(importList)) {
                // 默认覆盖所有数据
                sapImportBadNumberMapper.deleteAll();
                //Joran 2022-01-17 导入外胎不良后根据外胎施工获取多胎胚组成不良数据写入胎胚不良表，然后进行数据重算start
                toSapEmbryoBadNumberCalc(DateUtils.getNowDate(),importList);
                //Joran 2022-01-17 导入外胎不良后根据外胎施工获取多胎胚组成不良数据写入胎胚不良表，然后进行数据重算end
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
     * 根据导入的日期进行数据处理
     * @param importDate 导入不良日期，默认为最新的日期（因为导入线下都是汇总，默认都是最新日期）
     * @param importList 导入集合
     */
    private void toSapEmbryoBadNumberCalc(Date importDate, List<SapImportBadNumber> importList) {
        //格式化为0时0分0秒
        importDate=CxScheduleUtils.formatDateByZero(importDate);
        String importDateStr=DateUtils.parseDateToStr("yyyy-MM-dd",importDate);
        //获取全部硫化施工信息
        List<LhEngineTireConstructionInfo> lhEngineTireConstructionInfoList = lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(new LhEngineTireConstructionInfo());
        Map<String,List<LhEngineTireConstructionInfo>> sapListMap=new HashMap<>();
        //按照外胎进行分组
        if(StringUtils.isNotEmpty(lhEngineTireConstructionInfoList)){
            sapListMap=lhEngineTireConstructionInfoList.stream().collect(Collectors.groupingBy(LhEngineTireConstructionInfo::getSapCode));
        }
        //按照外胎进行组装不良数
        Map<String,Long> sapImportBadNumberMap=new HashMap<>();
        if(StringUtils.isNotEmpty(importList)){
            sapImportBadNumberMap=importList.stream() .collect(Collectors.toMap(SapImportBadNumber::getSapCode, SapImportBadNumber::getBadNum, (v1, v2) -> v2));
        }
        //组装导入的不良胎胚汇总数据
        List<TSapEmbryoBadNumber> importSapEmbryoBadNumberList=new ArrayList<>();
        //遍历导入的数据组装不良数据start
        //标记sap+胎胚处理过的不重复处理
        Map<String,String> sapEmbryoCodeMap=new HashMap<>();
        TSapEmbryoBadNumber sapEmbryoBadNumber=null;
        for(Map.Entry<String,Long> entry:sapImportBadNumberMap.entrySet()){
            String sapCode=entry.getKey();
            //不良数
            Long badNumber=entry.getValue();
            if(StringUtils.isNotEmpty(sapListMap)&&sapListMap.containsKey(sapCode)){
                List<LhEngineTireConstructionInfo> sapList=sapListMap.get(sapCode);
                for(LhEngineTireConstructionInfo lhEngineTireConstructionInfo:sapList){
                    String tireSapCode=lhEngineTireConstructionInfo.getSapCode();
                    String tireEmbryoCode=lhEngineTireConstructionInfo.getEmbryoCode();
                    String key= GenerageMapKeyUtils.createMapKey(tireSapCode,tireEmbryoCode);
                    if(!sapEmbryoCodeMap.containsKey(key)){
                        sapEmbryoBadNumber=new TSapEmbryoBadNumber();
                        sapEmbryoBadNumber.setBadDate(importDate);
                        sapEmbryoBadNumber.setSapCode(tireSapCode);
                        sapEmbryoBadNumber.setEmbryoCode(tireEmbryoCode);
                        sapEmbryoBadNumber.setBomDataVersion(lhEngineTireConstructionInfo.getEmbryoVersion());
                        sapEmbryoBadNumber.setBadNum(BigDecimal.valueOf(badNumber).intValue());
                        sapEmbryoBadNumber.setRemark(importDateStr+"外胎废次品导入生成");
                        importSapEmbryoBadNumberList.add(sapEmbryoBadNumber);
                        sapEmbryoCodeMap.put(key,key);
                    }
                }
            }
        }
        //遍历导入的数据组装不良数据end
        if(StringUtils.isNotEmpty(importSapEmbryoBadNumberList)){
            String month=DateUtils.parseDateToStr("yyyyMM",importDate);
            //1.进行接口月份数据删除
            cxLhEngineCommonMapper.removeBadNumberByMonth(month);
            //2.重新合并
            sapEmbryoBadNumberService.mergeSql(importSapEmbryoBadNumberList);
        }

        //获取到导入日期对应版本主表信息
        MdmMonthPlanMain mdmMonthPlanMain=mdmMonthPlanMainService.getValidPlanMainVersion(importDate);

        //进行版本重算
        if(mdmMonthPlanMain!=null){
            mdmMonthPlanAmountSumService.recalculateByApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
        }

    }
}
