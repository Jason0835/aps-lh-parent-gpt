package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.mapper.entity.CxProductConstructionInfoMapper;
import com.zlt.aps.cx.service.CxProductConstructionInfoService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxHalfPartConversion;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductConstructionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 投产施工信息Service业务层处理
 *
 * @author zlt
 * @date 2021-12-02
 */
@Service
public class CxProductConstructionInfoServiceImpl implements CxProductConstructionInfoService
{
	// 半部件类型编号
	// 1号胎体布
	private final static String HALF_PART_TYPE_TIRE_FABRIC_1 = "9";
	// 2号胎体布
	private final static String HALF_PART_TYPE_TIRE_FABRIC_2 = "10";
	// 3号胎体布
	private final static String HALF_PART_TYPE_TIRE_FABRIC_3 = "11";
    @Autowired
    private CxProductConstructionInfoMapper cxProductConstructionInfoMapper;

//    @Autowired
//    private MdmMonthPlanMainService mdmMonthPlanMainService;
//    @Autowired
//    private MdmMonthPlanAmountSumService mdmMonthPlanAmountSumService;

    /**
     * 查询投产施工信息
     *
     * @param id 投产施工信息ID
     * @return 投产施工信息
     */
    @Override
    public CxProductConstructionInfo selectCxProductConstructionInfoById(Long id)
    {
        return cxProductConstructionInfoMapper.selectCxProductConstructionInfoById(id);
    }

    /**
     * 查询投产施工信息列表
     *
     * @param cxProductConstructionInfo 投产施工信息
     * @return 投产施工信息
     */
    @Override
    public List<CxProductConstructionInfo> selectCxProductConstructionInfoList(CxProductConstructionInfo cxProductConstructionInfo)
    {
        return cxProductConstructionInfoMapper.selectCxProductConstructionInfoList(cxProductConstructionInfo);
    }

    @Override
    public List<CxProductConstructionInfo> selectCxScheduleMongthPlan(Long[] ids){
        return cxProductConstructionInfoMapper.selectCxScheduleMongthPlan(ids);
    }



    /**
     * 新增投产施工信息
     *
     * @param cxProductConstructionInfo 投产施工信息
     * @return 结果
     */
    @Override
    public int insertCxProductConstructionInfo(CxProductConstructionInfo cxProductConstructionInfo)
    {
        cxProductConstructionInfo.setBaseVale(null);
        int result= cxProductConstructionInfoMapper.insertCxProductConstructionInfo(cxProductConstructionInfo);
        //Joran 2021-12-04 调用月度汇总重算接口
        reCalculateCauseConstructionChange();
        return result;
    }

    /**
     * 修改投产施工信息
     *
     * @param cxProductConstructionInfo 投产施工信息
     * @return 结果
     */
    @Override
    public int updateCxProductConstructionInfo(CxProductConstructionInfo cxProductConstructionInfo) {
        cxProductConstructionInfo.setBaseVale(cxProductConstructionInfo.getId());
        int result=cxProductConstructionInfoMapper.updateCxProductConstructionInfo(cxProductConstructionInfo);
        //Joran 2021-12-04 调用月度汇总重算接口
        reCalculateCauseConstructionChange();
        return result;
    }

    /**
     * 修改投产施工信息
     *
     * @param cxProductConstructionInfo 投产施工信息
     * @return 结果
     */
    @Override
    public int updateCxProductConstructionInfo2(CxProductConstructionInfo cxProductConstructionInfo) {
        cxProductConstructionInfo.setBaseVale(cxProductConstructionInfo.getId());
        int result=cxProductConstructionInfoMapper.updateCxProductConstructionInfo2(cxProductConstructionInfo);
        //Joran 2021-12-04 调用月度汇总重算接口
        reCalculateCauseConstructionChange();
        return result;
    }

    @Override
    public int updateProductionStage(CxProductConstructionInfo cxProductConstructionInfo) {
        cxProductConstructionInfo.setBaseVale(cxProductConstructionInfo.getId());
        int result=cxProductConstructionInfoMapper.updateProductionStage(cxProductConstructionInfo);
        return result;
    }



    /**
     * 批量删除投产施工信息
     *
     * @param ids 需要删除的投产施工信息ID
     * @return 结果
     */
    @Override
    public int deleteCxProductConstructionInfoByIds(Long[] ids) {
        int result=cxProductConstructionInfoMapper.deleteCxProductConstructionInfoByIds(ids);
        //Joran 2021-12-04 调用月度汇总重算接口
        reCalculateCauseConstructionChange();
        return result;
    }

    /**
     * 删除投产施工信息信息
     *
     * @param id 投产施工信息ID
     * @return 结果
     */
    @Override
    public int deleteCxProductConstructionInfoById(Long id)
    {
        int result=cxProductConstructionInfoMapper.deleteCxProductConstructionInfoById(id);
        //Joran 2021-12-04 调用月度汇总重算接口
        reCalculateCauseConstructionChange();
        return result;
    }

    /**
     * 校验投产施工信息唯一性
     */
    @Override
    public String checkCxProductConstructionInfoUnique(CxProductConstructionInfo cxProductConstructionInfo) {
        if (cxProductConstructionInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxProductConstructionInfo> list = cxProductConstructionInfoMapper.checkCxProductConstructionInfoUnique(cxProductConstructionInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入投产施工信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxProductConstructionInfo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxProductConstructionInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxProductConstructionInfo cxProductConstructionInfo = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxProductConstructionInfo);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxProductConstructionInfo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                cxProductConstructionInfo.setBaseVale(null);
                importList.add(cxProductConstructionInfo);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
//            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
//                successNum = importList.size();
//                    cxProductConstructionInfoMapper.mergeSql(importList);
//            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxProductConstructionInfo cxProductConstructionInfo = list.get(i);
                    // 错误记录跳过
                    if (cxProductConstructionInfo.getId() != null && cxProductConstructionInfo.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkCxProductConstructionInfoUnique(cxProductConstructionInfo);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxProductConstructionInfo(cxProductConstructionInfo);
                    } else {
                        /*failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);*/
                        cxProductConstructionInfoMapper.updateCxProductConstructionInfoByCodeAndVersion(cxProductConstructionInfo);
                    }
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        //Joran 2021-12-04 调用月度汇总重算接口
        reCalculateCauseConstructionChange();
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 获取胎胚版本列表
     * @param pc
     * @return
     */
    @Override
    public List<CxProductConstructionInfo> getEmbryoVersions(CxProductConstructionInfo pc){
        List<CxProductConstructionInfo> list = cxProductConstructionInfoMapper.getEmbryoVersions(pc);
        return list;
    }

    /**
     * 版本施工变更进行月度汇总接口重算调用
     */
    @Override
    public void reCalculateCauseConstructionChange() {
        //1.查询月度主表中最新的月度版本进行重算
//       MdmMonthPlanMain mdmMonthPlanMain= mdmMonthPlanMainService.selectNewestPlanMain();
//       if(mdmMonthPlanMain!=null&& StringUtils.isNotEmpty(mdmMonthPlanMain.getMonthPlanApsVersion())){
//           mdmMonthPlanAmountSumService.recalculateByApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
//       }
    }

	/**
	 * 生成指定工序相关的施工信息excle的字节数组
	 *
	 * @param procedureType    工序类型
	 * @param materialCodeList 物料编号列表
	 * @return
	 */
    @Override
	public byte[] createProcedureConstructionExcel(String procedureType, List<String> materialCodeList) {
//		if (CollectionUtil.isEmpty(materialCodeList)) {
//			throw new RuntimeException();
//		}
//		List<CxProductConstructionInfoDto> resultList = cxProductConstructionInfoMapper
//				.selectProcedureConstructionList(procedureType, materialCodeList);
//		if (CollectionUtil.isEmpty(resultList)) {
//			// 没有数据则直接提示导出失败
//			return null;
//		}
//		ExcelUtil<CxProductConstructionInfoDto> excelUtil = new ExcelUtil<>(CxProductConstructionInfoDto.class);
//		excelUtil.init(resultList, "Sheet1", Type.EXPORT);
//		ByteArrayOutputStream os = new ByteArrayOutputStream();
//		excelUtil.exportExcel(os);

//		return os.toByteArray();
        return null;
	}

	/**
	 * 半部件规则换算，将胎胚数换算成各半部件数量
	 *
	 * @param queryParams
	 * @return
	 */
	@Override
	public List<CxHalfPartConversion> conversionHalfPartPlan(CxHalfPartConversion queryParams) {
		// 空值校验
		if (StringUtils.isEmpty(queryParams.getEmbryoCode()) || StringUtils.isEmpty(queryParams.getBomDataVersion())
				|| queryParams.getQueryPlan() == null || queryParams.getScheduleDate() == null) {
			return new ArrayList<>();
		}
		List<CxHalfPartConversion> halfPartList = cxProductConstructionInfoMapper.conversionHalfPartPlan(
				queryParams.getEmbryoCode(), queryParams.getBomDataVersion(), queryParams.getQueryPlan(),
				queryParams.getScheduleDate());
		this.changeHalfPartMachineName(halfPartList);
		return halfPartList;
	}

	/**
	 * 将半部件列表中的机台ID替换成机台的名称
	 * @param halfPartList	半部件列表
	 */
	private void changeHalfPartMachineName(List<CxHalfPartConversion> halfPartList) {
		// 取出所有的半部件机台
		List<CxHalfPartConversion> machineList = cxProductConstructionInfoMapper.listAllHalfPartMachine();
		for (CxHalfPartConversion halfPart : halfPartList) {
			String machineId = halfPart.getMachineId();
			if (StringUtils.isEmpty(machineId)) {
				continue;
			}
			// 半部件类型
			String halfPartType;
			// 胎体布需要全部转换成1号的类型
			if (HALF_PART_TYPE_TIRE_FABRIC_2.equals(halfPart.getHalfPartType())
					|| HALF_PART_TYPE_TIRE_FABRIC_3.equals(halfPart.getHalfPartType())) {
				halfPartType = HALF_PART_TYPE_TIRE_FABRIC_1;
			} else {
				halfPartType = halfPart.getHalfPartType();
			}
			// 将ID穿转换成机台名称
			StringBuffer machineName = new StringBuffer();
			String[] ids = machineId.split(",");
			for (String id : ids) {
				String name = machineList.stream()
						.filter(m -> halfPartType.equals(m.getHalfPartType()) && id.equals(m.getId().toString()))
						.findFirst().map(CxHalfPartConversion::getMachineName).orElse("");
				if (StringUtils.isNotBlank(name)) {
					machineName.append(name).append(",");
				}
			}
			// 去掉末尾多余的逗号
			if (machineName.length() > 0) {
				machineName.setLength(machineName.length() - 1);
			}
			halfPart.setMachineName(machineName.toString());
		}
	}

    /**
     * 根据排程日期、半部件类型、半部件编码，查询排程表是否有对应排程，有则返回排程id
     *
     * @param queryParams 查询参数
     * @return 查询到的排程id
     */
    @Override
    public Long getScheduleResultByParams(CxHalfPartConversion queryParams) {
        return cxProductConstructionInfoMapper.getScheduleResultByParams(queryParams);
    }

    /**
     * 根据半部件类型代号查询对应的机台信息
     * @param queryParams 半部件类型代号
     * @return 机台id和机台名称
     */
    @Override
    public List<CxHalfPartConversion> getMachineInfoListByHalfPartType(CxHalfPartConversion queryParams) {
        return cxProductConstructionInfoMapper.getMachineInfoListByHalfPartType(queryParams);
    }
}
