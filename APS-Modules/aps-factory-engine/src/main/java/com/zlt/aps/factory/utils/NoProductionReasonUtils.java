package com.zlt.aps.factory.utils;

/**
 * 不排产原因管理工具类
 * 转化成json格式，支持可多语言切换提取
 *
 * @author ZLT
 * @date 20250524
 */
public class NoProductionReasonUtils {
    /**
     * 扣除超出模具产能数:%1$s.
     *
     * @param overModCapQty
     * @return
     */
    public static String getOverModCaps(Long overModCapQty) {
        return JsonUtils.getLanguageJsonObject("alg.data.noProductionReason.overModCaps", overModCapQty).toString();
    }

    /**
     * 没有配置寸口产能，不排产
     *
     * @return
     */
    public static String getNoConfigurationSizeCapacity() {
        return JsonUtils.getLanguageJsonObject("alg.data.absMolding.noConfigurationSizeCapacity").toString();
    }

    /**
     * 扣除轮胎类型产能控制限制，%1s
     *
     * @param deductQty
     * @return
     */
    public static String getTireCapacityLimit(Long deductQty) {
        return JsonUtils.getLanguageJsonObject("alg.data.absMolding.tireCapacityLimit", deductQty).toString();
    }

    /**
     * 扣除轮胎类型产能控制后没有达到最小批量量，%1s
     *
     * @param deductQty
     * @return
     */
    public static String getTireCapacityMinLimit(Long deductQty) {
        return JsonUtils.getLanguageJsonObject("alg.data.absMolding.tireCapacityMinLimit", deductQty).toString();
    }

    /**
     * 扣除寸口产能控制限制，%1s
     *
     * @param deductQty
     * @return
     */
    public static String getSizeCapacityLimit(Long deductQty) {
        return JsonUtils.getLanguageJsonObject("alg.data.absMolding.sizeCapacityLimit", deductQty).toString();
    }

    /**
     * 扣除寸口产能控制限制，%1s
     *
     * @param deductQty
     * @return
     */
    public static String getSizeCapacityMinLimit(Long deductQty) {
        return JsonUtils.getLanguageJsonObject("alg.data.absMolding.sizeCapacityMinLimit", deductQty).toString();
    }

    /**
     * 模具产能不足
     *
     * @return
     */
    public static String getMouldNotEnough() {
        return JsonUtils.getLanguageJsonObject("alg.data.absMolding.mouldNotEnough").toString();
    }

    /**
     * 超出成型配比产能
     *
     * @return
     */
    public static String getExceedRatioCapacity() {
        return JsonUtils.getLanguageJsonObject("alg.data.production.passExceedCapacity").toString();
    }

    /**
     * 施工阶段错误
     *
     * @return
     */
    public static String getConstructionError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.constructionError").toString();
    }

    /**
     * 没有配置施工阶段
     *
     * @return
     */
    public static String getNoConfigurationConstructionError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.noConfigurationConstructionError").toString();
    }

    /**
     * 施工不是正式施工
     *
     * @return
     */
    public static String getNoFormalProductionConstructionError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.noFormalProductionConstructionError").toString();
    }

    /**
     * 没有硫化时间
     *
     * @return
     */
    public static String getCuringTimeError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.productCodeNoCuringTime").toString();
    }

    /**
     * 没有寸口错误
     *
     * @return
     */
    public static String getProSizeError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.noProSize").toString();
    }

    /**
     * 没有生胎、规格代号、成型法
     *
     * @return
     */
    public static String getEmbryoCodeError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.noEmbryoCode").toString();
    }

    /**
     * 没有配置最小批量，不排产
     *
     * @return
     */
    public static String getNoConfigurationMinQtyError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.noMinQtyConfiguration").toString();
    }

    /**
     * 排产总需求量没有达到最小批量，不排产
     *
     * @param sumProductionQty 总需求量
     * @param minQty           最小批量
     * @return
     */
    public static String getNoFallShortOfMinQtyError(Long sumProductionQty, Long minQty) {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.noFallShortOfMinQty", sumProductionQty, minQty).toString();
    }

    /**
     * 扣除拼模排产超出的产能
     *
     * @param subtractQty 扣减的排产量
     * @return
     */
    public static String getAssemblingMouldCapacity(Long subtractQty) {
        return JsonUtils.getLanguageJsonObject("alg.data.assemblingMould.overModCaps", subtractQty).toString();
    }

    /**
     * 没有配置模具
     *
     * @return
     */
    public static String getNoConfigurationMouldError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.noMould").toString();
    }

    /**
     * 模具没有产能
     *
     * @return
     */
    public static String getMouldNoCapacity() {
        return JsonUtils.getLanguageJsonObject("alg.data.mould.hasNoProduction").toString();
    }

    /**
     * 没有可排的模具
     *
     * @return
     */
    public static String getNoProductionMould() {
        return JsonUtils.getLanguageJsonObject("alg.data.mould.noProductionMould").toString();
    }

    /**
     * 分厂设置不排产
     *
     * @return
     */
    public static String getFactoryNoProductionError() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.factoryNoProduct").toString();
    }

    /**
     * 无排产量
     *
     * @return
     */
    public static String getNoProductionQty() {
        return JsonUtils.getLanguageJsonObject("alg.data.initCheck.noProductionQty").toString();
    }

    /**
     * 日产能或是日规格数限制
     *
     * @return
     */
    public static String getDayLimit() {
        return JsonUtils.getLanguageJsonObject("alg.data.mould.dayLimitCapacity").toString();
    }

    /**
     * 双模排产不排单
     *
     * @return
     */
    public static String getDoubleNoSingle() {
        return JsonUtils.getLanguageJsonObject("alg.data.noProductionReason.doubleNoSingle").toString();
    }
}
