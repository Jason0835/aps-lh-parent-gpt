package com.zlt.aps.common.engine.planmain;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.*;
import com.zlt.aps.common.engine.mapper.*;
import com.zlt.aps.common.engine.service.*;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxCloseOutRange;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;

/**
 * 月度计划剩余量汇总
 * @author Gim
 */
@Component("cxMonthAmountService")
@Slf4j
public class MdmMonthPlanAmountSumService {

    // ------------------------------------------------------------------------------------- 内部静态数据部分 -----------------------------------------------------------------------------------------
    private static final String APS_MAIN_PLAN = "APS";// 生成生产排程版本用
	private static final String IMPORT_MAIN_PLAN_VERSION = "IMPMDM";// 导入主计划版本
    private static final Integer DATA_SOURCE_FROM_MAIN = 0;// 数据来源主计划
    private static final Integer DATA_SOURCE_FROM_APS = 1;// 数据来源插单
	private static final Integer DATA_SOURCE_FROM_IMPORT = 2;// 数据来源导入
    private static String division = "\r\n---------------------------------------------------\r\n";// 日志分割符
    private static final Integer MODIFY = 1;// 修改
    private static final Integer ADD = 2;// 新增
    private static final Integer NEW_VERSION = 1;// 新版本
    private static final Integer RE_VERSION = 2;// 版本重算
    private final static Double DEFAULT_GDYY_BREADTH = new Double("1");// 钢带压延幅宽默认值：1
    private final static Double DEFAULT_XWYY_BREADTH = new Double("1.45");// 纤维压延幅宽默认值：1.45

    // ------------------------------------------------------------------------------------- 服务引入部分 -----------------------------------------------------------------------------------------
    @Autowired
    private MdmMonthProdPlanService mdmMonthProdPlanService;
    @Autowired
    private MdmMonthPlanMainService mdmMonthPlanMainService;
    @Autowired
    private MdmMonthPlanAnalysisService mdmMonthPlanAnalysisService;
//    @Resource
//    private TProductConstructionInfoMapper productConstructionInfoMapper;
    @Autowired
    private TCxMonthPlanSurplusService planSurplusService;
    @Autowired
    private TCxEmbryoMonthPlanSurplusService embryoMonthPlanSurplusService;
//    @Autowired
//    private TLhMonthStockService lhMonthStockService;
    @Resource
    private IncrementService versionService;
    @Autowired
    private CxTCd90BigRollService cd90BigRollService;
    @Autowired
    private CxTCd15BigRollService cd15BigRollService;
    @Autowired
    private CxTCd15ParamsService cd15ParamsService;
    @Autowired
    private CxTCd90ParamsService cd90ParamsService;
    @Resource
    private CxMonthStockCommonMapper cxMonthStockCommonMapper;
    @Resource
    private ParamsMapper paramsMapper;
    @Autowired
    private TSapEmbryoBadNumberService badNumberService;
    @Autowired
    private ProductConstructionService productConstructionService;
    // 半部件Service
    @Autowired
    private TGdcdMonthPlanSurplusService gdcdMonthPlanSurplusService;
    @Autowired
    private TGdyyMonthPlanSurplusService gdyyMonthPlanSurplusService;
    @Autowired
    private TGsqMonthPlanSurplusService gsqMonthPlanSurplusService;
    @Autowired
    private TLbcdMonthPlanSurplusService lbcdMonthPlanSurplusService;
    @Autowired
    private TNcMonthPlanSurplusService ncMonthPlanSurplusService;
    @Autowired
    private TTcMonthPlanSurplusService tcMonthPlanSurplusService;
    @Autowired
    private TTmMonthPlanSurplusService tmMonthPlanSurplusService;
    @Autowired
    private TTqMonthPlanSurplusService tqMonthPlanSurplusService;
    @Autowired
    private TXwyyMonthPlanSurplusService xwyyMonthPlanSurplusService;
    @Resource
    private TCxPlanProductStatusMapper planProductStatusMapper;
    @Resource
    private TSyncMps2ApsFacMapper syncMps2ApsFacMapper;
    @Resource
    private DayFinishMapper dayFinishMapper;
    @Autowired
    private HalfPartLogService halfPartLogService;
    @Resource
    private TMonthSumProcessLogMapper logMapper;
    @Autowired
    private TBaseMonthPlanSurplusService monthPlanSurplusService;
    @Autowired
    private TCxMonthPlanAdjustService adjustService;

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    // -------------------------------------------------------------------------------------汇算部分-----------------------------------------------------------------------------------------
    /**
     * 输入胎胚代码 + 数量换算各个半部件相应的消耗量
     * @param embryoCode 胎胚代码
     * @param num 数量
     * @return 消耗量
     */
    public MdmMonthPlanAnalysis getEmbryoConsumption(String embryoCode, Integer num, String bomDataVersion) {
        EngineConstructionInfo infoQuery = new EngineConstructionInfo();
        infoQuery.setEmbryoCode(embryoCode);
        infoQuery.setBomDataVersion(bomDataVersion);
        infoQuery.setDelFlag("0");
        // 施工信息
        Map<String, EngineProductConstructionInfo> infoMap = cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
        EngineProductConstructionInfo info = infoMap.get(GenerageMapKeyUtils.createMapKey(embryoCode, bomDataVersion));
        if (info == null) {
            return null;
        }
        // 大卷信息
        CxTCd15BigRoll cd15BigRoll = null;
        if (StringUtils.isNotBlank(info.getArticleCrownSpec())) {
            CxTCd15BigRoll cd15Query = new CxTCd15BigRoll();
            cd15Query.setBigRollCode(info.getArticleCrownSpec());
            cd15Query.setDelFlag("0");
            List<CxTCd15BigRoll> cd15BigRollList = cd15BigRollService.getByParams(cd15Query);
            if (!CollectionUtil.isEmpty(cd15BigRollList)) {
                cd15BigRoll = cd15BigRollList.get(0);
            }
        }
        CxTCd90BigRoll cd90BigRoll = null;
        if (StringUtils.isNotBlank(info.getCordSpec())) {
            CxTCd90BigRoll cx90Query = new CxTCd90BigRoll();
            cx90Query.setBigRollCode(info.getCordSpec());
            cx90Query.setDelFlag("0");
            List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByParams(cx90Query);
            if (!CollectionUtil.isEmpty(cd90BigRollList)) {
                cd90BigRoll = cd90BigRollList.get(0);
            }
        }
        // 大卷默认信息
        CxTCd15Params cd15Params = cd15ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        // 幅宽
        Double gdyyBreadth = getDoubleOrDefault(this.getGdyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_GDYY_BREADTH);
        Double xwyyBreadth = getDoubleOrDefault(this.getXwyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_XWYY_BREADTH);
        MdmMonthPlanAnalysis result = new MdmMonthPlanAnalysis();
        getEmbryoUnitConsumption(result, info, BigDecimal.valueOf(num), cd15BigRoll, cd90BigRoll, cd15Params, cd90Params, gdyyBreadth, xwyyBreadth);
        return result;
    }

    /**
     * 月计划总体汇总
     * @param planMainVersion 主计划版本
     * @param year 年
     * @param month 月
     * @param isFinal 是否定稿 0是1否
     */
    @Transactional
    public AjaxResult monthPlanAmountSum(String planMainVersion, String year, String month, Integer isFinal)  {
        if (StringUtils.isBlank(planMainVersion) || StringUtils.isBlank(year) || StringUtils.isBlank(month) || isFinal == null) {
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.income"));
        }
        // 判断 输入年、月 是否已经有数据了
        if (Integer.parseInt(month) < 10 && month.length() == 1) {
            month = "0" + month;
        }
        String toFinal = isFinal.toString();
        String toDeleteVersion = this.getToDeleteVersion(year, month, toFinal);
        // 日志记录
        MonthSumProcessLog log = new MonthSumProcessLog();
        log.setTitle("主计划下发分厂版本");
        StringBuilder logDetail = new StringBuilder();
        logDetail.append("【是否定稿：】").append(isFinal).append(division);
        logDetail.append("【主计划版本】：").append(planMainVersion).append(division);
        logDetail.append("【待删除版本】：").append(toDeleteVersion).append(division);
        Map<Long, MdmMonthProdPlan> toDeleteProPlanMap = new HashMap<>();
        if (StringUtils.isNotBlank(toDeleteVersion)) {
            // 旧计划明细数据
//            List<MdmMonthProdPlan> oldProdPlanList = mdmMonthProdPlanService.getByApsVersion(toDeleteVersion);
//            toDeleteProPlanMap = CollectionUtil.toMap(oldProdPlanList, MdmMonthProdPlan::getPlanSeq);
            mdmMonthProdPlanService.deleteByApsVersion(toDeleteVersion);
            mdmMonthPlanMainService.deleteByYearAndMonthAndIsFinal(year, month, isFinal.toString());
        }
        // 生成 生产排程计划版本主表、APS版本号
        MdmMonthPlanMain mdmMonthPlanMain = this.getMdmMonthPlanMain(planMainVersion, year, month, toFinal);
        // 根据主计划版本号 取主计划数据 存入 主计划月度生产计划表 （T_MDM_MONTH_PROD_PLAN）带上生产排程版本号
        List<TSyncMps2ApsFac> syncMps2ApsFacList = syncMps2ApsFacMapper.selectAllByProductionVersionAndIsDelete(planMainVersion);

        // 成型机台信息匹配
        List<CxMachineInfo> cxMachineInfoList = syncMps2ApsFacMapper.selectCxMachineInfoList();
        Map<String, CxMachineInfo> machineInfoMap = CollectionUtil.toMap(cxMachineInfoList, CxMachineInfo::getMachineName);
        if (CollectionUtil.isEmpty(syncMps2ApsFacList)) {
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.facPlan") + planMainVersion);
        }
        List<MdmMonthProdPlan> prodList = new ArrayList<>();
        for (TSyncMps2ApsFac sync : syncMps2ApsFacList) {
            if (StringUtils.isBlank(sync.getProcessCode())) {
                continue;
            }
            MdmMonthProdPlan prodPlan = new MdmMonthProdPlan();
            this.setBaseSysValue(prodPlan);
            prodPlan.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
            // 填充计划明细实体
            buildProdPlanCastor(sync, prodPlan, DATA_SOURCE_FROM_MAIN.toString(), machineInfoMap);
            prodList.add(prodPlan);
        }
        // 插入默认版本信息
        productConstructionService.initBomDataVersionByPlan(prodList);
        mdmMonthPlanMainService.insertMdmMonthPlanMain(mdmMonthPlanMain);
        mdmMonthProdPlanService.insertBatch(prodList);

        // 没施工不计算
        Boolean flag = Boolean.FALSE;
        for (MdmMonthProdPlan prodPlan : prodList) {
            if (StringUtils.isBlank(prodPlan.getBomDataVersion())) {
                flag = Boolean.TRUE;
                break;
            }
        }
        if (flag) {
            logDetail.append("【存在无施工信息的明细数据】").append(division);
            logDetail.append("【待删除版本】：").append(toDeleteVersion).append(division);
            // 删除
            planSurplusService.deleteByApsVersion(toDeleteVersion);
            embryoMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            tmMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            tcMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            ncMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            tqMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            gsqMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            gdcdMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            lbcdMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            gdyyMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            xwyyMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            // 删除投产表
            planProductStatusMapper.deleteByApsVersion(toDeleteVersion);
            // 删除月度分析汇总表
            mdmMonthPlanAnalysisService.deleteByApsVersion(toDeleteVersion);
            logDetail.append("【旧数据已删除完成】：").append(toDeleteVersion).append(division);
            log.setLogDetail(logDetail.toString());
            log.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
            this.setBaseSysValue(log);
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.construction.info"));
        }

        logDetail.append("【同步分厂明细数据】：").append(toJSONString(syncMps2ApsFacList)).append(division);
        logDetail.append("【旧计划明细数据】：").append(toJSONString(toDeleteProPlanMap)).append(division);
        // 月计划汇总
        this.buildMonthPlanSumCastor(isFinal, toDeleteVersion, mdmMonthPlanMain, prodList, log, logDetail, NEW_VERSION);
        return AjaxResult.success();
    }

//    /**
//     * 单规格新增
//     * @param prodPlan 新增实体
//     * @return
//     */
//    public AjaxResult unitProdPlanAdd(MdmMonthProdPlan prodPlan) {
//        // 日志记录
//        MonthSumProcessLog log = new MonthSumProcessLog();
//        log.setTitle("单规格新增");
//        String apsVersion = prodPlan.getMonthPlanApsVersion();
//        log.setMonthPlanApsVersion(apsVersion);
//        StringBuilder logDetail = new StringBuilder();
//        logDetail.append("【新增实体的APS版本】：").append(apsVersion).append(division);
//        logDetail.append("【计划明细】prodPlan：").append(toJSONString(prodPlan)).append(division);
//        return buildUnitCastor(prodPlan, 0L, log, logDetail, ADD);
//    }

//    /**
//     * 单规格调量
//     * @param preModifyQty 调整前的量
//     * @param prodPlan 调整后的实体
//     * @return
//     */
//    public AjaxResult unitProdPlanModify(Long preModifyQty, MdmMonthProdPlan prodPlan) {
//        if (preModifyQty.equals(prodPlan.getPlanModifyQty())) {
//            return AjaxResult.success();
//        }
//        Long modifyNum = prodPlan.getPlanModifyQty() - preModifyQty;
//        // 日志记录
//        MonthSumProcessLog log = new MonthSumProcessLog();
//        log.setTitle("单规格调量");
//        log.setMonthPlanApsVersion(prodPlan.getMonthPlanApsVersion());
//        StringBuilder logDetail = new StringBuilder();
//        logDetail.append("【调量APS版本】：").append(prodPlan.getMonthPlanApsVersion()).append(division);
//        logDetail.append("【调整前的量】preModifyQty：").append(toJSONString(preModifyQty)).append(division);
//        logDetail.append("【调整后的量】afterModifyQty：").append(toJSONString(prodPlan.getPlanModifyQty())).append(division);
//        logDetail.append("【计划明细】prodPlan：").append(toJSONString(prodPlan)).append(division);
//        logDetail.append("【实际调整量】modifyNum：").append(toJSONString(modifyNum)).append(division);
//        return buildUnitCastor(prodPlan, modifyNum, log, logDetail, MODIFY);
//    }

    /**
	 * 主计划导入
	 * 
	 * @param year            年
	 * @param month           月
	 * @param isFinal         是否定稿 0是1否
	 * @param prodList        主计划明细
	 */
    @Transactional
	public void importMonthPlan(String year, String month, Integer isFinal,
			List<MdmMonthProdPlan> prodList) {
		// 定稿标识，数据类型转换
		String toFinal = isFinal.toString();
		// 获取待删除生产排程版本
		String toDeleteVersion = this.getToDeleteVersion(year, month, toFinal);
        // 日志记录
        MonthSumProcessLog log = new MonthSumProcessLog();
        log.setTitle("生产计划表导入数据");
        StringBuilder logDetail = new StringBuilder();
        logDetail.append("【是否定稿：】").append(isFinal).append(division);
        logDetail.append("【待删除版本】：").append(toDeleteVersion).append(division);
		if (StringUtils.isNotBlank(toDeleteVersion)) {
//			// 旧计划明细数据
//			List<MdmMonthProdPlan> oldProdPlanList = mdmMonthProdPlanService.getByApsVersion(toDeleteVersion);
//            logDetail.append("【旧计划明细数据】：").append(toJSONString(oldProdPlanList)).append(division);
//			Map<String, MdmMonthProdPlan> toDeleteProPlanMap = oldProdPlanList.stream().collect(
//					Collectors.toMap(v -> this.createImportMonthPlanKey(v), Function.identity(), (v1, v2) -> v2));
			// 删除旧版本主表记录
			mdmMonthProdPlanService.deleteByApsVersion(toDeleteVersion);
            mdmMonthPlanMainService.deleteByYearAndMonthAndIsFinal(year, month, isFinal.toString());
//			// 遍历明细记录
//			for (MdmMonthProdPlan prodPlan : prodList) {
//				MdmMonthProdPlan oldPlan = toDeleteProPlanMap.get(this.createImportMonthPlanKey(prodPlan));
//				if (oldPlan != null) {
//				    // 转移旧计划明细数据
//                    transferProdPlanDataCastor(prodPlan, oldPlan);
//                }
//			}
		}
		// 导入的主计划版本由后台自动生成
		String planMainVersion = versionService.getSequence(getVersionPre(IMPORT_MAIN_PLAN_VERSION), 2);
		// 生成 生产排程计划版本主表、APS版本号
		MdmMonthPlanMain mdmMonthPlanMain = this.getMdmMonthPlanMain(planMainVersion, year, month, toFinal);
		// 明细的主计划排程版本与主表的APS版本号一致
		prodList.stream().forEach(v -> {
			v.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
			v.setDataSource(DATA_SOURCE_FROM_IMPORT.toString());
		});
        // 放入版本信息
        productConstructionService.initBomDataVersionByPlan(prodList);
        // 月度计划主表入库
        mdmMonthPlanMainService.insertMdmMonthPlanMain(mdmMonthPlanMain);
        log.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());

        // 月度计划明细记录入库
        mdmMonthProdPlanService.insertBatch(prodList);
        // 没施工不计算
        Boolean flag = Boolean.FALSE;
        for (MdmMonthProdPlan prodPlan : prodList) {
            if (StringUtils.isBlank(prodPlan.getBomDataVersion())) {
                flag = Boolean.TRUE;
                break;
            }
        }
        if (flag) {
            logDetail.append("【存在无施工信息的明细数据】").append(division);
            logDetail.append("【待删除版本】：").append(toDeleteVersion).append(division);
            // 删除
            planSurplusService.deleteByApsVersion(toDeleteVersion);
            embryoMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            tmMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            tcMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            ncMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            tqMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            gsqMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            gdcdMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            lbcdMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            gdyyMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            xwyyMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
            // 删除投产表
            planProductStatusMapper.deleteByApsVersion(toDeleteVersion);
            // 删除月度分析汇总表
            mdmMonthPlanAnalysisService.deleteByApsVersion(toDeleteVersion);
            logDetail.append("【旧数据已删除完成】：").append(toDeleteVersion).append(division);
            log.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
            log.setLogDetail(logDetail.toString());
            this.setBaseSysValue(log);
            logMapper.insert(log);
            return ;
        }

		// 月计划汇总
		this.buildMonthPlanSumCastor(isFinal, toDeleteVersion, mdmMonthPlanMain, prodList, log, logDetail, NEW_VERSION);
	}

    /**
     * 根据版本号进行重算
     * @param apsVersion 生产排程版本
     */
    public void recalculateByApsVersion(String apsVersion) {
        // 日志记录
        MonthSumProcessLog log = new MonthSumProcessLog();
        log.setTitle("版本重算");
        log.setMonthPlanApsVersion(apsVersion);
        StringBuilder logDetail = new StringBuilder();
        logDetail.append("【待重算版本】：").append(apsVersion).append(division);
        if (StringUtils.isBlank(apsVersion)) {
            logDetail.append("【无生产排程版本】");
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return;
        }
        MdmMonthPlanMain query = new MdmMonthPlanMain();
        query.setMonthPlanApsVersion(apsVersion);
        // 没版本主表不计算
        List<MdmMonthPlanMain> mdmMonthPlanMains = mdmMonthPlanMainService.selectMdmMonthPlanMainList(query);
        if (CollectionUtil.isEmpty(mdmMonthPlanMains)) {
            logDetail.append("【无生产排程版本主表】");
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return;
        }
        // 重算获取插单数据
//        List<EmbryoVersionVo> reList = embryoMonthPlanSurplusService.getEmbryoInsertVo(apsVersion);
        // 2021.12.3 重算不重算插单部分
        // 重算获取明细数据
        List<MdmMonthProdPlan> prodPlanList = mdmMonthProdPlanService.getByApsVersionOld(apsVersion);
        // 没施工不计算
        Boolean flag = Boolean.FALSE;
        for (MdmMonthProdPlan prodPlan : prodPlanList) {
            if (StringUtils.isBlank(prodPlan.getBomDataVersion())) {
                flag = Boolean.TRUE;
                break;
            }
        }
        // 没施工不计算
        if (flag) {
            logDetail.append("【存在无施工信息的明细数据】");
            log.setLogDetail(logDetail.toString());
            this.setBaseSysValue(log);
            logMapper.insert(log);
            return;
        }
        // 月计划汇总
        this.buildMonthPlanSumCastor(0, apsVersion, mdmMonthPlanMains.get(0), prodPlanList, log, logDetail, RE_VERSION);
//            buildHalfPartByEmbryoCastorNew(apsVersion, reList, log, logDetail);
    }

    /**
     * 月计划汇总 汇算部分
     * @param isFinal 是否定稿 0是1否
     * @param toDeleteVersion 待删除版本
     * @param mdmMonthPlanMain 主计划版本
     * @param prodList 计划明细表
     */
    @Transactional
    public void buildMonthPlanSumCastor(Integer isFinal, String toDeleteVersion, MdmMonthPlanMain mdmMonthPlanMain, List<MdmMonthProdPlan> prodList,
                                        MonthSumProcessLog log, StringBuilder logDetail, Integer calFlag) {
        // 计算过程记录
        log.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
        // 旧数据记录
        Map<String, TCxMonthPlanSurplus> toDeleteCxMap = new HashMap<>();
        Map<String, TCxEmbryoMonthPlanSurplus> toDeleteEmbryoMap = new HashMap<>();
        Map<String, TTmMonthPlanSurplus> toDeleteTmMap = new HashMap<>();
        Map<String, TTcMonthPlanSurplus> toDeleteTcMap = new HashMap<>();
        Map<String, TNcMonthPlanSurplus> toDeleteNcMap = new HashMap<>();
        Map<String, TTqMonthPlanSurplus> toDeleteTqMap = new HashMap<>();
        Map<String, TGsqMonthPlanSurplus> toDeleteGsqMap = new HashMap<>();
        Map<String, TGdcdMonthPlanSurplus> toDeleteCd15Map = new HashMap<>();
        Map<String, TLbcdMonthPlanSurplus> toDeleteCd90Map = new HashMap<>();
        Map<String, TGdyyMonthPlanSurplus> toDeleteGdyyMap = new HashMap<>();
        Map<String, TXwyyMonthPlanSurplus> toDeleteXwyyMap = new HashMap<>();
        // 定稿才插入投产表
        List<TCxPlanProductStatus> oldProductStatusList = new ArrayList<>();
        if (isFinal == 0) {
            oldProductStatusList = planProductStatusMapper.selectAllByMonthPlanApsVersion(toDeleteVersion);
        }
        if (StringUtils.isNotBlank(toDeleteVersion)) {
            // 存在旧版本则删除数据
            // 成型和半部件 定稿才删除
            if (isFinal == 0) {
                TCxMonthPlanSurplus cxQuery = new TCxMonthPlanSurplus();
                cxQuery.setMonthPlanApsVersion(toDeleteVersion);
                cxQuery.setDelFlag("0");
                List<TCxMonthPlanSurplus> toDeleteCxList = planSurplusService.getByParams(cxQuery);
//                if (!CollectionUtil.isEmpty(toDeleteCxList)) {
//                    toDeleteCxMap = CollectionUtil.toMap(toDeleteCxList, TCxMonthPlanSurplus::getSapCode);
//                }
                TCxEmbryoMonthPlanSurplus embryoQuery = new TCxEmbryoMonthPlanSurplus();
                embryoQuery.setMonthPlanApsVersion(toDeleteVersion);
                embryoQuery.setDelFlag("0");
                List<TCxEmbryoMonthPlanSurplus> toDeleteEmbryoList = embryoMonthPlanSurplusService.getByParams(embryoQuery);
//                if (!CollectionUtil.isEmpty(toDeleteEmbryoList)) {
//                    toDeleteEmbryoMap = CollectionUtil.toMap(toDeleteEmbryoList, TCxEmbryoMonthPlanSurplus::getMaterialCode);
//                }
                List<TTmMonthPlanSurplus> tmList = tmMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteTmMap = buildToDeleteMap(tmList);
                List<TTcMonthPlanSurplus> tcList = tcMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteTcMap = buildToDeleteMap(tcList);
                List<TNcMonthPlanSurplus> ncList = ncMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteNcMap = buildToDeleteMap(ncList);
                List<TTqMonthPlanSurplus> tqList = tqMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteTqMap = buildToDeleteMap(tqList);
                List<TGsqMonthPlanSurplus> gsqList = gsqMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteGsqMap = buildToDeleteMap(gsqList);
                List<TGdcdMonthPlanSurplus> cd15List = gdcdMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteCd15Map = buildToDeleteMap(cd15List);
                List<TLbcdMonthPlanSurplus> cd90List = lbcdMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteCd90Map = buildToDeleteMap(cd90List);
                List<TGdyyMonthPlanSurplus> gdyyList = gdyyMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteGdyyMap = buildToDeleteMap(gdyyList);
                List<TXwyyMonthPlanSurplus> xwyyList = xwyyMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//                toDeleteXwyyMap = buildToDeleteMap(xwyyList);
                // 删除旧数据
                toDeleteNinePlan(toDeleteVersion, toDeleteCxList, toDeleteEmbryoList, tmList, tcList, ncList, tqList, gsqList, cd15List, cd90List, gdyyList, xwyyList, logDetail);
                // 删除投产表
                planProductStatusMapper.deleteByApsVersion(toDeleteVersion);
                logDetail.append("【旧投产表已删除完成】：").append(toDeleteVersion).append(division);
            }
            // 删除月度分析汇总表
            mdmMonthPlanAnalysisService.deleteByApsVersion(toDeleteVersion);
            logDetail.append("【旧月度分析汇总表已删除完成】：").append(toDeleteVersion).append(division);
        }
        logDetail.append("【旧数据部分】").append(division);
        logDetail.append("【成型旧数据】：").append(toJSONString(toDeleteCxMap)).append(division)
                    .append("【胎胚旧数据】：").append(toJSONString(toDeleteEmbryoMap)).append(division)
                    .append("【胎面旧数据】：").append(toJSONString(toDeleteTmMap)).append(division)
                    .append("【胎侧旧数据】：").append(toJSONString(toDeleteTcMap)).append(division)
                    .append("【内衬旧数据】：").append(toJSONString(toDeleteNcMap)).append(division)
                    .append("【胎圈旧数据】：").append(toJSONString(toDeleteTqMap)).append(division)
                    .append("【钢丝圈旧数据】：").append(toJSONString(toDeleteGsqMap)).append(division)
                    .append("【15度裁断旧数据】：").append(toJSONString(toDeleteCd15Map)).append(division)
                    .append("【90度裁断旧数据】：").append(toJSONString(toDeleteCd90Map)).append(division)
                    .append("【钢带压延旧数据】：").append(toJSONString(toDeleteGdyyMap)).append(division)
                    .append("【纤维压延旧数据】：").append(toJSONString(toDeleteXwyyMap)).append(division)
        ;

        // 定稿才插入
        if (isFinal == 0) {
            logDetail.append("【旧投产表】:").append(toJSONString(oldProductStatusList)).append(division);
            HashMap<String, List<MdmMonthProdPlan>> planMap = new HashMap<>();
            HashMap<String, List<MdmMonthProdPlan>> prodMap = new HashMap<>();
            for (MdmMonthProdPlan prodPlan : prodList) {
                List<MdmMonthProdPlan> existList1 = planMap.computeIfAbsent(GenerageMapKeyUtils.createMapKey(prodPlan.getMaterialCode(), prodPlan.getEmbryoCode(), prodPlan.getBomDataVersion()), k -> new ArrayList<>());
                existList1.add(prodPlan);
                List<MdmMonthProdPlan> existList2 = prodMap.computeIfAbsent(GenerageMapKeyUtils.createMapKey(prodPlan.getEmbryoCode(), prodPlan.getBomDataVersion()), k -> new ArrayList<>());
                existList2.add(prodPlan);
            }
            List<TCxPlanProductStatus> productStatusList = new ArrayList<>();
            Map<String, TCxPlanProductStatus> oldProductStatusMap = CollectionUtil.toMap(oldProductStatusList, obj -> GenerageMapKeyUtils.createMapKey(obj.getSapCode(), obj.getEmbryoCode(), obj.getBomDataVersion()));
            for (String key : planMap.keySet()) {
                TCxPlanProductStatus status = buildProductStatus(mdmMonthPlanMain, planMap, key, oldProductStatusMap);
                productStatusList.add(status);
            }
            // 插入投产表
            planProductStatusMapper.insertBatch(productStatusList);
            logDetail.append("【计划部分】").append(division);
            logDetail.append("【计划明细】：").append(toJSONString(prodList)).append(division);
            logDetail.append("【新投产表】:").append(toJSONString(productStatusList)).append(division);
            // 获取旧版本插单数据
            // 2021.12.3 重算不需要插单数据
//            List<EmbryoVersionVo> oldInsertList = embryoMonthPlanSurplusService.getEmbryoInsertVo(toDeleteVersion);
            List<EmbryoVersionVo> dataList = new ArrayList<>();
            for (String code : prodMap.keySet()) {
                List<MdmMonthProdPlan> prodPlans = prodMap.get(code);
                EmbryoVersionVo embryoVersionVo = new EmbryoVersionVo();
                embryoVersionVo.setEmbryoCode(prodPlans.get(0).getEmbryoCode());
                embryoVersionVo.setBomDataVersion(prodPlans.get(0).getBomDataVersion());
                Integer totalNum = 0;
                for (MdmMonthProdPlan prodPlan : prodPlans) {
                    totalNum += prodPlan.getActualArrangement() == null ? 0 : prodPlan.getActualArrangement().intValue();
                }
                embryoVersionVo.setTotalPlanQty(totalNum);
                dataList.add(embryoVersionVo);
            }
            // 半部件插入
            insertHalfPart(mdmMonthPlanMain, toDeleteCxMap, toDeleteEmbryoMap, toDeleteTmMap, toDeleteTcMap, toDeleteNcMap,
                    toDeleteTqMap, toDeleteGsqMap, toDeleteCd15Map, toDeleteCd90Map, toDeleteGdyyMap, toDeleteXwyyMap,
                    prodList, logDetail, dataList, toDeleteVersion, calFlag);
        }
        // 日志插入
        log.setLogDetail(logDetail.toString());
        this.setBaseSysValue(log);
        logMapper.insert(log);
    }

    /**
     * 插单重算半部件部分
     * @param apsVersion aps版本
     * @param embryoMonthPlanSurplus 胎胚工序
     */
    public AjaxResult recalculateHalfPartByInsertEmbryo(String apsVersion, TCxEmbryoMonthPlanSurplus embryoMonthPlanSurplus) {
        // 日志记录
        MonthSumProcessLog log = new MonthSumProcessLog();
        log.setTitle("插单");
        log.setMonthPlanApsVersion(apsVersion);
        StringBuilder logDetail = new StringBuilder();
        logDetail.append("【待插单版本】：").append(apsVersion).append(division);
        if (embryoMonthPlanSurplus == null) {
            logDetail.append("插单胎胚不存在");
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.embryo.empty") + apsVersion);
        }
        // 版本数据
        MdmMonthPlanMain query = new MdmMonthPlanMain();
        query.setMonthPlanApsVersion(apsVersion);
        List<MdmMonthPlanMain> monthPlanMains = mdmMonthPlanMainService.selectMdmMonthPlanMainList(query);
        if (CollectionUtil.isEmpty(monthPlanMains)) {
            logDetail.append("计划版本不存在");
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.apsVersion.empty"));
        }
        MdmMonthPlanMain mdmMonthPlanMain = monthPlanMains.get(0);
        logDetail.append("【版本信息】：").append(toJSONString(mdmMonthPlanMain)).append(division);
        logDetail.append("【胎胚计划汇总表】：").append(toJSONString(embryoMonthPlanSurplus)).append(division);
        // 施工信息
        EngineConstructionInfo queryInfo = new EngineConstructionInfo();
        queryInfo.setBomDataVersion(embryoMonthPlanSurplus.getBomDataVersion());
        queryInfo.setEmbryoCode(embryoMonthPlanSurplus.getMaterialCode());
        // 施工信息
        Map<String, EngineProductConstructionInfo> totalInfoMap = cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
        List<EngineProductConstructionInfo> constructionInfoList = new ArrayList<>();
        EngineProductConstructionInfo thisInfo = totalInfoMap.get(GenerageMapKeyUtils.createMapKey(embryoMonthPlanSurplus.getMaterialCode(), embryoMonthPlanSurplus.getBomDataVersion()));
        if (thisInfo != null) {
            constructionInfoList.add(thisInfo);
        }
        if (CollectionUtil.isEmpty(constructionInfoList)) {
            logDetail.append("施工信息为空");
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.construction.info"));
        }
        logDetail.append("【施工信息】：").append(toJSONString(constructionInfoList)).append(division);
        // 胎胚map
        Map<String, EngineProductConstructionInfo> embryoCodeMap = new HashMap<>();
        // 大卷信息
        List<String> beltSpecList = new ArrayList<>();
        List<String> crodSpecList = new ArrayList<>();
        for (EngineProductConstructionInfo info : constructionInfoList) {
            embryoCodeMap.put(GenerageMapKeyUtils.createMapKey(info.getEmbryoCode(), info.getEmbryoVersion()), info);
            if (StringUtils.isNotBlank(info.getArticleCrownSpec())) {
                beltSpecList.add(info.getArticleCrownSpec());
            }
            if (StringUtils.isNotBlank(info.getCordSpec())) {
                crodSpecList.add(info.getCordSpec());
            }
        }
        List<CxTCd15BigRoll> cd15BigRollList = cd15BigRollService.getByBeltSpecList(beltSpecList);
        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(crodSpecList);
        Map<String, CxTCd15BigRoll> cd15BigRollMap = CollectionUtil.toMap(cd15BigRollList, CxTCd15BigRoll::getBigRollCode);
        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
        // 大卷默认信息
        CxTCd15Params cd15Params = cd15ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        logDetail.append("【CD15大卷信息】：").append(toJSONString(cd15BigRollMap)).append(division);
        logDetail.append("【CD90大卷信息】：").append(toJSONString(cd90BigRollMap)).append(division);
        logDetail.append("【CD15大卷默认信息】：").append(toJSONString(cd15Params)).append(division);
        logDetail.append("【CD90大卷默认信息】：").append(toJSONString(cd90Params)).append(division);
        // 幅宽
        Double gdyyBreadth = getDoubleOrDefault(this.getGdyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_GDYY_BREADTH);
        Double xwyyBreadth = getDoubleOrDefault(this.getXwyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_XWYY_BREADTH);
        logDetail.append("【钢带压延幅宽】：").append(gdyyBreadth).append(division);
        logDetail.append("【纤维压延幅宽】：").append(gdyyBreadth).append(division);

        // 后续半部件调量用的分析汇总实体
        EngineProductConstructionInfo constructionInfo = embryoCodeMap.get(GenerageMapKeyUtils.createMapKey(embryoMonthPlanSurplus.getMaterialCode(), embryoMonthPlanSurplus.getBomDataVersion()));
        MdmMonthPlanAnalysis modifyAnalysis = new MdmMonthPlanAnalysis();
        getEmbryoUnitConsumption(modifyAnalysis, constructionInfo, embryoMonthPlanSurplus.getMonthPlanModifyQty(),
                cd15BigRollMap.get(constructionInfo.getArticleCrownSpec()), cd90BigRollMap.get(constructionInfo.getCordSpec()),
                cd15Params, cd90Params, gdyyBreadth, xwyyBreadth);

        // 重算半部件部分
        List<String> tmCodeList = new ArrayList<>();
        List<String> tcCodeList = new ArrayList<>();
        List<String> ncCodeList = new ArrayList<>();
        List<String> cd15CodeList = new ArrayList<>();
        List<String> cd90CodeList = new ArrayList<>();
        List<String> tqCodeList = new ArrayList<>();
        List<String> gsqCodeList = new ArrayList<>();
        List<String> gdyyCodeList = new ArrayList<>();
        List<String> xwyyCodeList = new ArrayList<>();
        // 填充物料编号List
        this.buildCodeList(constructionInfoList, tmCodeList, tcCodeList, ncCodeList, cd15CodeList, cd90CodeList, tqCodeList, gsqCodeList, gdyyCodeList, xwyyCodeList);
        // 旧半部件数据
        List<TTmMonthPlanSurplus> oldTmList = tmMonthPlanSurplusService.getByCodeList(apsVersion, tmCodeList);
        Map<String, TTmMonthPlanSurplus> oldTmMap = buildToDeleteMap(oldTmList);
        List<TTcMonthPlanSurplus> oldTcList = tcMonthPlanSurplusService.getByCodeList(apsVersion, tcCodeList);
        Map<String, TTcMonthPlanSurplus> oldTcMap = buildToDeleteMap(oldTcList);
        List<TNcMonthPlanSurplus> oldNcList = ncMonthPlanSurplusService.getByCodeList(apsVersion, ncCodeList);
        Map<String, TNcMonthPlanSurplus> oldNcMap = buildToDeleteMap(oldNcList);
        List<TTqMonthPlanSurplus> oldTqList = tqMonthPlanSurplusService.getByCodeList(apsVersion, tqCodeList);
        Map<String, TTqMonthPlanSurplus> oldTqMap = buildToDeleteMap(oldTqList);
        List<TGsqMonthPlanSurplus> oldGsqList = gsqMonthPlanSurplusService.getByCodeList(apsVersion, gsqCodeList);
        Map<String, TGsqMonthPlanSurplus> oldGsqMap = buildToDeleteMap(oldGsqList);
        List<TGdcdMonthPlanSurplus> oldCd15List = gdcdMonthPlanSurplusService.getByCodeList(apsVersion, cd15CodeList);
        Map<String, TGdcdMonthPlanSurplus> oldCd15Map = buildToDeleteMap(oldCd15List);
        List<TLbcdMonthPlanSurplus> oldCd90List = lbcdMonthPlanSurplusService.getByCodeList(apsVersion, cd90CodeList);
        Map<String, TLbcdMonthPlanSurplus> oldCd90Map = buildToDeleteMap(oldCd90List);
        List<TGdyyMonthPlanSurplus> oldGdyyList = gdyyMonthPlanSurplusService.getByCodeList(apsVersion, gdyyCodeList);
        Map<String, TGdyyMonthPlanSurplus> oldGdyyMap = buildToDeleteMap(oldGdyyList);
        List<TXwyyMonthPlanSurplus> oldXwyyList = xwyyMonthPlanSurplusService.getByCodeList(apsVersion, xwyyCodeList);
        Map<String, TXwyyMonthPlanSurplus> oldXwyyMap = buildToDeleteMap(oldXwyyList);
        // 更新半部件数据Map
        this.buildUnitModifyHalfPartCastor(logDetail, mdmMonthPlanMain, modifyAnalysis, oldTmMap, oldTcMap, oldNcMap, oldTqMap, oldGsqMap, oldCd15Map, oldCd90Map, oldGdyyMap, oldXwyyMap, ADD);
        // 更新
        this.updateUnitModify(log, logDetail, null, null, null, oldTmMap, oldTcMap, oldNcMap, oldTqMap, oldGsqMap, oldCd15Map, oldCd90Map, oldGdyyMap, oldXwyyMap);
        return AjaxResult.success();

    }

//    /**
//     * 根据aps版本 + 胎胚工序重算
//     * @param apsVersion aps版本
//     * @param embryoMonthPlanSurplusList 胎胚工序
//     */
//    public AjaxResult buildHalfPartByEmbryoCastor(String apsVersion, List<TCxEmbryoMonthPlanSurplus> embryoMonthPlanSurplusList) {
//        if (CollectionUtil.isEmpty(embryoMonthPlanSurplusList)) {
//            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.embryo.empty") + apsVersion);
//        }
//        MdmMonthPlanMain query = new MdmMonthPlanMain();
//        query.setMonthPlanApsVersion(apsVersion);
//        List<MdmMonthPlanMain> monthPlanMains = mdmMonthPlanMainService.selectMdmMonthPlanMainList(query);
//        if (CollectionUtil.isEmpty(monthPlanMains)) {
//            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.apsVersion.empty") + apsVersion);
//        }
//        MdmMonthPlanMain mdmMonthPlanMain = monthPlanMains.get(0);
//        // 日志记录
//        MonthSumProcessLog log = new MonthSumProcessLog();
//        log.setTitle("版本重算");
//        log.setMonthPlanApsVersion(apsVersion);
//        StringBuilder logDetail = new StringBuilder();
//        logDetail.append("【待重算版本】：").append(apsVersion).append(division);
//        List<String> embryoCodeList = CollectionUtil.propertiesToList(embryoMonthPlanSurplusList, TCxEmbryoMonthPlanSurplus::getMaterialCode);
//        logDetail.append("【胎胚代码List】：").append(toJSONString(embryoCodeList)).append(division);
//        // 施工信息
//        List<EngineConstructionInfo> engineConstructionInfos = constructionInfoService.selectEngineConstructionInfoListBatch(embryoCodeList);
//        logDetail.append("【施工信息】：").append(toJSONString(engineConstructionInfos)).append(division);
//        List<String> tmCodeList = new ArrayList<>();
//        List<String> tcCodeList = new ArrayList<>();
//        List<String> ncCodeList = new ArrayList<>();
//        List<String> cd15CodeList = new ArrayList<>();
//        List<String> cd90CodeList = new ArrayList<>();
//        List<String> tqCodeList = new ArrayList<>();
//        List<String> gsqCodeList = new ArrayList<>();
//        List<String> gdyyCodeList = new ArrayList<>();
//        List<String> xwyyCodeList = new ArrayList<>();
//        buildCodeList(engineConstructionInfos, tmCodeList, tcCodeList, ncCodeList, cd15CodeList, cd90CodeList, tqCodeList, gsqCodeList, gdyyCodeList, xwyyCodeList);
//        // 旧半部件数据
//        List<TTmMonthPlanSurplus> oldTmList = tmMonthPlanSurplusService.getByCodeList(apsVersion, tmCodeList);
//        Map<String, TTmMonthPlanSurplus> toDeleteTmMap = buildToDeleteMap(oldTmList);
//        List<TTcMonthPlanSurplus> oldTcList = tcMonthPlanSurplusService.getByCodeList(apsVersion, tcCodeList);
//        Map<String, TTcMonthPlanSurplus> toDeleteTcMap = buildToDeleteMap(oldTcList);
//        List<TNcMonthPlanSurplus> oldNcList = ncMonthPlanSurplusService.getByCodeList(apsVersion, ncCodeList);
//        Map<String, TNcMonthPlanSurplus> toDeleteNcMap = buildToDeleteMap(oldNcList);
//        List<TTqMonthPlanSurplus> oldTqList = tqMonthPlanSurplusService.getByCodeList(apsVersion, tqCodeList);
//        Map<String, TTqMonthPlanSurplus> toDeleteTqMap = buildToDeleteMap(oldTqList);
//        List<TGsqMonthPlanSurplus> oldGsqList = gsqMonthPlanSurplusService.getByCodeList(apsVersion, gsqCodeList);
//        Map<String, TGsqMonthPlanSurplus> toDeleteGsqMap = buildToDeleteMap(oldGsqList);
//        List<TGdcdMonthPlanSurplus> oldCd15List = gdcdMonthPlanSurplusService.getByCodeList(apsVersion, cd15CodeList);
//        Map<String, TGdcdMonthPlanSurplus> toDeleteCd15Map = buildToDeleteMap(oldCd15List);
//        List<TLbcdMonthPlanSurplus> oldCd90List = lbcdMonthPlanSurplusService.getByCodeList(apsVersion, cd90CodeList);
//        Map<String, TLbcdMonthPlanSurplus> toDeleteCd90Map = buildToDeleteMap(oldCd90List);
//        List<TGdyyMonthPlanSurplus> oldGdyyList = gdyyMonthPlanSurplusService.getByCodeList(apsVersion, gdyyCodeList);
//        Map<String, TGdyyMonthPlanSurplus> toDeleteGdyyMap = buildToDeleteMap(oldGdyyList);
//        List<TXwyyMonthPlanSurplus> oldXwyyList = xwyyMonthPlanSurplusService.getByCodeList(apsVersion, xwyyCodeList);
//        Map<String, TXwyyMonthPlanSurplus> toDeleteXwyyMap = buildToDeleteMap(oldXwyyList);
//        logDetail.append("【旧数据部分】").append(division);
//        logDetail.append("【胎面旧数据】：").append(toJSONString(toDeleteTmMap)).append(division)
//                .append("【胎侧旧数据】：").append(toJSONString(toDeleteTcMap)).append(division)
//                .append("【内衬旧数据】：").append(toJSONString(toDeleteNcMap)).append(division)
//                .append("【胎圈旧数据】：").append(toJSONString(toDeleteTqMap)).append(division)
//                .append("【钢丝圈旧数据】：").append(toJSONString(toDeleteGsqMap)).append(division)
//                .append("【15度裁断旧数据】：").append(toJSONString(toDeleteCd15Map)).append(division)
//                .append("【90度裁断旧数据】：").append(toJSONString(toDeleteCd90Map)).append(division)
//                .append("【钢带压延旧数据】：").append(toJSONString(toDeleteGdyyMap)).append(division)
//                .append("【纤维压延旧数据】：").append(toJSONString(toDeleteXwyyMap)).append(division)
//        ;
//        // 删除旧数据
//        toDeleteNinePlanExceptCxTp(apsVersion);
////        toDeleteNinePlan(apsVersion, tmCodeList, tcCodeList, ncCodeList, cd15CodeList, cd90CodeList, tqCodeList, gsqCodeList, gdyyCodeList, xwyyCodeList);
//        // 新半部件List
//        List<TTmMonthPlanSurplus> tmList = new ArrayList<>();
//        List<TTcMonthPlanSurplus> tcList = new ArrayList<>();
//        List<TNcMonthPlanSurplus> ncList = new ArrayList<>();
//        List<TGdcdMonthPlanSurplus> cd15List = new ArrayList<>();
//        List<TLbcdMonthPlanSurplus> cd90List = new ArrayList<>();
//        List<TTqMonthPlanSurplus> tqList = new ArrayList<>();
//        List<TGsqMonthPlanSurplus> gsqList = new ArrayList<>();
//        List<TGdyyMonthPlanSurplus> gdyyList = new ArrayList<>();
//        List<TXwyyMonthPlanSurplus> xwyyList = new ArrayList<>();
//        // 胎胚map
//        Map<String, EngineConstructionInfo> embryoCodeMap = new HashMap<>();
//        // 大卷信息
//        List<String> beltSpecList = new ArrayList<>();
//        List<String> crodSpecList = new ArrayList<>();
//        for (EngineConstructionInfo info : engineConstructionInfos) {
//            embryoCodeMap.put(info.getEmbryoCode(), info);
//            if (StringUtils.isNotBlank(info.getArticleCrownSpec())) {
//                beltSpecList.add(info.getArticleCrownSpec());
//            }
//            if (StringUtils.isNotBlank(info.getCordSpec())) {
//                crodSpecList.add(info.getCordSpec());
//            }
//        }
//        List<CxTCd15BigRoll> cd15BigRollList = cd15BigRollService.getByBeltSpecList(beltSpecList);
//        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(crodSpecList);
//        Map<String, CxTCd15BigRoll> cd15BigRollMap = CollectionUtil.toMap(cd15BigRollList, CxTCd15BigRoll::getBigRollCode);
//        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
//        // 大卷默认信息
//        CxTCd15Params cd15Params = cd15ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
//        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
//        logDetail.append("【CD15大卷信息】：").append(toJSONString(cd15BigRollMap)).append(division);
//        logDetail.append("【CD90大卷信息】：").append(toJSONString(cd90BigRollMap)).append(division);
//        logDetail.append("【CD15大卷默认信息】：").append(toJSONString(cd15Params)).append(division);
//        logDetail.append("【CD90大卷默认信息】：").append(toJSONString(cd90Params)).append(division);
//        List<MdmMonthPlanAnalysis> analysisList = new ArrayList<>();
//        // 幅宽
//        Double gdyyBreadth = getDoubleOrDefault(this.getGdyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_GDYY_BREADTH);
//        Double xwyyBreadth = getDoubleOrDefault(this.getXwyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_XWYY_BREADTH);
//        logDetail.append("【钢带压延幅宽】：").append(gdyyBreadth).append(division);
//        logDetail.append("【纤维压延幅宽】：").append(gdyyBreadth).append(division);
//        for (TCxEmbryoMonthPlanSurplus embryoMonthPlanSurplus : embryoMonthPlanSurplusList) {
//            // 月计划量 + 调整量 + 不良 - 月结库存 = 最终月计划量
//            BigDecimal planNum = embryoMonthPlanSurplus.getMonthPlanQty().add(embryoMonthPlanSurplus.getMonthPlanModifyQty()).add(embryoMonthPlanSurplus.getEmbryoBadQty()).subtract(embryoMonthPlanSurplus.getLastMonthStock());
//            MdmMonthPlanAnalysis analysis = new MdmMonthPlanAnalysis();
//            EngineConstructionInfo constructionInfo = embryoCodeMap.get(embryoMonthPlanSurplus.getMaterialCode());
//            // 单耗换算
//            analysis.setEmbryoCode(embryoMonthPlanSurplus.getMaterialCode());
//            getEmbryoUnitConsumption(analysis, constructionInfo, planNum, cd15BigRollMap.get(constructionInfo.getArticleCrownSpec()), cd90BigRollMap.get(constructionInfo.getCordSpec()), cd15Params, cd90Params, gdyyBreadth, xwyyBreadth);
//            analysisList.add(analysis);
//        }
//        logDetail.append("【半部件计划数量】：").append(toJSONString(analysisList)).append(division);
//
//        // 填充半部件实体
//        buildHalfPartCastor(mdmMonthPlanMain, analysisList, toDeleteTmMap, toDeleteTcMap, toDeleteNcMap, toDeleteTqMap, toDeleteGsqMap, toDeleteCd15Map, toDeleteCd90Map,
//                toDeleteGdyyMap, toDeleteXwyyMap, tmList, tcList, ncList, cd15List, cd90List, tqList, gsqList, gdyyList, xwyyList, logDetail, embryoCodeMap, cd15BigRollMap, cd90BigRollMap, cd15Params, cd90Params);
//        // 插入
//        tmMonthPlanSurplusService.addBatch(tmList);
//        tcMonthPlanSurplusService.addBatch(tcList);
//        ncMonthPlanSurplusService.addBatch(ncList);
//        tqMonthPlanSurplusService.addBatch(tqList);
//        gsqMonthPlanSurplusService.addBatch(gsqList);
//        gdyyMonthPlanSurplusService.addBatch(gdyyList);
//        xwyyMonthPlanSurplusService.addBatch(xwyyList);
//        lbcdMonthPlanSurplusService.addBatch(cd90List);
//        gdcdMonthPlanSurplusService.addBatch(cd15List);
//        logDetail.append("【胎面工序计划量汇总表】：").append(toJSONString(tmList)).append(division);
//        logDetail.append("【胎侧工序计划量汇总表】：").append(toJSONString(tcList)).append(division);
//        logDetail.append("【内衬工序计划量汇总表】：").append(toJSONString(ncList)).append(division);
//        logDetail.append("【CD15工序计划量汇总表】：").append(toJSONString(cd15List)).append(division);
//        logDetail.append("【CD90工序计划量汇总表】：").append(toJSONString(cd90List)).append(division);
//        logDetail.append("【胎圈工序计划量汇总表】：").append(toJSONString(tqList)).append(division);
//        logDetail.append("【钢丝圈工序计划量汇总表】：").append(toJSONString(gsqList)).append(division);
//        logDetail.append("【钢带压延工序计划量汇总表】：").append(toJSONString(gdyyList)).append(division);
//        logDetail.append("【纤维压延工序计划量汇总表】：").append(toJSONString(xwyyList)).append(division);
//        // 日志插入
//        log.setLogDetail(logDetail.toString());
//        log.setBaseVale(null);
//        logMapper.insert(log);
//        return AjaxResult.success();
//    }

//    /**
//     * 根据aps版本 + 胎胚工序重算  新
//     * @param apsVersion aps版本
//     * @param dataList 数据源
//     */
//    public AjaxResult buildHalfPartByEmbryoCastorNew(String apsVersion, List<EmbryoVersionVo> dataList, MonthSumProcessLog log, StringBuilder logDetail ) {
//        if (CollectionUtil.isEmpty(dataList)) {
//            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.embryo.empty") + apsVersion);
//        }
//
//        MdmMonthPlanMain query = new MdmMonthPlanMain();
//        query.setMonthPlanApsVersion(apsVersion);
//        List<MdmMonthPlanMain> monthPlanMains = mdmMonthPlanMainService.selectMdmMonthPlanMainList(query);
//        if (CollectionUtil.isEmpty(monthPlanMains)) {
//            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.apsVersion.empty") + apsVersion);
//        }
//        MdmMonthPlanMain mdmMonthPlanMain = monthPlanMains.get(0);
//
//        // 施工信息
//        Map<String, EngineProductConstructionInfo> totalInfoMap = cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
//        List<EngineProductConstructionInfo> engineConstructionInfos = new ArrayList<>();
//        for (EmbryoVersionVo vo : dataList) {
//            EngineProductConstructionInfo info = totalInfoMap.get(GenerageMapKeyUtils.createMapKey(vo.getEmbryoCode(), vo.getBomDataVersion()));
//            if (info != null) {
//                engineConstructionInfos.add(info);
//            }
//        }
//        logDetail.append("【施工信息】：").append(toJSONString(engineConstructionInfos)).append(division);
//        logDetail.append("【胎胚信息List】：").append(toJSONString(dataList)).append(division);
//
//        // 重算外胎胎胚
//        Map<String, TCxMonthPlanSurplus> toDeleteCxMap = new HashMap<>();
//        Map<String, TCxEmbryoMonthPlanSurplus> toDeleteEmbryoMap = new HashMap<>();
//        TCxMonthPlanSurplus cxQuery = new TCxMonthPlanSurplus();
//        cxQuery.setMonthPlanApsVersion(apsVersion);
//        cxQuery.setDelFlag("0");
//        List<TCxMonthPlanSurplus> toDeleteCxList = planSurplusService.getByParams(cxQuery);
//        if (!CollectionUtil.isEmpty(toDeleteCxList)) {
//            toDeleteCxMap = CollectionUtil.toMap(toDeleteCxList, TCxMonthPlanSurplus::getSapCode);
//        }
//        TCxEmbryoMonthPlanSurplus embryoQuery = new TCxEmbryoMonthPlanSurplus();
//        embryoQuery.setMonthPlanApsVersion(apsVersion);
//        embryoQuery.setDelFlag("0");
//        List<TCxEmbryoMonthPlanSurplus> toDeleteEmbryoList = embryoMonthPlanSurplusService.getByParams(embryoQuery);
//        if (!CollectionUtil.isEmpty(toDeleteEmbryoList)) {
//            toDeleteEmbryoMap = CollectionUtil.toMap(toDeleteEmbryoList, TCxEmbryoMonthPlanSurplus::getMaterialCode);
//        }
//        List<String> tmCodeList = new ArrayList<>();
//        List<String> tcCodeList = new ArrayList<>();
//        List<String> ncCodeList = new ArrayList<>();
//        List<String> cd15CodeList = new ArrayList<>();
//        List<String> cd90CodeList = new ArrayList<>();
//        List<String> tqCodeList = new ArrayList<>();
//        List<String> gsqCodeList = new ArrayList<>();
//        List<String> gdyyCodeList = new ArrayList<>();
//        List<String> xwyyCodeList = new ArrayList<>();
//        buildCodeList(engineConstructionInfos, tmCodeList, tcCodeList, ncCodeList, cd15CodeList, cd90CodeList, tqCodeList, gsqCodeList, gdyyCodeList, xwyyCodeList);
//        // 旧半部件数据
//        List<TTmMonthPlanSurplus> oldTmList = tmMonthPlanSurplusService.getByCodeList(apsVersion, tmCodeList);
//        Map<String, TTmMonthPlanSurplus> toDeleteTmMap = buildToDeleteMap(oldTmList);
//        List<TTcMonthPlanSurplus> oldTcList = tcMonthPlanSurplusService.getByCodeList(apsVersion, tcCodeList);
//        Map<String, TTcMonthPlanSurplus> toDeleteTcMap = buildToDeleteMap(oldTcList);
//        List<TNcMonthPlanSurplus> oldNcList = ncMonthPlanSurplusService.getByCodeList(apsVersion, ncCodeList);
//        Map<String, TNcMonthPlanSurplus> toDeleteNcMap = buildToDeleteMap(oldNcList);
//        List<TTqMonthPlanSurplus> oldTqList = tqMonthPlanSurplusService.getByCodeList(apsVersion, tqCodeList);
//        Map<String, TTqMonthPlanSurplus> toDeleteTqMap = buildToDeleteMap(oldTqList);
//        List<TGsqMonthPlanSurplus> oldGsqList = gsqMonthPlanSurplusService.getByCodeList(apsVersion, gsqCodeList);
//        Map<String, TGsqMonthPlanSurplus> toDeleteGsqMap = buildToDeleteMap(oldGsqList);
//        List<TGdcdMonthPlanSurplus> oldCd15List = gdcdMonthPlanSurplusService.getByCodeList(apsVersion, cd15CodeList);
//        Map<String, TGdcdMonthPlanSurplus> toDeleteCd15Map = buildToDeleteMap(oldCd15List);
//        List<TLbcdMonthPlanSurplus> oldCd90List = lbcdMonthPlanSurplusService.getByCodeList(apsVersion, cd90CodeList);
//        Map<String, TLbcdMonthPlanSurplus> toDeleteCd90Map = buildToDeleteMap(oldCd90List);
//        List<TGdyyMonthPlanSurplus> oldGdyyList = gdyyMonthPlanSurplusService.getByCodeList(apsVersion, gdyyCodeList);
//        Map<String, TGdyyMonthPlanSurplus> toDeleteGdyyMap = buildToDeleteMap(oldGdyyList);
//        List<TXwyyMonthPlanSurplus> oldXwyyList = xwyyMonthPlanSurplusService.getByCodeList(apsVersion, xwyyCodeList);
//        Map<String, TXwyyMonthPlanSurplus> toDeleteXwyyMap = buildToDeleteMap(oldXwyyList);
//        logDetail.append("【旧数据部分】").append(division);
//        logDetail.append("【胎面旧数据】：").append(toJSONString(toDeleteTmMap)).append(division)
//                .append("【胎侧旧数据】：").append(toJSONString(toDeleteTcMap)).append(division)
//                .append("【内衬旧数据】：").append(toJSONString(toDeleteNcMap)).append(division)
//                .append("【胎圈旧数据】：").append(toJSONString(toDeleteTqMap)).append(division)
//                .append("【钢丝圈旧数据】：").append(toJSONString(toDeleteGsqMap)).append(division)
//                .append("【15度裁断旧数据】：").append(toJSONString(toDeleteCd15Map)).append(division)
//                .append("【90度裁断旧数据】：").append(toJSONString(toDeleteCd90Map)).append(division)
//                .append("【钢带压延旧数据】：").append(toJSONString(toDeleteGdyyMap)).append(division)
//                .append("【纤维压延旧数据】：").append(toJSONString(toDeleteXwyyMap)).append(division)
//        ;
//        // 删除旧数据
//        toDeleteNinePlanExceptCxTp(apsVersion);
//        // 新半部件List
//        List<TTmMonthPlanSurplus> tmList = new ArrayList<>();
//        List<TTcMonthPlanSurplus> tcList = new ArrayList<>();
//        List<TNcMonthPlanSurplus> ncList = new ArrayList<>();
//        List<TGdcdMonthPlanSurplus> cd15List = new ArrayList<>();
//        List<TLbcdMonthPlanSurplus> cd90List = new ArrayList<>();
//        List<TTqMonthPlanSurplus> tqList = new ArrayList<>();
//        List<TGsqMonthPlanSurplus> gsqList = new ArrayList<>();
//        List<TGdyyMonthPlanSurplus> gdyyList = new ArrayList<>();
//        List<TXwyyMonthPlanSurplus> xwyyList = new ArrayList<>();
//        // 施工map
//        Map<String, EngineProductConstructionInfo> infoMap = new HashMap<>();
//        // 大卷信息
//        List<String> beltSpecList = new ArrayList<>();
//        List<String> crodSpecList = new ArrayList<>();
//        for (EngineProductConstructionInfo info : engineConstructionInfos) {
//            // 用胎胚代码+施工版本做key
//            if (info == null) {
//                continue;
//            }
//            infoMap.put(GenerageMapKeyUtils.createMapKey(info.getEmbryoCode(), info.getEmbryoVersion()), info);
//            if (StringUtils.isNotBlank(info.getArticleCrownSpec())) {
//                beltSpecList.add(info.getArticleCrownSpec());
//            }
//            if (StringUtils.isNotBlank(info.getCordSpec())) {
//                crodSpecList.add(info.getCordSpec());
//            }
//        }
//        List<CxTCd15BigRoll> cd15BigRollList = cd15BigRollService.getByBeltSpecList(beltSpecList);
//        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(crodSpecList);
//        Map<String, CxTCd15BigRoll> cd15BigRollMap = CollectionUtil.toMap(cd15BigRollList, CxTCd15BigRoll::getBigRollCode);
//        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
//        // 大卷默认信息
//        CxTCd15Params cd15Params = cd15ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
//        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
//        logDetail.append("【CD15大卷信息】：").append(toJSONString(cd15BigRollMap)).append(division);
//        logDetail.append("【CD90大卷信息】：").append(toJSONString(cd90BigRollMap)).append(division);
//        logDetail.append("【CD15大卷默认信息】：").append(toJSONString(cd15Params)).append(division);
//        logDetail.append("【CD90大卷默认信息】：").append(toJSONString(cd90Params)).append(division);
//        List<MdmMonthPlanAnalysis> analysisList = new ArrayList<>();
//        // 幅宽
//        Double gdyyBreadth = getDoubleOrDefault(this.getGdyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_GDYY_BREADTH);
//        Double xwyyBreadth = getDoubleOrDefault(this.getXwyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_XWYY_BREADTH);
//        logDetail.append("【钢带压延幅宽】：").append(gdyyBreadth).append(division);
//        logDetail.append("【纤维压延幅宽】：").append(gdyyBreadth).append(division);
//        HashMap<String, List<EmbryoVersionVo>> dataMap = CollectionUtil.toMapList(dataList, EmbryoVersionVo::getEmbryoCode);
//
//        // 取上个月的 月结库存 ，这个月的 不良量  和 本版本的修正信息
//        String year = mdmMonthPlanMain.getYear();
//        String month = mdmMonthPlanMain.getMonth();
//        String stockMonth = year + "-" + month;
//        String lastMonth = DateUtil.getLast1MonthString(stockMonth);
//        // 月结库存从成型月结里拿
//        List<CxMonthStock> monthStockList = cxMonthStockCommonMapper.selectByEmbryoVersionList(dataList, lastMonth);
//        Map<String, CxMonthStock> monthStockMap = CollectionUtil.toMap(monthStockList, obj -> GenerageMapKeyUtils.createMapKey(obj.getEmbryoCode(), obj.getBomDataVersion()));
//        // 修正量去修正表找
//        List<TCxMonthPlanAdjust> adjustList = adjustService.selectByEmbryoVersionList(apsVersion, dataList);
//        Map<String, TCxMonthPlanAdjust> adjustMap = CollectionUtil.toMap(adjustList, obj -> GenerageMapKeyUtils.createMapKey(obj.getEmbryoCode(), obj.getBomDataVersion()));
//        // 不良去不良表找
//        List<TSapEmbryoBadNumber> badList = badNumberService.getByEmbryoVersionList(stockMonth, dataList);
//        Map<String, TSapEmbryoBadNumber> badMap = CollectionUtil.toMap(badList, obj -> GenerageMapKeyUtils.createMapKey(obj.getEmbryoCode(), obj.getBomDataVersion()));
//
//        // 计算半部件最终月计划量
//        this.buildFinalPlanNum(infoMap, cd15BigRollMap, cd90BigRollMap, cd15Params, cd90Params, analysisList, gdyyBreadth, xwyyBreadth,
//                dataMap, monthStockMap, adjustMap, badMap);
//
//        logDetail.append("【半部件计划数量】：").append(toJSONString(analysisList)).append(division);
//
//        // 填充半部件实体
//        buildHalfPartCastor(mdmMonthPlanMain, analysisList, toDeleteTmMap, toDeleteTcMap, toDeleteNcMap, toDeleteTqMap, toDeleteGsqMap, toDeleteCd15Map, toDeleteCd90Map,
//                toDeleteGdyyMap, toDeleteXwyyMap, tmList, tcList, ncList, cd15List, cd90List, tqList, gsqList, gdyyList, xwyyList, logDetail, infoMap, cd15BigRollMap,
//                cd90BigRollMap, cd15Params, cd90Params);
//        // 插入
//        tmMonthPlanSurplusService.addBatch(tmList);
//        tcMonthPlanSurplusService.addBatch(tcList);
//        ncMonthPlanSurplusService.addBatch(ncList);
//        tqMonthPlanSurplusService.addBatch(tqList);
//        gsqMonthPlanSurplusService.addBatch(gsqList);
//        gdyyMonthPlanSurplusService.addBatch(gdyyList);
//        xwyyMonthPlanSurplusService.addBatch(xwyyList);
//        lbcdMonthPlanSurplusService.addBatch(cd90List);
//        gdcdMonthPlanSurplusService.addBatch(cd15List);
//        logDetail.append("【胎面工序计划量汇总表】：").append(toJSONString(tmList)).append(division);
//        logDetail.append("【胎侧工序计划量汇总表】：").append(toJSONString(tcList)).append(division);
//        logDetail.append("【内衬工序计划量汇总表】：").append(toJSONString(ncList)).append(division);
//        logDetail.append("【CD15工序计划量汇总表】：").append(toJSONString(cd15List)).append(division);
//        logDetail.append("【CD90工序计划量汇总表】：").append(toJSONString(cd90List)).append(division);
//        logDetail.append("【胎圈工序计划量汇总表】：").append(toJSONString(tqList)).append(division);
//        logDetail.append("【钢丝圈工序计划量汇总表】：").append(toJSONString(gsqList)).append(division);
//        logDetail.append("【钢带压延工序计划量汇总表】：").append(toJSONString(gdyyList)).append(division);
//        logDetail.append("【纤维压延工序计划量汇总表】：").append(toJSONString(xwyyList)).append(division);
//        // 日志插入
//        log.setLogDetail(logDetail.toString());
//        log.setBaseVale(null);
//        logMapper.insert(log);
//        return AjaxResult.success();
//    }

    /**
     * 计算半部件最终月计划量
     * @param infoMap
     * @param cd15BigRollMap
     * @param cd90BigRollMap
     * @param cd15Params
     * @param cd90Params
     * @param analysisList
     * @param gdyyBreadth
     * @param xwyyBreadth
     * @param dataMap
     * @param monthStockMap
     * @param adjustMap
     * @param badMap
     */
    private void buildFinalPlanNum(Map<String, EngineProductConstructionInfo> infoMap, Map<String, CxTCd15BigRoll> cd15BigRollMap, Map<String, CxTCd90BigRoll> cd90BigRollMap,
                                   CxTCd15Params cd15Params, CxTCd90Params cd90Params, List<MdmMonthPlanAnalysis> analysisList, Double gdyyBreadth, Double xwyyBreadth,
                                   HashMap<String, List<EmbryoVersionVo>> dataMap,
                                   Map<String, CxMonthStock> monthStockMap, Map<String, TCxMonthPlanAdjust> adjustMap, Map<String, TSapEmbryoBadNumber> badMap) {
        for (String embryoCode : dataMap.keySet()) {
            // 同胎胚计算
            List<EmbryoVersionVo> voList = dataMap.get(embryoCode);
            for (EmbryoVersionVo vo : voList) {
                // 同胎胚不同版本分别计算
                // 月计划量 + 调整量 + 不良 - 月结库存 = 最终月计划量
                String key = GenerageMapKeyUtils.createMapKey(vo.getEmbryoCode(), vo.getBomDataVersion());
                TCxMonthPlanAdjust adjust = adjustMap.get(key);
                if (adjust != null) {
                    vo.setTotalPlanQty(vo.getTotalPlanQty() + adjust.getPlanModifyQty());
                }
                TSapEmbryoBadNumber bad = badMap.get(key);
                if (bad != null) {
                    vo.setTotalPlanQty(vo.getTotalPlanQty() + bad.getBadNum());
                }
                CxMonthStock cxMonthStock = monthStockMap.get(key);
                if (cxMonthStock != null) {
                    vo.setTotalPlanQty(vo.getTotalPlanQty() - (StringUtils.isBlank(cxMonthStock.getStockNum()) ? 0 : Integer.parseInt(cxMonthStock.getStockNum())));
                }
                // 根据胎胚最终月计划量计算半部件的量
                if (vo.getTotalPlanQty() <= 0) {
                    // 计划量小于或等于0不用做
                    continue;
                }
                EngineProductConstructionInfo constructionInfo = infoMap.get(key);
                if (constructionInfo == null) {
                    continue;
                }
                MdmMonthPlanAnalysis analysis = new MdmMonthPlanAnalysis();
                analysis.setEmbryoCode(vo.getEmbryoCode());
                analysis.setBomDataVersion(vo.getBomDataVersion());
                getEmbryoUnitConsumption(analysis, constructionInfo, BigDecimal.valueOf(vo.getTotalPlanQty()), cd15BigRollMap.get(constructionInfo.getArticleCrownSpec()), cd90BigRollMap.get(constructionInfo.getCordSpec()), cd15Params, cd90Params, gdyyBreadth, xwyyBreadth);
                analysisList.add(analysis);
            }
        }
    }

    /**
     * 获取生产排程版本（月计划前端用）
     * @param planMainVersion 主计划版本
     * @param year 年
     * @param month 月
     * @param isFinal 是否定稿 0是1否
     * @return 生产排程版本
     */
    public String getApsMainPlanVersion(String planMainVersion, String year, String month, String isFinal) {
        List<MdmMonthPlanMain> isExistList = checkPlanMainExist(year, month, isFinal);
        if (!CollectionUtil.isEmpty(isExistList)) {
            return isExistList.get(0).getMonthPlanApsVersion();
        }
        MdmMonthPlanMain mdmMonthPlanMain = getMdmMonthPlanMain(planMainVersion, year, month, isFinal);
        mdmMonthPlanMainService.insertMdmMonthPlanMain(mdmMonthPlanMain);
        return mdmMonthPlanMain.getMonthPlanApsVersion();
    }


    // -------------------------------------------------------------------------------------调整部分-----------------------------------------------------------------------------------------

        /**
         * 成型调整
         * @param apsVersion 生产排程版本
         * @param sapCode sap品号
         * @param embryoCode 胎胚代码
         * @param modifyNum 调整量
         * @param adjustSource 调整源头：0：投产列表；1：成型排程
         * @throws NotFoundException
         */
    @Transactional
    public AjaxResult updateCx(String apsVersion, String sapCode, String embryoCode, Integer modifyNum, String adjustSource, String bomDataVersion){
        MonthSumProcessLog log = new MonthSumProcessLog();
        log.setMonthPlanApsVersion(apsVersion);
        setBaseSysValue(log);
        log.setTitle("成型修正");
        StringBuilder logDetail = new StringBuilder();
        if (StringUtils.isBlank(apsVersion) || StringUtils.isBlank(sapCode) || StringUtils.isBlank(embryoCode) || StringUtils.isBlank(adjustSource) || StringUtils.isBlank(bomDataVersion)) {
            logDetail.append(I18nUtil.getMessage("mdm.error.message.income"));
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.income"));
        }
        if (modifyNum == null) {
            modifyNum = 0;
        }
        // 胎胚
        TCxEmbryoMonthPlanSurplus cxEmbryoQuery = new TCxEmbryoMonthPlanSurplus();
        cxEmbryoQuery.setMonthPlanApsVersion(apsVersion);
        cxEmbryoQuery.setMaterialCode(embryoCode);
        cxEmbryoQuery.setDelFlag("0");
        List<TCxEmbryoMonthPlanSurplus> cxEmbryoMonthPlanSurplusList = embryoMonthPlanSurplusService.getByParams(cxEmbryoQuery);
        if (CollectionUtil.isEmpty(cxEmbryoMonthPlanSurplusList)) {
            logDetail.append(I18nUtil.getMessage("mdm.error.message.embryoPlan"));
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.embryoPlan"));
        }
        TCxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus = cxEmbryoMonthPlanSurplusList.get(0);
        logDetail.append("【旧胎胚信息】：").append(toJSONString(cxEmbryoMonthPlanSurplus)).append(division);
        // 版本
        MdmMonthPlanMain queryMain = new MdmMonthPlanMain();
        queryMain.setMonthPlanApsVersion(apsVersion);
        List<MdmMonthPlanMain> planMains = mdmMonthPlanMainService.selectMdmMonthPlanMainList(queryMain);
        if (CollectionUtil.isEmpty(planMains)) {
            logDetail.append(I18nUtil.getMessage("mdm.error.message.apsVersion.empty"));
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.apsVersion.empty"));
        }
        MdmMonthPlanMain planMain = planMains.get(0);
        logDetail.append("【版本信息】：").append(toJSONString(planMain)).append(division);
        // 取外胎
        TCxMonthPlanSurplus cxQuery = new TCxMonthPlanSurplus();
        cxQuery.setMonthPlanApsVersion(apsVersion);
        cxQuery.setSapCode(sapCode);
        cxQuery.setDelFlag("0");
        List<TCxMonthPlanSurplus> cxMonthPlanSurplusList = planSurplusService.getByParams(cxQuery);
        if (CollectionUtil.isEmpty(cxMonthPlanSurplusList)) {
            logDetail.append(I18nUtil.getMessage("mdm.error.message.cxPlan"));
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.cxPlan"));
        }
        TCxMonthPlanSurplus cxMonthPlanSurplus = cxMonthPlanSurplusList.get(0);
        logDetail.append("【旧外胎信息】：").append(toJSONString(cxMonthPlanSurplus)).append(division);
        // 调整量 + 调整半部件
        // 查询是否有旧调整数据
        TCxMonthPlanAdjust adjustQuery = new TCxMonthPlanAdjust();
        adjustQuery.setMonthPlanApsVersion(apsVersion);
        adjustQuery.setSapCode(sapCode);
        adjustQuery.setEmbryoCode(embryoCode);
        adjustQuery.setBomDataVersion(bomDataVersion);
        List<TCxMonthPlanAdjust> adjustList = adjustService.selectTCxMonthPlanAdjustList(adjustQuery);
        logDetail.append("【旧调整信息】：").append(toJSONString(adjustList)).append(division);
        // 待调整差额
        Integer toCalculateNum = 0;
        if (CollectionUtil.isEmpty(adjustList)) {
            // 没有旧数据则新增调整
            TCxMonthPlanAdjust adjust = new TCxMonthPlanAdjust();
            this.buildAdjustCastor(apsVersion, sapCode, embryoCode, modifyNum, adjustSource, adjust, bomDataVersion);
            adjustList.add(adjust);
            toCalculateNum += modifyNum;
        } else {
            Integer flag = 0;
            for (TCxMonthPlanAdjust adjust : adjustList) {
                // 有旧数据判断是否同一来源
                if (adjust.getAdjustSource().equals(adjustSource)) {
                    // 同来源则修改
                    Integer oldModify = adjust.getPlanModifyQty();
                    adjust.setPlanModifyQty(modifyNum);
                    adjust.setBaseVale(adjust.getId());
                    toCalculateNum = modifyNum - oldModify;
                    flag = 1;
                }
            }
            if (flag == 0) {
                // 不同来源则新增
                TCxMonthPlanAdjust adjust = new TCxMonthPlanAdjust();
                this.buildAdjustCastor(apsVersion, sapCode, embryoCode, modifyNum, adjustSource, adjust, bomDataVersion);
                adjustList.add(adjust);
                toCalculateNum += modifyNum;
            }
        }
        // 如果待调整差额为零，则没有调整
        if (toCalculateNum == 0) {
            logDetail.append("【待调整差额】：").append(toCalculateNum).append(division);
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.success();
        }
        logDetail.append("【待调整差额】：").append(toCalculateNum).append(division);
        // 胎胚计算 旧修改加上与本次的修改差额
        cxEmbryoMonthPlanSurplus.setMonthPlanModifyQty(cxEmbryoMonthPlanSurplus.getMonthPlanModifyQty().add(BigDecimal.valueOf(toCalculateNum)));
        AjaxResult result = checkRemain(cxEmbryoMonthPlanSurplus);
        // 判断剩余量是否允许
        if ((int) result.get(Constants.CODE) == HttpStatus.ERROR) {
            logDetail.append("胎胚剩余量不允许").append(division);
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return result;
        }
        cxEmbryoMonthPlanSurplus.setMonthRemainQty(getEmbryoRemainQtyUpdateCx(cxEmbryoMonthPlanSurplus));
        logDetail.append("【新胎胚数据】：").append(toJSONString(cxEmbryoMonthPlanSurplus)).append(division);

        // 修改外胎
        cxMonthPlanSurplus.setPlanModifyQty(cxMonthPlanSurplus.getPlanModifyQty() + toCalculateNum);
        cxMonthPlanSurplus.setMonthRemainQty(getMonthRemainQtyUpdateCx(cxMonthPlanSurplus));
        logDetail.append("【新外胎数据】：").append(toJSONString(cxMonthPlanSurplus)).append(division);

        // ============================== 基础信息 ====================================================================================
        // 施工信息
        Map<String, EngineProductConstructionInfo> totalInfoMap = cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
        EngineProductConstructionInfo constructionInfo = totalInfoMap.get(GenerageMapKeyUtils.createMapKey(embryoCode, bomDataVersion));
        if (constructionInfo == null) {
            logDetail.append("施工信息为空").append(division);
            log.setLogDetail(logDetail.toString());
            logMapper.insert(log);
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.construction.info"));
        }
        logDetail.append("【施工信息】：").append(toJSONString(constructionInfo)).append(division);
        // 个数
        // 大卷信息
        CxTCd15BigRoll cd15BigRoll = null;
        if (StringUtils.isNotBlank(constructionInfo.getArticleCrownSpec())) {
            CxTCd15BigRoll cd15Query = new CxTCd15BigRoll();
            cd15Query.setBigRollCode(constructionInfo.getArticleCrownSpec());
            cd15Query.setDelFlag("0");
            List<CxTCd15BigRoll> cd15BigRollList = cd15BigRollService.getByParams(cd15Query);
            if (!CollectionUtil.isEmpty(cd15BigRollList)) {
                cd15BigRoll = cd15BigRollList.get(0);
            }
        }
        logDetail.append("【CD15大卷信息】：").append(toJSONString(cd15BigRoll)).append(division);
        CxTCd90BigRoll cd90BigRoll = null;
        if (StringUtils.isNotBlank(constructionInfo.getCordSpec())) {
            CxTCd90BigRoll cx90Query = new CxTCd90BigRoll();
            cx90Query.setBigRollCode(constructionInfo.getCordSpec());
            cx90Query.setDelFlag("0");
            List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByParams(cx90Query);
            if (!CollectionUtil.isEmpty(cd90BigRollList)) {
                cd90BigRoll = cd90BigRollList.get(0);
            }
        }
        logDetail.append("【CD90大卷信息】：").append(toJSONString(cd90BigRoll)).append(division);
        // 大卷默认信息
        CxTCd15Params cd15Params = cd15ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);

        logDetail.append("【CD15大卷默认信息】：").append(toJSONString(cd15Params)).append(division);
        logDetail.append("【CD90大卷默认信息】：").append(toJSONString(cd90Params)).append(division);
        // 幅宽
        Double gdyyBreadth = getDoubleOrDefault(this.getGdyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_GDYY_BREADTH);
        Double xwyyBreadth = getDoubleOrDefault(this.getXwyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_XWYY_BREADTH);
        logDetail.append("【钢带压延幅宽】：").append(gdyyBreadth).append(division);
        logDetail.append("【纤维压延幅宽】：").append(gdyyBreadth).append(division);

        // =======================================  修正半部件部分 =======================================================================
        //半部件计划量=胎胚计划量+调整量
        // 计算差额即可
        BigDecimal modifyBig = BigDecimal.valueOf(toCalculateNum);
        BigDecimal divide = BigDecimal.valueOf(1000);

        // 胎面
        List<TTmMonthPlanSurplus> oldTmList = new ArrayList<>();
        TTmMonthPlanSurplus tm = null;
        if (StringUtils.isNotBlank(constructionInfo.getTreadCode())) {
            oldTmList = tmMonthPlanSurplusService.getByCodeList(apsVersion, Collections.singletonList(constructionInfo.getTreadCode()));
            BigDecimal tmModifyNum = constructionInfo.getTreadShoulderLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getTreadShoulderLength()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
            if (!CollectionUtil.isEmpty(oldTmList)) {
                tm = oldTmList.get(0);
                logDetail.append("【胎面旧数据】：").append(toJSONString(tm)).append(division);
                logDetail.append("【胎面ModifyNum】：").append(tmModifyNum).append(division);
                buildModify(tm, tmModifyNum);
            } else {
                tm = new TTmMonthPlanSurplus();
                BigDecimal tmNum = constructionInfo.getTreadShoulderLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getTreadShoulderLength()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
                buildHalfPartUpdateCx(tmNum, tm, planMain, constructionInfo.getTreadCode());
            }
        }
        logDetail.append("【胎面数据】：").append(toJSONString(tm)).append(division);

        // 胎侧
        List<TTcMonthPlanSurplus> oldTcList = new ArrayList<>();
        TTcMonthPlanSurplus tc = null;
        if (StringUtils.isNotBlank(constructionInfo.getSidewallCode())) {
            oldTcList = tcMonthPlanSurplusService.getByCodeList(apsVersion, Collections.singletonList(constructionInfo.getSidewallCode()));
            BigDecimal tcModifyNum = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
            if (!CollectionUtil.isEmpty(oldTcList)) {
                tc = oldTcList.get(0);
                logDetail.append("【胎侧旧数据】：").append(toJSONString(tc)).append(division);
                logDetail.append("【胎侧ModifyNum】：").append(tcModifyNum).append(division);
                buildModify(tc, tcModifyNum);
            } else  {
                tc = new TTcMonthPlanSurplus();
                BigDecimal tcNum = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
                buildHalfPartUpdateCx(tcNum, tc, planMain, constructionInfo.getTreadCode());
            }
        }
        logDetail.append("【胎侧数据】：").append(toJSONString(tc)).append(division);

        // 内衬
        List<TNcMonthPlanSurplus> oldNcList = new ArrayList<>();
        TNcMonthPlanSurplus nc = null;
        if (StringUtils.isNotBlank(constructionInfo.getInsideCode())) {
            oldNcList = ncMonthPlanSurplusService.getByCodeList(apsVersion, Collections.singletonList(constructionInfo.getInsideCode()));
            BigDecimal ncNum = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
            if (!CollectionUtil.isEmpty(oldNcList)) {
                nc = oldNcList.get(0);
                logDetail.append("【内衬旧数据】：").append(toJSONString(nc)).append(division);
                logDetail.append("【内衬ModifyNum】：").append(ncNum).append(division);
                buildModify(nc, ncNum);
            } else {
                nc = new TNcMonthPlanSurplus();
                buildHalfPartUpdateCx(ncNum, nc, planMain, constructionInfo.getTreadCode());
            }
        }
        logDetail.append("【内衬数据】：").append(toJSONString(nc)).append(division);

        // 胎圈
        List<TTqMonthPlanSurplus> oldTqList = new ArrayList<>();
        TTqMonthPlanSurplus tq = null;
        BigDecimal planNumTwoPart = modifyBig.multiply(BigDecimal.valueOf(2L));
        if (StringUtils.isNotBlank(constructionInfo.getTireRingCode())) {
            oldTqList = tqMonthPlanSurplusService.getByCodeList(apsVersion, Collections.singletonList(constructionInfo.getTireRingCode()));
            if (!CollectionUtil.isEmpty(oldTqList)) {
                tq = oldTqList.get(0);
                logDetail.append("【胎圈旧数据】：").append(toJSONString(tq)).append(division);
                logDetail.append("【胎圈ModifyNum】：").append(planNumTwoPart).append(division);
                buildModify(tq, planNumTwoPart);
            } else {
                tq = new TTqMonthPlanSurplus();
                buildHalfPartUpdateCx(planNumTwoPart, tq, planMain, constructionInfo.getTreadCode());
            }
        }
        logDetail.append("【胎圈数据】：").append(toJSONString(tq)).append(division);

        // 钢丝圈
        List<TGsqMonthPlanSurplus> oldGsqList = new ArrayList<>();
        TGsqMonthPlanSurplus gsq = null;
        if (StringUtils.isNotBlank(constructionInfo.getBeadCode())) {
            oldGsqList = gsqMonthPlanSurplusService.getByCodeList(apsVersion, Collections.singletonList(constructionInfo.getBeadCode()));
            if (!CollectionUtil.isEmpty(oldGsqList)) {
                gsq = oldGsqList.get(0);
                logDetail.append("【钢丝圈旧数据】：").append(toJSONString(gsq)).append(division);
                logDetail.append("【钢丝圈ModifyNum】：").append(planNumTwoPart).append(division);
                buildModify(gsq, planNumTwoPart);
            } else {
                gsq = new TGsqMonthPlanSurplus();
                buildHalfPartUpdateCx(planNumTwoPart, gsq, planMain, constructionInfo.getTreadCode());
            }
        }
        logDetail.append("【钢丝圈数据】：").append(toJSONString(gsq)).append(division);

        // 15裁断
        List<String> cd15CodeList = new ArrayList<>();
        if (StringUtils.isNotBlank(constructionInfo.getBeltCode1())) {
            cd15CodeList.add(constructionInfo.getBeltCode1());
        }
        if (StringUtils.isNotBlank(constructionInfo.getBeltCode2())) {
            cd15CodeList.add(constructionInfo.getBeltCode2());
        }
        List<TGdcdMonthPlanSurplus> cd15List = gdcdMonthPlanSurplusService.getByCodeList(apsVersion, cd15CodeList);
        // 15度裁断本次总修正量差额
        BigDecimal cd15One = BigDecimal.ZERO;
//        BigDecimal cd15Two = BigDecimal.ZERO;
        Map<String, TGdcdMonthPlanSurplus> oldCd15Map = new HashMap<>();
        if (!CollectionUtil.isEmpty(cd15List)) {
            oldCd15Map = CollectionUtil.toMap(cd15List, TGdcdMonthPlanSurplus::getMaterialCode);
        }
        List<String> oldCd15CodeList = new ArrayList<>(oldCd15Map.keySet());
        // 待更新15度List
        List<TGdcdMonthPlanSurplus> toUpdateCd15List = new ArrayList<>();
        // 待新增15度List
        List<TGdcdMonthPlanSurplus> toAddCd15List = new ArrayList<>();
        if (StringUtils.isNotBlank(constructionInfo.getBeltCode1())) {
            BigDecimal cd15Num = constructionInfo.getFitDrumPerimeter() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getFitDrumPerimeter()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
            cd15One = cd15Num;
            if (oldCd15CodeList.contains(constructionInfo.getBeltCode1())) {
                // 如果旧版本有则修改
                TGdcdMonthPlanSurplus oldCd15 = oldCd15Map.get(constructionInfo.getBeltCode1());
                logDetail.append("【CD15旧数据】：").append(toJSONString(oldCd15)).append(division);
                logDetail.append("【CD15ModifyNum】：").append(cd15Num).append(division);
                buildModify(oldCd15, cd15Num);
                toUpdateCd15List.add(oldCd15);
            } else {
                // 旧版本没有则新增
                TGdcdMonthPlanSurplus cd15OneEntity = new TGdcdMonthPlanSurplus();
                buildHalfPartUpdateCx(cd15Num, cd15OneEntity, planMain, constructionInfo.getBeltCode1());
                toAddCd15List.add(cd15OneEntity);
            }
        }
        if (StringUtils.isNotBlank(constructionInfo.getBeltCode2())) {
            BigDecimal cd15Num = constructionInfo.getFitDrumPerimeter() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getFitDrumPerimeter()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
//            cd15Two = cd15Num;
            if (oldCd15CodeList.contains(constructionInfo.getBeltCode1())) {
                TGdcdMonthPlanSurplus oldCd15 = oldCd15Map.get(constructionInfo.getBeltCode2());
                logDetail.append("【CD15旧数据】：").append(toJSONString(oldCd15)).append(division);
                logDetail.append("【CD15ModifyNum】：").append(cd15Num).append(division);
                buildModify(oldCd15, cd15Num);
                toUpdateCd15List.add(oldCd15);
            } else {
                TGdcdMonthPlanSurplus cd15TwoEntity = new TGdcdMonthPlanSurplus();
                buildHalfPartUpdateCx(cd15Num, cd15TwoEntity, planMain, constructionInfo.getBeltCode2());
                toAddCd15List.add(cd15TwoEntity);
            }
        }
        logDetail.append("【CD15更新数据】：").append(toJSONString(toUpdateCd15List)).append(division);
        logDetail.append("【CD15新增数据】：").append(toJSONString(toAddCd15List)).append(division);

        // 90裁断
        // 90度裁断本次总修正量差额
        BigDecimal cd90One = BigDecimal.ZERO;
        BigDecimal cd90Two = BigDecimal.ZERO;
        BigDecimal cd90Three = BigDecimal.ZERO;
        List<String> cd90CodeList = new ArrayList<>();
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode1())) {
            cd90CodeList.add(constructionInfo.getTireFabricCode1());
        }
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode2())) {
            cd90CodeList.add(constructionInfo.getTireFabricCode2());
        }
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode3())) {
            cd90CodeList.add(constructionInfo.getTireFabricCode3());
        }
        // 90度旧数据
        List<TLbcdMonthPlanSurplus> cd90List = lbcdMonthPlanSurplusService.getByCodeList(apsVersion, cd90CodeList);
        Map<String, TLbcdMonthPlanSurplus> oldCd90Map = new HashMap<>();
        if (!CollectionUtil.isEmpty(cd90List)) {
            oldCd90Map = CollectionUtil.toMap(cd90List, TLbcdMonthPlanSurplus::getMaterialCode);
        }
        List<String> oldCd90CodeList = new ArrayList<>(oldCd90Map.keySet());
        // 待更新90度List
        List<TLbcdMonthPlanSurplus> toUpdateCd90List = new ArrayList<>();
        // 待新增90度List
        List<TLbcdMonthPlanSurplus> toAddCd90List = new ArrayList<>();
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode1())) {
            BigDecimal cd90Num = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
            cd90One = cd90Num;
            if (oldCd90CodeList.contains(constructionInfo.getTireFabricCode1())) {
                // 如果旧版本有则修改
                TLbcdMonthPlanSurplus oldCd90 = oldCd90Map.get(constructionInfo.getTireFabricCode1());
                logDetail.append("【CD90旧数据】：").append(toJSONString(oldCd90)).append(division);
                logDetail.append("【CD90ModifyNum】：").append(cd90Num).append(division);
                buildModify(oldCd90, cd90Num);
                toUpdateCd90List.add(oldCd90);
            } else {
                // 旧版本没有则新增
                TLbcdMonthPlanSurplus cd90OneEntity = new TLbcdMonthPlanSurplus();
                buildHalfPartUpdateCx(cd90Num, cd90OneEntity, planMain, constructionInfo.getTireFabricCode1());
                toAddCd90List.add(cd90OneEntity);
            }
        }
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode2())) {
            BigDecimal cd90Num = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
            cd90Two = cd90Num;
            if (oldCd90CodeList.contains(constructionInfo.getTireFabricCode2())) {
                // 如果旧版本有则修改
                TLbcdMonthPlanSurplus oldCd90 = oldCd90Map.get(constructionInfo.getTireFabricCode2());
                logDetail.append("【CD90旧数据】：").append(toJSONString(oldCd90)).append(division);
                logDetail.append("【CD90ModifyNum】：").append(cd90Num).append(division);
                buildModify(oldCd90, cd90Num);
                toUpdateCd90List.add(oldCd90);
            } else {
                // 旧版本没有则新增
                TLbcdMonthPlanSurplus cd90TwoEntity = new TLbcdMonthPlanSurplus();
                buildHalfPartUpdateCx(cd90Num, cd90TwoEntity, planMain, constructionInfo.getTireFabricCode2());
                toAddCd90List.add(cd90TwoEntity);
            }
        }
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode3())) {
            BigDecimal cd90Num = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(modifyBig).divide(divide, 3, BigDecimal.ROUND_UP);
            cd90Three = cd90Num;
            if (oldCd90CodeList.contains(constructionInfo.getTireFabricCode3())) {
                // 如果旧版本有则修改
                TLbcdMonthPlanSurplus oldCd90 = oldCd90Map.get(constructionInfo.getTireFabricCode3());
                logDetail.append("【CD90旧数据】：").append(toJSONString(oldCd90)).append(division);
                logDetail.append("【CD90ModifyNum】：").append(cd90Num).append(division);
                buildModify(oldCd90, cd90Num);
                toUpdateCd90List.add(oldCd90);
            } else {
                // 旧版本没有则新增
                TLbcdMonthPlanSurplus cd90ThreeEntity = new TLbcdMonthPlanSurplus();
                buildHalfPartUpdateCx(cd90Num, cd90ThreeEntity, planMain, constructionInfo.getTireFabricCode3());
                toAddCd90List.add(cd90ThreeEntity);
            }
        }
        logDetail.append("【CD90更新数据】：").append(toJSONString(toUpdateCd90List)).append(division);
        logDetail.append("【CD90新增数据】：").append(toJSONString(toAddCd90List)).append(division);

        // 钢带压延
        TGdyyMonthPlanSurplus gdyy = null;
        List<TGdyyMonthPlanSurplus> oldGdyyList = new ArrayList<>();
        if (StringUtils.isNotBlank(constructionInfo.getArticleCrownSpec())) {
            // 钢带压延旧数据
            oldGdyyList = gdyyMonthPlanSurplusService.getByCodeList(apsVersion, Collections.singletonList(constructionInfo.getArticleCrownSpec()));
            // 个数计算
            BigDecimal gdyyModifyNum2 = BigDecimal.ZERO;
            BigDecimal gdyyPlanNum2 = BigDecimal.ZERO;
            BigDecimal gdyyModifyTotalPlanNum = cd15One.multiply(constructionInfo.getBeltCraft1() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getBeltCraft1())
                    .divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(gdyyBreadth), 3, BigDecimal.ROUND_UP);

            if (!CollectionUtil.isEmpty(oldGdyyList)) {
                gdyy = oldGdyyList.get(0);
                logDetail.append("【钢带压延旧数据】：").append(toJSONString(gdyy)).append(division);
                logDetail.append("【钢带压延ModifyNum】：").append(gdyyModifyTotalPlanNum).append(division);
                buildModify(gdyy, gdyyModifyTotalPlanNum);
            } else {
                gdyy = new TGdyyMonthPlanSurplus();
                buildHalfPartUpdateCx(gdyyModifyTotalPlanNum, gdyy, planMain, constructionInfo.getArticleCrownSpec());
            }
            // 如果大卷信息维护表中没有数据，从默认表中取，并且值不能是0
            if (cd15BigRoll != null && cd15BigRoll.getActClothLength() != null && !cd15BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                gdyyPlanNum2 = gdyy.getMonthPlanQty().divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                gdyyModifyNum2 = gdyy.getMonthPlanModifyQty().divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
            } else if (cd15Params != null && cd15Params.getParamValue() != null && !BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())).equals(BigDecimal.ZERO)) {
                gdyyPlanNum2 = gdyy.getMonthPlanQty().divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                gdyyModifyNum2 = gdyy.getMonthPlanModifyQty().divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
            }
            gdyy.setMonthPlanQty2(gdyyPlanNum2);
            gdyy.setMonthPlanModifyQty2(gdyyModifyNum2);
            gdyy.setMonthRemainQty2(gdyy.getMonthPlanQty2().add(gdyy.getMonthPlanModifyQty2()).subtract(gdyy.getMonthFinishQty2()).setScale(3, RoundingMode.UP));
        }
        logDetail.append("【钢带压延数据】：").append(toJSONString(gdyy)).append(division);

        // 纤维压延
        List<TXwyyMonthPlanSurplus> oldXwyyList = new ArrayList<>();
        TXwyyMonthPlanSurplus xwyy = null;
        if (StringUtils.isNotBlank(constructionInfo.getCordSpec())) {
            // 纤维压延旧数据
            oldXwyyList = xwyyMonthPlanSurplusService.getByCodeList(apsVersion, Collections.singletonList(constructionInfo.getCordSpec()));
            // 个数计算
            BigDecimal xwyyPlanNum2 = BigDecimal.ZERO;
            BigDecimal xwyyModifyNum2 = BigDecimal.ZERO;
            BigDecimal xwyyFinishNum2 = BigDecimal.ZERO;
            BigDecimal xwyyModifyTotalPlanNum = cd90One.multiply(constructionInfo.getTireFabricCraft1() == null ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(constructionInfo.getTireFabricCraft1())).divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(xwyyBreadth), 3, BigDecimal.ROUND_UP)
                    .add(cd90Two.multiply(constructionInfo.getTireFabricCraft2() == null ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(constructionInfo.getTireFabricCraft2())).divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(xwyyBreadth), 3, BigDecimal.ROUND_UP))
                    .add((cd90Three.multiply(constructionInfo.getTireFabricCraft3() == null ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(constructionInfo.getTireFabricCraft3())).divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(xwyyBreadth), 3, BigDecimal.ROUND_UP)));

            if (!CollectionUtil.isEmpty(oldXwyyList)) {
                xwyy = oldXwyyList.get(0);
                logDetail.append("【纤维压延旧数据】：").append(toJSONString(xwyy)).append(division);
                logDetail.append("【纤维压延ModifyNum】：").append(xwyyModifyTotalPlanNum).append(division);
                buildModify(xwyy, xwyyModifyTotalPlanNum);
            } else {
                xwyy = new TXwyyMonthPlanSurplus();
                buildHalfPartUpdateCx(xwyyModifyTotalPlanNum, xwyy, planMain, constructionInfo.getCordSpec());
            }
            // 如果大卷信息维护表中没有数据，从默认表中取，并且值不能是0
            if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null && !cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                xwyyPlanNum2 = xwyy.getMonthPlanQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                xwyyModifyNum2 = xwyy.getMonthPlanModifyQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                xwyyFinishNum2 = xwyy.getMonthFinishQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
            } else if (cd90Params != null && cd90Params.getParamValue() != null && !BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())).equals(BigDecimal.ZERO)){
                xwyyPlanNum2 = xwyy.getMonthPlanQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                xwyyModifyNum2 = xwyy.getMonthPlanModifyQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                xwyyFinishNum2 = xwyy.getMonthFinishQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
            }
            xwyy.setMonthPlanQty2(xwyyPlanNum2);
            xwyy.setMonthPlanModifyQty2(xwyyModifyNum2);
            xwyy.setMonthFinishQty2(xwyyFinishNum2);
            xwyy.setMonthRemainQty2(xwyy.getMonthPlanQty2().add(xwyy.getMonthPlanModifyQty2()).subtract(xwyy.getMonthFinishQty2()));
        }
        logDetail.append("【纤维压延数据】：").append(toJSONString(xwyy)).append(division);

        // 更新
        if (!CollectionUtil.isEmpty(oldTmList)) {
            tmMonthPlanSurplusService.update(tm);
        } else if (tm != null) {
            tmMonthPlanSurplusService.add(tm);
        }
        if (!CollectionUtil.isEmpty(oldTcList)) {
            tcMonthPlanSurplusService.update(tc);
        } else if (tc != null) {
            tcMonthPlanSurplusService.add(tc);
        }
        if (!CollectionUtil.isEmpty(oldNcList)) {
            ncMonthPlanSurplusService.update(nc);
        } else if (nc != null) {
            ncMonthPlanSurplusService.add(nc);
        }
        if (!CollectionUtil.isEmpty(oldTqList)) {
            tqMonthPlanSurplusService.update(tq);
        } else if (tq != null) {
            tqMonthPlanSurplusService.add(tq);
        }
        if (!CollectionUtil.isEmpty(oldGsqList)) {
            gsqMonthPlanSurplusService.update(gsq);
        } else if (gsq != null) {
            gsqMonthPlanSurplusService.add(gsq);
        }
        if (!CollectionUtil.isEmpty(toAddCd15List)) {
            gdcdMonthPlanSurplusService.addBatch(toAddCd15List);
        }
        if (!CollectionUtil.isEmpty(toUpdateCd15List)) {
            toUpdateCd15List.forEach(obj -> gdcdMonthPlanSurplusService.update(obj));
        }
        if (!CollectionUtil.isEmpty(toAddCd90List)) {
            lbcdMonthPlanSurplusService.addBatch(toAddCd90List);
        }
        if (!CollectionUtil.isEmpty(toUpdateCd90List)) {
            toUpdateCd90List.forEach(obj -> lbcdMonthPlanSurplusService.update(obj));
        }
        if (!CollectionUtil.isEmpty(oldGdyyList)) {
            gdyyMonthPlanSurplusService.update(gdyy);
        } else if (gdyy != null) {
            gdyyMonthPlanSurplusService.add(gdyy);
        }
        if (!CollectionUtil.isEmpty(oldXwyyList)) {
            xwyyMonthPlanSurplusService.update(xwyy);
        } else if (xwyy != null) {
            xwyyMonthPlanSurplusService.add(xwyy);
        }
        planSurplusService.update(cxMonthPlanSurplus);
        embryoMonthPlanSurplusService.update(cxEmbryoMonthPlanSurplus);
        adjustService.mergeSql(adjustList);
        logDetail.append("【修正完成量】");
        log.setLogDetail(logDetail.toString());
        logMapper.insert(log);

        return AjaxResult.success();
    }


    // -------------------------------------------------------------------------------------内部方法部分-----------------------------------------------------------------------------------------

    /**
     * 设置默认值
     * @param entity
     * @param <K>
     */
    private <K extends ApsBaseEntity> void  setBaseSysValue(K entity) {
        try {
            entity.setBaseVale(null);
        } catch (Exception e) {
            entity.setDelFlag("0");
            entity.setCreateBy("system");
            entity.setUpdateBy("system");
            entity.setCreateTime(new Date());
            entity.setUpdateTime(new Date());
        }
    }


    /**
     * 调量设值
     * @param apsVersion
     * @param sapCode
     * @param embryoCode
     * @param modifyNum
     * @param adjustSource
     * @param adjust
     */
    private void buildAdjustCastor(String apsVersion, String sapCode, String embryoCode, Integer modifyNum, String adjustSource, TCxMonthPlanAdjust adjust, String bomDataVersion) {
        adjust.setBaseVale(null);
        adjust.setMonthPlanApsVersion(apsVersion);
        adjust.setSapCode(sapCode);
        adjust.setEmbryoCode(embryoCode);
        adjust.setAdjustSource(adjustSource);
        adjust.setPlanModifyQty(modifyNum);
        adjust.setBomDataVersion(bomDataVersion);
    }

    // ==================== 单规格处理部分 ==================
//    /**
//     * 单规格处理
//     * @param prodPlan 计划明细
//     * @param modifyNum 调整量
//     * @param log 日志
//     * @param logDetail 日志明细
//     * @param handleKey 处理key 1.MODIFY 2.ADD
//     * @return AjaxResult
//     */
//    private AjaxResult buildUnitCastor(MdmMonthProdPlan prodPlan, Long modifyNum, MonthSumProcessLog log, StringBuilder logDetail, Integer handleKey) {
//        // 入参key错误处理
//        if (!handleKey.equals(MODIFY) && !handleKey.equals(ADD)) {
//            logDetail.append("handleKey错误");
//            log.setLogDetail(logDetail.toString());
//            logMapper.insert(log);
//            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.income"));
//        }
//        String apsVersion = prodPlan.getMonthPlanApsVersion();
//        // 胎胚信息
//        List<String> embryoCodeList = Collections.singletonList(prodPlan.getEmbryoCode());
//        logDetail.append("【胎胚代码List】：").append(toJSONString(embryoCodeList)).append(division);
//        // 施工信息Map
//        List<EngineConstructionInfo> engineConstructionInfos = constructionInfoService.selectEngineConstructionInfoListBatch(embryoCodeList);
//        logDetail.append("【施工信息】：").append(toJSONString(engineConstructionInfos)).append(division);
//        Map<String, EngineConstructionInfo> embryoCodeMap = new HashMap<>();
//        // 大卷信息
//        List<String> beltSpecList = new ArrayList<>();
//        List<String> crodSpecList = new ArrayList<>();
//        for (EngineConstructionInfo info : engineConstructionInfos) {
//            embryoCodeMap.put(info.getEmbryoCode(), info);
//            if (StringUtils.isNotBlank(info.getArticleCrownSpec())) {
//                beltSpecList.add(info.getArticleCrownSpec());
//            }
//            if (StringUtils.isNotBlank(info.getCordSpec())) {
//                crodSpecList.add(info.getCordSpec());
//            }
//        }
//        List<CxTCd15BigRoll> cd15BigRollList = cd15BigRollService.getByBeltSpecList(beltSpecList);
//        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(crodSpecList);
//        Map<String, CxTCd15BigRoll> cd15BigRollMap = CollectionUtil.toMap(cd15BigRollList, CxTCd15BigRoll::getBigRollCode);
//        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
//        // 大卷默认信息
//        CxTCd15Params cd15Params = cd15ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
//        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
//        logDetail.append("【CD15大卷信息】：").append(toJSONString(cd15BigRollMap)).append(division);
//        logDetail.append("【CD90大卷信息】：").append(toJSONString(cd90BigRollMap)).append(division);
//        logDetail.append("【CD15大卷默认信息】：").append(toJSONString(cd15Params)).append(division);
//        logDetail.append("【CD90大卷默认信息】：").append(toJSONString(cd90Params)).append(division);
//
//        // 按（物料编号 + 胚胎代码 + 库存地点）分组汇总   月度分析汇总
//        MdmMonthPlanAnalysis analysisQuery = new MdmMonthPlanAnalysis();
//        analysisQuery.setMonthPlanApsVersion(prodPlan.getMonthPlanApsVersion());
//        analysisQuery.setMaterialCode(prodPlan.getMaterialCode());
//        analysisQuery.setEmbryoCode(prodPlan.getEmbryoCode());
//        analysisQuery.setStorageLocation(prodPlan.getStorageLocation());
//        List<MdmMonthPlanAnalysis> oldAnalysisList = mdmMonthPlanAnalysisService.getByParams(analysisQuery);
//        // 幅宽
//        Double gdyyBreadth = getDoubleOrDefault(this.getGdyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_GDYY_BREADTH);
//        Double xwyyBreadth = getDoubleOrDefault(this.getXwyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_XWYY_BREADTH);
//        logDetail.append("【钢带压延幅宽】：").append(gdyyBreadth).append(division);
//        logDetail.append("【纤维压延幅宽】：").append(gdyyBreadth).append(division);
//        if (CollectionUtil.isEmpty(oldAnalysisList)) {
//            oldAnalysisList = buildMdmMonthPlanAnalysesCastor(Collections.singletonList(prodPlan), embryoCodeMap, cd15BigRollMap, cd90BigRollMap, cd15Params, cd90Params, logDetail, gdyyBreadth, xwyyBreadth);
//        }
//        MdmMonthPlanAnalysis oldAnalysis = oldAnalysisList.get(0);
//        oldAnalysis.setActualArrangement(oldAnalysis.getActualArrangement() + modifyNum.intValue());
//        // 重新计算分析汇总中半部件月度计划量
//        // 胚胎计划量
//        BigDecimal planNum = BigDecimal.valueOf(oldAnalysis.getActualArrangement());
//        // 对应胚胎代码施工信息
//        EngineConstructionInfo constructionInfo = embryoCodeMap.get(oldAnalysis.getEmbryoCode());
//        getEmbryoUnitConsumption(oldAnalysis, constructionInfo, planNum, cd15BigRollMap.get(constructionInfo.getArticleCrownSpec()), cd90BigRollMap.get(constructionInfo.getCordSpec()), cd15Params, cd90Params, gdyyBreadth, xwyyBreadth);
//        // 后续半部件调量用的分析汇总实体
//        MdmMonthPlanAnalysis modifyAnalysis = new MdmMonthPlanAnalysis();
//        // 仅计算修正量所需的量
//        if (handleKey.equals(MODIFY)) {
//            getEmbryoUnitConsumption(modifyAnalysis, constructionInfo, BigDecimal.valueOf(modifyNum), cd15BigRollMap.get(constructionInfo.getArticleCrownSpec()), cd90BigRollMap.get(constructionInfo.getCordSpec()), cd15Params, cd90Params, gdyyBreadth, xwyyBreadth);
//        } else if (handleKey.equals(ADD)){
//            getEmbryoUnitConsumption(modifyAnalysis, constructionInfo, BigDecimal.valueOf(prodPlan.getActualArrangement()), cd15BigRollMap.get(constructionInfo.getArticleCrownSpec()), cd90BigRollMap.get(constructionInfo.getCordSpec()), cd15Params, cd90Params, gdyyBreadth, xwyyBreadth);
//        }
//
//        // 计划版本数据
//        MdmMonthPlanMain mainQuery = new MdmMonthPlanMain();
//        mainQuery.setMonthPlanApsVersion(prodPlan.getMonthPlanApsVersion());
//        List<MdmMonthPlanMain> mdmMonthPlanMains = mdmMonthPlanMainService.selectMdmMonthPlanMainList(mainQuery);
//        if (CollectionUtil.isEmpty(mdmMonthPlanMains)) {
//            logDetail.append("计划版本不存在");
//            log.setLogDetail(logDetail.toString());
//            logMapper.insert(log);
//            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.apsVersion.empty"));
//        }
//        MdmMonthPlanMain mdmMonthPlanMain = mdmMonthPlanMains.get(0);
//        // 待投产数据
//        TCxPlanProductStatus oldStatus = planProductStatusMapper.selectOneByMonthPlanApsVersionAndSapCodeAndEmbryoCode(prodPlan.getMonthPlanApsVersion(), prodPlan.getMaterialCode(), prodPlan.getEmbryoCode());
//        logDetail.append("【旧待投产数据：】").append(toJSONString(oldStatus)).append(division);
//        if (oldStatus.getId() == null) {
//            oldStatus = buildProductStatus(mdmMonthPlanMain, CollectionUtil.toMapList(Collections.singletonList(prodPlan), obj -> obj.getMaterialCode() + "+" + obj.getEmbryoCode()), prodPlan.getMaterialCode() + "+" + prodPlan.getEmbryoCode(), new HashMap<>());
//        } else {
//            oldStatus.setMonthPlanTotalQty(oldStatus.getMonthPlanTotalQty() + prodPlan.getActualArrangement().intValue());
//        }
//        logDetail.append("【新待投产数据：】").append(toJSONString(oldStatus)).append(division);
//
//        // 按（物料编号）分组汇总   成型工序外胎计划量汇总表
//        List<TCxMonthPlanSurplus> oldCxList = planSurplusService.getBySapCodeAndApsVersion(Collections.singletonList(prodPlan.getMaterialCode()), prodPlan.getMonthPlanApsVersion());
//        if (CollectionUtil.isEmpty(oldCxList)) {
//            // 为空一定是新增
//            oldCxList = buildCxMonthPlanSurplusesCastor(mdmMonthPlanMain, oldAnalysisList, new HashMap<>(), DATA_SOURCE_FROM_MAIN);
//        } else {
//            TCxMonthPlanSurplus oldCx = oldCxList.get(0);
//            if (handleKey.equals(MODIFY)) {
//                oldCx.setPlanModifyQty(oldCx.getPlanModifyQty() + modifyNum.intValue());
//            } else if (handleKey.equals(ADD)){
//                oldCx.setMonthPlanQty(oldCx.getMonthPlanQty() + prodPlan.getActualArrangement().intValue());
//            }
//            oldCx.setMonthRemainQty(getMonthRemainQty(oldCx));
//            oldCx.setBaseVale(oldCx.getId());
//        }
//        // 按（胚胎代码）分组汇总    成型工序胎胚计划量汇总表
//        List<TCxEmbryoMonthPlanSurplus> oldEmbryoList = embryoMonthPlanSurplusService.getByEmbryoListAndApsVersion(Collections.singletonList(prodPlan.getEmbryoCode()), prodPlan.getMonthPlanApsVersion());
//        if (CollectionUtil.isEmpty(oldEmbryoList)) {
//            // 为空一定是新增
//            oldEmbryoList = buildCxEmbryoMonthPlanSurplusesCastor(mdmMonthPlanMain, oldAnalysisList, new HashMap<>(), DATA_SOURCE_FROM_MAIN);
//        } else {
//            TCxEmbryoMonthPlanSurplus oldEmbryo = oldEmbryoList.get(0);
//            if (handleKey.equals(MODIFY)) {
//                oldEmbryo.setMonthPlanModifyQty(oldEmbryo.getMonthPlanModifyQty().add(BigDecimal.valueOf(modifyNum)));
//            } else if (handleKey.equals(ADD)){
//                oldEmbryo.setMonthPlanQty(oldEmbryo.getMonthPlanQty().add(BigDecimal.valueOf(prodPlan.getActualArrangement())));
//            }
//            oldEmbryo.setMonthRemainQty(getEmbryoRemainQty(oldEmbryo));
//            oldEmbryo.setBaseVale(oldEmbryo.getId());
//        }
//
//        // 半部件部分
//        List<String> tmCodeList = new ArrayList<>();
//        List<String> tcCodeList = new ArrayList<>();
//        List<String> ncCodeList = new ArrayList<>();
//        List<String> cd15CodeList = new ArrayList<>();
//        List<String> cd90CodeList = new ArrayList<>();
//        List<String> tqCodeList = new ArrayList<>();
//        List<String> gsqCodeList = new ArrayList<>();
//        List<String> gdyyCodeList = new ArrayList<>();
//        List<String> xwyyCodeList = new ArrayList<>();
//        // 填充物料编号List
//        this.buildCodeList(engineConstructionInfos, tmCodeList, tcCodeList, ncCodeList, cd15CodeList, cd90CodeList, tqCodeList, gsqCodeList, gdyyCodeList, xwyyCodeList);
//
//        // 旧半部件数据
//        List<TTmMonthPlanSurplus> oldTmList = tmMonthPlanSurplusService.getByCodeList(apsVersion, tmCodeList);
//        Map<String, TTmMonthPlanSurplus> oldTmMap = buildToDeleteMap(oldTmList);
//        List<TTcMonthPlanSurplus> oldTcList = tcMonthPlanSurplusService.getByCodeList(apsVersion, tcCodeList);
//        Map<String, TTcMonthPlanSurplus> oldTcMap = buildToDeleteMap(oldTcList);
//        List<TNcMonthPlanSurplus> oldNcList = ncMonthPlanSurplusService.getByCodeList(apsVersion, ncCodeList);
//        Map<String, TNcMonthPlanSurplus> oldNcMap = buildToDeleteMap(oldNcList);
//        List<TTqMonthPlanSurplus> oldTqList = tqMonthPlanSurplusService.getByCodeList(apsVersion, tqCodeList);
//        Map<String, TTqMonthPlanSurplus> oldTqMap = buildToDeleteMap(oldTqList);
//        List<TGsqMonthPlanSurplus> oldGsqList = gsqMonthPlanSurplusService.getByCodeList(apsVersion, gsqCodeList);
//        Map<String, TGsqMonthPlanSurplus> oldGsqMap = buildToDeleteMap(oldGsqList);
//        List<TGdcdMonthPlanSurplus> oldCd15List = gdcdMonthPlanSurplusService.getByCodeList(apsVersion, cd15CodeList);
//        Map<String, TGdcdMonthPlanSurplus> oldCd15Map = buildToDeleteMap(oldCd15List);
//        List<TLbcdMonthPlanSurplus> oldCd90List = lbcdMonthPlanSurplusService.getByCodeList(apsVersion, cd90CodeList);
//        Map<String, TLbcdMonthPlanSurplus> oldCd90Map = buildToDeleteMap(oldCd90List);
//        List<TGdyyMonthPlanSurplus> oldGdyyList = gdyyMonthPlanSurplusService.getByCodeList(apsVersion, gdyyCodeList);
//        Map<String, TGdyyMonthPlanSurplus> oldGdyyMap = buildToDeleteMap(oldGdyyList);
//        List<TXwyyMonthPlanSurplus> oldXwyyList = xwyyMonthPlanSurplusService.getByCodeList(apsVersion, xwyyCodeList);
//        Map<String, TXwyyMonthPlanSurplus> oldXwyyMap = buildToDeleteMap(oldXwyyList);
//        // 更新半部件数据Map
//        this.buildUnitModifyHalfPartCastor(logDetail, mdmMonthPlanMain, modifyAnalysis, oldTmMap, oldTcMap, oldNcMap, oldTqMap, oldGsqMap, oldCd15Map, oldCd90Map, oldGdyyMap, oldXwyyMap, handleKey);
//        // 更新
//        this.updateUnitModify(log, logDetail, oldAnalysis, oldCxList, oldEmbryoList, oldTmMap, oldTcMap, oldNcMap, oldTqMap, oldGsqMap, oldCd15Map, oldCd90Map, oldGdyyMap, oldXwyyMap);
//        return AjaxResult.success();
//    }

    /**
     * 更新半部件数据Map
     * @param logDetail
     * @param modifyAnalysis
     * @param oldTmMap
     * @param oldTcMap
     * @param oldNcMap
     * @param oldTqMap
     * @param oldGsqMap
     * @param oldCd15Map
     * @param oldCd90Map
     * @param oldGdyyMap
     * @param oldXwyyMap
     */
    private void buildUnitModifyHalfPartCastor(StringBuilder logDetail, MdmMonthPlanMain mdmMonthPlanMain, MdmMonthPlanAnalysis modifyAnalysis,
                                               Map<String, TTmMonthPlanSurplus> oldTmMap, Map<String, TTcMonthPlanSurplus> oldTcMap,
                                               Map<String, TNcMonthPlanSurplus> oldNcMap, Map<String, TTqMonthPlanSurplus> oldTqMap,
                                               Map<String, TGsqMonthPlanSurplus> oldGsqMap, Map<String, TGdcdMonthPlanSurplus> oldCd15Map,
                                               Map<String, TLbcdMonthPlanSurplus> oldCd90Map, Map<String, TGdyyMonthPlanSurplus> oldGdyyMap,
                                               Map<String, TXwyyMonthPlanSurplus> oldXwyyMap, Integer handleKey) {
        logDetail.append("【旧数据部分】").append(division);
        logDetail.append("【胎面旧数据】：").append(toJSONString(oldTmMap)).append(division)
                .append("【胎侧旧数据】：").append(toJSONString(oldTcMap)).append(division)
                .append("【内衬旧数据】：").append(toJSONString(oldNcMap)).append(division)
                .append("【胎圈旧数据】：").append(toJSONString(oldTqMap)).append(division)
                .append("【钢丝圈旧数据】：").append(toJSONString(oldGsqMap)).append(division)
                .append("【15度裁断旧数据】：").append(toJSONString(oldCd15Map)).append(division)
                .append("【90度裁断旧数据】：").append(toJSONString(oldCd90Map)).append(division)
                .append("【钢带压延旧数据】：").append(toJSONString(oldGdyyMap)).append(division)
                .append("【纤维压延旧数据】：").append(toJSONString(oldXwyyMap)).append(division)
        ;
        TTmMonthPlanSurplus tm = new TTmMonthPlanSurplus();
        buildModifyHalfPart(oldTmMap, BigDecimal.valueOf(modifyAnalysis.getTmMonthPlanQty()), modifyAnalysis.getTmCode(), tm, mdmMonthPlanMain, handleKey);
        TTcMonthPlanSurplus tc = new TTcMonthPlanSurplus();
        buildModifyHalfPart(oldTcMap, modifyAnalysis.getTcMonthPlanQty(), modifyAnalysis.getTcCode(), tc, mdmMonthPlanMain, handleKey);
        TNcMonthPlanSurplus nc = new TNcMonthPlanSurplus();
        buildModifyHalfPart(oldNcMap, modifyAnalysis.getNcMonthPlanQty(), modifyAnalysis.getNcCode(), nc, mdmMonthPlanMain, handleKey);
        TTqMonthPlanSurplus tq = new TTqMonthPlanSurplus();
        buildModifyHalfPart(oldTqMap, modifyAnalysis.getTqMonthPlanQty(), modifyAnalysis.getTqCode(), tq, mdmMonthPlanMain, handleKey);
        TGsqMonthPlanSurplus gsq = new TGsqMonthPlanSurplus();
        buildModifyHalfPart(oldGsqMap, modifyAnalysis.getGsqMonthPlanQty(), modifyAnalysis.getGsqCode(), gsq, mdmMonthPlanMain, handleKey);
        TGdcdMonthPlanSurplus cd15One = new TGdcdMonthPlanSurplus();
        buildModifyHalfPart(oldCd15Map, modifyAnalysis.getCd15OneMonthPlanQty(), modifyAnalysis.getCd15OneCode(), cd15One, mdmMonthPlanMain, handleKey);
        TGdcdMonthPlanSurplus cd15Two = new TGdcdMonthPlanSurplus();
        buildModifyHalfPart(oldCd15Map, modifyAnalysis.getCd15TwoMonthPlanQty(), modifyAnalysis.getCd15TwoCode(), cd15Two, mdmMonthPlanMain, handleKey);
        TLbcdMonthPlanSurplus cd90One = new TLbcdMonthPlanSurplus();
        buildModifyHalfPart(oldCd90Map, modifyAnalysis.getCd90OneMonthPlanQty(), modifyAnalysis.getCd90OneCode(), cd90One, mdmMonthPlanMain, handleKey);
        TLbcdMonthPlanSurplus cd90Two = new TLbcdMonthPlanSurplus();
        buildModifyHalfPart(oldCd90Map, modifyAnalysis.getCd90TwoMonthPlanQty(), modifyAnalysis.getCd90TwoCode(), cd90Two, mdmMonthPlanMain, handleKey);
        TLbcdMonthPlanSurplus cd90Three = new TLbcdMonthPlanSurplus();
        buildModifyHalfPart(oldCd90Map, modifyAnalysis.getCd90ThreeMonthPlanQty(), modifyAnalysis.getCd90ThreeCode(), cd90Three, mdmMonthPlanMain, handleKey);
        // 钢带压延和纤维压延还有个数要算
        TGdyyMonthPlanSurplus gdyy = new TGdyyMonthPlanSurplus();
        buildModifyHalfPart(oldGdyyMap, modifyAnalysis.getGdyyMonthPlanQty(), modifyAnalysis.getGdyyCode(), gdyy, mdmMonthPlanMain, handleKey);
        for (String code : oldGdyyMap.keySet()) {
            TGdyyMonthPlanSurplus oldGdyy = oldGdyyMap.get(code);
            if (handleKey.equals(MODIFY)) {
                oldGdyy.setMonthPlanModifyQty2(oldGdyy.getMonthPlanModifyQty2().add(BigDecimal.valueOf(modifyAnalysis.getGdyyMonthPlanNumQty())));
            } else if (handleKey.equals(ADD)) {
                oldGdyy.setMonthPlanQty2(oldGdyy.getMonthPlanQty().add(BigDecimal.valueOf(modifyAnalysis.getGdyyMonthPlanNumQty())));
            }
            oldGdyy.setMonthRemainQty2(oldGdyy.getMonthPlanQty2().add(oldGdyy.getMonthPlanModifyQty2()).subtract(oldGdyy.getMonthFinishQty2()));
        }
        TXwyyMonthPlanSurplus xwyy = new TXwyyMonthPlanSurplus();
        buildModifyHalfPart(oldXwyyMap, modifyAnalysis.getXwyyMonthPlanQty(), modifyAnalysis.getXwyyCode(), xwyy, mdmMonthPlanMain, handleKey);
        for (String code : oldXwyyMap.keySet()) {
            TXwyyMonthPlanSurplus oldXwyy = oldXwyyMap.get(code);
            if (handleKey.equals(MODIFY)) {
                oldXwyy.setMonthPlanModifyQty2(oldXwyy.getMonthPlanModifyQty2().add(BigDecimal.valueOf(modifyAnalysis.getGdyyMonthPlanNumQty())));
            } else if (handleKey.equals(ADD)) {
                oldXwyy.setMonthPlanQty2(oldXwyy.getMonthPlanQty().add(BigDecimal.valueOf(modifyAnalysis.getGdyyMonthPlanNumQty())));
            }
            oldXwyy.setMonthRemainQty2(oldXwyy.getMonthPlanQty2().add(oldXwyy.getMonthPlanModifyQty2()).subtract(oldXwyy.getMonthFinishQty2()));
        }
        logDetail.append("【新数据部分】").append(division);
        logDetail.append("【胎面新数据】：").append(toJSONString(oldTmMap)).append(division)
                .append("【胎侧新数据】：").append(toJSONString(oldTcMap)).append(division)
                .append("【内衬新数据】：").append(toJSONString(oldNcMap)).append(division)
                .append("【胎圈新数据】：").append(toJSONString(oldTqMap)).append(division)
                .append("【钢丝圈新数据】：").append(toJSONString(oldGsqMap)).append(division)
                .append("【15度裁断新数据】：").append(toJSONString(oldCd15Map)).append(division)
                .append("【90度裁断新数据】：").append(toJSONString(oldCd90Map)).append(division)
                .append("【钢带压延新数据】：").append(toJSONString(oldGdyyMap)).append(division)
                .append("【纤维压延新数据】：").append(toJSONString(oldXwyyMap)).append(division)
        ;
    }

    /**
     * 单规格更新
     * @param log
     * @param logDetail
     * @param oldAnalysis
     * @param oldCxList
     * @param oldEmbryoList
     * @param oldTmMap
     * @param oldTcMap
     * @param oldNcMap
     * @param oldTqMap
     * @param oldGsqMap
     * @param oldCd15Map
     * @param oldCd90Map
     * @param oldGdyyMap
     * @param oldXwyyMap
     */
    private void updateUnitModify(MonthSumProcessLog log, StringBuilder logDetail, MdmMonthPlanAnalysis oldAnalysis, List<TCxMonthPlanSurplus> oldCxList, List<TCxEmbryoMonthPlanSurplus> oldEmbryoList, Map<String, TTmMonthPlanSurplus> oldTmMap, Map<String, TTcMonthPlanSurplus> oldTcMap, Map<String, TNcMonthPlanSurplus> oldNcMap, Map<String, TTqMonthPlanSurplus> oldTqMap, Map<String, TGsqMonthPlanSurplus> oldGsqMap, Map<String, TGdcdMonthPlanSurplus> oldCd15Map, Map<String, TLbcdMonthPlanSurplus> oldCd90Map, Map<String, TGdyyMonthPlanSurplus> oldGdyyMap, Map<String, TXwyyMonthPlanSurplus> oldXwyyMap) {
        mdmMonthPlanAnalysisService.update(oldAnalysis);
        planSurplusService.mergeSql(oldCxList);
        embryoMonthPlanSurplusService.mergeSql(oldEmbryoList);
        monthPlanSurplusService.mergeTm(new ArrayList<>(oldTmMap.values()));
        monthPlanSurplusService.mergeTc(new ArrayList<>(oldTcMap.values()));
        monthPlanSurplusService.mergeNc(new ArrayList<>(oldNcMap.values()));
        monthPlanSurplusService.mergeTq(new ArrayList<>(oldTqMap.values()));
        monthPlanSurplusService.mergeGsq(new ArrayList<>(oldGsqMap.values()));
        monthPlanSurplusService.mergeCd15(new ArrayList<>(oldCd15Map.values()));
        monthPlanSurplusService.mergeCd90(new ArrayList<>(oldCd90Map.values()));
        monthPlanSurplusService.mergeGdyy(new ArrayList<>(oldGdyyMap.values()));
        monthPlanSurplusService.mergeXwyy(new ArrayList<>(oldXwyyMap.values()));

        // 日志插入
        setBaseSysValue(log);
        log.setLogDetail(logDetail.toString());
        logMapper.insert(log);
    }

    // ==================== 基础信息获取部分 ==================
    /**
     * 获取纤维压延工序参数map
     *
     * @return
     */
    private Map<String, String> getXwyyParamsMap() {
        return cd90ParamsService.listXwyyParams().stream()
                .collect(Collectors.toMap(XwyyParamsVo::getParamCode, XwyyParamsVo::getParamValue, (v1, v2) -> v2));
    }

    /**
     * 获取钢带压延工序参数map
     *
     * @return
     */
    private Map<String, String> getGdyyParamsMap() {
        return cd15ParamsService.listGdyyParams().stream()
                .collect(Collectors.toMap(XwyyParamsVo::getParamCode, XwyyParamsVo::getParamValue, (v1, v2) -> v2));
    }

    /**
     * 获取待删除生产排程版本
     * @param year
     * @param month
     * @param toFinal
     * @return
     */
    private String getToDeleteVersion(String year, String month, String toFinal) {
        List<MdmMonthPlanMain> isExistList = checkPlanMainExist(year, month, toFinal);
        if (CollectionUtil.isEmpty(isExistList)) {
            return "";
        }
        MdmMonthPlanMain toDeletePlanMain = isExistList.get(0);
        return toDeletePlanMain.getMonthPlanApsVersion();
    }

    /**
     * 获取APS版本
     * @return APS版本
     */
    private static String getVersionPre(String prefix) {
        Date now = DateUtils.getNowDate();
        String apsYear = String.format("%tY", now);
        String apsMonth = String.format("%tm", now);
        String apsDay = String.format("%td", now);
        return prefix + apsYear + apsMonth + apsDay;
    }

    // ==================== 数据校验部分 ==================

    /**
     * 判断是否存在旧生产排程版本
     * @param year
     * @param month
     * @param toFinal
     * @return
     */
    private List<MdmMonthPlanMain> checkPlanMainExist(String year, String month, String toFinal) {
        MdmMonthPlanMain preQuery = new MdmMonthPlanMain();
        preQuery.setYear(year);
        preQuery.setMonth(month);
        preQuery.setIsFinalized(toFinal);
        return mdmMonthPlanMainService.selectMdmMonthPlanMainList(preQuery);
    }

    /**
     * 判断剩余量是否合格
     * @param monthPlanSurplus
     * @return
     */
    private static AjaxResult checkRemain(TCxEmbryoMonthPlanSurplus monthPlanSurplus) {
        BigDecimal monthRemainQty = monthPlanSurplus.getMonthPlanQty().add(monthPlanSurplus.getMonthPlanModifyQty()).add(monthPlanSurplus.getEmbryoBadQty()).subtract(monthPlanSurplus.getLastMonthStock()).subtract(monthPlanSurplus.getMonthFinishQty());
        if (monthRemainQty.compareTo(BigDecimal.ZERO) < 0) {
            return AjaxResult.error(I18nUtil.getMessage("mdm.error.message.remain"));
        }
        return AjaxResult.success();
    }

    // ==================== 基础数值计算获取部分 ==================

    // 获取开始日期
    private Integer getStartNum(MdmMonthProdPlan prodPlan) {
        if (prodPlan.getProductQty1() != null && prodPlan.getProductQty1() > 0) {
            return 1;
        }
        if (prodPlan.getProductQty2() != null && prodPlan.getProductQty2() > 0) {
            return 2;
        }
        if (prodPlan.getProductQty3() != null && prodPlan.getProductQty3() > 0) {
            return 3;
        }
        if (prodPlan.getProductQty4() != null && prodPlan.getProductQty4() > 0) {
            return 4;
        }
        if (prodPlan.getProductQty5() != null && prodPlan.getProductQty5() > 0) {
            return 5;
        }
        if (prodPlan.getProductQty6() != null && prodPlan.getProductQty6() > 0) {
            return 6;
        }
        if (prodPlan.getProductQty7() != null && prodPlan.getProductQty7() > 0) {
            return 7;
        }
        if (prodPlan.getProductQty8() != null && prodPlan.getProductQty8() > 0) {
            return 8;
        }
        if (prodPlan.getProductQty9() != null && prodPlan.getProductQty9() > 0) {
            return 9;
        }
        if (prodPlan.getProductQty10() != null && prodPlan.getProductQty10() > 0) {
            return 10;
        }
        if (prodPlan.getProductQty11() != null && prodPlan.getProductQty11() > 0) {
            return 11;
        }
        if (prodPlan.getProductQty12() != null && prodPlan.getProductQty12() > 0) {
            return 12;
        }
        if (prodPlan.getProductQty13() != null && prodPlan.getProductQty13() > 0) {
            return 13;
        }
        if (prodPlan.getProductQty14() != null && prodPlan.getProductQty14() > 0) {
            return 14;
        }
        if (prodPlan.getProductQty15() != null && prodPlan.getProductQty15() > 0) {
            return 15;
        }
        if (prodPlan.getProductQty16() != null && prodPlan.getProductQty16() > 0) {
            return 16;
        }
        if (prodPlan.getProductQty17() != null && prodPlan.getProductQty17() > 0) {
            return 17;
        }
        if (prodPlan.getProductQty18() != null && prodPlan.getProductQty18() > 0) {
            return 18;
        }
        if (prodPlan.getProductQty19() != null && prodPlan.getProductQty19() > 0) {
            return 19;
        }
        if (prodPlan.getProductQty20() != null && prodPlan.getProductQty20() > 0) {
            return 20;
        }
        if (prodPlan.getProductQty21() != null && prodPlan.getProductQty21() > 0) {
            return 21;
        }
        if (prodPlan.getProductQty22() != null && prodPlan.getProductQty22() > 0) {
            return 22;
        }
        if (prodPlan.getProductQty23() != null && prodPlan.getProductQty23() > 0) {
            return 23;
        }
        if (prodPlan.getProductQty24() != null && prodPlan.getProductQty24() > 0) {
            return 24;
        }
        if (prodPlan.getProductQty25() != null && prodPlan.getProductQty25() > 0) {
            return 25;
        }
        if (prodPlan.getProductQty26() != null && prodPlan.getProductQty26() > 0) {
            return 26;
        }
        if (prodPlan.getProductQty27() != null && prodPlan.getProductQty27() > 0) {
            return 27;
        }
        if (prodPlan.getProductQty28() != null && prodPlan.getProductQty28() > 0) {
            return 28;
        }
        if (prodPlan.getProductQty29() != null && prodPlan.getProductQty29() > 0) {
            return 29;
        }
        if (prodPlan.getProductQty30() != null && prodPlan.getProductQty30() > 0) {
            return 30;
        }
        if (prodPlan.getProductQty31() != null && prodPlan.getProductQty31() > 0) {
            return 31;
        }
        return 1;
    }

    // 获取结束日期
    private Integer getEndNum(MdmMonthProdPlan prodPlan) {
        if (prodPlan.getProductQty31() != null && prodPlan.getProductQty31() > 0) {
            return 31;
        }
        if (prodPlan.getProductQty30() != null && prodPlan.getProductQty30() > 0) {
            return 30;
        }
        if (prodPlan.getProductQty29() != null && prodPlan.getProductQty29() > 0) {
            return 29;
        }
        if (prodPlan.getProductQty28() != null && prodPlan.getProductQty28() > 0) {
            return 28;
        }
        if (prodPlan.getProductQty27() != null && prodPlan.getProductQty27() > 0) {
            return 27;
        }
        if (prodPlan.getProductQty26() != null && prodPlan.getProductQty26() > 0) {
            return 26;
        }
        if (prodPlan.getProductQty25() != null && prodPlan.getProductQty25() > 0) {
            return 25;
        }
        if (prodPlan.getProductQty24() != null && prodPlan.getProductQty24() > 0) {
            return 24;
        }
        if (prodPlan.getProductQty23() != null && prodPlan.getProductQty23() > 0) {
            return 23;
        }
        if (prodPlan.getProductQty22() != null && prodPlan.getProductQty22() > 0) {
            return 22;
        }
        if (prodPlan.getProductQty21() != null && prodPlan.getProductQty21() > 0) {
            return 21;
        }
        if (prodPlan.getProductQty20() != null && prodPlan.getProductQty20() > 0) {
            return 20;
        }
        if (prodPlan.getProductQty19() != null && prodPlan.getProductQty19() > 0) {
            return 19;
        }
        if (prodPlan.getProductQty18() != null && prodPlan.getProductQty18() > 0) {
            return 18;
        }
        if (prodPlan.getProductQty17() != null && prodPlan.getProductQty17() > 0) {
            return 17;
        }
        if (prodPlan.getProductQty16() != null && prodPlan.getProductQty16() > 0) {
            return 16;
        }
        if (prodPlan.getProductQty15() != null && prodPlan.getProductQty15() > 0) {
            return 15;
        }
        if (prodPlan.getProductQty14() != null && prodPlan.getProductQty14() > 0) {
            return 14;
        }
        if (prodPlan.getProductQty13() != null && prodPlan.getProductQty13() > 0) {
            return 13;
        }
        if (prodPlan.getProductQty12() != null && prodPlan.getProductQty12() > 0) {
            return 12;
        }
        if (prodPlan.getProductQty11() != null && prodPlan.getProductQty11() > 0) {
            return 11;
        }
        if (prodPlan.getProductQty10() != null && prodPlan.getProductQty10() > 0) {
            return 10;
        }
        if (prodPlan.getProductQty9() != null && prodPlan.getProductQty9() > 0) {
            return 9;
        }
        if (prodPlan.getProductQty8() != null && prodPlan.getProductQty8() > 0) {
            return 8;
        }
        if (prodPlan.getProductQty7() != null && prodPlan.getProductQty7() > 0) {
            return 7;
        }
        if (prodPlan.getProductQty6() != null && prodPlan.getProductQty6() > 0) {
            return 6;
        }
        if (prodPlan.getProductQty5() != null && prodPlan.getProductQty5() > 0) {
            return 5;
        }
        if (prodPlan.getProductQty4() != null && prodPlan.getProductQty4() > 0) {
            return 4;
        }
        if (prodPlan.getProductQty3() != null && prodPlan.getProductQty3() > 0) {
            return 3;
        }
        if (prodPlan.getProductQty2() != null && prodPlan.getProductQty2() > 0) {
            return 2;
        }
        if (prodPlan.getProductQty1() != null && prodPlan.getProductQty1() > 0) {
            return 1;
        }
        return 31;
    }

    /**
     * 月计划剩余量获取
     * @param k
     * @param <K>
     * @return
     */
    private static <K extends MonthPlanSurplusBaseEntity> BigDecimal getBaseMonthRemainQty(K k) {
        return k.getMonthPlanQty().add(k.getMonthPlanModifyQty()).subtract(k.getMonthFinishQty()).setScale(3, RoundingMode.UP);
    }

    /**
     * 获取胎胚单耗换算
     * @param analysis 数据存储实体
     * @param constructionInfo 施工信息
     * @param planNum 计划量
     * @param cd15BigRoll 钢带压延信息
     * @param cd90BigRoll 纤维压延信息
     */
    private static void getEmbryoUnitConsumption(MdmMonthPlanAnalysis analysis, EngineProductConstructionInfo constructionInfo,
                                                 BigDecimal planNum, CxTCd15BigRoll cd15BigRoll, CxTCd90BigRoll cd90BigRoll,
                                                 CxTCd15Params cd15Params, CxTCd90Params cd90Params, Double gdyyBreadth, Double xwyyBreadth) {
        BigDecimal divide = BigDecimal.valueOf(1000);
        // 1.胎面月度计划量
        if (StringUtils.isNotBlank(constructionInfo.getTreadCode())) {
            BigDecimal tmNum = constructionInfo.getTreadShoulderLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getTreadShoulderLength()).multiply(planNum).divide(divide, 3, BigDecimal.ROUND_UP);
            analysis.setTmCode(constructionInfo.getTreadCode());
            analysis.setTmMonthPlanQty(tmNum.doubleValue());
        } else {
            analysis.setTmMonthPlanQty(0d);
        }
        // 2.胎侧月度计划量
        if (StringUtils.isNotBlank(constructionInfo.getSidewallCode())) {
            BigDecimal tcNum = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(planNum).divide(divide, 3, BigDecimal.ROUND_UP);
            analysis.setTcCode(constructionInfo.getSidewallCode());
            analysis.setTcMonthPlanQty(tcNum);
        } else {
            analysis.setTcMonthPlanQty(BigDecimal.ZERO);
        }

        // 3.内衬
        if (StringUtils.isNotBlank(constructionInfo.getInsideCode())) {
            BigDecimal tcNum = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(planNum).divide(divide, 3, BigDecimal.ROUND_UP);
            analysis.setNcCode(constructionInfo.getInsideCode());
            analysis.setNcMonthPlanQty(tcNum);
        } else {
            analysis.setNcMonthPlanQty(BigDecimal.ZERO);
        }

        // 15度 90度 默认值
        analysis.setCd15OneMonthPlanQty(BigDecimal.ZERO);
        analysis.setCd15TwoMonthPlanQty(BigDecimal.ZERO);
        analysis.setCd90OneMonthPlanQty(BigDecimal.ZERO);
        analysis.setCd90TwoMonthPlanQty(BigDecimal.ZERO);
        analysis.setCd90ThreeMonthPlanQty(BigDecimal.ZERO);
        // 4.15度裁断
        if (StringUtils.isNotBlank(constructionInfo.getBeltCode1())) {
            // 1#
            analysis.setCd15OneCode(constructionInfo.getBeltCode1());
            BigDecimal cd15OneNum = constructionInfo.getFitDrumPerimeter() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getFitDrumPerimeter()).multiply(planNum).divide(divide, 3, BigDecimal.ROUND_UP);
            analysis.setCd15OneMonthPlanQty(cd15OneNum);
        }
        if (StringUtils.isNotBlank(constructionInfo.getBeltCode2())) {
            // 2#
            analysis.setCd15TwoCode(constructionInfo.getBeltCode2());
            BigDecimal cd15TwoNum = constructionInfo.getFitDrumPerimeter() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getFitDrumPerimeter()).multiply(planNum).divide(divide, 3, BigDecimal.ROUND_UP);
            analysis.setCd15TwoMonthPlanQty(cd15TwoNum);
        }
        BigDecimal cd15OneNum = analysis.getCd15OneMonthPlanQty().multiply(constructionInfo.getBeltCraft1() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getBeltCraft1())
                .divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(gdyyBreadth), 3, BigDecimal.ROUND_UP);
        BigDecimal cd15TwoNum = analysis.getCd15TwoMonthPlanQty().multiply(constructionInfo.getBeltCraft2() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getBeltCraft2())
                .divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(gdyyBreadth), 3, BigDecimal.ROUND_UP);
        BigDecimal cd15Num = cd15OneNum.add(cd15TwoNum);
//        analysis.setGdcdMonthPlanQty(analysis.getCd15OneMonthPlanQty());
        // 5.90度裁断
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode1())) {
            // 1#
            analysis.setCd90OneCode(constructionInfo.getTireFabricCode1());
            BigDecimal cd90OneNum = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(planNum).divide(divide, 3, BigDecimal.ROUND_UP);
            analysis.setCd90OneMonthPlanQty(cd90OneNum);
        }
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode2())) {
            // 2#
            analysis.setCd90TwoCode(constructionInfo.getTireFabricCode2());
            BigDecimal cd90TwoNum = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(planNum).divide(divide, 3, BigDecimal.ROUND_UP);
            analysis.setCd90TwoMonthPlanQty(cd90TwoNum);
        }
        if (StringUtils.isNotBlank(constructionInfo.getTireFabricCode3())) {
            // 3#
            analysis.setCd90ThreeCode(constructionInfo.getTireFabricCode3());
            BigDecimal cd90ThreeNum = constructionInfo.getSidewallLength() == null ? BigDecimal.ZERO : BigDecimal.valueOf(constructionInfo.getSidewallLength()).multiply(planNum).divide(divide, 3, BigDecimal.ROUND_UP);
            analysis.setCd90ThreeMonthPlanQty(cd90ThreeNum);
        }
        BigDecimal cd90Num = analysis.getCd90OneMonthPlanQty().multiply(constructionInfo.getTireFabricCraft1() == null ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(constructionInfo.getTireFabricCraft1())).divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(xwyyBreadth), 3, BigDecimal.ROUND_UP)
                .add((analysis.getCd90TwoMonthPlanQty()).multiply(constructionInfo.getTireFabricCraft2() == null ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(constructionInfo.getTireFabricCraft2())).divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(xwyyBreadth), 3, BigDecimal.ROUND_UP))
                .add((analysis.getCd90ThreeMonthPlanQty().multiply(constructionInfo.getTireFabricCraft3() == null ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(constructionInfo.getTireFabricCraft3())).divide(divide, 3, BigDecimal.ROUND_UP)).divide(BigDecimal.valueOf(xwyyBreadth), 3, BigDecimal.ROUND_UP)));
        analysis.setLbcdMonthPlanQty(analysis.getCd90OneMonthPlanQty().add(analysis.getCd90TwoMonthPlanQty()).add(analysis.getCd90ThreeMonthPlanQty()));
        // 6.胎圈
        if (StringUtils.isNotBlank(constructionInfo.getTireRingCode())) {
            analysis.setTqCode(constructionInfo.getTireRingCode());
            // 胎圈钢丝圈算两个
            analysis.setTqMonthPlanQty(planNum.multiply(BigDecimal.valueOf(2L)));
        } else {
            analysis.setTqMonthPlanQty(BigDecimal.ZERO);
        }

        // 7.钢丝圈
        if (StringUtils.isNotBlank(constructionInfo.getBeadCode())) {
            analysis.setGsqCode(constructionInfo.getBeadCode());
            analysis.setGsqMonthPlanQty(planNum.multiply(BigDecimal.valueOf(2L)));
        } else {
            analysis.setGsqMonthPlanQty(BigDecimal.ZERO);
        }
        // 8.钢带压延
        if (StringUtils.isNotBlank(constructionInfo.getArticleCrownSpec())) {
            analysis.setGdyyCode(constructionInfo.getArticleCrownSpec());
            // 8.1长度
            analysis.setGdyyMonthPlanQty(cd15Num);
            // 8.2个数
            // 如果大卷信息维护表中没有数据，从默认表中取
            BigDecimal gdyyNum = BigDecimal.ZERO;
            if (cd15BigRoll != null && cd15BigRoll.getActClothLength() != null && !cd15BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                gdyyNum = cd15Num.divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                analysis.setGdyyMonthPlanNumQty(gdyyNum.intValue());
            } else if (cd15Params != null && cd15Params.getParamValue() != null && !Double.valueOf(cd15Params.getParamValue()).equals(0d)) {
                gdyyNum = cd15Num.divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                analysis.setGdyyMonthPlanNumQty(gdyyNum.intValue());
            } else {
                analysis.setGdyyMonthPlanNumQty(0);
            }
        } else {
            analysis.setGdyyMonthPlanQty(BigDecimal.ZERO);
            analysis.setGdyyMonthPlanNumQty(0);
        }
        // 9.纤维压延
        if (StringUtils.isNotBlank(constructionInfo.getCordSpec())) {
            analysis.setXwyyCode(constructionInfo.getCordSpec());
            // 9.1长度
            analysis.setXwyyMonthPlanQty(cd90Num);
            // 9.2个数
            if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null) {
                BigDecimal xwyyNum = cd90Num.divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                analysis.setXwyyMonthPlanNumQty(xwyyNum.intValue());
            } else if (cd90Params != null && cd90Params.getParamValue() != null && !Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
                BigDecimal xwyyNum = cd90Num.divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                analysis.setXwyyMonthPlanNumQty(xwyyNum.intValue());
            } else {
                analysis.setXwyyMonthPlanNumQty(0);
            }
        } else {
            analysis.setXwyyMonthPlanQty(BigDecimal.ZERO);
            analysis.setXwyyMonthPlanNumQty(0);
        }

    }

    /**
     * 获取月计划剩余量
     * @param monthPlanSurplus
     * @return
     */
    private static int getMonthRemainQty(TCxMonthPlanSurplus monthPlanSurplus, List<CxCloseOutRange> rangeList) {
        Double rangeValue = 0d;
        if (!CollectionUtil.isEmpty(rangeList)) {
            // 收尾系数
            for (CxCloseOutRange range : rangeList) {
                if (range.getCloseOutRangeMinimum() != null && range.getCloseOutRangeMaximum() != null ){
                    if (range.getCloseOutRangeMinimum() <= monthPlanSurplus.getMonthPlanQty() && range.getCloseOutRangeMaximum() >= monthPlanSurplus.getMonthPlanQty()) {
                        rangeValue = range.getRangeValue();
                        break;
                    }
                }
            }
        }

        // 计划量 = 1 + 系数
        double v = 1 + rangeValue / 100;
        int monthRemainQty = BigDecimal.valueOf(monthPlanSurplus.getMonthPlanQty()).multiply(BigDecimal.valueOf(v)).intValue() + monthPlanSurplus.getPlanModifyQty() + monthPlanSurplus.getSapBadQty() - monthPlanSurplus.getLastMonthStock() - monthPlanSurplus.getMonthFinishQty();

        return monthRemainQty;
    }

    /**
     * 获取月计划剩余量
     * @param monthPlanSurplus
     * @return
     */
    private static int getMonthRemainQtyUpdateCx(TCxMonthPlanSurplus monthPlanSurplus) {
        int monthRemainQty = monthPlanSurplus.getMonthPlanQty() + monthPlanSurplus.getPlanModifyQty() + monthPlanSurplus.getSapBadQty() - monthPlanSurplus.getLastMonthStock() - monthPlanSurplus.getMonthFinishQty();

        return monthRemainQty;
    }

    /**
     * 获取胎胚月计划剩余量
     * @param embryoMonthPlanSurplus
     * @return
     */
    private static BigDecimal getEmbryoRemainQty(TCxEmbryoMonthPlanSurplus embryoMonthPlanSurplus, List<CxCloseOutRange> rangeList) {
        Double rangeValue = 0d;
        if (!CollectionUtil.isEmpty(rangeList)) {
            // 收尾系数
            for (CxCloseOutRange range : rangeList) {
                if (range.getCloseOutRangeMinimum() != null && range.getCloseOutRangeMaximum() != null) {
                    if (embryoMonthPlanSurplus.getMonthPlanQty().compareTo(BigDecimal.valueOf(range.getCloseOutRangeMinimum())) > -1 && BigDecimal.valueOf(range.getCloseOutRangeMaximum()).compareTo(embryoMonthPlanSurplus.getMonthPlanQty()) > -1) {
                        rangeValue = range.getRangeValue();
                        break;
                    }
                }
            }
        }

        // 计划量 = 1 + 系数
        double v = 1 + rangeValue / 100;
        BigDecimal embryoRemainQty = embryoMonthPlanSurplus.getMonthPlanQty().multiply(BigDecimal.valueOf(v)).add(embryoMonthPlanSurplus.getMonthPlanModifyQty()).add(embryoMonthPlanSurplus.getEmbryoBadQty()).subtract(embryoMonthPlanSurplus.getLastMonthStock()).subtract(embryoMonthPlanSurplus.getMonthFinishQty()).setScale(3, RoundingMode.UP);

        return embryoRemainQty;
    }

    /**
     * 获取胎胚月计划剩余量
     * @param embryoMonthPlanSurplus
     * @return
     */
    private static BigDecimal getEmbryoRemainQtyUpdateCx(TCxEmbryoMonthPlanSurplus embryoMonthPlanSurplus) {
        BigDecimal embryoRemainQty = embryoMonthPlanSurplus.getMonthPlanQty().add(embryoMonthPlanSurplus.getMonthPlanModifyQty()).add(embryoMonthPlanSurplus.getEmbryoBadQty()).subtract(embryoMonthPlanSurplus.getLastMonthStock()).subtract(embryoMonthPlanSurplus.getMonthFinishQty()).setScale(3, RoundingMode.UP);

        return embryoRemainQty;
    }

    /**
     * 获取调量的月计划剩余量
     * @param k
     * @param modifyNum
     * @param <K>
     */
    private static <K extends MonthPlanSurplusBaseEntity> void buildModify(K k, BigDecimal modifyNum) {
//        k.setMonthPlanModifyQty(modifyNum.setScale(3, RoundingMode.UP));
        // 旧修改计划量+本次计划与旧计划差额=最终修改量
        k.setMonthPlanQty(k.getMonthPlanQty().add(modifyNum));
        k.setMonthRemainQty(k.getMonthPlanQty().subtract(k.getMonthFinishQty()).setScale(3, RoundingMode.UP));
    }

    /**
     * 获取完成量回报的月计划剩余量
     * @param k
     * @param finishBig
     * @param <K>
     */
    private static <K extends MonthPlanSurplusBaseEntity> void buildFinish(K k, BigDecimal finishBig) {
//        k.setMonthFinishQty(k.getMonthFinishQty().add(finishBig).setScale(3, RoundingMode.UP));
        k.setMonthFinishQty(finishBig.setScale(3, RoundingMode.UP));
        k.setMonthRemainQty(k.getMonthPlanQty().add(k.getMonthPlanModifyQty()).subtract(k.getMonthFinishQty()).setScale(3, RoundingMode.UP));
        if (k.getMonthRemainQty().compareTo(BigDecimal.ZERO) < 1) {
            k.setMonthRemainQty(BigDecimal.ZERO);
        }
    }

    /**
     * 构建排产计划明细的唯一标识
     * @param prodPlan	排产明细记录
     * @return	唯一标识
     */
    private String createImportMonthPlanKey(MdmMonthProdPlan prodPlan) {
        // 标识字段：生产排程计划版本号 + 物料编号 + 成型胎胚代码 + 质量等级 + 库存地点 + 硫化机台编号 + 成型机台编号，用“##”分隔
        return StringUtils.join(new String[] { prodPlan.getMonthPlanApsVersion(), prodPlan.getMaterialCode(),
                prodPlan.getEmbryoCode(), prodPlan.getQualityGrade(), prodPlan.getStorageLocation(),
                prodPlan.getLhMachineCode(), prodPlan.getCxMachineCode() }, "##");
    }

    // ==================== 数据转移、实体类数据填充部分 ==================

    /**
     *  按（物料编号 + 胚胎代码 + 库存地点）分组汇总
     * @param prodList 主计划月度生产计划明细
     * @param embryoCodeMap 施工信息对象map
     * @return 月度分析汇总表
     */
    private List<MdmMonthPlanAnalysis> buildMdmMonthPlanAnalysesCastor(List<MdmMonthProdPlan> prodList, Map<String, EngineProductConstructionInfo> embryoCodeMap,
                                                                              Map<String, CxTCd15BigRoll> cd15BigRollMap, Map<String, CxTCd90BigRoll> cd90BigRollMap,
                                                                              CxTCd15Params cd15Params, CxTCd90Params cd90Params, StringBuilder logDetail, Double gdyyBreadth, Double xwyyBreadth) {

        List<MdmMonthPlanAnalysis> analysisList = new ArrayList<>();
        // 按（物料编号 + 胚胎代码 + 库存地点 + 施工版本）分组
        HashMap<String, List<MdmMonthProdPlan>> prodMap = CollectionUtil.toMapList(prodList, obj -> obj.getMaterialCode() + "+" + obj.getEmbryoCode() + "+" + obj.getStorageLocation() + "+" + obj.getBomDataVersion());
        for (String key : prodMap.keySet()) {
            List<MdmMonthProdPlan> prodPlanList = prodMap.get(key);
            MdmMonthPlanAnalysis analysis = new MdmMonthPlanAnalysis();
            MdmMonthProdPlan prodPlan = prodPlanList.get(0);
            analysis.setMonthPlanApsVersion(prodPlan.getMonthPlanApsVersion());
            analysis.setMaterialCode(prodPlan.getMaterialCode());
            analysis.setEmbryoCode(prodPlan.getEmbryoCode());
            analysis.setBomDataVersion(prodPlan.getBomDataVersion());
            // 对应胚胎代码施工信息
            EngineProductConstructionInfo constructionInfo = embryoCodeMap.get(GenerageMapKeyUtils.createMapKey(analysis.getEmbryoCode(), analysis.getBomDataVersion()));
            if (constructionInfo == null) {
                logDetail.append("【该胎胚代码的施工信息为空】：").append(analysis.getEmbryoCode()).append(division);
                continue;
            }
            // 物料编号 + 胚胎代码 + 库存地点 本月所需 所有数量
            Long num = 0L;
            for (MdmMonthProdPlan plan : prodPlanList) {
                Long modifyNum = (plan.getPlanModifyQty() == null ? 0L : plan.getPlanModifyQty());
                num = num + plan.getActualArrangement() + modifyNum;
//                num = num + plan.getActualArrangement();
            }
            analysis.setQualityGrade(prodPlan.getQualityGrade());
            analysis.setStorageLocation(prodPlan.getStorageLocation());
            analysis.setActualArrangement(Math.toIntExact(num));
//            analysis.setSpecialRequirements(prodPlan.getSpecialRequirements());
//            analysis.setCxMonthPlanQty(Math.toIntExact(num));

            // 计算半部件月度计划量
            // 胚胎计划量
            BigDecimal planNum = BigDecimal.valueOf(analysis.getActualArrangement());
            getEmbryoUnitConsumption(analysis, constructionInfo, planNum, cd15BigRollMap.get(constructionInfo.getArticleCrownSpec()), cd90BigRollMap.get(constructionInfo.getCordSpec()), cd15Params, cd90Params, gdyyBreadth, xwyyBreadth);
            this.setBaseSysValue(analysis);
            analysisList.add(analysis);
        }
        return analysisList;
    }

    /**
     * 按（物料编号）分组汇总
     * @param mdmMonthPlanMain APS生产排程版本主表
     * @param analysisList 月度分析汇总表
     * @return 成型工序计划量汇总表
     */
    private List<TCxMonthPlanSurplus> buildCxMonthPlanSurplusesCastor(MdmMonthPlanMain mdmMonthPlanMain, List<MdmMonthPlanAnalysis> analysisList,
                                                                      Map<String, TCxMonthPlanSurplus> toDeleteCxMap, Integer dataFrom, Integer calFlag,
                                                                      List<CxCloseOutRange> rangeList) {
        HashMap<String, List<MdmMonthPlanAnalysis>> materialMap = CollectionUtil.toMapList(analysisList, MdmMonthPlanAnalysis::getMaterialCode);
        List<TCxMonthPlanSurplus> monthPlanSurplusList = new ArrayList<>();
        // 2021.12.3 没有插单数据
        // 判断是否是旧插单数据且新版本中没有包含
//        for (String aps : toDeleteCxMap.keySet()) {
//            TCxMonthPlanSurplus oldAps = toDeleteCxMap.get(aps);
//            if (materialMap.get(aps) == null && oldAps.getDataSource().equals(DATA_SOURCE_FROM_APS)) {
//                oldAps.setMonthPlanVersion(mdmMonthPlanMain.getMonthPlanVersion());
//                oldAps.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
//                oldAps.setId(null);
//                oldAps.setCreateBy(null);
//                oldAps.setCreateTime(null);
//                oldAps.setUpdateBy(null);
//                oldAps.setUpdateTime(null);
//                oldAps.setDelFlag("0");
//                monthPlanSurplusList.add(oldAps);
//            }
//        }
        // 新增 + 旧数据（调整量完成量等）转移重算
        // 2021.12.6 调整量不转移
        for (String key : materialMap.keySet()) {
            List<MdmMonthPlanAnalysis> prodPlanList = materialMap.get(key);
            Long num = 0L;
            for (MdmMonthPlanAnalysis mdmMonthProdPlan : prodPlanList) {
                num = num + mdmMonthProdPlan.getActualArrangement();
            }
            TCxMonthPlanSurplus monthPlanSurplus = new TCxMonthPlanSurplus();
            this.setBaseSysValue(monthPlanSurplus);
            monthPlanSurplus.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
            monthPlanSurplus.setMonthPlanVersion(mdmMonthPlanMain.getMonthPlanVersion());
            monthPlanSurplus.setYear(mdmMonthPlanMain.getYear());
            monthPlanSurplus.setMonth(mdmMonthPlanMain.getMonth());
            MdmMonthPlanAnalysis prodPlan = prodPlanList.get(0);
            monthPlanSurplus.setSapCode(prodPlan.getMaterialCode());
            monthPlanSurplus.setEmbryoCode(prodPlan.getEmbryoCode());
            TCxMonthPlanSurplus oldPlan = toDeleteCxMap.get(monthPlanSurplus.getSapCode());
            // 2021.12.09 外胎里没月结和不良
            monthPlanSurplus.setLastMonthStock(0);
            monthPlanSurplus.setSapBadQty(0);
            if (oldPlan != null) {
//                monthPlanSurplus.setLastMonthStock(oldPlan.getLastMonthStock());
//                monthPlanSurplus.setSapBadQty(oldPlan.getSapBadQty());
                monthPlanSurplus.setMonthFinishQty(oldPlan.getMonthFinishQty());
                if (calFlag.equals(RE_VERSION)) {
                    monthPlanSurplus.setPlanModifyQty(oldPlan.getPlanModifyQty());
                } else {
                    monthPlanSurplus.setPlanModifyQty(0);
                }
            } else {
                monthPlanSurplus.setPlanModifyQty(0);
                monthPlanSurplus.setMonthFinishQty(0);
            }
            monthPlanSurplus.setMonthPlanQty(num.intValue());
            monthPlanSurplus.setDataSource(dataFrom);
            // 剩余量=月度计划量+计划修正量+不良数量-月结库存-月度完成量
            monthPlanSurplus.setMonthRemainQty(getMonthRemainQty(monthPlanSurplus, rangeList));
            monthPlanSurplusList.add(monthPlanSurplus);
        }
        return monthPlanSurplusList;
    }

    /**
     * 按（胚胎代码）分组汇总
     * @param mdmMonthPlanMain APS生产排程版本主表
     * @param analysisList  月度分析汇总表
     * @return 成型工序胎胚计划量汇总表
     */
    private List<TCxEmbryoMonthPlanSurplus> buildCxEmbryoMonthPlanSurplusesCastor(MdmMonthPlanMain mdmMonthPlanMain, List<MdmMonthPlanAnalysis> analysisList,
                                                                                  Map<String, TCxEmbryoMonthPlanSurplus> toDeleteEmbryoMap, Integer dataFrom, Integer calFlag,
                                                                                  List<CxCloseOutRange> rangeList) {
        HashMap<String, List<MdmMonthPlanAnalysis>> embryoMap = CollectionUtil.toMapList(analysisList, MdmMonthPlanAnalysis::getEmbryoCode);
        List<TCxEmbryoMonthPlanSurplus> embryoMonthPlanSurplusList = new ArrayList<>();
        // 2021.12.3 没有插单数据
        // 判断是否是旧插单数据且新版本中没有包含
//        for (String code : toDeleteEmbryoMap.keySet()) {
//            TCxEmbryoMonthPlanSurplus oldEmbryoMonthPlan = toDeleteEmbryoMap.get(code);
//            if (embryoMap.get(code) == null && oldEmbryoMonthPlan.getDataSource().equals(DATA_SOURCE_FROM_APS)) {
//                oldEmbryoMonthPlan.setMonthPlanVersion(mdmMonthPlanMain.getMonthPlanVersion());
//                oldEmbryoMonthPlan.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
//                oldEmbryoMonthPlan.setDelFlag("0");
//                oldEmbryoMonthPlan.setId(null);
//                oldEmbryoMonthPlan.setCreateBy(null);
//                oldEmbryoMonthPlan.setCreateTime(null);
//                oldEmbryoMonthPlan.setUpdateBy(null);
//                oldEmbryoMonthPlan.setUpdateTime(null);
//                embryoMonthPlanSurplusList.add(oldEmbryoMonthPlan);
//            }
//        }
        // 新增 + 旧数据（调整量完成量等）转移重算
        for (String embryoCode : embryoMap.keySet()) {
            List<MdmMonthPlanAnalysis> embryoList = embryoMap.get(embryoCode);
            Long num = 0L;
            for (MdmMonthPlanAnalysis embryo : embryoList) {
                num = num + embryo.getActualArrangement();
            }
            TCxEmbryoMonthPlanSurplus embryoMonthPlanSurplus = new TCxEmbryoMonthPlanSurplus();
            this.setBaseSysValue(embryoMonthPlanSurplus);
            embryoMonthPlanSurplus.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
            embryoMonthPlanSurplus.setMonthPlanVersion(mdmMonthPlanMain.getMonthPlanVersion());
            embryoMonthPlanSurplus.setYear(mdmMonthPlanMain.getYear());
            embryoMonthPlanSurplus.setMonth(mdmMonthPlanMain.getMonth());
            embryoMonthPlanSurplus.setMaterialCode(embryoCode);
            TCxEmbryoMonthPlanSurplus oldPlan = toDeleteEmbryoMap.get(embryoCode);
            // 2021.12.6 新版本的话调整量不转移，重算要转移
          /*  if (calFlag.equals(RE_VERSION)) {
                embryoMonthPlanSurplus.setMonthPlanModifyQty(oldPlan.getMonthPlanModifyQty());
            } else {
                embryoMonthPlanSurplus.setMonthPlanModifyQty(BigDecimal.ZERO);
            }*/
            if (oldPlan != null) {
                embryoMonthPlanSurplus.setLastMonthStock(oldPlan.getLastMonthStock());
                embryoMonthPlanSurplus.setEmbryoBadQty(oldPlan.getEmbryoBadQty());
                embryoMonthPlanSurplus.setMonthFinishQty(oldPlan.getMonthFinishQty());
                // 2021.12.6 新版本的话调整量不转移，重算要转移
                if (calFlag.equals(RE_VERSION)) {
                    embryoMonthPlanSurplus.setMonthPlanModifyQty(oldPlan.getMonthPlanModifyQty());
                } else {
                    embryoMonthPlanSurplus.setMonthPlanModifyQty(BigDecimal.ZERO);
                }
            } else {
                embryoMonthPlanSurplus.setLastMonthStock(BigDecimal.ZERO);
                embryoMonthPlanSurplus.setEmbryoBadQty(BigDecimal.ZERO);
                embryoMonthPlanSurplus.setMonthFinishQty(BigDecimal.ZERO);
                embryoMonthPlanSurplus.setMonthPlanModifyQty(BigDecimal.ZERO);
            }
            embryoMonthPlanSurplus.setMonthPlanQty(BigDecimal.valueOf(num));
            embryoMonthPlanSurplus.setDataSource(dataFrom);
            // 剩余量=月度计划量+计划修正量+不良数量-月结库存-月度完成量
            embryoMonthPlanSurplus.setMonthRemainQty(getEmbryoRemainQty(embryoMonthPlanSurplus, rangeList));
            embryoMonthPlanSurplusList.add(embryoMonthPlanSurplus);
        }
        return embryoMonthPlanSurplusList;
    }

    /**
     * 填充物料编号List
     * @param engineConstructionInfos 施工信息
     * @param tmCodeList
     * @param tcCodeList
     * @param ncCodeList
     * @param cd15CodeList
     * @param cd90CodeList
     * @param tqCodeList
     * @param gsqCodeList
     * @param gdyyCodeList
     * @param xwyyCodeList
     */
    private void buildCodeList(List<EngineProductConstructionInfo> engineConstructionInfos, List<String> tmCodeList, List<String> tcCodeList, List<String> ncCodeList, List<String> cd15CodeList, List<String> cd90CodeList, List<String> tqCodeList, List<String> gsqCodeList, List<String> gdyyCodeList, List<String> xwyyCodeList) {
        for (EngineProductConstructionInfo info : engineConstructionInfos) {
            if (StringUtils.isNotBlank(info.getTreadCode())) {
                tmCodeList.add(info.getTreadCode());
            }
            if (StringUtils.isNotBlank(info.getSidewallCode())) {
                tcCodeList.add(info.getSidewallCode());
            }
            if (StringUtils.isNotBlank(info.getInsideCode())) {
                ncCodeList.add(info.getInsideCode());
            }
            if (StringUtils.isNotBlank(info.getBeltCode1())) {
                // 1#
                cd15CodeList.add(info.getBeltCode1());
            }
            if (StringUtils.isNotBlank(info.getBeltCode2())) {
                // 2#
                cd15CodeList.add(info.getBeltCode2());
            }
            if (StringUtils.isNotBlank(info.getTireFabricCode1())) {
                // 1#
                cd90CodeList.add(info.getTireFabricCode1());
            }
            if (StringUtils.isNotBlank(info.getTireFabricCode2())) {
                // 2#
                cd90CodeList.add(info.getTireFabricCode2());
            }
            if (StringUtils.isNotBlank(info.getTireFabricCode3())) {
                // 3#
                cd90CodeList.add(info.getTireFabricCode3());
            }
            if (StringUtils.isNotBlank(info.getTireRingCode())) {
                tqCodeList.add(info.getTireRingCode());
            }
            if (StringUtils.isNotBlank(info.getBeadCode())) {
                gsqCodeList.add(info.getBeadCode());
            }
            if (StringUtils.isNotBlank(info.getArticleCrownSpec())) {
                gdyyCodeList.add(info.getArticleCrownSpec());
            }
            if (StringUtils.isNotBlank(info.getCordSpec())) {
                xwyyCodeList.add(info.getCordSpec());
            }
        }
    }

    /**
     * 转移旧明细数据
     * @param prodPlan
     * @param oldPlan
     */
    private static void transferProdPlanDataCastor(MdmMonthProdPlan prodPlan, MdmMonthProdPlan oldPlan) {
        // 如果有旧数据要将修正量+完成量放入新实体
        prodPlan.setPlanModifyQty(oldPlan.getPlanModifyQty());
        prodPlan.setFinishQty1(oldPlan.getFinishQty1());
        prodPlan.setFinishQty2(oldPlan.getFinishQty2());
        prodPlan.setFinishQty3(oldPlan.getFinishQty3());
        prodPlan.setFinishQty4(oldPlan.getFinishQty4());
        prodPlan.setFinishQty5(oldPlan.getFinishQty5());
        prodPlan.setFinishQty6(oldPlan.getFinishQty6());
        prodPlan.setFinishQty7(oldPlan.getFinishQty7());
        prodPlan.setFinishQty8(oldPlan.getFinishQty8());
        prodPlan.setFinishQty9(oldPlan.getFinishQty9());
        prodPlan.setFinishQty10(oldPlan.getFinishQty10());
        prodPlan.setFinishQty11(oldPlan.getFinishQty11());
        prodPlan.setFinishQty12(oldPlan.getFinishQty12());
        prodPlan.setFinishQty13(oldPlan.getFinishQty13());
        prodPlan.setFinishQty14(oldPlan.getFinishQty14());
        prodPlan.setFinishQty15(oldPlan.getFinishQty15());
        prodPlan.setFinishQty16(oldPlan.getFinishQty16());
        prodPlan.setFinishQty17(oldPlan.getFinishQty17());
        prodPlan.setFinishQty18(oldPlan.getFinishQty18());
        prodPlan.setFinishQty19(oldPlan.getFinishQty19());
        prodPlan.setFinishQty20(oldPlan.getFinishQty20());
        prodPlan.setFinishQty21(oldPlan.getFinishQty21());
        prodPlan.setFinishQty22(oldPlan.getFinishQty22());
        prodPlan.setFinishQty23(oldPlan.getFinishQty23());
        prodPlan.setFinishQty24(oldPlan.getFinishQty24());
        prodPlan.setFinishQty25(oldPlan.getFinishQty25());
        prodPlan.setFinishQty26(oldPlan.getFinishQty26());
        prodPlan.setFinishQty27(oldPlan.getFinishQty27());
        prodPlan.setFinishQty28(oldPlan.getFinishQty28());
        prodPlan.setFinishQty29(oldPlan.getFinishQty29());
        prodPlan.setFinishQty30(oldPlan.getFinishQty30());
        prodPlan.setFinishQty31(oldPlan.getFinishQty31());
    }

    /**
     * 填充投产表
     * @param mdmMonthPlanMain
     * @param planMap
     * @param key
     * @return
     */
    private TCxPlanProductStatus buildProductStatus(MdmMonthPlanMain mdmMonthPlanMain, HashMap<String, List<MdmMonthProdPlan>> planMap, String key, Map<String, TCxPlanProductStatus> oldProductStatusMap) {
        TCxPlanProductStatus status = new TCxPlanProductStatus();
        StringBuilder monthPlanIds = new StringBuilder();
        List<MdmMonthProdPlan> mdmMonthProdPlans = planMap.get(key);
        // 旧投产数据
        TCxPlanProductStatus oldStatus = oldProductStatusMap.get(key);
        Long total = 0L;
        StringBuilder specialRequirements = new StringBuilder();
        for (MdmMonthProdPlan last : mdmMonthProdPlans) {
            // 拼接计划ids
            if (StringUtils.isBlank(monthPlanIds)) {
                monthPlanIds.append(last.getId());
            } else {
                monthPlanIds.append(",").append(last.getId());
            }
            // 拼接特殊需求
            if (StringUtils.isNotBlank(last.getSpecialRequirements())) {
                if (StringUtils.isBlank(specialRequirements.toString())) {
                    specialRequirements.append(last.getSpecialRequirements());
                } else {
                    specialRequirements.append(";").append(last.getSpecialRequirements());
                }
            }
            // 计算总实际安排
            total += last.getActualArrangement();
        }
        this.setBaseSysValue(status);
        status.setSpecialRequirements(specialRequirements.toString());
        status.setMonthPlanIds(monthPlanIds.toString());
        status.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
        MdmMonthProdPlan prodPlan = mdmMonthProdPlans.get(0);
        // 获取开始时间和结束时间
        Date beginDate = prodPlan.getBeginDate();
        Date endDate = prodPlan.getEndDate();
        if (beginDate == null) {
            Integer startNum = getStartNum(prodPlan);
            beginDate = DateUtil.getDate(Integer.parseInt(mdmMonthPlanMain.getYear()), Integer.parseInt(mdmMonthPlanMain.getMonth()), startNum);
        }
        if (endDate == null) {
            Integer endNum = getEndNum(prodPlan);
            endDate = DateUtil.getDate(Integer.parseInt(mdmMonthPlanMain.getYear()), Integer.parseInt(mdmMonthPlanMain.getMonth()), endNum);
        }
        status.setSapCode(prodPlan.getMaterialCode());// 物料编号
        status.setEmbryoCode(prodPlan.getEmbryoCode());// 胎胚代码
        status.setBomDataVersion(prodPlan.getBomDataVersion());// 施工信息版本
        status.setSpecDimension(prodPlan.getSpecDimension());// 寸口
        status.setProductDetail(prodPlan.getSpecDesc());// 产品描述
        status.setBeginDate(DateUtil.getDateInt(beginDate) + "");
        status.setEndDate(DateUtil.getDateInt(endDate) + "");
        // 旧数据转移
        if (oldStatus != null && oldStatus.getId() != null) {
            status.setProductStatus(oldStatus.getProductStatus());
            status.setMarkUnProduct(oldStatus.getMarkUnProduct());
        } else {
            status.setProductStatus("0");
            status.setMarkUnProduct("0");
        }
        status.setMonthPlanTotalQty(total.intValue());
        return status;
    }

    /**
     * 获取生产排程版本
     * @param planMainVersion
     * @param year
     * @param month
     * @param toFinal
     * @return
     */
    private MdmMonthPlanMain getMdmMonthPlanMain(String planMainVersion, String year, String month, String toFinal) {
        MdmMonthPlanMain mdmMonthPlanMain = new MdmMonthPlanMain();
        String versionPre = getVersionPre(APS_MAIN_PLAN);
        mdmMonthPlanMain.setMonthPlanApsVersion(versionService.getSequence(versionPre, 2));
        mdmMonthPlanMain.setMonthPlanVersion(planMainVersion);
        mdmMonthPlanMain.setYear(year);
        mdmMonthPlanMain.setMonth(month);
        mdmMonthPlanMain.setIsFinalized(toFinal);
        this.setBaseSysValue(mdmMonthPlanMain);
        return mdmMonthPlanMain;
    }

    /**
     * 填充计划明细实体
     * @param mps2ApsFac
     * @param prodPlan
     * @param dataSource
     */
    private static void buildProdPlanCastor(TSyncMps2ApsFac mps2ApsFac, MdmMonthProdPlan prodPlan, String dataSource, Map<String, CxMachineInfo> machineInfoMap) {
//        if (mps2ApsFac.getPlanSeq() != null && toDeleteProdPlanMap.get(mps2ApsFac.getPlanSeq()) != null) {
//            // 如果有旧数据要将修正量+完成量放入新实体
//            MdmMonthProdPlan oldPlan = toDeleteProdPlanMap.get(mps2ApsFac.getPlanSeq());
//            transferProdPlanDataCastor(prodPlan, oldPlan);
//        }
        prodPlan.setMaterialCode(mps2ApsFac.getProductCode());
        prodPlan.setSpecDesc(mps2ApsFac.getProductDescription());
        prodPlan.setEmbryoCode(mps2ApsFac.getProcessCode());
        prodPlan.setQualityGrade(mps2ApsFac.getLevelCode());
        prodPlan.setSpecDimension(mps2ApsFac.getProSize().doubleValue());
        prodPlan.setStorageLocation(mps2ApsFac.getStorType());
        prodPlan.setSpecialRequirements(mps2ApsFac.getRemark());
        prodPlan.setExpectedExcessArrears(mps2ApsFac.getEstimateShortQty().longValue());
        prodPlan.setTheoryProductionPlan(mps2ApsFac.getTheoryProdReqQty().longValue());
        prodPlan.setActualArrangement(mps2ApsFac.getTotalQty().longValue());
        prodPlan.setBalance(mps2ApsFac.getDifferenceQty().longValue());
        if (StringUtils.isNotBlank(mps2ApsFac.getMoldingMachineCode())) {
            CxMachineInfo machineInfo = machineInfoMap.get(mps2ApsFac.getMoldingMachineCode());
            if (machineInfo != null) {
                prodPlan.setCxMachineCode(machineInfo.getMachineCode());
            }
        }
        prodPlan.setBeginDate(mps2ApsFac.getBeginDate());
        prodPlan.setEndDate(mps2ApsFac.getEndDate());
        prodPlan.setProductQty1(mps2ApsFac.getDay1());
        prodPlan.setProductQty2(mps2ApsFac.getDay2());
        prodPlan.setProductQty3(mps2ApsFac.getDay3());
        prodPlan.setProductQty4(mps2ApsFac.getDay4());
        prodPlan.setProductQty5(mps2ApsFac.getDay5());
        prodPlan.setProductQty6(mps2ApsFac.getDay6());
        prodPlan.setProductQty7(mps2ApsFac.getDay7());
        prodPlan.setProductQty8(mps2ApsFac.getDay8());
        prodPlan.setProductQty9(mps2ApsFac.getDay9());
        prodPlan.setProductQty10(mps2ApsFac.getDay10());
        prodPlan.setProductQty11(mps2ApsFac.getDay11());
        prodPlan.setProductQty12(mps2ApsFac.getDay12());
        prodPlan.setProductQty13(mps2ApsFac.getDay13());
        prodPlan.setProductQty14(mps2ApsFac.getDay14());
        prodPlan.setProductQty15(mps2ApsFac.getDay15());
        prodPlan.setProductQty16(mps2ApsFac.getDay16());
        prodPlan.setProductQty17(mps2ApsFac.getDay17());
        prodPlan.setProductQty18(mps2ApsFac.getDay18());
        prodPlan.setProductQty19(mps2ApsFac.getDay19());
        prodPlan.setProductQty20(mps2ApsFac.getDay20());
        prodPlan.setProductQty21(mps2ApsFac.getDay21());
        prodPlan.setProductQty22(mps2ApsFac.getDay22());
        prodPlan.setProductQty23(mps2ApsFac.getDay23());
        prodPlan.setProductQty24(mps2ApsFac.getDay24());
        prodPlan.setProductQty25(mps2ApsFac.getDay25());
        prodPlan.setProductQty26(mps2ApsFac.getDay26());
        prodPlan.setProductQty27(mps2ApsFac.getDay27());
        prodPlan.setProductQty28(mps2ApsFac.getDay28());
        prodPlan.setProductQty29(mps2ApsFac.getDay29());
        prodPlan.setProductQty30(mps2ApsFac.getDay30());
        prodPlan.setProductQty31(mps2ApsFac.getDay31());
        prodPlan.setDataSource(dataSource);
    }

    /**
     * 填充半部件List
     */
//    private static void buildHalfPartCastor(MdmMonthPlanMain mdmMonthPlanMain, List<MdmMonthPlanAnalysis> analysisList,
//                                            Map<String, TTmMonthPlanSurplus> toDeleteTmMap, Map<String, TTcMonthPlanSurplus> toDeleteTcMap,
//                                            Map<String, TNcMonthPlanSurplus> toDeleteNcMap, Map<String, TTqMonthPlanSurplus> toDeleteTqMap,
//                                            Map<String, TGsqMonthPlanSurplus> toDeleteGsqMap, Map<String, TGdcdMonthPlanSurplus> toDeleteCd15Map,
//                                            Map<String, TLbcdMonthPlanSurplus> toDeleteCd90Map, Map<String, TGdyyMonthPlanSurplus> toDeleteGdyyMap,
//                                            Map<String, TXwyyMonthPlanSurplus> toDeleteXwyyMap,
//                                            List<TTmMonthPlanSurplus> tmList, List<TTcMonthPlanSurplus> tcList,
//                                            List<TNcMonthPlanSurplus> ncList, List<TGdcdMonthPlanSurplus> cd15List,
//                                            List<TLbcdMonthPlanSurplus> cd90List, List<TTqMonthPlanSurplus> tqList,
//                                            List<TGsqMonthPlanSurplus> gsqList, List<TGdyyMonthPlanSurplus> gdyyList,
//                                            List<TXwyyMonthPlanSurplus> xwyyList, StringBuilder logDetail,
//                                            Map<String, EngineProductConstructionInfo> infoMap,
//                                            Map<String, CxTCd15BigRoll> cd15BigRollMap, Map<String, CxTCd90BigRoll> cd90BigRollMap,
//                                            CxTCd15Params cd15Params, CxTCd90Params cd90Params
//    ) {
//        Map<String, BigDecimal> tmMap = new HashMap<>();
//        Map<String, BigDecimal> tcMap = new HashMap<>();
//        Map<String, BigDecimal> ncMap = new HashMap<>();
//        Map<String, BigDecimal> tqMap = new HashMap<>();
//        Map<String, BigDecimal> gsqMap = new HashMap<>();
//        Map<String, BigDecimal> cd15Map = new HashMap<>();
//        Map<String, BigDecimal> cd90Map = new HashMap<>();
//        Map<String, BigDecimal> gdyyMap = new HashMap<>();
//        Map<String, BigDecimal> gdyyNumMap = new HashMap<>();
//        Map<String, BigDecimal> xwyyMap = new HashMap<>();
//        Map<String, BigDecimal> xwyyNumMap = new HashMap<>();
//        for (MdmMonthPlanAnalysis analysis : analysisList) {
//            // 获取半部件量
//            buildHalfPartMap(tmMap, analysis.getTmCode(), BigDecimal.valueOf(analysis.getTmMonthPlanQty()));
//            buildHalfPartMap(tcMap, analysis.getTcCode(), analysis.getTcMonthPlanQty());
//            buildHalfPartMap(ncMap, analysis.getNcCode(), analysis.getNcMonthPlanQty());
//            buildHalfPartMap(tqMap, analysis.getTqCode(), analysis.getTqMonthPlanQty());
//            buildHalfPartMap(gsqMap, analysis.getGsqCode(), analysis.getGsqMonthPlanQty());
//            buildHalfPartMap(cd15Map, analysis.getCd15OneCode(), analysis.getCd15OneMonthPlanQty());
//            buildHalfPartMap(cd15Map, analysis.getCd15TwoCode(), analysis.getCd15TwoMonthPlanQty());
//            buildHalfPartMap(cd90Map, analysis.getCd90OneCode(), analysis.getCd90OneMonthPlanQty());
//            buildHalfPartMap(cd90Map, analysis.getCd90TwoCode(), analysis.getCd90TwoMonthPlanQty());
//            buildHalfPartMap(cd90Map, analysis.getCd90ThreeCode(), analysis.getCd90ThreeMonthPlanQty());
//            buildHalfPartMap(gdyyMap, analysis.getGdyyCode(), analysis.getGdyyMonthPlanQty());
//            buildHalfPartMap(xwyyMap, analysis.getXwyyCode(), analysis.getXwyyMonthPlanQty());
//        }
//        // 获取钢带压延和纤维压延的个数 同Code另外合计
//        HashMap<String, List<MdmMonthPlanAnalysis>> gdyyListMap = CollectionUtil.toMapList(analysisList, MdmMonthPlanAnalysis::getGdyyCode);
//        for (String gdyyCode : gdyyListMap.keySet()) {
//            List<MdmMonthPlanAnalysis> mdmMonthPlanAnalyses = gdyyListMap.get(gdyyCode);
//            BigDecimal length = BigDecimal.ZERO;
//            EngineProductConstructionInfo info = infoMap.get(GenerageMapKeyUtils.createMapKey(mdmMonthPlanAnalyses.get(0).getEmbryoCode(), mdmMonthPlanAnalyses.get(0).getBomDataVersion()));
//            if (info == null) {
//                continue;
//            }
//            CxTCd15BigRoll cd15BigRoll = cd15BigRollMap.get(info.getArticleCrownSpec());
//            for (MdmMonthPlanAnalysis analysis : mdmMonthPlanAnalyses) {
//                length = length.add(analysis.getGdyyMonthPlanQty());
//            }
//            // 个数
//            // 如果大卷信息维护表中没有数据，从默认表中取
//            BigDecimal gdyyNum = BigDecimal.ZERO;
//            // 旧数据个数重算
//            TGdyyMonthPlanSurplus toDelete = toDeleteGdyyMap.get(gdyyCode);
//            if (cd15BigRoll != null && cd15BigRoll.getActClothLength() != null && !cd15BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
//                gdyyNum = length.divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
//                if (toDelete != null) {
//                    BigDecimal planQty2 = toDelete.getMonthPlanQty().divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
//                    BigDecimal modifyQty2 = toDelete.getMonthPlanModifyQty().divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
//                    BigDecimal finishQty2 = toDelete.getMonthFinishQty().divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
//                    toDelete.setMonthPlanQty2(planQty2);
//                    toDelete.setMonthPlanModifyQty2(modifyQty2);
//                    toDelete.setMonthFinishQty2(finishQty2);
//                    toDeleteGdyyMap.put(gdyyCode, toDelete);
//                }
//            } else if (!Double.valueOf(cd15Params.getParamValue()).equals(0d)) {
//                gdyyNum = length.divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
//                if (toDelete != null) {
//                    BigDecimal planQty2 = toDelete.getMonthPlanQty().divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
//                    BigDecimal modifyQty2 = toDelete.getMonthPlanModifyQty().divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
//                    BigDecimal finishQty2 = toDelete.getMonthFinishQty().divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
//                    toDelete.setMonthPlanQty2(planQty2);
//                    toDelete.setMonthPlanModifyQty2(modifyQty2);
//                    toDelete.setMonthFinishQty2(finishQty2);
//                    toDeleteGdyyMap.put(gdyyCode, toDelete);
//                }
//            }
//            buildHalfPartMap(gdyyNumMap, gdyyCode, gdyyNum);
//        }
//        HashMap<String, List<MdmMonthPlanAnalysis>> xwyyListMap = CollectionUtil.toMapList(analysisList, MdmMonthPlanAnalysis::getXwyyCode);
//        for (String xwyyCode : xwyyListMap.keySet()) {
//            List<MdmMonthPlanAnalysis> mdmMonthPlanAnalyses = xwyyListMap.get(xwyyCode);
//            BigDecimal length = BigDecimal.ZERO;
//            EngineProductConstructionInfo info = infoMap.get(GenerageMapKeyUtils.createMapKey(mdmMonthPlanAnalyses.get(0).getEmbryoCode(), mdmMonthPlanAnalyses.get(0).getBomDataVersion()));
//            if (info == null) {
//                continue;
//            }
//            CxTCd90BigRoll cd90BigRoll = cd90BigRollMap.get(info.getCordSpec());
//            for (MdmMonthPlanAnalysis analysis : mdmMonthPlanAnalyses) {
//                length = length.add(analysis.getXwyyMonthPlanQty());
//            }
//            // 个数
//            // 如果大卷信息维护表中没有数据，从默认表中取
//            BigDecimal xwyyNum = BigDecimal.ZERO;
//            // 旧数据个数重算
//            TXwyyMonthPlanSurplus toDelete = toDeleteXwyyMap.get(xwyyCode);
//            if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null && !cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
//                xwyyNum = length.divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
//                if (toDelete != null) {
//                    BigDecimal planQty2 = toDelete.getMonthPlanQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
//                    BigDecimal modifyQty2 = toDelete.getMonthPlanModifyQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
//                    BigDecimal finishQty2 = toDelete.getMonthFinishQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
//                    toDelete.setMonthPlanQty2(planQty2);
//                    toDelete.setMonthPlanModifyQty2(modifyQty2);
//                    toDelete.setMonthFinishQty2(finishQty2);
//                    toDeleteXwyyMap.put(xwyyCode, toDelete);
//                }
//            } else if (!Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
//                xwyyNum = length.divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
//                if (toDelete != null) {
//                    BigDecimal planQty2 = toDelete.getMonthPlanQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
//                    BigDecimal modifyQty2 = toDelete.getMonthPlanModifyQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
//                    BigDecimal finishQty2 = toDelete.getMonthFinishQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
//                    toDelete.setMonthPlanQty2(planQty2);
//                    toDelete.setMonthPlanModifyQty2(modifyQty2);
//                    toDelete.setMonthFinishQty2(finishQty2);
//                    toDeleteXwyyMap.put(xwyyCode, toDelete);
//                }
//            }
//            buildHalfPartMap(xwyyNumMap, xwyyCode, xwyyNum);
//        }
//        logDetail.append("【胎面半部件的量】：").append(toJSONString(tmMap)).append(division);
//        logDetail.append("【胎侧半部件的量】：").append(toJSONString(tcMap)).append(division);
//        logDetail.append("【内衬半部件的量】：").append(toJSONString(ncMap)).append(division);
//        logDetail.append("【胎圈半部件的量】：").append(toJSONString(tqMap)).append(division);
//        logDetail.append("【钢丝圈半部件的量】：").append(toJSONString(gsqMap)).append(division);
//        logDetail.append("【CD15半部件的量】：").append(toJSONString(cd15Map)).append(division);
//        logDetail.append("【CD90半部件的量】：").append(toJSONString(cd90Map)).append(division);
//        logDetail.append("【钢带压延半部件的量】：").append(toJSONString(gdyyMap)).append(division);
//        logDetail.append("【钢带压延半部件个数的量】：").append(toJSONString(gdyyNumMap)).append(division);
//        logDetail.append("【纤维压延半部件的量】：").append(toJSONString(xwyyMap)).append(division);
//        logDetail.append("【纤维压延半部件个数的量】：").append(toJSONString(xwyyNumMap)).append(division);
//
//        // 填充半部件实体
//        for (String code : tmMap.keySet()) {
//            TTmMonthPlanSurplus monthPlanSurplus = new TTmMonthPlanSurplus();
//            buildHalfPart(toDeleteTmMap, tmMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            tmList.add(monthPlanSurplus);
//        }
//        for (String code : tcMap.keySet()) {
//            TTcMonthPlanSurplus monthPlanSurplus = new TTcMonthPlanSurplus();
//            buildHalfPart(toDeleteTcMap, tcMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            tcList.add(monthPlanSurplus);
//        }
//        for (String code : ncMap.keySet()) {
//            TNcMonthPlanSurplus monthPlanSurplus = new TNcMonthPlanSurplus();
//            buildHalfPart(toDeleteNcMap, ncMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            ncList.add(monthPlanSurplus);
//        }
//        for (String code : cd15Map.keySet()) {
//            TGdcdMonthPlanSurplus monthPlanSurplus = new TGdcdMonthPlanSurplus();
//            buildHalfPart(toDeleteCd15Map, cd15Map.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            cd15List.add(monthPlanSurplus);
//        }
//        for (String code : cd90Map.keySet()) {
//            TLbcdMonthPlanSurplus monthPlanSurplus = new TLbcdMonthPlanSurplus();
//            buildHalfPart(toDeleteCd90Map, cd90Map.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            cd90List.add(monthPlanSurplus);
//        }
//        for (String code : tqMap.keySet()) {
//            TTqMonthPlanSurplus monthPlanSurplus = new TTqMonthPlanSurplus();
//            buildHalfPart(toDeleteTqMap, tqMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            tqList.add(monthPlanSurplus);
//        }
//        for (String code : gsqMap.keySet()) {
//            TGsqMonthPlanSurplus monthPlanSurplus = new TGsqMonthPlanSurplus();
//            buildHalfPart(toDeleteGsqMap, gsqMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            gsqList.add(monthPlanSurplus);
//        }
//        for (String code : gdyyMap.keySet()) {
//            TGdyyMonthPlanSurplus monthPlanSurplus = new TGdyyMonthPlanSurplus();
//            buildHalfPart(toDeleteGdyyMap, gdyyMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            // 钢带压延个数
//            TGdyyMonthPlanSurplus toDelete = toDeleteGdyyMap.get(code);
//            BigDecimal gdyyNum = gdyyNumMap.get(code);
//            monthPlanSurplus.setMonthPlanQty2(gdyyNum);
//            if (toDelete == null) {
//                monthPlanSurplus.setMonthPlanModifyQty2(BigDecimal.ZERO);
//                monthPlanSurplus.setMonthFinishQty2(BigDecimal.ZERO);
//            } else {
//                monthPlanSurplus.setMonthPlanModifyQty2(toDelete.getMonthPlanModifyQty2());
//                monthPlanSurplus.setMonthFinishQty2(toDelete.getMonthFinishQty2());
//            }
//            monthPlanSurplus.setMonthRemainQty2(monthPlanSurplus.getMonthPlanQty2().add(monthPlanSurplus.getMonthPlanModifyQty2()).subtract(monthPlanSurplus.getMonthFinishQty2()));
//            gdyyList.add(monthPlanSurplus);
//        }
//        for (String code : xwyyMap.keySet()) {
//            TXwyyMonthPlanSurplus monthPlanSurplus = new TXwyyMonthPlanSurplus();
//            buildHalfPart(toDeleteXwyyMap, xwyyMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
//            // 纤维压延个数
//            TXwyyMonthPlanSurplus toDelete = toDeleteXwyyMap.get(code);
//            BigDecimal xwyyNum = xwyyNumMap.get(code);
//            monthPlanSurplus.setMonthPlanQty2(xwyyNum);
//            if (toDelete == null) {
//                monthPlanSurplus.setMonthPlanModifyQty2(BigDecimal.ZERO);
//                monthPlanSurplus.setMonthFinishQty2(BigDecimal.ZERO);
//            } else {
//                monthPlanSurplus.setMonthPlanModifyQty2(toDelete.getMonthPlanModifyQty2());
//                monthPlanSurplus.setMonthFinishQty2(toDelete.getMonthFinishQty2());
//            }
//            monthPlanSurplus.setMonthRemainQty2(monthPlanSurplus.getMonthPlanQty2().add(monthPlanSurplus.getMonthPlanModifyQty2()).subtract(monthPlanSurplus.getMonthFinishQty2()));
//            xwyyList.add(monthPlanSurplus);
//        }
//    }

    /**
     * 填充半部件List
     */
    private static void buildHalfPartCastorNew(MdmMonthPlanMain mdmMonthPlanMain, List<MdmMonthPlanAnalysis> analysisList,
                                            Map<String, TTmMonthPlanSurplus> toDeleteTmMap, Map<String, TTcMonthPlanSurplus> toDeleteTcMap,
                                            Map<String, TNcMonthPlanSurplus> toDeleteNcMap, Map<String, TTqMonthPlanSurplus> toDeleteTqMap,
                                            Map<String, TGsqMonthPlanSurplus> toDeleteGsqMap, Map<String, TGdcdMonthPlanSurplus> toDeleteCd15Map,
                                            Map<String, TLbcdMonthPlanSurplus> toDeleteCd90Map, Map<String, TGdyyMonthPlanSurplus> toDeleteGdyyMap,
                                            Map<String, TXwyyMonthPlanSurplus> toDeleteXwyyMap,
                                            List<TTmMonthPlanSurplus> tmList, List<TTcMonthPlanSurplus> tcList,
                                            List<TNcMonthPlanSurplus> ncList, List<TGdcdMonthPlanSurplus> cd15List,
                                            List<TLbcdMonthPlanSurplus> cd90List, List<TTqMonthPlanSurplus> tqList,
                                            List<TGsqMonthPlanSurplus> gsqList, List<TGdyyMonthPlanSurplus> gdyyList,
                                            List<TXwyyMonthPlanSurplus> xwyyList, StringBuilder logDetail,
                                            Map<String, EngineProductConstructionInfo> infoMap,
                                            Map<String, CxTCd15BigRoll> cd15BigRollMap, Map<String, CxTCd90BigRoll> cd90BigRollMap,
                                            CxTCd15Params cd15Params, CxTCd90Params cd90Params
    ) {
        Map<String, BigDecimal> tmMap = new HashMap<>();
        Map<String, BigDecimal> tcMap = new HashMap<>();
        Map<String, BigDecimal> ncMap = new HashMap<>();
        Map<String, BigDecimal> tqMap = new HashMap<>();
        Map<String, BigDecimal> gsqMap = new HashMap<>();
        Map<String, BigDecimal> cd15Map = new HashMap<>();
        Map<String, BigDecimal> cd90Map = new HashMap<>();
        Map<String, BigDecimal> gdyyMap = new HashMap<>();
        Map<String, BigDecimal> gdyyNumMap = new HashMap<>();
        Map<String, BigDecimal> xwyyMap = new HashMap<>();
        Map<String, BigDecimal> xwyyNumMap = new HashMap<>();
        for (MdmMonthPlanAnalysis analysis : analysisList) {
            // 获取半部件量
            buildHalfPartMap(tmMap, analysis.getTmCode(), BigDecimal.valueOf(analysis.getTmMonthPlanQty()));
            buildHalfPartMap(tcMap, analysis.getTcCode(), analysis.getTcMonthPlanQty());
            buildHalfPartMap(ncMap, analysis.getNcCode(), analysis.getNcMonthPlanQty());
            buildHalfPartMap(tqMap, analysis.getTqCode(), analysis.getTqMonthPlanQty());
            buildHalfPartMap(gsqMap, analysis.getGsqCode(), analysis.getGsqMonthPlanQty());
            buildHalfPartMap(cd15Map, analysis.getCd15OneCode(), analysis.getCd15OneMonthPlanQty());
            buildHalfPartMap(cd15Map, analysis.getCd15TwoCode(), analysis.getCd15TwoMonthPlanQty());
            buildHalfPartMap(cd90Map, analysis.getCd90OneCode(), analysis.getCd90OneMonthPlanQty());
            buildHalfPartMap(cd90Map, analysis.getCd90TwoCode(), analysis.getCd90TwoMonthPlanQty());
            buildHalfPartMap(cd90Map, analysis.getCd90ThreeCode(), analysis.getCd90ThreeMonthPlanQty());
            buildHalfPartMap(gdyyMap, analysis.getGdyyCode(), analysis.getGdyyMonthPlanQty());
            buildHalfPartMap(xwyyMap, analysis.getXwyyCode(), analysis.getXwyyMonthPlanQty());
        }
        // 获取钢带压延和纤维压延的个数 同Code另外合计
        HashMap<String, List<MdmMonthPlanAnalysis>> gdyyListMap = CollectionUtil.toMapList(analysisList, MdmMonthPlanAnalysis::getGdyyCode);
        for (String gdyyCode : gdyyListMap.keySet()) {
            List<MdmMonthPlanAnalysis> mdmMonthPlanAnalyses = gdyyListMap.get(gdyyCode);
            BigDecimal length = BigDecimal.ZERO;
            EngineProductConstructionInfo info = infoMap.get(GenerageMapKeyUtils.createMapKey(mdmMonthPlanAnalyses.get(0).getEmbryoCode(), mdmMonthPlanAnalyses.get(0).getBomDataVersion()));
            if (info == null) {
                continue;
            }
            CxTCd15BigRoll cd15BigRoll = cd15BigRollMap.get(info.getArticleCrownSpec());
            for (MdmMonthPlanAnalysis analysis : mdmMonthPlanAnalyses) {
                length = length.add(analysis.getGdyyMonthPlanQty());
            }
            // 个数
            // 如果大卷信息维护表中没有数据，从默认表中取
            BigDecimal gdyyNum = BigDecimal.ZERO;
            // 旧数据个数重算
            TGdyyMonthPlanSurplus toDelete = toDeleteGdyyMap.get(gdyyCode);
            if (cd15BigRoll != null && cd15BigRoll.getActClothLength() != null && !cd15BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                gdyyNum = length.divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                if (toDelete != null) {
                    BigDecimal planQty2 = toDelete.getMonthPlanQty().divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                    BigDecimal modifyQty2 = toDelete.getMonthPlanModifyQty().divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                    BigDecimal finishQty2 = toDelete.getMonthFinishQty().divide(cd15BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                    toDelete.setMonthPlanQty2(planQty2);
                    toDelete.setMonthPlanModifyQty2(modifyQty2);
                    toDelete.setMonthFinishQty2(finishQty2);
                    toDeleteGdyyMap.put(gdyyCode, toDelete);
                }
            } else if (!Double.valueOf(cd15Params.getParamValue()).equals(0d)) {
                gdyyNum = length.divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                if (toDelete != null) {
                    BigDecimal planQty2 = toDelete.getMonthPlanQty().divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                    BigDecimal modifyQty2 = toDelete.getMonthPlanModifyQty().divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                    BigDecimal finishQty2 = toDelete.getMonthFinishQty().divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                    toDelete.setMonthPlanQty2(planQty2);
                    toDelete.setMonthPlanModifyQty2(modifyQty2);
                    toDelete.setMonthFinishQty2(finishQty2);
                    toDeleteGdyyMap.put(gdyyCode, toDelete);
                }
            }
            buildHalfPartMap(gdyyNumMap, gdyyCode, gdyyNum);
        }
        HashMap<String, List<MdmMonthPlanAnalysis>> xwyyListMap = CollectionUtil.toMapList(analysisList, MdmMonthPlanAnalysis::getXwyyCode);
        for (String xwyyCode : xwyyListMap.keySet()) {
            List<MdmMonthPlanAnalysis> mdmMonthPlanAnalyses = xwyyListMap.get(xwyyCode);
            BigDecimal length = BigDecimal.ZERO;
            EngineProductConstructionInfo info = infoMap.get(GenerageMapKeyUtils.createMapKey(mdmMonthPlanAnalyses.get(0).getEmbryoCode(), mdmMonthPlanAnalyses.get(0).getBomDataVersion()));
            if (info == null) {
                continue;
            }
            CxTCd90BigRoll cd90BigRoll = cd90BigRollMap.get(info.getCordSpec());
            for (MdmMonthPlanAnalysis analysis : mdmMonthPlanAnalyses) {
                length = length.add(analysis.getXwyyMonthPlanQty());
            }
            // 个数
            // 如果大卷信息维护表中没有数据，从默认表中取
            BigDecimal xwyyNum = BigDecimal.ZERO;
            // 旧数据个数重算
            TXwyyMonthPlanSurplus toDelete = toDeleteXwyyMap.get(xwyyCode);
            if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null && !cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                xwyyNum = length.divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                if (toDelete != null) {
                    BigDecimal planQty2 = toDelete.getMonthPlanQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                    BigDecimal modifyQty2 = toDelete.getMonthPlanModifyQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                    BigDecimal finishQty2 = toDelete.getMonthFinishQty().divide(cd90BigRoll.getActClothLength(), 0, BigDecimal.ROUND_UP);
                    toDelete.setMonthPlanQty2(planQty2);
                    toDelete.setMonthPlanModifyQty2(modifyQty2);
                    toDelete.setMonthFinishQty2(finishQty2);
                    toDeleteXwyyMap.put(xwyyCode, toDelete);
                }
            } else if (!Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
                xwyyNum = length.divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                if (toDelete != null) {
                    BigDecimal planQty2 = toDelete.getMonthPlanQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                    BigDecimal modifyQty2 = toDelete.getMonthPlanModifyQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                    BigDecimal finishQty2 = toDelete.getMonthFinishQty().divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 0, BigDecimal.ROUND_UP);
                    toDelete.setMonthPlanQty2(planQty2);
                    toDelete.setMonthPlanModifyQty2(modifyQty2);
                    toDelete.setMonthFinishQty2(finishQty2);
                    toDeleteXwyyMap.put(xwyyCode, toDelete);
                }
            }
            buildHalfPartMap(xwyyNumMap, xwyyCode, xwyyNum);
        }
        logDetail.append("【胎面半部件的量】：").append(toJSONString(tmMap)).append(division);
        logDetail.append("【胎侧半部件的量】：").append(toJSONString(tcMap)).append(division);
        logDetail.append("【内衬半部件的量】：").append(toJSONString(ncMap)).append(division);
        logDetail.append("【胎圈半部件的量】：").append(toJSONString(tqMap)).append(division);
        logDetail.append("【钢丝圈半部件的量】：").append(toJSONString(gsqMap)).append(division);
        logDetail.append("【CD15半部件的量】：").append(toJSONString(cd15Map)).append(division);
        logDetail.append("【CD90半部件的量】：").append(toJSONString(cd90Map)).append(division);
        logDetail.append("【钢带压延半部件的量】：").append(toJSONString(gdyyMap)).append(division);
        logDetail.append("【钢带压延半部件个数的量】：").append(toJSONString(gdyyNumMap)).append(division);
        logDetail.append("【纤维压延半部件的量】：").append(toJSONString(xwyyMap)).append(division);
        logDetail.append("【纤维压延半部件个数的量】：").append(toJSONString(xwyyNumMap)).append(division);

        // 填充半部件实体
        for (String code : tmMap.keySet()) {
            TTmMonthPlanSurplus monthPlanSurplus = new TTmMonthPlanSurplus();
            buildHalfPartNew(toDeleteTmMap, tmMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            tmList.add(monthPlanSurplus);
        }
        for (String code : tcMap.keySet()) {
            TTcMonthPlanSurplus monthPlanSurplus = new TTcMonthPlanSurplus();
            buildHalfPartNew(toDeleteTcMap, tcMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            tcList.add(monthPlanSurplus);
        }
        for (String code : ncMap.keySet()) {
            TNcMonthPlanSurplus monthPlanSurplus = new TNcMonthPlanSurplus();
            buildHalfPartNew(toDeleteNcMap, ncMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            ncList.add(monthPlanSurplus);
        }
        for (String code : cd15Map.keySet()) {
            TGdcdMonthPlanSurplus monthPlanSurplus = new TGdcdMonthPlanSurplus();
            buildHalfPartNew(toDeleteCd15Map, cd15Map.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            cd15List.add(monthPlanSurplus);
        }
        for (String code : cd90Map.keySet()) {
            TLbcdMonthPlanSurplus monthPlanSurplus = new TLbcdMonthPlanSurplus();
            buildHalfPartNew(toDeleteCd90Map, cd90Map.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            cd90List.add(monthPlanSurplus);
        }
        for (String code : tqMap.keySet()) {
            TTqMonthPlanSurplus monthPlanSurplus = new TTqMonthPlanSurplus();
            buildHalfPartNew(toDeleteTqMap, tqMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            tqList.add(monthPlanSurplus);
        }
        for (String code : gsqMap.keySet()) {
            TGsqMonthPlanSurplus monthPlanSurplus = new TGsqMonthPlanSurplus();
            buildHalfPartNew(toDeleteGsqMap, gsqMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            gsqList.add(monthPlanSurplus);
        }
        for (String code : gdyyMap.keySet()) {
            TGdyyMonthPlanSurplus monthPlanSurplus = new TGdyyMonthPlanSurplus();
            buildHalfPartNew(toDeleteGdyyMap, gdyyMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            // 钢带压延个数
            TGdyyMonthPlanSurplus toDelete = toDeleteGdyyMap.get(code);
            BigDecimal gdyyNum = gdyyNumMap.get(code);
            monthPlanSurplus.setMonthPlanQty2(gdyyNum);
            // 2021.12.6 修正量不转移
            monthPlanSurplus.setMonthPlanModifyQty2(BigDecimal.ZERO);
            if (toDelete == null) {
                monthPlanSurplus.setMonthFinishQty2(BigDecimal.ZERO);
            } else {
//                monthPlanSurplus.setMonthPlanModifyQty2(toDelete.getMonthPlanModifyQty2());
                monthPlanSurplus.setMonthFinishQty2(toDelete.getMonthFinishQty2());
            }
            monthPlanSurplus.setMonthRemainQty2(monthPlanSurplus.getMonthPlanQty2().add(monthPlanSurplus.getMonthPlanModifyQty2()).subtract(monthPlanSurplus.getMonthFinishQty2()));
            gdyyList.add(monthPlanSurplus);
        }
        for (String code : xwyyMap.keySet()) {
            TXwyyMonthPlanSurplus monthPlanSurplus = new TXwyyMonthPlanSurplus();
            buildHalfPartNew(toDeleteXwyyMap, xwyyMap.get(code), monthPlanSurplus, mdmMonthPlanMain, code);
            // 纤维压延个数
            TXwyyMonthPlanSurplus toDelete = toDeleteXwyyMap.get(code);
            BigDecimal xwyyNum = xwyyNumMap.get(code);
            monthPlanSurplus.setMonthPlanQty2(xwyyNum);
            // 2021.12.6 修正量不转移
            monthPlanSurplus.setMonthPlanModifyQty2(BigDecimal.ZERO);
            if (toDelete == null) {
                monthPlanSurplus.setMonthFinishQty2(BigDecimal.ZERO);
            } else {
//                monthPlanSurplus.setMonthPlanModifyQty2(toDelete.getMonthPlanModifyQty2());
                monthPlanSurplus.setMonthFinishQty2(toDelete.getMonthFinishQty2());
            }
            monthPlanSurplus.setMonthRemainQty2(monthPlanSurplus.getMonthPlanQty2().add(monthPlanSurplus.getMonthPlanModifyQty2()).subtract(monthPlanSurplus.getMonthFinishQty2()));
            xwyyList.add(monthPlanSurplus);
        }
    }

    /**
     * 旧数据转移保存到日志
     * @param old
     * @param log
     */
    private void buildHalfPartLogCastor(MonthPlanSurplusBaseEntity old, ProcedureSurplusLog log) {
        BeanUtils.copyProperties(old, log);
        log.setId(null);
        setBaseSysValue(log);
    }

    /**
     * 填充旧半部件数据map
     * @param kList 旧半部件数据List
     * @param <K> 实体
     * @return 旧半部件数据map
     */
    private static <K extends MonthPlanSurplusBaseEntity> Map<String, K> buildToDeleteMap(List<K> kList) {
        if (!CollectionUtil.isEmpty(kList)) {
            return CollectionUtil.toMap(kList, K::getMaterialCode);
        }
        return new HashMap<>();
    }

    /**
     * 填充旧半部件数据map
     */
    private static <K extends MonthPlanSurplusBaseEntity> void buildDayFinishMap(List<DayFinishVo> finishList, Map<String, K> toDeleteMap) {
        if (!CollectionUtil.isEmpty(finishList)) {
            Map<String, DayFinishVo> finishVoMap = CollectionUtil.toMap(finishList, DayFinishVo::getMaterialCode);
            for (String code : finishVoMap.keySet()) {
                DayFinishVo dayFinishVo = finishVoMap.get(code);
                K old = toDeleteMap.get(code);
                if (old != null) {
                    old.setMonthFinishQty(dayFinishVo.getDayFinishQty());
                } else {
                    MonthPlanSurplusBaseEntity k = new MonthPlanSurplusBaseEntity();
                    k.setMaterialCode(code);
                    k.setMonthFinishQty(dayFinishVo.getDayFinishQty());
                    toDeleteMap.put(code, (K) k);
                }
            }
        }
    }

    /**
     * 填充半部件实体
     */
    private static <K extends MonthPlanSurplusBaseEntity> void buildHalfPart(Map<String, K> toDeleteMap, BigDecimal num, K k, MdmMonthPlanMain mdmMonthPlanMain,
                                                                             String code) {
        k.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
        k.setMonthPlanVersion(mdmMonthPlanMain.getMonthPlanVersion());
        k.setYear(mdmMonthPlanMain.getYear());
        k.setMonth(mdmMonthPlanMain.getMonth());
        k.setMaterialCode(code);
        k.setMonthPlanQty(num);
        K toDeleteK = toDeleteMap.get(code);
        if (toDeleteK == null) {
            k.setMonthPlanModifyQty(BigDecimal.ZERO);
            k.setMonthFinishQty(BigDecimal.ZERO);
        } else {
            k.setMonthPlanModifyQty(toDeleteK.getMonthPlanModifyQty());
            k.setMonthFinishQty(toDeleteK.getMonthFinishQty());
        }
        k.setMonthRemainQty(getBaseMonthRemainQty(k));
    }

    /**
     * 填充半部件实体
     * 不转移修正量
     */
    private static <K extends MonthPlanSurplusBaseEntity> void buildHalfPartNew(Map<String, K> toDeleteMap, BigDecimal num, K k, MdmMonthPlanMain mdmMonthPlanMain,
                                                                             String code) {
        k.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
        k.setMonthPlanVersion(mdmMonthPlanMain.getMonthPlanVersion());
        k.setYear(mdmMonthPlanMain.getYear());
        k.setMonth(mdmMonthPlanMain.getMonth());
        k.setMaterialCode(code);
        k.setMonthPlanQty(num);
        k.setMonthPlanModifyQty(BigDecimal.ZERO);
        K toDeleteK = toDeleteMap.get(code);
        if (toDeleteK == null) {
            k.setMonthFinishQty(BigDecimal.ZERO);
        } else {
            k.setMonthFinishQty(toDeleteK.getMonthFinishQty());
        }
        k.setMonthRemainQty(getBaseMonthRemainQty(k));
    }
    /**
     * 填充半部件实体 updateCx用
     */
    private static <K extends MonthPlanSurplusBaseEntity> void buildHalfPartUpdateCx(BigDecimal num, K k, MdmMonthPlanMain mdmMonthPlanMain, String code) {
        k.setMonthPlanApsVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
        k.setMonthPlanVersion(mdmMonthPlanMain.getMonthPlanVersion());
        k.setYear(mdmMonthPlanMain.getYear());
        k.setMonth(mdmMonthPlanMain.getMonth());
        k.setMaterialCode(code);
        k.setMonthPlanQty(num);
        k.setMonthPlanModifyQty(BigDecimal.ZERO);
        k.setMonthFinishQty(BigDecimal.ZERO);
        k.setMonthRemainQty(getBaseMonthRemainQty(k));
    }

    /**
     * 修改半部件实体调整量+剩余量
     */
    private static <K extends MonthPlanSurplusBaseEntity> void buildModifyHalfPart(Map<String, K> oldMap, BigDecimal num, String code, K newK, MdmMonthPlanMain mdmMonthPlanMain, Integer handleKey) {
        if (StringUtils.isBlank(code)) {
            return;
        }
        K k = oldMap.get(code);
        if (k == null) {
            k = newK;
            k.setBaseVale(null);
            buildHalfPart(new HashMap<>(), num, k, mdmMonthPlanMain, code);
            oldMap.put(code, k);
        } else {
            if (handleKey.equals(MODIFY)) {
                k.setMonthPlanModifyQty(k.getMonthPlanModifyQty().add(num));
            } else if (handleKey.equals(ADD)) {
                k.setMonthPlanQty(k.getMonthPlanQty().add(num));
            }
            k.setMonthRemainQty(getBaseMonthRemainQty(k));
            k.setBaseVale(k.getId());
        }
    }

    /**
     * 获取半部件量Map
     */
    private static void buildHalfPartMap(Map<String, BigDecimal> numMap, String code, BigDecimal num) {
        if (StringUtils.isBlank(code)) {
            return;
        }
        BigDecimal oldNum = numMap.get(code);
        if (oldNum == null) {
            oldNum = num;
        } else {
            oldNum = oldNum.add(num);
        }
        numMap.put(code, oldNum);
    }


    // ==================== 数据操作部分 ==================
    /**
     * 插入成型、胎胚、半部件
     * @param mdmMonthPlanMain 生产排程版本
     * @param toDeleteCxMap 旧成型数据
     * @param toDeleteEmbryoMap 旧胎胚数据
     * @param toDeleteTmMap 旧胎面数据
     * @param toDeleteTcMap 旧胎侧数据
     * @param toDeleteNcMap 旧内衬数据
     * @param toDeleteTqMap 旧胎圈数据
     * @param toDeleteGsqMap 旧钢丝圈数据
     * @param toDeleteCd15Map 旧C15数据
     * @param toDeleteCd90Map 旧CD90数据
     * @param toDeleteGdyyMap 旧钢带压延数据
     * @param toDeleteXwyyMap 旧纤维压延数据
     * @param prodList 生产计划
     */
    private void insertHalfPart(MdmMonthPlanMain mdmMonthPlanMain,
                                Map<String, TCxMonthPlanSurplus> toDeleteCxMap,
                                Map<String, TCxEmbryoMonthPlanSurplus> toDeleteEmbryoMap,
                                Map<String, TTmMonthPlanSurplus> toDeleteTmMap,
                                Map<String, TTcMonthPlanSurplus> toDeleteTcMap,
                                Map<String, TNcMonthPlanSurplus> toDeleteNcMap,
                                Map<String, TTqMonthPlanSurplus> toDeleteTqMap,
                                Map<String, TGsqMonthPlanSurplus> toDeleteGsqMap,
                                Map<String, TGdcdMonthPlanSurplus> toDeleteCd15Map,
                                Map<String, TLbcdMonthPlanSurplus> toDeleteCd90Map,
                                Map<String, TGdyyMonthPlanSurplus> toDeleteGdyyMap,
                                Map<String, TXwyyMonthPlanSurplus> toDeleteXwyyMap,
                                List<MdmMonthProdPlan> prodList,
                                StringBuilder logDetail,
                                List<EmbryoVersionVo> dataList,
                                String toDeleteVersion,
                                Integer calFlag
    ) {
        // 施工信息
        Map<String, EngineProductConstructionInfo> totalInfoMap = cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
        List<EngineProductConstructionInfo> infoList = new ArrayList<>();
        for (EmbryoVersionVo vo : dataList) {
            EngineProductConstructionInfo info = totalInfoMap.get(GenerageMapKeyUtils.createMapKey(vo.getEmbryoCode(), vo.getBomDataVersion()));
            if (info != null) {
                infoList.add(info);
            }
        }
        // 施工信息Map
        logDetail.append("【施工信息】：").append(toJSONString(infoList)).append(division);
        Map<String, EngineProductConstructionInfo> infoMap = new HashMap<>();
        // 大卷信息
        List<String> beltSpecList = new ArrayList<>();
        List<String> crodSpecList = new ArrayList<>();
        for (EngineProductConstructionInfo info : infoList) {
            if (info == null) {
                continue;
            }
            infoMap.put(GenerageMapKeyUtils.createMapKey(info.getEmbryoCode(), info.getEmbryoVersion()), info);
            if (StringUtils.isNotBlank(info.getArticleCrownSpec())) {
                beltSpecList.add(info.getArticleCrownSpec());
            }
            if (StringUtils.isNotBlank(info.getCordSpec())) {
                crodSpecList.add(info.getCordSpec());
            }
        }
        List<CxTCd15BigRoll> cd15BigRollList = cd15BigRollService.getByBeltSpecList(beltSpecList);
        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(crodSpecList);
        Map<String, CxTCd15BigRoll> cd15BigRollMap = CollectionUtil.toMap(cd15BigRollList, CxTCd15BigRoll::getBigRollCode);
        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
        // 大卷默认信息
        CxTCd15Params cd15Params = cd15ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        logDetail.append("【CD15大卷信息】：").append(toJSONString(cd15BigRollMap)).append(division);
        logDetail.append("【CD90大卷信息】：").append(toJSONString(cd90BigRollMap)).append(division);
        logDetail.append("【CD15大卷默认信息】：").append(toJSONString(cd15Params)).append(division);
        logDetail.append("【CD90大卷默认信息】：").append(toJSONString(cd90Params)).append(division);

        // 幅宽
        Double gdyyBreadth = getDoubleOrDefault(this.getGdyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_GDYY_BREADTH);
        Double xwyyBreadth = getDoubleOrDefault(this.getXwyyParamsMap().get(EngineConstants.BREADTH), DEFAULT_XWYY_BREADTH);
        logDetail.append("【钢带压延幅宽】：").append(gdyyBreadth).append(division);
        logDetail.append("【纤维压延幅宽】：").append(gdyyBreadth).append(division);
        // 取上个月的 月结库存
        // 取本月的不良和完成量
        // 取上个版本的修正或者这个版本的修正
        String year = mdmMonthPlanMain.getYear();
        String month = mdmMonthPlanMain.getMonth();
        String stockMonth = year + "-" + month;

        String lastMonth = DateUtil.getLast1MonthString(stockMonth);

        // 外胎月结库存
        // 2021.12.09 外胎没有月结不良
//        List<String> sapCodeList = CollectionUtil.propertiesToList(prodList, MdmMonthProdPlan::getMaterialCode);
//        List<TLhMonthStock> wtStockList = lhMonthStockService.selectBySapCodeAndMonth(sapCodeList, lastMonth);
//        if (CollectionUtil.isEmpty(wtStockList)) {
//            for (TLhMonthStock wt : wtStockList) {
//                TCxMonthPlanSurplus oldWt = toDeleteCxMap.get(wt.getSapCode());
//                if (oldWt != null && wt.getStockNum() != null) {
//                    oldWt.setLastMonthStock(wt.getStockNum());
//                }
//            }
//        }

        // 月结库存从成型月结里拿
        Map<String, EmbryoVersionVo> dataMap = CollectionUtil.toMap(dataList, obj -> GenerageMapKeyUtils.createMapKey(obj.getEmbryoCode(), obj.getBomDataVersion()));
        Map<String, CxMonthStock> monthStockMap = this.getCxMonthStockMap(toDeleteEmbryoMap, dataList, lastMonth, dataMap);

        // 修正量去修正表找
        Map<String, TCxMonthPlanAdjust> adjustEmbryoMap = new HashMap<>();
        this.getAdjustMap(adjustEmbryoMap, toDeleteCxMap, toDeleteEmbryoMap, toDeleteVersion, calFlag, dataMap);

        // 不良去不良表找 取本月的不良
        Map<String, TSapEmbryoBadNumber> badMap = this.getBadNumberMap(toDeleteEmbryoMap, dataList, stockMonth, dataMap);

        // 完成量去日完成量表找
        // 胎胚
        List<DayFinishVo> finishVoList = dayFinishMapper.selectCxDayByFinishDate(stockMonth);
        if (!CollectionUtil.isEmpty(finishVoList)) {
            Map<String, DayFinishVo> finishVoMap = CollectionUtil.toMap(finishVoList, DayFinishVo::getMaterialCode);
            for (String embryoCode : finishVoMap.keySet()) {
                DayFinishVo cxDayFinishQty = finishVoMap.get(embryoCode);
                TCxEmbryoMonthPlanSurplus old = toDeleteEmbryoMap.get(embryoCode);
                if (old != null) {
                    old.setMonthFinishQty(cxDayFinishQty.getDayFinishQty());
                } else {
                    TCxEmbryoMonthPlanSurplus newEmbryo = new TCxEmbryoMonthPlanSurplus();
                    newEmbryo.setMaterialCode(embryoCode);
                    newEmbryo.setMonthPlanModifyQty(BigDecimal.ZERO);
                    newEmbryo.setLastMonthStock(BigDecimal.ZERO);
                    newEmbryo.setEmbryoBadQty(BigDecimal.ZERO);
                    newEmbryo.setMonthFinishQty(cxDayFinishQty.getDayFinishQty());
                    toDeleteEmbryoMap.put(embryoCode, newEmbryo);
                }
            }
        }
        // 外胎
        List<DayFinishVo> lhFinishList = dayFinishMapper.selectLhDayByFinishDate(stockMonth);
        if (!CollectionUtil.isEmpty(lhFinishList)) {
            Map<String, DayFinishVo> finishVoMap = CollectionUtil.toMap(lhFinishList, DayFinishVo::getMaterialCode);
            for (String code : finishVoMap.keySet()) {
                DayFinishVo cxDayFinishQty = finishVoMap.get(code);
                TCxMonthPlanSurplus old = toDeleteCxMap.get(code);
                if (old != null) {
                    old.setMonthFinishQty(cxDayFinishQty.getDayFinishQty().intValue());
                } else {
                    TCxMonthPlanSurplus newSap = new TCxMonthPlanSurplus();
                    newSap.setSapCode(code);
                    newSap.setPlanModifyQty(0);
                    newSap.setMonthFinishQty(cxDayFinishQty.getDayFinishQty().intValue());
                    toDeleteCxMap.put(code, newSap);
                }
            }
        }
        // 胎面
        List<DayFinishVo> tmFinishList = dayFinishMapper.selectTmByScheduleDate(stockMonth);
        buildDayFinishMap(tmFinishList, toDeleteTmMap);
        // 胎侧
        List<DayFinishVo> tcFinishList = dayFinishMapper.selectTcByScheduleDate(stockMonth);
        buildDayFinishMap(tcFinishList, toDeleteTcMap);
        // 内衬
        List<DayFinishVo> ncFinishList = dayFinishMapper.selectNcByScheduleDate(stockMonth);
        buildDayFinishMap(ncFinishList, toDeleteNcMap);
        // 胎圈
        List<DayFinishVo> tqFinishList = dayFinishMapper.selectTqByScheduleDate(stockMonth);
        buildDayFinishMap(tqFinishList, toDeleteTqMap);
        // 钢丝圈
        List<DayFinishVo> gsqFinishList = dayFinishMapper.selectGsqByScheduleDate(stockMonth);
        buildDayFinishMap(gsqFinishList, toDeleteGsqMap);
        // 纤维压延
        List<DayFinishVo> xwyyFinishList = dayFinishMapper.selectXwyyByScheduleDate(stockMonth);
        if (!CollectionUtil.isEmpty(xwyyFinishList)) {
            Map<String, DayFinishVo> finishVoMap = CollectionUtil.toMap(xwyyFinishList, DayFinishVo::getMaterialCode);
            for (String code : finishVoMap.keySet()) {
                DayFinishVo dayFinishVo = finishVoMap.get(code);
                TXwyyMonthPlanSurplus old = toDeleteXwyyMap.get(code);
                if (old != null) {
                    old.setMonthFinishQty(dayFinishVo.getDayFinishQty());
                } else {
                    TXwyyMonthPlanSurplus entity = new TXwyyMonthPlanSurplus();
                    entity.setMaterialCode(code);
                    entity.setMonthFinishQty(dayFinishVo.getDayFinishQty());
                    toDeleteXwyyMap.put(code, entity);
                }
            }
        }
        // CD15
        List<DayFinishVo> cd15FinishList = dayFinishMapper.selectCd15ByScheduleDate(stockMonth);
        buildDayFinishMap(cd15FinishList, toDeleteCd15Map);
        // CD90
        List<DayFinishVo> cd90FinishList = dayFinishMapper.selectCd90ByScheduleDate(stockMonth);
        buildDayFinishMap(cd90FinishList, toDeleteCd90Map);

        List<CxCloseOutRange> rangeList = paramsMapper.listCloseRangeParams();

        // 按（物料编号 + 胚胎代码 + 库存地点 + 施工版本）分组   月度分析汇总
        List<MdmMonthPlanAnalysis> analysisList = buildMdmMonthPlanAnalysesCastor(prodList, infoMap, cd15BigRollMap, cd90BigRollMap, cd15Params, cd90Params, logDetail, gdyyBreadth, xwyyBreadth);
        mdmMonthPlanAnalysisService.addBatch(analysisList);
        logDetail.append("【月度分析汇总表明细】：").append(toJSONString(analysisList)).append(division);

        // 按（物料编号）分组汇总   成型工序外胎计划量汇总表
        List<TCxMonthPlanSurplus> monthPlanSurplusList = buildCxMonthPlanSurplusesCastor(mdmMonthPlanMain, analysisList, toDeleteCxMap, DATA_SOURCE_FROM_MAIN, calFlag, rangeList);
        planSurplusService.addBatch(monthPlanSurplusList);
        logDetail.append("【成型工序外胎计划量汇总表】：").append(toJSONString(monthPlanSurplusList)).append(division);

        // 按（胚胎代码）分组汇总    成型工序胎胚计划量汇总表
        List<TCxEmbryoMonthPlanSurplus> embryoMonthPlanSurplusList = buildCxEmbryoMonthPlanSurplusesCastor(mdmMonthPlanMain, analysisList, toDeleteEmbryoMap, DATA_SOURCE_FROM_MAIN, calFlag, rangeList);
        embryoMonthPlanSurplusService.addBatch(embryoMonthPlanSurplusList);
        logDetail.append("【成型工序胎胚计划量汇总表】：").append(toJSONString(embryoMonthPlanSurplusList)).append(division);

        // 计算半部件计划数量 存入 各半部件表
        // 用调整后的胎胚+插单量来算半部件的量
        List<MdmMonthPlanAnalysis> toBuildHalfPartAnalysisList = new ArrayList<>();
        // 移除无用插单
//        Map<String, TCxEmbryoMonthPlanSurplus> embryoMap = CollectionUtil.toMap(embryoMonthPlanSurplusList, TCxEmbryoMonthPlanSurplus::getMaterialCode);
        // 新版本中存在这条插单胎胚 或者 数据来源是不是插单的则移除  2021.12.3 无插单数据
//        dataList.removeIf(obj -> embryoMap.get(obj.getEmbryoCode()) != null || !embryoMap.get(obj.getEmbryoCode()).getDataSource().equals(DATA_SOURCE_FROM_APS));
        // 本版本所有计划量
//        for (String code : prodMap.keySet()) {
//            List<MdmMonthProdPlan> prodPlans = prodMap.get(code);
//            EmbryoVersionVo embryoVersionVo = new EmbryoVersionVo();
//            embryoVersionVo.setEmbryoCode(prodPlans.get(0).getEmbryoCode());
//            embryoVersionVo.setBomDataVersion(prodPlans.get(0).getBomDataVersion());
//            Integer totalNum = 0;
//            for (MdmMonthProdPlan prodPlan : prodPlans) {
//                totalNum += prodPlan.getMonthTotalPlanQty();
//            }
//            embryoVersionVo.setTotalPlanQty(totalNum);
//            dataList.add(embryoVersionVo);
//        }

        HashMap<String, List<EmbryoVersionVo>> embryoDataMap = CollectionUtil.toMapList(dataList, EmbryoVersionVo::getEmbryoCode);
        // 计算半部件最终月计划量
        this.buildFinalPlanNum(infoMap, cd15BigRollMap, cd90BigRollMap, cd15Params, cd90Params, toBuildHalfPartAnalysisList, gdyyBreadth, xwyyBreadth,
                embryoDataMap, monthStockMap, adjustEmbryoMap, badMap);

        logDetail.append("【半部件计划数量】：").append(toJSONString(toBuildHalfPartAnalysisList)).append(division);

        List<TTmMonthPlanSurplus> tmList = new ArrayList<>();
        List<TTcMonthPlanSurplus> tcList = new ArrayList<>();
        List<TNcMonthPlanSurplus> ncList = new ArrayList<>();
        List<TGdcdMonthPlanSurplus> cd15List = new ArrayList<>();
        List<TLbcdMonthPlanSurplus> cd90List = new ArrayList<>();
        List<TTqMonthPlanSurplus> tqList = new ArrayList<>();
        List<TGsqMonthPlanSurplus> gsqList = new ArrayList<>();
        List<TGdyyMonthPlanSurplus> gdyyList = new ArrayList<>();
        List<TXwyyMonthPlanSurplus> xwyyList = new ArrayList<>();
        // 填充半部件List 旧数据调整量、完成量存入
        buildHalfPartCastorNew(mdmMonthPlanMain, toBuildHalfPartAnalysisList, toDeleteTmMap, toDeleteTcMap, toDeleteNcMap, toDeleteTqMap, toDeleteGsqMap, toDeleteCd15Map, toDeleteCd90Map,
                toDeleteGdyyMap, toDeleteXwyyMap, tmList, tcList, ncList, cd15List, cd90List, tqList, gsqList, gdyyList, xwyyList, logDetail, infoMap, cd15BigRollMap, cd90BigRollMap, cd15Params, cd90Params);
        // 插入
        tmMonthPlanSurplusService.addBatch(tmList);
        tcMonthPlanSurplusService.addBatch(tcList);
        ncMonthPlanSurplusService.addBatch(ncList);
        tqMonthPlanSurplusService.addBatch(tqList);
        gsqMonthPlanSurplusService.addBatch(gsqList);
        gdyyMonthPlanSurplusService.addBatch(gdyyList);
        xwyyMonthPlanSurplusService.addBatch(xwyyList);
        lbcdMonthPlanSurplusService.addBatch(cd90List);
        gdcdMonthPlanSurplusService.addBatch(cd15List);
        logDetail.append("【胎面工序计划量汇总表】：").append(toJSONString(tmList)).append(division);
        logDetail.append("【胎侧工序计划量汇总表】：").append(toJSONString(tcList)).append(division);
        logDetail.append("【内衬工序计划量汇总表】：").append(toJSONString(ncList)).append(division);
        logDetail.append("【CD15工序计划量汇总表】：").append(toJSONString(cd15List)).append(division);
        logDetail.append("【CD90工序计划量汇总表】：").append(toJSONString(cd90List)).append(division);
        logDetail.append("【胎圈工序计划量汇总表】：").append(toJSONString(tqList)).append(division);
        logDetail.append("【钢丝圈工序计划量汇总表】：").append(toJSONString(gsqList)).append(division);
        logDetail.append("【钢带压延工序计划量汇总表】：").append(toJSONString(gdyyList)).append(division);
        logDetail.append("【纤维压延工序计划量汇总表】：").append(toJSONString(xwyyList)).append(division);
        logDetail.append("【汇总完成】");
    }

    /**
     * 获取不良Map
     * @param toDeleteEmbryoMap
     * @param dataList
     * @param stockMonth
     * @return
     */
    private Map<String, TSapEmbryoBadNumber> getBadNumberMap(Map<String, TCxEmbryoMonthPlanSurplus> toDeleteEmbryoMap, List<EmbryoVersionVo> dataList, String stockMonth, Map<String, EmbryoVersionVo> dataMap) {
        List<TSapEmbryoBadNumber> badList = badNumberService.getByEmbryoVersionList(stockMonth, dataList);
        Map<String, TSapEmbryoBadNumber> badMap = new HashMap<>();
        if (!CollectionUtil.isEmpty(badList)) {
            HashMap<String, List<TSapEmbryoBadNumber>> badNumMap = CollectionUtil.toMapList(badList, TSapEmbryoBadNumber::getEmbryoCode);
            for (String embryoCode : badNumMap.keySet()) {
                // 取同胎胚旧不良
                List<TSapEmbryoBadNumber> badNumberList = badNumMap.get(embryoCode);
                // 取同胎胚旧汇总数据
                TCxEmbryoMonthPlanSurplus oldEmbryo = toDeleteEmbryoMap.get(embryoCode);
                BigDecimal totalNum = BigDecimal.ZERO;
                for (TSapEmbryoBadNumber bad : badNumberList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(bad.getEmbryoCode(), bad.getBomDataVersion());
                    badMap.putIfAbsent(mapKey, bad);
                    if (bad.getBadNum() != null && dataMap.get(mapKey) != null) {
                        totalNum = totalNum.add(BigDecimal.valueOf(bad.getBadNum()));
                    }
                }
                // set不良
                if (oldEmbryo != null) {
                    oldEmbryo.setEmbryoBadQty(totalNum);
                } else {
                    TCxEmbryoMonthPlanSurplus newEmbryo = new TCxEmbryoMonthPlanSurplus();
                    newEmbryo.setMaterialCode(embryoCode);
                    newEmbryo.setEmbryoBadQty(totalNum);
                    newEmbryo.setMonthPlanModifyQty(BigDecimal.ZERO);
                    newEmbryo.setLastMonthStock(BigDecimal.ZERO);
                    newEmbryo.setMonthFinishQty(BigDecimal.ZERO);
                    toDeleteEmbryoMap.put(embryoCode, newEmbryo);
                }
            }
        }
        return badMap;
    }

    /**
     * 获取修正map
     * @param toDeleteEmbryoMap
     * @param toDeleteVersion
     * @param calFlag
     * @return
     */
    private void getAdjustMap( Map<String, TCxMonthPlanAdjust> adjustEmbryoMap, Map<String, TCxMonthPlanSurplus> toDeleteCxMap,
                                                         Map<String, TCxEmbryoMonthPlanSurplus> toDeleteEmbryoMap, String toDeleteVersion, Integer calFlag,
                               Map<String, EmbryoVersionVo> dataMap) {
        // 2021.12.6 新版本下发，旧修正量直接丢弃
        // 如果是版本重算则去调整表找
        if (calFlag.equals(RE_VERSION)) {
            List<TCxMonthPlanAdjust> adjustList = adjustService.selectAllByApsVersionList(toDeleteVersion);
            if (!CollectionUtil.isEmpty(adjustList)) {
                Map<String, List<TCxMonthPlanAdjust>> adjustCxNumMap = new HashMap<>();
                Map<String, List<TCxMonthPlanAdjust>> adjustEmbryoNumMap = new HashMap<>();
                for (TCxMonthPlanAdjust adjust : adjustList) {
                    List<TCxMonthPlanAdjust> exists = adjustCxNumMap.computeIfAbsent(adjust.getSapCode(), k -> new ArrayList<>());
                    exists.add(adjust);
                    List<TCxMonthPlanAdjust> exists2 = adjustEmbryoNumMap.computeIfAbsent(adjust.getEmbryoCode(), k -> new ArrayList<>());
                    exists2.add(adjust);
                }

                // 外胎
                for (String sapCode : adjustCxNumMap.keySet()) {
                    List<TCxMonthPlanAdjust> adjustList1 = adjustCxNumMap.get(sapCode);
                    BigDecimal totalNum = BigDecimal.ZERO;
                    for (TCxMonthPlanAdjust adjust : adjustList1) {
                        if (adjust.getPlanModifyQty() != null) {
                            totalNum = totalNum.add(BigDecimal.valueOf(adjust.getPlanModifyQty()));
                        }
                    }
                    TCxMonthPlanSurplus wt = toDeleteCxMap.get(sapCode);
                    if (wt != null) {
                        wt.setPlanModifyQty(totalNum.intValue());
                    } else {
                        TCxMonthPlanSurplus newSap = new TCxMonthPlanSurplus();
                        newSap.setSapCode(sapCode);
                        newSap.setPlanModifyQty(totalNum.intValue());
                        newSap.setMonthFinishQty(0);
                        toDeleteCxMap.put(sapCode, newSap);
                    }
                }

                // 胎胚
                for (String embryoCode : adjustEmbryoNumMap.keySet()) {
                    // 拿同胎胚修正
                    List<TCxMonthPlanAdjust> adjustList1 = adjustEmbryoNumMap.get(embryoCode);
                    // 同施工版本分组
                    HashMap<String, List<TCxMonthPlanAdjust>> groupMap = CollectionUtil.toMapList(adjustList1, obj -> GenerageMapKeyUtils.createMapKey(obj.getEmbryoCode(), obj.getBomDataVersion()));
                    // 同胎胚总量
                    BigDecimal totalNum = BigDecimal.ZERO;
                    for (String key : groupMap.keySet()) {
                        TCxMonthPlanAdjust adjust1 = new TCxMonthPlanAdjust();
                        List<TCxMonthPlanAdjust> adjustList2 = groupMap.get(key);
                        TCxMonthPlanAdjust adjust = adjustList2.get(0);
                        adjust1.setEmbryoCode(adjust.getEmbryoCode());
                        adjust1.setBomDataVersion(adjust.getBomDataVersion());
                        // 同胎胚+施工总量
                        BigDecimal thisTotalNum = BigDecimal.ZERO;
                        for (TCxMonthPlanAdjust adjust2 : adjustList2) {
                            if (adjust2.getPlanModifyQty() != null) {
                                thisTotalNum = thisTotalNum.add(BigDecimal.valueOf(adjust2.getPlanModifyQty()));
                            }
                        }
                        adjust1.setPlanModifyQty(thisTotalNum.intValue());
                        adjustEmbryoMap.putIfAbsent(GenerageMapKeyUtils.createMapKey(adjust1.getEmbryoCode(), adjust1.getBomDataVersion()), adjust1);
                        if (dataMap.get(GenerageMapKeyUtils.createMapKey(adjust1.getEmbryoCode(), adjust1.getBomDataVersion())) != null) {
                            totalNum = totalNum.add(thisTotalNum);
                        }
                    }

                    // 拿同胎胚旧数据
                    TCxEmbryoMonthPlanSurplus oldEmbryo = toDeleteEmbryoMap.get(embryoCode);
                    // set同胎胚修正值
                    if (oldEmbryo != null) {
                        oldEmbryo.setMonthPlanModifyQty(totalNum);
                    } else {
                        TCxEmbryoMonthPlanSurplus newEmbryo = new TCxEmbryoMonthPlanSurplus();
                        newEmbryo.setMaterialCode(embryoCode);
                        newEmbryo.setMonthPlanModifyQty(totalNum);
                        newEmbryo.setLastMonthStock(BigDecimal.ZERO);
                        newEmbryo.setMonthFinishQty(BigDecimal.ZERO);
                        newEmbryo.setEmbryoBadQty(BigDecimal.ZERO);
                        toDeleteEmbryoMap.put(embryoCode, newEmbryo);
                    }
                }
            }
        }
    }

    /**
     * 获取月结库存map
     * @param toDeleteEmbryoMap
     * @param dataList
     * @param lastMonth
     * @return
     */
    private Map<String, CxMonthStock> getCxMonthStockMap(Map<String, TCxEmbryoMonthPlanSurplus> toDeleteEmbryoMap, List<EmbryoVersionVo> dataList, String lastMonth, Map<String, EmbryoVersionVo> dataMap) {
        List<CxMonthStock> monthStockList = cxMonthStockCommonMapper.selectByEmbryoVersionList(dataList, lastMonth);
        Map<String, CxMonthStock> monthStockMap = new HashMap<>();
        if (!CollectionUtil.isEmpty(monthStockList)) {
            HashMap<String, List<CxMonthStock>> embryoMap = CollectionUtil.toMapList(monthStockList, CxMonthStock::getEmbryoCode);
            for (String embryoCode : embryoMap.keySet()) {
                // 拿同胎胚月结
                List<CxMonthStock> cxMonthStocks = embryoMap.get(embryoCode);
                BigDecimal totalNum = BigDecimal.ZERO;
                for (CxMonthStock cxMonthStock : cxMonthStocks) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(cxMonthStock.getEmbryoCode(), cxMonthStock.getBomDataVersion());
                    monthStockMap.putIfAbsent(mapKey, cxMonthStock);
                    if (StringUtils.isNotBlank(cxMonthStock.getStockNum()) && dataMap.get(mapKey) != null) {
                        totalNum = totalNum.add(BigDecimal.valueOf(Double.parseDouble(cxMonthStock.getStockNum())));
                    }
                }

                // set汇总数据
                // 取同胎胚旧汇总数据
                TCxEmbryoMonthPlanSurplus old = toDeleteEmbryoMap.get(embryoCode);
                if (old != null) {
                    old.setLastMonthStock(totalNum);
                } else {
                    TCxEmbryoMonthPlanSurplus newEmbryo = new TCxEmbryoMonthPlanSurplus();
                    newEmbryo.setMaterialCode(embryoCode);
                    newEmbryo.setLastMonthStock(totalNum);
                    newEmbryo.setEmbryoBadQty(BigDecimal.ZERO);
                    newEmbryo.setMonthFinishQty(BigDecimal.ZERO);
                    newEmbryo.setMonthPlanModifyQty(BigDecimal.ZERO);
                    toDeleteEmbryoMap.put(embryoCode, newEmbryo);
                }

            }
        }
        return monthStockMap;
    }

//    /**
//     * 删除旧计划半部件数据
//     * @param toDeleteVersion 需要删除的版本
//     */
//    private void toDeleteNinePlan(String toDeleteVersion, List<String> tmCodeList, List<String> tcCodeList, List<String> ncCodeList,
//                                  List<String> cd15CodeList, List<String> cd90CodeList, List<String> tqCodeList, List<String> gsqCodeList,
//                                  List<String> gdyyCodeList, List<String> xwyyCodeList) {
//        List<TTmMonthPlanSurplus> toDeleteTmList = tmMonthPlanSurplusService.getByCodeList(toDeleteVersion, tmCodeList);
//        List<TTcMonthPlanSurplus> toDeleteTcList = tcMonthPlanSurplusService.getByCodeList(toDeleteVersion, tcCodeList);
//        List<TNcMonthPlanSurplus> toDeleteNcList = ncMonthPlanSurplusService.getByCodeList(toDeleteVersion, ncCodeList);
//        List<TTqMonthPlanSurplus> toDeleteTqList = tqMonthPlanSurplusService.getByCodeList(toDeleteVersion, tqCodeList);
//        List<TGsqMonthPlanSurplus> toDeleteGsqList = gsqMonthPlanSurplusService.getByCodeList(toDeleteVersion, gsqCodeList);
//        List<TGdcdMonthPlanSurplus> toDeleteCd15List = gdcdMonthPlanSurplusService.getByCodeList(toDeleteVersion, cd15CodeList);
//        List<TLbcdMonthPlanSurplus> toDeleteCd90List = lbcdMonthPlanSurplusService.getByCodeList(toDeleteVersion, cd90CodeList);
//        List<TGdyyMonthPlanSurplus> toDeleteGdyyList = gdyyMonthPlanSurplusService.getByCodeList(toDeleteVersion, gdyyCodeList);
//        List<TXwyyMonthPlanSurplus> toDeleteXwyyList = xwyyMonthPlanSurplusService.getByCodeList(toDeleteVersion, xwyyCodeList);
//        List<ProcedureSurplusLog> halfList = new ArrayList<>();
//
//        for (TTmMonthPlanSurplus old : toDeleteTmList) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TM);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        for (TTcMonthPlanSurplus old : toDeleteTcList) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TC);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        for (TNcMonthPlanSurplus old : toDeleteNcList) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_NC);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        for (TTqMonthPlanSurplus old : toDeleteTqList) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TQ);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        for (TGsqMonthPlanSurplus old : toDeleteGsqList) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_GSQ);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        for (TGdcdMonthPlanSurplus old : toDeleteCd15List) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD15);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        for (TLbcdMonthPlanSurplus old : toDeleteCd90List) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD90);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        for (TGdyyMonthPlanSurplus old : toDeleteGdyyList) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_GDYY);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        for (TXwyyMonthPlanSurplus old : toDeleteXwyyList) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_XWYY);
//            buildHalfPartLogCastor(old, log);
//            halfList.add(log);
//        }
//        // 日志转移
//        halfPartLogService.addCxHalfPartLog(halfList);
//        // 删除
//        tmMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, tmCodeList);
//        tcMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, tcCodeList);
//        ncMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, ncCodeList);
//        tqMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, tqCodeList);
//        gsqMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, gsqCodeList);
//        gdcdMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, cd15CodeList);
//        lbcdMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, cd90CodeList);
//        gdyyMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, gdyyCodeList);
//        xwyyMonthPlanSurplusService.deleteByApsVersionAndCodeList(toDeleteVersion, xwyyCodeList);
//    }

    /**
     * 插单重算删除旧计划半部件数据（除了成型和胎胚）
     * @param toDeleteVersion 需要删除的版本
     */
    private void toDeleteNinePlanExceptCxTp(String toDeleteVersion) {
//        TCxMonthPlanSurplus cxQuery = new TCxMonthPlanSurplus();
//        cxQuery.setMonthPlanApsVersion(toDeleteVersion);
//        List<TCxMonthPlanSurplus> toDeleteCxList = planSurplusService.getByParams(cxQuery);
//        TCxEmbryoMonthPlanSurplus embryoQuery = new TCxEmbryoMonthPlanSurplus();
//        embryoQuery.setMonthPlanApsVersion(toDeleteVersion);
//        List<TCxEmbryoMonthPlanSurplus> toDeleteEmbryoList = embryoMonthPlanSurplusService.getByParams(embryoQuery);
        List<TTmMonthPlanSurplus> toDeleteTmList = tmMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<TTcMonthPlanSurplus> toDeleteTcList = tcMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<TNcMonthPlanSurplus> toDeleteNcList = ncMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<TTqMonthPlanSurplus> toDeleteTqList = tqMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<TGsqMonthPlanSurplus> toDeleteGsqList = gsqMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<TGdcdMonthPlanSurplus> toDeleteCd15List = gdcdMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<TLbcdMonthPlanSurplus> toDeleteCd90List = lbcdMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<TGdyyMonthPlanSurplus> toDeleteGdyyList = gdyyMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<TXwyyMonthPlanSurplus> toDeleteXwyyList = xwyyMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<CxMonthPlanSurplusLog> wtList = new ArrayList<>();
        List<ProcedureSurplusLog> halfList = new ArrayList<>();
//        for (TCxMonthPlanSurplus old : toDeleteCxList) {
//            CxMonthPlanSurplusLog log = new CxMonthPlanSurplusLog();
//            BeanUtils.copyProperties(old, log);
//            log.setId(null);
//            log.setBaseVale(null);
//            wtList.add(log);
//        }
//        for (TCxEmbryoMonthPlanSurplus old : toDeleteEmbryoList) {
//            ProcedureSurplusLog log = new ProcedureSurplusLog();
//            BeanUtils.copyProperties(old, log);
//            log.setId(null);
//            log.setBaseVale(null);
//            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_CX);
//            halfList.add(log);
//        }
        for (TTmMonthPlanSurplus old : toDeleteTmList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TM);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TTcMonthPlanSurplus old : toDeleteTcList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TC);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TNcMonthPlanSurplus old : toDeleteNcList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_NC);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TTqMonthPlanSurplus old : toDeleteTqList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TQ);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TGsqMonthPlanSurplus old : toDeleteGsqList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_GSQ);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TGdcdMonthPlanSurplus old : toDeleteCd15List) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD15);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TLbcdMonthPlanSurplus old : toDeleteCd90List) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD90);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TGdyyMonthPlanSurplus old : toDeleteGdyyList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_GDYY);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TXwyyMonthPlanSurplus old : toDeleteXwyyList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_XWYY);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        // 日志转移
//        halfPartLogService.addLhLog(wtList);
        halfPartLogService.addCxHalfPartLog(halfList);

        // 删除
//        planSurplusService.deleteByApsVersion(toDeleteVersion);
//        embryoMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        tmMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        tcMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        ncMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        tqMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        gsqMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        gdcdMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        lbcdMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        gdyyMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        xwyyMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
    }

    /**
     * 删除旧计划半部件数据
     */
    @Transactional
    void toDeleteNinePlan(String toDeleteVersion, List<TCxMonthPlanSurplus> toDeleteCxList, List<TCxEmbryoMonthPlanSurplus> toDeleteEmbryoList,
                          List<TTmMonthPlanSurplus> toDeleteTmList, List<TTcMonthPlanSurplus> toDeleteTcList, List<TNcMonthPlanSurplus> toDeleteNcList,
                          List<TTqMonthPlanSurplus> toDeleteTqList, List<TGsqMonthPlanSurplus> toDeleteGsqList, List<TGdcdMonthPlanSurplus> toDeleteCd15List,
                          List<TLbcdMonthPlanSurplus> toDeleteCd90List, List<TGdyyMonthPlanSurplus> toDeleteGdyyList, List<TXwyyMonthPlanSurplus> toDeleteXwyyList,
                          StringBuilder logDetail) {
//        TCxMonthPlanSurplus cxQuery = new TCxMonthPlanSurplus();
//        cxQuery.setMonthPlanApsVersion(toDeleteVersion);
//        List<TCxMonthPlanSurplus> toDeleteCxList = planSurplusService.getByParams(cxQuery);
//        TCxEmbryoMonthPlanSurplus embryoQuery = new TCxEmbryoMonthPlanSurplus();
//        embryoQuery.setMonthPlanApsVersion(toDeleteVersion);
//        List<TCxEmbryoMonthPlanSurplus> toDeleteEmbryoList = embryoMonthPlanSurplusService.getByParams(embryoQuery);
//        List<TTmMonthPlanSurplus> toDeleteTmList = tmMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<TTcMonthPlanSurplus> toDeleteTcList = tcMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<TNcMonthPlanSurplus> toDeleteNcList = ncMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<TTqMonthPlanSurplus> toDeleteTqList = tqMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<TGsqMonthPlanSurplus> toDeleteGsqList = gsqMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<TGdcdMonthPlanSurplus> toDeleteCd15List = gdcdMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<TLbcdMonthPlanSurplus> toDeleteCd90List = lbcdMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<TGdyyMonthPlanSurplus> toDeleteGdyyList = gdyyMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
//        List<TXwyyMonthPlanSurplus> toDeleteXwyyList = xwyyMonthPlanSurplusService.getByApsVersion(toDeleteVersion);
        List<CxMonthPlanSurplusLog> wtList = new ArrayList<>();
        List<ProcedureSurplusLog> halfList = new ArrayList<>();
        for (TCxMonthPlanSurplus old : toDeleteCxList) {
            CxMonthPlanSurplusLog log = new CxMonthPlanSurplusLog();
            BeanUtils.copyProperties(old, log);
            log.setId(null);
            setBaseSysValue(log);
            wtList.add(log);
        }
        for (TCxEmbryoMonthPlanSurplus old : toDeleteEmbryoList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            BeanUtils.copyProperties(old, log);
            log.setId(null);
            setBaseSysValue(log);
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_CX);
            halfList.add(log);
        }
        for (TTmMonthPlanSurplus old : toDeleteTmList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TM);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TTcMonthPlanSurplus old : toDeleteTcList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TC);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TNcMonthPlanSurplus old : toDeleteNcList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_NC);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TTqMonthPlanSurplus old : toDeleteTqList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_TQ);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TGsqMonthPlanSurplus old : toDeleteGsqList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_GSQ);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TGdcdMonthPlanSurplus old : toDeleteCd15List) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD15);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TLbcdMonthPlanSurplus old : toDeleteCd90List) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD90);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TGdyyMonthPlanSurplus old : toDeleteGdyyList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_GDYY);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        for (TXwyyMonthPlanSurplus old : toDeleteXwyyList) {
            ProcedureSurplusLog log = new ProcedureSurplusLog();
            log.setProcedureCode(ApsConstant.PROCEDURE_CODE_XWYY);
            buildHalfPartLogCastor(old, log);
            halfList.add(log);
        }
        // 日志转移
        halfPartLogService.addLhLog(wtList);
        //Joran 2021-12-15 添加对汇总日志历史版本数据进行逻辑删除，只保留最新一份数据为非逻辑删除
        halfPartLogService.removeHistoryVersion(toDeleteVersion);
        halfPartLogService.addCxHalfPartLog(halfList);
        logDetail.append("【旧外胎、胎胚、半部件数据已转移到日志表】：").append(toDeleteVersion).append(division);

        // 删除
        planSurplusService.deleteByApsVersion(toDeleteVersion);
        embryoMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        tmMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        tcMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        ncMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        tqMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        gsqMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        gdcdMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        lbcdMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        gdyyMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        xwyyMonthPlanSurplusService.deleteByApsVersion(toDeleteVersion);
        logDetail.append("【旧外胎、胎胚、半部件数据已删除完成】：").append(toDeleteVersion).append(division);
    }

}
