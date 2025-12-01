package com.zlt.mix.schedule.engine.constants;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 终炼母炼排程算法常量
 * 
 * @author hakimryan
 *
 */
public class GlueEngineConstants {
	/**
	 * 配方类型
	 */
	public static final String RECIPE_TYPE_C2Z = "C2Z"; // 掺胶
	public static final String RECIPE_TYPE_ZZ = "ZZ"; // 正正
	public static final String RECIPE_TYPE_S = "S"; // 试制
	public static final String RECIPE_TYPE_CS = "CS"; // 掺胶试制
	public static final String RECIPE_TYPE_F = "F"; // 返炼
	public static final String RECIPE_TYPE_X = "X"; // 洗车
	public static final String RECIPE_TYPE_PTZ = "PTZ"; // M5特殊胶
	
	/**
	 * 物料类型
	 */
	public static final String MAJOR_TYPE_XL = "2"; // 小料
	public static final String MAJOR_TYPE_SL = "3"; // 塑炼胶
	public static final String MAJOR_TYPE_ML = "4"; // 母炼胶
	public static final String MAJOR_TYPE_ZL = "5"; // 终炼胶
	public static final String MAJOR_TYPE_FH = "6"; // 返回胶
	public static final String MAJOR_TYPE_BHG = "B"; // 不合格胶
	public static final String MAJOR_TYPE_WASH = "X"; // 洗胶
	public static final String MAJOR_TYPE_MIX = "C"; // 掺胶
	
	/**
	 * 班次
	 */
	public static final int SHIFT_CLASS_MID = 1; // 中班
	public static final int SHIFT_CLASS_NIGHT = 2; // 夜班
	public static final int SHIFT_CLASS_DAY = 3; // 白班
	
	/**
	 * 机台状态
	 */
	public static final String MACHINE_STATE_OFF = "0"; // 关机，不排产
	public static final String MACHINE_STATE_ON = "1"; // 在产
	public static final String MACHINE_STATE_WAIT = "2"; // 等待排产
	
	/**
	 * 是否
	 */
	public static final String YES_OR_NO_NO_0 = "0"; // 否
	public static final String YES_OR_NO_YES_1 = "1"; // 是
	
	/**
	 * 待支领量使用导入数据：是
	 */
	public static final String GLUE_UNCLAIMED_IMPORT_YES = "1";
	
	/**
	 * 1000，用于毫秒或者毫米的换算
	 */
	public static final BigDecimal THOUSAND = new BigDecimal("1000");

	// 排程系统参数KEY
	/**
	 * 单班单规格最大排产数
	 */
	public static final String MAX_PRODUCT_QTY = "MAX_PRODUCT_QTY";
	/**
	 * 单班单规格母胶最大排产数
	 */
	public static final String MAX_PRODUCT_ML_QTY = "MAX_PRODUCT_ML_QTY";
	/**
	 * 单班单规格最大排产数超范围比率
	 */
	public static final String MAX_PRODUCT_RATE = "MAX_PRODUCT_RATE";
	/**
	 * 单班单规格母胶最大排产数超范围比率
	 */
	public static final String MAX_PRODUCT_ML_RATE = "MAX_PRODUCT_ML_RATE";
	/**
	 * 单规格最小排产数
	 */
	public static final String MIN_PRODUCT_STOCK = "MIN_PRODUCT_STOCK";
	/**
	 * 密炼间隔时间
	 */
	public static final String MIX_INTERVAL_TIME = "MIX_INTERVAL_TIME";
	/**
	 * 切换排程间隔时间（秒）
	 */
	public static final String SCHEDULE_SWITCH_TIME = "SCHEDULE_SWITCH_TIME";
	/**
	 * 用餐时间（分钟）
	 */
	public static final String DINNER_TIME = "DINNER_TIME";
	/**
	 * 待支领量使用导入数据
	 */
	public static final String GLUE_UNCLAIMED_IMPORT = "GLUE_UNCLAIMED_IMPORT";

	/**
	 * 当天排程需要加上昨日剩余量
	 */
	public static final String IS_ADD_LAST_SURPLUS = "IS_ADD_LAST_SURPLUS";
	/**
	 * 首批完成冷却车数
	 */
	public static final String FIRST_BATCH_GLUE_NUM = "FIRST_BATCH_GLUE_NUM";
	/**
	 * 小批量生产数
	 */
	public static final String SMALL_BATCH_NUM = "SMALL_BATCH_NUM";
	/**
	 * 胶料自动补量开关
	 */
	public static final String GLUE_SUPPLEMENT_SWITCH = "GLUE_SUPPLEMENT_SWITCH";

	/**
	 * 需考虑排程的物料类型：终炼胶、母炼胶
	 */
	public static final List<String> SCHEDULE_MAJOR_TYPE = Arrays.asList(new String[] { MAJOR_TYPE_ZL, MAJOR_TYPE_ML });
	/**
	 * 需考虑掺胶的物料类型：返回胶、不合格胶、洗胶、掺胶
	 */
	public static final List<String> MIX_MAJOR_TYPE = Arrays
			.asList(new String[] { MAJOR_TYPE_FH, MAJOR_TYPE_BHG, MAJOR_TYPE_WASH, MAJOR_TYPE_MIX });
	/**
	 * 需考虑库存消耗的物料类型：返回胶、不合格胶、母炼胶、洗胶
	 */
	public static final List<String> STOCK_MAJOR_TYPE = Arrays
			.asList(new String[] { MAJOR_TYPE_FH, MAJOR_TYPE_BHG, MAJOR_TYPE_ML, MAJOR_TYPE_WASH });

	/**
	 * 是否高能耗(对应数据字典，ISORNOT，0-是，1-否)
	 */
	public static final String ISORNOT_YES = "0"; // 是
	public static final String ISORNOT_NO = "1"; // 否

	/**
	 * 优先高耗能开始时间
	 */
	public static final String HIGH_CONSUMPTION_BEGIN = "HIGH_CONSUMPTION_BEGIN";
	/**
	 * 默认优先高耗能开始时间
	 */
	public static final String DEFAULT_HIGH_CONSUMPTION_BEGIN = "0";
	/**
	 * 优先高耗能结束时间
	 */
	public static final String HIGH_CONSUMPTION_END = "HIGH_CONSUMPTION_END";
	/**
	 * 默认优先高耗能结束时间
	 */
	public static final String DEFAULT_HIGH_CONSUMPTION_END = "0";

	/**
	 * 母炼默认日用量备库倍数
	 */
	public static final double ML_DEFAULT_DAILY_DOSE_STOCK_RATE = 2.5;
	/**
	 * 母炼日用量备库倍数
	 */
	public static final String ML_DAILY_DOSE_STOCK_RATE = "ML_DAILY_DOSE_STOCK_RATE";
	/**
	 * 终炼默认日用量备库倍数
	 */
	public static final double ZL_DEFAULT_DAILY_DOSE_STOCK_RATE = 1.5;
	/**
	 * 终炼日用量备库倍数
	 */
	public static final String ZL_DAILY_DOSE_STOCK_RATE = "ZL_DAILY_DOSE_STOCK_RATE";
	/**
	 * 生产量凑整数
	 */
	public static final double PRODUCE_QTY_ROUND_UP = 5D;

}
