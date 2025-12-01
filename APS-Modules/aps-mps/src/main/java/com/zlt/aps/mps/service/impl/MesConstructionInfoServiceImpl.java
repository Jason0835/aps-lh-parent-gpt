package com.zlt.aps.mps.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.alibaba.nacos.shaded.com.google.common.base.Objects;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.mps.common.InsideRubberSortEnum;
import com.zlt.aps.mps.common.MaterTypeEnum;
import com.zlt.aps.mps.common.SideRubberSortEnum;
import com.zlt.aps.mps.domain.LhTireConstructionInfo;
import com.zlt.aps.mps.domain.TMesConstructionInfo;
import com.zlt.aps.mps.domain.TMesConstructionMapping;
import com.zlt.aps.mps.domain.TMesConstructionParam;
import com.zlt.aps.mps.domain.TMesPlmBomInfo;
import com.zlt.aps.mps.mapper.TMesConstructionInfoMapper;
import com.zlt.aps.mps.service.MesConstructionInfoService;

import lombok.extern.slf4j.Slf4j;

/**
 * 施工表同步接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-9-13 14:07:21
 */
@Service("mesConstructionInfoService")
@Slf4j
public class MesConstructionInfoServiceImpl implements MesConstructionInfoService {
    /**
     * 批量处理数
     */
    private static final int BATCH_NUM = 100;
    /**
     * 1000，主要用于米与毫米的换算
     */
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    /**
     * 删除列
     */
    private static final String DEL_FLAG = "DEL_FLAG";
    /**
     * 合模压力
     */
    private static final String CLAMPING_PRESSURE = "CLAMPING_PRESSURE";
    /**
     * 硫化时长
     */
    private static final String CURING_TIME = "CURING_TIME";
    /**
     * 胎圈尺寸
     */
    private static final String HEXAGON_RUBBER_DIMENSION = "HEXAGON_RUBBER_DIMENSION";
    /**
     * 补强/封口胶
     */
    private static final String REINFORCE_SEAL_GLUE = "REINFORCE_SEAL_GLUE";
    /**
     * 断面宽
     */
    private static final String SECTION_WIDTH = "SECTION_WIDTH";
    /**
     * 成型法
     */
    private static final String CONS_TYPE = "CONS_TYPE";
    /**
     * 寸口信息
     */
    private static final String DIMENSION = "DIMENSION";
    /**
     * 半钢胎体
     */
    private static final String TIRE_FABRIC = "TIRE_FABRIC";
    /**
     * 胎面胶料
     */
    private static final String TREAD_RUBBER_CATEGORY = "TREAD_RUBBER_CATEGORY";
    /**
     * 胎侧胶料
     */
    private static final String SIDEWALL_RUBBER = "SIDEWALL_RUBBER";
    /**
     * 内衬胶料
     */
    private static final String INSIDE_RUBBER = "INSIDE_RUBBER";
    /**
     * 胎侧长度
     */
    private static final String SIDEWALL_LENGTH = "SIDEWALL_LENGTH";
    /**
     * 机头宽度
     */
    private static final String NOSE_WIDTH = "NOSE_WIDTH";
    /**
     * 规格描述
     */
    private static final String SPEC_DESC = "SPEC_DESC";
    /**
     * 原线代码
     */
    private static final String ORIGINAL_LINE_CODE = "ORIGINAL_LINE_CODE";
    /**
     * 帘线规格
     */
    private static final String CORD_SPEC = "CORD_SPEC";
    /**
     * 胎面长度
     */
    private static final String TREAD_SHOULDER_LENGTH = "TREAD_SHOULDER_LENGTH";
    /**
     * 胎胚物料品号
     */
    private static final String SAP_CODE = "SAP_CODE";
    /**
     * 生产阶段：试制阶段
     */
    private final static String PRODUCTION_TEST_STAGE = "1";
    /**
     * 半钢胎体相关字段
     */
    private static final List<String> COLUMN_NAME_TIRE_FABRIC = Arrays.asList(new String[] { "TIRE_FABRIC_CODE1",
            "TIRE_FABRIC_SAP1", "TIRE_FABRIC_CODE2", "TIRE_FABRIC_SAP2", "TIRE_FABRIC_CODE3", "TIRE_FABRIC_SAP3",
            "TIRE_FABRIC1_VERSION", "TIRE_FABRIC2_VERSION", "TIRE_FABRIC3_VERSION" });

    @Autowired
    private TMesConstructionInfoMapper tMesConstructionInfoMapper;

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    /**
     * 将指定版本的bom数据合并到施工表中
     * 
     * @param dataVersion bom数据版本
     * @return
     */
    @Transactional
    @Override
    public AjaxResult mergeBomToConstruction(String dataVersion) {
        // TODO 删除本次同步待删除的施工版本信息
//		tMesConstructionInfoMapper.deleteConstructionVersionInfo(dataVersion);

        // 将正常bom数据合并至施工表中
        return this.mergeIntoAps(dataVersion, null);
    }

    /**
     * 将指定版本的物料数据合并到施工表中
     * 
     * @param dataVersion plm数据版本
     * @return
     */
    @Transactional
    @Override
    public AjaxResult mergePlmToConstruction(String dataVersion) {
        // 将正常plm数据合并至施工表中
        return this.mergeIntoAps(null, dataVersion);
    }

    /**
     * 将指定时间之后的施工版本更新到投产施工表中
     * 
     * @param updateTime
     * @return
     */
    @Transactional
    @Override
    public AjaxResult mergeProductConstructionInfo(Date updateTime) {
        // 将当天更新的施工版本数据全部同步到投产施工表中
//        tMesConstructionInfoMapper.mergeProductConstructionInfo(updateTime);
//        tMesConstructionInfoMapper.updateProductConstructionInfo();
//        tMesConstructionInfoMapper.addNewProductConstructionInfo();
        return AjaxResult.success();
    }

    /**
     * 将数据合并至排程系统中
     * 
     * @param bomDataVersion bom数据版本
     * @param plmDataVersion plm参数数据版本
     * @return
     */
    private AjaxResult mergeIntoAps(String bomDataVersion, String plmDataVersion) {
        // 处理施工表
        this.mergeConstructionInfo(bomDataVersion);
        // TODO 处理硫化施工表
//		this.mergLhConstructionInfo(bomDataVersion, plmDataVersion);
        // TODO 处理配方

        // TODO 处理成品物料表

        // 清除施工表缓存
        cxEngineQuotaCommonService.delCacheConstructionInfoMap();
        return AjaxResult.success();
    }

    /**
     * 合并施工表中间库信息，要根据数据版本合并。bom版本跟plm版本只有一个版本号有值
     * 
     * @param bomDataVersion bom数据版本
     * @param matDataVersion 物料数据版本
     */
    private void mergeConstructionInfo(String dataVersion) {
        // 取出施工表字段映射配置
        List<TMesConstructionMapping> mappingList = tMesConstructionInfoMapper.selectTPlmBomConstructionMapping();
        if (CollectionUtil.isEmpty(mappingList)) {
            throw new RuntimeException(I18nUtil.getMessage("mes.error.message.construction.mapping"));
        }
        // 取出施工表字段映射配置中的所有字段
        List<String> columnNameList = mappingList.stream().map(TMesConstructionMapping::getColumnName).distinct()
                .collect(Collectors.toList());
        // 校验映射表字段名配置是否合法，防止sql注入
        this.checkIllegalColumnName(columnNameList);

        // 获取系统所有BOM树状列表（根节点是胎胚）
        List<TMesPlmBomInfo> allBomList = this.getAllBomInfoList();
        // 构建待更新胎胚BOM列表
        List<TMesPlmBomInfo> embryoList = this.buildUpdateEmbryoBomInfo(allBomList, dataVersion);
        // 处理施工阶段，子节点是试制，则要将胎胚节点也更新为试制
        this.handleProductionStage(embryoList);
        // 重置遍历标记
        allBomList.stream().filter(TMesPlmBomInfo::getIsSearch).forEach(bom -> bom.setIsSearch(false));
        // 构建待更新施工记录
        List<TMesConstructionParam> resultList = this.buildConstruction(embryoList, mappingList);
        // 更新施工版本表
        for (TMesConstructionParam param: resultList) {
//            if (tMesConstructionInfoMapper.checkConstructionInfo(param) > 0) {
                tMesConstructionInfoMapper.updateConstructionInfo(param);
//            }
//            else {
//                tMesConstructionInfoMapper.insertConstructionInfo(param);
//            }
        }
    }

    /**
     * 构建BOM列表，已按树结构将各节点关联起来
     * 
     * @return
     */
    private List<TMesPlmBomInfo> getAllBomInfoList() {
        List<TMesPlmBomInfo> allBomList = tMesConstructionInfoMapper.selectBomInfo();
        // 构建树结构
        for (TMesPlmBomInfo bom : allBomList) {
            String parentMaterialCode = bom.getChildMaterialCode();
            String parentMaterialVersion = bom.getChildMaterialVersion();
            // 将父编号与父版本与本节点匹配的节点都挂到本节点的子节点列表中
            if (StringUtils.isNotEmpty(parentMaterialCode) && StringUtils.isNotEmpty(parentMaterialVersion)) {
                bom.setChildren(allBomList.stream()
                        .filter(child -> parentMaterialCode.equals(child.getParentMaterialCode())
                                && parentMaterialVersion.equals(child.getParentMaterialVersion()))
                        .collect(Collectors.toList()));
            }
            // 子节点列表的父节点全部更新成本节点
            for (TMesPlmBomInfo child : bom.getChildren()) {
                child.setParent(bom);
            }
        }
        return allBomList;
    }

    /**
     * 处理施工阶段，子节点是试制，则要将胎胚节点也更新为试制
     * 
     * @param embryoList
     */
    private void handleProductionStage(List<TMesPlmBomInfo> embryoList) {
        for (TMesPlmBomInfo embryo : embryoList) {
            Queue<TMesPlmBomInfo> searchList = new LinkedList<>();
            searchList.add(embryo);
            while (CollectionUtils.isNotEmpty(searchList)) {
                TMesPlmBomInfo bom = searchList.poll();
                if (bom.getIsSearch()) { // 已遍历的节点跳过，防止死循环
                    continue;
                }
                if (PRODUCTION_TEST_STAGE.equals(bom.getProductionStageCode())) {
                    embryo.setProductionStageCode(PRODUCTION_TEST_STAGE);
                    break;
                }
                bom.setIsSearch(true);
                searchList.addAll(bom.getChildren());
            }
        }
    }

    /**
     * 构建待更新胎胚BOM列表
     * 
     * @param allBomList
     * @param bomDataVersion
     * @param matDataVersion
     * @return
     */
    private List<TMesPlmBomInfo> buildUpdateEmbryoBomInfo(List<TMesPlmBomInfo> allBomList, String dataVersion) {
        // 根据版本号取出本次更新涉及的节点
        List<TMesPlmBomInfo> updateBomList = allBomList.stream()
                .filter(bom -> Objects.equal(bom.getBomDataVersion(), dataVersion)
                        || Objects.equal(bom.getMatDataVersion(), dataVersion))
                .collect(Collectors.toList());
        // 根据更新节点向上检索胎胚
        List<TMesPlmBomInfo> embryoList = new ArrayList<>();
        for (TMesPlmBomInfo updateBom : updateBomList) {
            TMesPlmBomInfo parent = updateBom;
            while (parent != null && !parent.getIsSearch()) {
                parent.setIsSearch(true);// 打上已遍历的标记，防止死循环
                if (MaterTypeEnum.GT.getCode().equals(parent.getChildMaterialNameCode())) { // 如果是胎胚节点，则加入到胎胚列表中
                    embryoList.add(parent);
                    break;
                }
                parent = parent.getParent(); // 判断下一个父节点
            }
        }
        // 重置遍历标记
        allBomList.stream().filter(TMesPlmBomInfo::getIsSearch).forEach(bom -> bom.setIsSearch(false));
        return embryoList;
    }

    /**
     * 构建待更新施工记录
     * 
     * @param embryoList  BOM树根节点列表
     * @param mappingList 映射配置
     * @return
     */
    private List<TMesConstructionParam> buildConstruction(List<TMesPlmBomInfo> embryoList,
            List<TMesConstructionMapping> mappingList) {
        // 映射关系先按字段名分组，部分字段需要特殊处理因此有配置两笔
        Map<String, List<TMesConstructionMapping>> mappingGroup = mappingList.stream().collect(Collectors.groupingBy(TMesConstructionMapping::getColumnName));
        List<TMesConstructionParam> resultList = new ArrayList<>();
        // 将树转换成施工表的数据结构
        for (TMesPlmBomInfo embryo : embryoList) {
            String embryoCode = embryo.getChildCode();

            TMesConstructionParam param = new TMesConstructionParam();
            param.setEmbryoCode(embryoCode);
            param.setEmbryoVersion(embryo.getChildMaterialVersion());
            List<TMesConstructionInfo> columnList = new ArrayList<>();
            param.setColumnList(columnList);
            Map<String, List<TMesPlmBomInfo>> bomMap = new HashMap<>();
            // 优先处理成型法
            String method = this.searchParams("LS Chengxingguxingshi", embryo.getRoClassificationAttrs());
            List<TMesConstructionMapping> consTypeMapping = mappingGroup.get(CONS_TYPE);
            if (CollectionUtils.isNotEmpty(consTypeMapping)) {
                String moldingMethod = "2";
                for (TMesConstructionMapping mapping: consTypeMapping) {
                    if (moldingMethod.equals("1")) {
                        break;
                    }
                    moldingMethod = this.handleColumnValueAfterSave(CONS_TYPE, method, embryoCode, mapping);
                }
                embryo.setMoldingMethod(moldingMethod);
            }
            
            for (Entry<String, List<TMesConstructionMapping>> entry: mappingGroup.entrySet()) {
                TMesConstructionMapping mapping = this.chooseMapping(embryo, entry.getValue());
                // 根据字段映射构建列名与数值
                String columnName = mapping.getConstructionColumn();
                if (CONS_TYPE.equals(columnName)) {
                    TMesConstructionInfo column = new TMesConstructionInfo();
                    column.setColumnName(columnName);
                    column.setColumnValue(embryo.getMoldingMethod());
                    columnList.add(column);
                    continue;
                }
                String columnValue = null;
                String parentMaterialNameCode = mapping.getParentMaterialNameCode();
                String childMaterialNameCode = mapping.getChildMaterialNameCode();
                String paramCode = mapping.getParamCode();

                // 根据子物料编号+父物料编号匹配节点（可能有多个），先从缓存中查找
                String bomKey = GenerageMapKeyUtils.createMapKey(parentMaterialNameCode, childMaterialNameCode);
                List<TMesPlmBomInfo> matchList = bomMap.get(bomKey);
                // 缓存中没有，则从bom树中检索
                if (CollectionUtil.isEmpty(matchList)) {
                    matchList = new ArrayList<>();
                    bomMap.put(bomKey, matchList);
                    Queue<TMesPlmBomInfo> searchList = new LinkedList<>();
                    List<TMesPlmBomInfo> checkedList = new ArrayList<>();
                    searchList.add(embryo);
                    while (CollectionUtils.isNotEmpty(searchList)) {
                        TMesPlmBomInfo bom = searchList.poll();
                        if (bom.getIsSearch()) { // 已遍历的节点跳过，防止死循环
                            continue;
                        }
                        checkedList.add(bom);
                        bom.setIsSearch(true); // 打上已遍历标记
                        // 查看当前节点是否匹配
                        if (StringUtils.isEmpty(parentMaterialNameCode)
                                && Objects.equal(childMaterialNameCode, bom.getChildMaterialNameCode())
                                || Objects.equal(parentMaterialNameCode, bom.getParentMaterialNameCode())
                                        && Objects.equal(childMaterialNameCode, bom.getChildMaterialNameCode())) {
                            matchList.add(bom);
                        } else {
                            searchList.addAll(bom.getChildren()); // 不匹配则继续往子节点检索
                        }
                    }
                    // 结束后重置已遍历标记
                    checkedList.stream().filter(TMesPlmBomInfo::getIsSearch).forEach(bom -> bom.setIsSearch(false));
                }
                if (CollectionUtils.isEmpty(matchList)) {
                    continue;
                }
                // 如果有多笔，按指定规则去重或者合并数据
                TMesPlmBomInfo matchBom = this.mergeBomInfo(embryo, mapping, matchList, columnName);
                if (matchBom != null) {
                    switch (mapping.getSourceColumn()) {
                    case "1": // 取BOM的CHILD_CODE
                        columnValue = matchBom.getChildCode();
                        break;
                    case "2": // 取BOM的SAP_CODE
                        columnValue = matchBom.getChildSapCode();
                        break;
                    case "3": // 取BOM的PARENT_CODE
                        columnValue = matchBom.getParentCode();
                        break;
                    case "4": // 取bom的roClassificationAttrs参数（物料参数）
                        columnValue = this.searchParams(paramCode, matchBom.getRoClassificationAttrs());
                        break;
                    case "5": // 取BOM表的DOSAGE
                        BigDecimal dosage = matchBom.getDosage();
                        // 长度单位如果是M，要换算成毫米
                        if ("M".equals(matchBom.getUnit())) {
                            dosage = dosage.multiply(THOUSAND);
                        }
                        columnValue = dosage.toString();
                        break;
                    case "6": // 取BOM表的BOM_VERSION
                        columnValue = matchBom.getChildMaterialVersion();
                        break;
                    case "7": // 生产阶段，根据子物料号开头字母判断，ES为量产，ET为试制，EX为量试
                        columnValue = matchBom.getChildMaterialCode().startsWith("ES")? "0": "1";
                        break;
                    case "8": // 取bom的childMaterialName参数
                        columnValue = matchBom.getChildMaterialName();
                        break;
                    case "9": // 取bom的classificationAttrs参数（施工参数）
                        columnValue = this.searchParams(paramCode, matchBom.getClassificationAttrs());
                        break;
                    default:
                        break;
                    }
                }

                // 部分栏位需要特殊处理
                columnValue = this.handleColumnValueAfterSave(columnName, columnValue, embryoCode, mapping);
                
                // 数据校验
                if (StringUtils.isNotEmpty(columnValue)) {
                    String regularExpression = mapping.getRegularExpression();
                    if (StringUtils.isNotEmpty(regularExpression)) {
                        // 字段数值与正则表达式匹配验证
                        if (!Pattern.compile(regularExpression).matcher(columnValue).find()) {
                            continue; // 校验不通过的忽略该字段
                        }
                    }
                }

                TMesConstructionInfo column = new TMesConstructionInfo();
                column.setColumnName(columnName);
                column.setColumnValue(columnValue);
                columnList.add(column);
            }
            if (CollectionUtils.isEmpty(columnList)) {
                continue;
            }
            resultList.add(param);
        }
        return resultList;
    }

    /**
     * 查找参数
     * 
     * @param paramCode
     * @param roClassificationAttrs
     * @return
     */
    private String searchParams(String paramCode, String roClassificationAttrs) {
        if (StringUtils.isNotEmpty(paramCode)) {
            if (StringUtils.isNotEmpty(roClassificationAttrs) && JSONValidator.from(roClassificationAttrs).validate()) {
                JSONObject json = JSONObject.parseObject(roClassificationAttrs);
                for (Entry<String, Object> entry : json.entrySet()) {// key是code_name的格式，由于name是中文，配置表只配置前半部分code
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        String[] keyArr = key.split("_");
                        if (keyArr.length > 0 && paramCode.equals(keyArr[0])) {
                            return String.valueOf(value);
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 选择复合条件的映射记录，通常是一个栏位有多个映射来源，需要选择复合条件的映射配置
     * @param embryo    胎胚bom
     * @param mappingList   映射列表
     * @return
     */
    private TMesConstructionMapping chooseMapping(TMesPlmBomInfo embryo, List<TMesConstructionMapping> mappingList) {
        TMesConstructionMapping mapping = CollectionUtil.firstElement(mappingList);
        String columnName = mapping.getColumnName();
        if (NOSE_WIDTH.equals(columnName)) { // 机头宽度
            String method = embryo.getMoldingMethod();
            for (TMesConstructionMapping tMapping: mappingList) {
                if (method != null && method.equals(tMapping.getSortCode())) { // 选择成型法匹配的映射
                    return tMapping;
                }
            }
        }
        return mapping;
    }
    
    /**
     * 根据映射配置匹配到多个bom记录时，需要根据指定条件合并bom
     * 
     * @param constructionMap
     */
    private TMesPlmBomInfo mergeBomInfo(TMesPlmBomInfo embryo, TMesConstructionMapping mappingList, List<TMesPlmBomInfo> bomInfo, String columnName) {
        // 如果是胎体布相关，则需要根据位置选择具体层数具体的层数
        if (columnName.startsWith(TIRE_FABRIC)) {
            String sortCode = mappingList.getSortCode();
            return embryo.getChildren().stream().filter(s -> Objects.equal(sortCode, s.getAHComponentLocation()))
                    .findAny().orElse(null);
        }
        return CollectionUtil.firstElement(bomInfo);
    }

    /**
     * 比较两个id的大小，值对象的ID是包括所有层级父节点的ID，格式样例：/祖父ID/父ID/本节点ID 因此比较大小应该要拆分解析，从顶层往下开始比较
     * 结果需要倒序排序
     * 
     * @param id1 原ID
     * @param id2 待比较ID
     * @return
     */
    private int compareId(String id1, String id2) {
        if (StringUtils.isEmpty(id1) || StringUtils.isEmpty(id2)) {
            return 0;
        }
        // 拆分ID
        String[] idArray1 = id2.split("/");
        String[] idArray2 = id1.split("/");
        // 判断两个ID的长度是否相等
        boolean isLevelEquals = idArray1.length == idArray2.length;
        // 取较短的进行比对
        int length = idArray1.length < idArray2.length ? idArray1.length : idArray2.length;
        int result = 0;
        for (int i = 0; i < length; i++) {
            // 比对同一层的ID
            String idItem1 = idArray1[i];
            String idItem2 = idArray2[i];
            if (StringUtils.isEmpty(idItem1) || StringUtils.isEmpty(idItem2)) {
                // 空值不处理
                continue;
            }
            BigDecimal idNum1 = new BigDecimal(idItem1);
            BigDecimal idNum2 = new BigDecimal(idItem2);
            result = idNum1.compareTo(idNum2);
            // 如果不相等，则直接返回比对结果
            if (result != 0) {
                return result;
            }
        }
        // 如果上述比对结果相等，则比对两者ID层级
        if (!isLevelEquals) {
            return new Integer(idArray1.length).compareTo(new Integer(idArray2.length));
        }
        return result;
    }

    /**
     * 合并硫化施工表中间库信息，要根据数据版本合并。bom版本跟plm版本只有一个版本号有值
     * 
     * @param bomDataVersion bom数据版本
     * @param plmDataVersion plm参数数据版本
     */
    private void mergLhConstructionInfo(String bomDataVersion, String plmDataVersion) {
        // 查询出本次更新会影响到的硫化施工表信息
        List<TMesConstructionInfo> lhConstructionList = tMesConstructionInfoMapper
                .selectLhConstructionInfo(bomDataVersion, plmDataVersion);
        if (CollectionUtils.isNotEmpty(lhConstructionList)) {
            Date currentDate = DateUtils.getNowDate();
            // 硫化施工表待更新列表
            List<LhTireConstructionInfo> updateList = new ArrayList<>();
            // 遍历施工表字段列表
            lhConstructionList.stream().forEach(c -> {
                String sapCode = Optional.ofNullable(c.getParentMaterialCode()).orElse("");
                String embryoCode = Optional.ofNullable(c.getEmbryoCode()).orElse("");
                String embryoVersion = Optional.ofNullable(c.getEmbryoVersion()).orElse("");
                // 字段名
                String columnName = c.getColumnName();
                // 字段值，如果状态为已删除，则该字段清空
                String columnValue = ApsConstant.DEL_FLAG_NORMAL.equals(c.getStatus()) ? c.getColumnValue() : null;
                // 遍历更新列表，根据sap + 胎胚号匹配
                LhTireConstructionInfo conInfo = updateList.stream().filter(lc -> sapCode.equals(lc.getSapCode())
                        && embryoCode.equals(lc.getEmbryoCode()) && embryoVersion.equals(lc.getEmbryoVersion()))
                        .findAny().orElseGet(() -> {
                            LhTireConstructionInfo lhConInfo = new LhTireConstructionInfo();
                            lhConInfo.setEmbryoCode(embryoCode);
                            lhConInfo.setSapCode(sapCode);
                            lhConInfo.setEmbryoVersion(embryoVersion);
                            updateList.add(lhConInfo);
                            return lhConInfo;
                        });
                if (CLAMPING_PRESSURE.equals(columnName)) {
                    // 合模压力
                    conInfo.setClampingPressure(NumberUtils.createBigDecimal(columnValue));
                } else if (CURING_TIME.equals(columnName)) {
                    // 硫化时间
                    conInfo.setCuringTime(NumberUtils.createBigDecimal(columnValue));
                } else if (SPEC_DESC.equals(columnName)) {
                    // 规格描述，需要截取胎胚之后的文字
                    String newColumnValue = columnValue != null ? columnValue.replaceFirst(embryoCode, "")
                            : columnValue;// 取回的数据是：胎胚描述$$$外胎描述，默认取外胎描述，如果外胎描述为空，取胎胚描述，胎胚描述要先去掉前面的胎胚号
                    if (StringUtils.isNotEmpty(newColumnValue)) {
                        String[] columnValues = newColumnValue.split("\\$\\$\\$");
                        for (int i = columnValues.length - 1; i >= 0; i--) { // 优先看外胎描述，因此从后往前取
                            String value = StringUtils.trim(columnValues[i]);
                            if (StringUtils.isNotEmpty(value)) {
                                conInfo.setSpecDesc(value);
                                break;
                            }
                        }
                    }
                } else if (DEL_FLAG.equals(columnName)) {
                    // 删除标识
                    conInfo.setDelFlag(columnValue);
                }
                conInfo.setUpdateTime(currentDate);
            });
            // 更新硫化施工表
            // 分批处理数据
            List<LhTireConstructionInfo> batchList = new ArrayList<>();
            int total = updateList.size();
            int index = 0;
            for (LhTireConstructionInfo info : updateList) {
                batchList.add(info);
                index++;
                if (batchList.size() < BATCH_NUM && index < total) {
                    continue;
                }
                tMesConstructionInfoMapper.mergeLhConstructionInfo(batchList);
                batchList.clear();
                log.info("硫化施工表处理进度：" + index + "/" + total);
            }
        }
    }

    /**
     * 构建施工信息列表，一个胎号生成一个施工信息
     * 
     * @param mappingList      施工表字段映射配置
     * @param columnNameList   施工表字段列表
     * @param constructionList PLM与BOM信息合并后的施工表字段值列表
     * @return
     */
    private Map<String, TMesConstructionParam> buildConstructionParam(List<TMesConstructionMapping> mappingList,
            List<String> columnNameList, List<TMesConstructionInfo> constructionList) {
        // 将施工表字段映射配置转换成map<字段名, 字段转换配置>
        Map<String, List<TMesConstructionMapping>> mappingMap = mappingList.stream()
                .collect(Collectors.groupingBy(TMesConstructionMapping::getColumnName));
        // 服务器系统时间
        Date currentDate = DateUtils.getNowDate();
        // 构建施工信息对象，用于数据库更新
        // 相同的胎胚号字段信息合并到同一个施工信息对象中，并存放在map<胎胚号，施工信息>中
        Map<String, TMesConstructionParam> constructionMap = new HashMap<>();
        for (TMesConstructionInfo columnInfo : constructionList) {
            String embryoCode = columnInfo.getEmbryoCode();
            String embryoVersion = columnInfo.getEmbryoVersion();
            String sapVersion = columnInfo.getSapVersion();
            String columnName = columnInfo.getColumnName();
            // 校验更新字段数值格式是否正确，需要依据施工表映射配置的正则表达式校验
            // 多栏位合并的字段，只需要取第一个就可以
            List<TMesConstructionMapping> columnMappingList = mappingMap.get(columnName);
            this.checkDataFormatError(columnInfo, CollectionUtil.firstElement(columnMappingList));
            // 通过胎胚号 + 胎胚版本号 匹配施工信息
            String constructionKey = StringUtils.join(new String[] { embryoCode, embryoVersion }, "%");
            TMesConstructionParam construction = constructionMap.get(constructionKey);
            if (construction == null) {
                // 如果没有匹配到相同胎胚号的施工对象，则需要新建一个
                construction = new TMesConstructionParam();
                construction.setEmbryoCode(embryoCode);
                construction.setEmbryoVersion(embryoVersion);
                construction.setSapVersion(sapVersion);
                construction.setCurrentDate(currentDate);
                // 施工信息的各字段需要预先生成好，数值放空，主要是需要预先确定好字段顺序，否则在后续的merge中无法实现批量操作
                construction.setColumnList(this.buildColumnList(columnNameList, embryoCode, embryoVersion));
                constructionMap.put(constructionKey, construction);
            }
            // 将字段数值添加至施工信息对应字段中
            List<TMesConstructionInfo> columnList = construction.getColumnMap().get(columnName);
            if (columnList == null) {
                columnList = new ArrayList<>();
                construction.getColumnMap().put(columnName, columnList);
            }
            columnList.add(columnInfo);
        }

        // 各栏位数据合并
        this.mergeColumn(constructionMap);
        return constructionMap;
    }

    /**
     * 合并字段
     * 
     * @param constructionMap
     */
    private void mergeColumn(Map<String, TMesConstructionParam> constructionMap) {
        // 遍历施工信息
        for (TMesConstructionParam construction : constructionMap.values()) {
            // 胎胚号
            String embryoCode = construction.getEmbryoCode();
            Map<String, List<TMesConstructionInfo>> columnMap = construction.getColumnMap();
            // 遍历施工表的各栏位
            for (Entry<String, List<TMesConstructionInfo>> entry : columnMap.entrySet()) {
                // 栏位名
                String columnName = entry.getKey();
                // 栏位数值
                TMesConstructionInfo newColumnInfo = null;
                // 栏位信息列表（可能有多个）
                List<TMesConstructionInfo> columnMapList = entry.getValue();
                if (CollectionUtils.isNotEmpty(columnMapList)) {
                    // 如果栏位数值有值，需要根据字段名选择对应的去重/合并策略
                    newColumnInfo = this.getDistinctValueColumn(columnName, columnMapList);
                    // 部分栏位数值需要在保存前处理
//                    String columnValue = this.handleColumnValueAfterSave(columnName, newColumnInfo.getColumnValue(),
//                            embryoCode);
//                    newColumnInfo.setColumnValue(columnValue);
                }
                // 该栏位的最终数值
                TMesConstructionInfo finalColumnInfo = newColumnInfo;
                // 将数值更新到施工表内的指定字段中
                construction.getColumnList().stream().filter(column -> column.getColumnName().equals(columnName))
                        .findAny().ifPresent(column -> {
                            column.setColumnValue(finalColumnInfo.getColumnValue());
                            column.setChildMaterialCode(finalColumnInfo.getChildMaterialCode());
                            column.setModify(true);
                            // 取sapcode的更新日期
                            if (SAP_CODE.equals(column.getColumnName())) {
                                // 本次字段的更新时间
                                construction.setUpdateDate(finalColumnInfo.getUpdateDate());
                                construction.setId(finalColumnInfo.getId());
                            }
                        });
            }

            // 部分栏位需要依赖其他字段进行去重选择，因此要在前置字段完成解析后再处理
            // 处理原线代码：依赖帘线规格栏位，需要取下一层的值
            this.handleOriginalLineCode(construction);
        }
    }

    /**
     * 原线代码的去重需要单独处理，需要取帘线规格下一层的值
     * 
     * @param construction 施工信息
     */
    private void handleOriginalLineCode(TMesConstructionParam construction) {
        // 栏位信息
        Map<String, List<TMesConstructionInfo>> columnMap = construction.getColumnMap();
        // 帘线规格栏位信息
        TMesConstructionInfo cordSpecColumn = construction.getColumnList().stream()
                .filter(column -> column.getColumnName().equals(CORD_SPEC)).findAny().orElse(null);
        // 原线代码栏位信息
        TMesConstructionInfo oriColumn = construction.getColumnList().stream()
                .filter(column -> column.getColumnName().equals(ORIGINAL_LINE_CODE)).findAny().orElse(null);
        // 非空校验
        if (cordSpecColumn == null || oriColumn == null || cordSpecColumn.getChildMaterialCode() == null) {
            return;
        }
        // 取出原线代码的字段列表
        List<TMesConstructionInfo> originalLineCodeList = columnMap.get(ORIGINAL_LINE_CODE);
        if (CollectionUtils.isEmpty(originalLineCodeList)) {
            return;
        }
        originalLineCodeList.stream()
                // 1、通过帘线规格的子物料代码与原线代码的父物料代码关联数据；2、只需要处理本次版本更新涉及的数据
                .filter(c -> c.isModify() && cordSpecColumn.getChildMaterialCode().equals(c.getParentMaterialCode()))
                // 按更新时间排序，取出最新的一笔
                .sorted(this.buildLastupdateSortor()).findFirst()
                // 能匹配上后，将数据更新至原线代码字段中
                .ifPresent(c -> {
                    // 取出原线代码
                    oriColumn.setColumnValue(c.getColumnValue());
                    oriColumn.setChildMaterialCode(c.getChildMaterialCode());
                    oriColumn.setModify(true);
                });
    }

    /**
     * 部分栏位数值需要在保存前处理
     * 
     * @param columnName  栏位名称
     * @param columnValue 栏位数值
     * @param embryoCode  胎胚号
     * @return
     */
    private String handleColumnValueAfterSave(String columnName, String columnValue, String embryoCode, TMesConstructionMapping mapping) {
        if (StringUtil.isEmpty(columnValue)) {
            return columnValue;
        }
        String newColumnValue = columnValue;
        // 部分字段需要处理
        // 寸口信息，取R后面的2位数字
        if (DIMENSION.equals(columnName)) {
            // 字符R的位置
            int flagIndex = columnValue.indexOf("R");
            newColumnValue = this.substring(columnValue, flagIndex, 2, true);
        } else
        // 断面宽，取第一个“/”的左3位数字
        if (SECTION_WIDTH.equals(columnName)) {
            int index = columnValue.indexOf("/");
            if (index < -0) {
                // 没有"/"则取"R"左边的3位数字
                index = columnValue.indexOf("R");
            }
            // 截取长度
            int length = index >= 3 ? 3 : index;
            if (length > 0) {
                int startIndex = index - length;
                int endIndex = index;
                newColumnValue = columnValue.substring(startIndex, endIndex);
            }
        } else
        // 成型法
        if (CONS_TYPE.equals(columnName)) {
            if (StringUtils.isNotEmpty(columnValue) && columnValue.equals(mapping.getSortCode())) {
                newColumnValue = "1";
            } else {
                newColumnValue = "2";
            }
        }
        // 规格描述，需要截取第一个空格之前的文字
        if (SPEC_DESC.equals(columnName)) {
            newColumnValue = columnValue.substring(0, columnValue.indexOf("")).trim();
        }
        return newColumnValue;
    }

    /**
     * 截取字符串
     * 
     * @param str        待截取字段
     * @param startIndex 起始下标
     * @param subLength  截取长度，大于0时限制反馈的长度；否则直接截取到末尾再返回结果
     * @param isDigits   true：只截取数字；false：只截取非数字
     * @return
     */
    private String substring(String str, int startIndex, int subLength, boolean isDigits) {
        int index = startIndex;
        StringBuilder strBuilder = new StringBuilder();
        // 向后检索字符，检查下一个字符是否符合条件
        while (true) {
            // 下一个字符下标
            int currentIndex = ++index;
            if (currentIndex == str.length()) {
                // 防止下标越界
                break;
            }
            // 取出下一个字符
            String nextChar = String.valueOf(str.charAt(currentIndex));
            // 根据参数判断当前字符是否符合条件
            if (isDigits && NumberUtils.isDigits(nextChar) || !isDigits && !NumberUtils.isDigits(nextChar)) {
                strBuilder.append(nextChar);
            } else if (strBuilder.length() > 0) {
                // 如果不符合条件，但是之前曾经截取到正确的字符，则反馈之前截取到的字符
                break;
            }
            // 如果有长度限制，则截取达到指定长度后中止
            if (subLength > 0 && strBuilder.length() == subLength) {
                break;
            }
        }
        return strBuilder.toString();
    }

    /**
     * 获取去重后的值
     * 
     * @param columnName
     * @param columnValue
     * @param columnMapList
     * @return
     */
    private TMesConstructionInfo getDistinctValueColumn(String columnName, List<TMesConstructionInfo> columnMapList) {
        TMesConstructionInfo newColumnInfo = null;
        // 胎面胶料，如果有多个胶，需要斜杠合并起来，直接按配置的顺序号排序。
        if (TREAD_RUBBER_CATEGORY.equals(columnName)) {
            newColumnInfo = this.joinColumnValue(columnMapList, "/",
                    Comparator.comparing(TMesConstructionInfo::getSortCode));
        } else
        // 胎侧胶料，如果有多个胶，需要斜杠合并起来，合并顺序是HS开头的胶放第一个，HA开头的胶放第二个。
        if (SIDEWALL_RUBBER.equals(columnName)) {
            newColumnInfo = this.joinColumnValue(columnMapList, "/", new Comparator<TMesConstructionInfo>() {
                @Override
                public int compare(TMesConstructionInfo o1, TMesConstructionInfo o2) {
                    Integer sort1 = SideRubberSortEnum.getSortNo(o1.getColumnValue());
                    Integer sort2 = SideRubberSortEnum.getSortNo(o2.getColumnValue());
                    return sort1.compareTo(sort2);
                }
            });
        } else
        // 内衬胶料，如果有多个胶，需要斜杠合并起来，合并顺序是HL开头的胶放第一个，HF开头的胶放第二个。
        if (INSIDE_RUBBER.equals(columnName)) {
            newColumnInfo = this.joinColumnValue(columnMapList, "/", new Comparator<TMesConstructionInfo>() {
                @Override
                public int compare(TMesConstructionInfo o1, TMesConstructionInfo o2) {
                    Integer sort1 = InsideRubberSortEnum.getSortNo(o1.getColumnValue());
                    Integer sort2 = InsideRubberSortEnum.getSortNo(o2.getColumnValue());
                    return sort1.compareTo(sort2);
                }
            });
        } else
        // 胎圈尺寸，显示格式为：三角胶高 * 三角胶底宽，通过字段映射配置的排序号决定先后顺序
        if (HEXAGON_RUBBER_DIMENSION.equals(columnName)) {
            newColumnInfo = this.joinColumnValue(columnMapList, "*", TMesConstructionInfo::getSortCode,
                    Comparator.comparing(TMesConstructionInfo::getSortCode));
        } else
        // 补强/封口胶，显示格式为：半钢补强胶/半钢封口胶，通过字段映射配置的排序号决定先后顺序
        if (REINFORCE_SEAL_GLUE.equals(columnName)) {
            newColumnInfo = this.joinColumnValue(columnMapList, "/",
                    Comparator.comparing(TMesConstructionInfo::getSortCode));
        } else
        // 半钢胎胚，需要对字段名重命名：根据用量从小到大确定序号，如果编号相同时取更新时间最晚一笔的用量
        if (COLUMN_NAME_TIRE_FABRIC.contains(columnName)) {
            // 取出最新操作的栏位信息
            TMesConstructionInfo latestColumnInfo = columnMapList.stream().sorted(this.buildLastupdateSortor())
                    .findFirst().get();
            // 如果最新更新字段为空，则直接清空该字段
            if (StringUtils.isEmpty(latestColumnInfo.getColumnValue())) {
                return latestColumnInfo;
            }
            // 子物料名称编码
            String childMaterialNameCode = latestColumnInfo.getChildMaterialNameCode();
            if (TIRE_FABRIC.equals(childMaterialNameCode)) {
                // 半钢胎胚，不带序号的形式
                // 本字段序号
                Integer seq;
                // 字段数值列表
                List<TMesConstructionInfo> columnList;
                // 先判断是否版本
                if (columnName.endsWith("_VERSION")) {
                    // 如果确认是胎体布版本，则讲字段名序号后面的字符移除掉
                    String finalColumnName = columnName.replace("_VERSION", "");
                    // 字段名最后一位为本字段序号
                    seq = Integer.parseInt(finalColumnName.substring(finalColumnName.length() - 1));
                    // 获取数值列表
                    columnList = columnMapList.stream()
                            // 过滤，按ID分组，各自保留更新时间最晚的一笔
                            .collect(Collectors.groupingBy(TMesConstructionInfo::getChildMaterialCode,
                                    Collectors.collectingAndThen(Collectors.maxBy(this.buildLastupdateSortor()),
                                            Optional::get)))
                            .values().stream()
                            // 排序，按用量从小到大排序
                            .sorted(Comparator.comparing(TMesConstructionInfo::getDosage)).collect(Collectors.toList());
                } else {
                    // 字段名最后一位为本字段序号
                    seq = Integer.parseInt(columnName.substring(columnName.length() - 1));
                    // 获取数值列表
                    columnList = columnMapList.stream()
                            // 过滤，按列数值：胎体号分组，各自保留更新时间最晚的一笔
                            .collect(Collectors.groupingBy(TMesConstructionInfo::getColumnValue,
                                    Collectors.collectingAndThen(Collectors.maxBy(this.buildLastupdateSortor()),
                                            Optional::get)))
                            .values().stream()
                            // 排序，按用量从小到大排序
                            .sorted(Comparator.comparing(TMesConstructionInfo::getDosage)).collect(Collectors.toList());
                }
                if (CollectionUtils.isNotEmpty(columnList)) {
                    if (seq <= columnList.size()) {
                        // 取指定序号的字段数值为本字段的最终数值
                        newColumnInfo = columnList.get(seq - 1);
                    }
                }
                // 均不符合条件的，字段值直接赋值为空
                if (newColumnInfo == null) {
                    newColumnInfo = new TMesConstructionInfo();
                    newColumnInfo.setChildMaterialCode(null);
                    newColumnInfo.setColumnValue(null);
                }
            } else {
                // 半钢胎胚，带序号的，直接取最新的一笔
                newColumnInfo = columnMapList.stream().sorted(this.buildLastupdateSortor()).findFirst().get();
            }
        } else
        // 机头宽度，需要根据胎胚成型法取对应的值
        if (NOSE_WIDTH.contains(columnName)) {
            newColumnInfo = columnMapList.stream()
                    // 只保留对应次数成型法（1次/2次）的数据，根据胎胚号的首字母判断：Y-1次法；E-2次法
                    .filter(c -> c.getEmbryoCode().startsWith(c.getSortCode()))
                    // 取最晚更新的一笔
                    .sorted(this.buildLastupdateSortor()).findFirst().orElse(null);
        } else {
            // 其余情况直接取更新时间最晚的值
            newColumnInfo = columnMapList.stream().sorted(buildLastupdateSortor()).findFirst().get();
        }
        // 如果都不符合条件，则直接取第一笔即可
        return newColumnInfo != null ? newColumnInfo : CollectionUtil.firstElement(columnMapList);
    }

    /**
     * 构建最后更新排序器
     * 
     * @return
     */
    private Comparator<TMesConstructionInfo> buildLastupdateSortor() {
        // 按更新时间倒序排序
        return Comparator.comparing(TMesConstructionInfo::getUpdateDate, Comparator.reverseOrder())
                // 如果BOM更新时间相等，则按BOM创建时间倒序排序
                .thenComparing(TMesConstructionInfo::getCreateDate, Comparator.reverseOrder())
                // 如果BOM创建时间也相等，则按PLM更新时间倒序排序
                .thenComparing(TMesConstructionInfo::getPlmUpdateDate, Comparator.reverseOrder())
                // 如果PLM更新时间也相等，则按ID倒序排序
                .thenComparing(new Comparator<TMesConstructionInfo>() {
                    @Override
                    public int compare(TMesConstructionInfo c1, TMesConstructionInfo c2) {
                        return compareId(c1.getId(), c2.getId());
                    }
                });
    }

    /**
     * 对字段列表排序去重后，用指定分隔符将字段数值拼接成字符串
     * 
     * @param columnMapList 字段列表
     * @param separator     分隔符
     * @param sorter        排序比对器
     * @return
     */
    private TMesConstructionInfo joinColumnValue(List<TMesConstructionInfo> columnMapList, String separator,
            Comparator<TMesConstructionInfo> sorter) {
        // 根据字段值（一般是编号）分组去重
        return this.joinColumnValue(columnMapList, separator, TMesConstructionInfo::getColumnValue, sorter);
    }

    /**
     * 对字段列表排序后，用指定分隔符讲字段数值拼接成字符串
     * 
     * @param columnMapList 字段列表
     * @param separator     分隔符
     * @param groupKey      分组栏位
     * @param sorter        排序比对器
     * @return
     */
    private TMesConstructionInfo joinColumnValue(List<TMesConstructionInfo> columnMapList, String separator,
            Function<TMesConstructionInfo, String> groupKey, Comparator<TMesConstructionInfo> sorter) {
        // 过滤空值
        List<TMesConstructionInfo> resultColumnList = columnMapList.stream()
                .filter(c -> StringUtils.isNotEmpty(c.getColumnValue())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(resultColumnList)) {
            // 如果所有元素都是空，则直接在原列表中返回第一个即可
            return CollectionUtil.firstElement(columnMapList);
        }
        // 取出最新操作的栏位信息
        TMesConstructionInfo latestColumnInfo = resultColumnList.stream().sorted(this.buildLastupdateSortor())
                .findFirst().get();
        // 如果最新更新字段为空，则直接清空该字段
        if (StringUtils.isEmpty(latestColumnInfo.getColumnValue())) {
            return latestColumnInfo;
        }
        // 先对字段信息去重以及排序
        List<TMesConstructionInfo> newColumnMapList = resultColumnList.stream()
                // 过滤掉数值为空的
                .filter(c -> StringUtils.isNotEmpty(c.getColumnValue()))
                // 将列数值重复的数据过滤，保留更新时间最晚的一笔
                .collect(Collectors.groupingBy(groupKey,
                        Collectors.collectingAndThen(Collectors.maxBy(this.buildLastupdateSortor()), Optional::get)))
                .values().stream()
                // 按排序比对器排序
                .sorted(sorter).collect(Collectors.toList());
        // 取出字段值
        List<String> valueList = newColumnMapList.stream().map(TMesConstructionInfo::getColumnValue)
                .filter(c -> StringUtil.isNotEmpty(c)).collect(Collectors.toList());
        // 按排好序的字段列表用指定分隔符拼接起来
        String columnValue = StringUtils.join(valueList, separator);
        // 构建一个新对象存值返回
        if (CollectionUtils.isNotEmpty(valueList)) {
            TMesConstructionInfo oldColumnInfo = CollectionUtil.firstElement(newColumnMapList);
            TMesConstructionInfo newColumnInfo = new TMesConstructionInfo();
            BeanUtils.copyProperties(oldColumnInfo, newColumnInfo);
            newColumnInfo.setColumnValue(columnValue);
            return newColumnInfo;
        }
        return latestColumnInfo;
    }

    /**
     * 校验更新字段数值格式是否正确
     * 
     * @param columnInfo    施工信息字段信息
     * @param columnMapping 施工表映射配置，需要使用其中配置的数据格式正则表达式
     */
    private void checkDataFormatError(TMesConstructionInfo columnInfo, TMesConstructionMapping columnMapping) {
        String columnName = columnInfo.getColumnName();
        String columnValue = columnInfo.getColumnValue();
        // 该字段有更新数值的时候才需要验证，为空直接跳过
        if (StringUtils.isNotEmpty(columnValue)) {
            boolean regularResult = true;
            // 取出正则表达式
            String regularExpression = columnMapping.getRegularExpression();
            if (StringUtils.isNotEmpty(regularExpression)) {
                // 字段数值与正则表达式匹配验证
                regularResult = Pattern.compile(regularExpression).matcher(columnValue).find();
            }
            if (!regularResult) {
                // 没有验证通过，则返回错误信息
                String errorMsg = I18nUtil.getMessage("mes.error.message.construction.regular");
                errorMsg = StringUtils.format(errorMsg, columnInfo.getChildMaterialCode(), columnName, columnValue,
                        columnMapping.getErrorTips());
                throw new RuntimeException(errorMsg);
            }
        }
    }

    /**
     * 
     * 构建施工表已配置映射的字段信息列表
     * 
     * @param columnNameList 字段名列表
     * @param embryoCode     胎胚号
     * @return
     */
    private List<TMesConstructionInfo> buildColumnList(List<String> columnNameList, String embryoCode,
            String embryoVersion) {
        List<TMesConstructionInfo> columnList = new ArrayList<>(columnNameList.size());
        for (String columnName : columnNameList) {
            // 初始化每个字段的信息
            TMesConstructionInfo column = new TMesConstructionInfo();
            column.setEmbryoCode(embryoCode);
            column.setEmbryoVersion(embryoVersion);
            column.setColumnName(columnName);
            columnList.add(column);
        }
        return columnList;
    }

    /**
     * 校验配置的字段名是否合法，由于数据库操作语句的字段为字符串替换，需要防止sql注入
     * 
     * @param columnName 字段名称列表
     */
    private void checkIllegalColumnName(List<String> columnNameList) {
        // 正则表达式
        Pattern pattern = Pattern.compile(
                "\\b(and|exec|insert|select|drop|grant|alter|delete|update|count|chr|mid|master|truncate|char|declare|or)\\b|(\\*|;|\\+|'|%|--)");
        for (String columnName : columnNameList) {
            if (pattern.matcher(columnName.toLowerCase()).find()) {
                // 匹配上就说明有sql注入的可能性，需要返回错误提示信息
                String errorMsg = I18nUtil.getMessage("mes.error.message.construction.legal");
                throw new RuntimeException(StringUtils.format(errorMsg, columnName));
            }
        }
    }
}
