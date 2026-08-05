<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="800px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    append-to-body
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("ui.frame.btn.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ $t("ui.frame.btn.confirm") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import infoForm from "@/views/components/infoForm.vue";
import {
  insertTask,
  listScheduleShiftDates,
} from "@/api/gsq/scheduleResult";
import { listEnabledMachines } from "@/api/gsq/machine";

export default {
  name: "GsqInsertOrderDialog",
  components: { infoForm },
  dicts: ["biz_factory_name"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      loading: false,
      form: {},
      // 机台下拉数据源
      gsqMachines: [],
      // 6班次日期列表（D日中班/D+1日夜早中/D+2日夜早）
      dateList: [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ],
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("ui.data.column.gsqScheduleResult.factoryCodeRequired"),
            trigger: "change",
          },
        ],
        scheduleDateQuery: [
          {
            required: true,
            message: this.$t("ui.data.column.gsqScheduleResult.scheduleDateRequired"),
            trigger: "blur",
          },
        ],
        steelRingCode: [
          {
            required: true,
            message: this.$t("ui.data.column.gsqScheduleResult.steelRingCodeRequired"),
            trigger: "blur",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("ui.data.column.gsqScheduleResult.machineCodeRequired"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible;
      },
      set(val) {
        this.$emit("update:visible", val);
      },
    },
    title() {
      return this.$t("ui.data.column.gsqScheduleResult.insertOrderTitle");
    },
    columns() {
      const columns = [
        // 基础信息区
        {
          type: "title",
          label: this.$t("ui.data.column.gsqScheduleResult.baseInfo"),
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.scheduleDate"),
          prop: "scheduleDateQuery",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          format: "yyyy-MM-dd",
          clearable: false,
          listeners: {
            change: this.handleScheduleDateChange,
          },
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.steelRingCode"),
          prop: "steelRingCode",
          type: "input",
          listeners: {
            blur: this.handleSteelRingCodeBlur,
          },
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.twiningDiscCode"),
          prop: "twiningDiscCode",
          type: "input",
        },
        {
          label: this.$t("ui.data.column.gsqScheduleResult.machineCode"),
          prop: "machineCode",
          type: "select",
          dictData: this.gsqMachines,
          filterable: true,
          clearable: true,
        },
      ];

      // 动态生成6个班次表单区域（每个班次包含：顺序、计划量、原因分析）
      // 班次顺序：1班=D日中班，2/3/4班=D+1日夜早中，5/6班=D+2日夜早
      for (let i = 1; i <= 6; i++) {
        columns.push(
          { type: "title", label: this.shiftBannerTitle(i) },
          {
            label: this.$t("ui.data.column.gsqScheduleResult.sequence"),
            prop: `class${i}Sequence`,
            span: 8,
            type: "number",
            min: 0,
          },
          {
            label: this.$t("ui.data.column.gsqScheduleResult.planQty"),
            prop: `class${i}PlanQty`,
            span: 8,
            type: "number",
            min: 0,
          },
          {
            label: this.$t("ui.data.column.gsqScheduleResult.analysis"),
            prop: `class${i}Analysis`,
            span: 8,
            type: "input",
          }
        );
      }

      columns.push({
        label: this.$t("ui.data.column.gsqScheduleResult.anchorTaskId"),
        prop: "anchorTaskId",
        span: 24,
        placeholder: this.$t("ui.data.column.gsqScheduleResult.anchorTaskIdPlaceholder"),
      });

      columns.push({
        label: this.$t("ui.data.column.gsqScheduleResult.remark"),
        prop: "remark",
        type: "textarea",
        span: 24,
      });

      return columns;
    },
  },
  async created() {
    this.form = {
      factoryCode: "",
      scheduleDateQuery: moment().add(1, "days").format("YYYY-MM-DD"),
      steelRingCode: "",
      twiningDiscCode: "",
      machineCode: "",
      class1PlanQty: 0,
      class1Sequence: null,
      class1Analysis: "",
      class2PlanQty: 0,
      class2Sequence: null,
      class2Analysis: "",
      class3PlanQty: 0,
      class3Sequence: null,
      class3Analysis: "",
      class4PlanQty: 0,
      class4Sequence: null,
      class4Analysis: "",
      class5PlanQty: 0,
      class5Sequence: null,
      class5Analysis: "",
      class6PlanQty: 0,
      class6Sequence: null,
      class6Analysis: "",
      remark: "",
    };
    await this.loadMachines();
    await this.fetchScheduleShiftDates(this.form.scheduleDateQuery);
  },
  methods: {
    hide() {
      this.dialogVisible = false;
    },
    /** 班次名称映射（与胎圈口径一致） */
    shiftPeriodName(shiftType) {
      const map = {
        night: this.$t("ui.data.column.scheduleResult.nightShift"),
        morning: this.$t("ui.data.column.scheduleResult.morningShift"),
        afternoon: this.$t("ui.data.column.scheduleResult.middleShift"),
      };
      return map[shiftType] || "";
    },
    /** 班次标题：班次名称 + 班次日期（用于动态表单分隔标题） */
    shiftBannerTitle(classIndex) {
      const item = this.dateList[classIndex - 1];
      if (!item) return "";
      const shiftName = this.shiftPeriodName(item.shiftType);
      return shiftName + " " + (item.shiftDate || "");
    },
    /** 获取6班次日期列表（D日中班/D+1日夜早中/D+2日夜早） */
    async fetchScheduleShiftDates(scheduleDate) {
      const empty = [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ];
      if (!scheduleDate) {
        this.dateList = empty;
        return;
      }
      try {
        const res = await listScheduleShiftDates({ scheduleDateQuery: scheduleDate });
        if (Array.isArray(res) && res.length) {
          this.dateList = res;
        } else {
          this.dateList = empty;
        }
      } catch (error) {
        console.error(error);
        this.dateList = empty;
      }
    },
    /** 加载钢丝圈机台列表（仅启用状态），用于机台下拉框数据源 */
    async loadMachines() {
      try {
        const res = await listEnabledMachines();
        // 下拉显示机台编号
        this.gsqMachines = (res || []).map((r) => ({
          label: r.machineCode,
          value: r.machineCode,
        }));
      } catch (error) {
        console.error(error);
        this.gsqMachines = [];
      }
    },
    /** 排程日期变更：重新拉取6班次日期 */
    handleScheduleDateChange(val) {
      this.fetchScheduleShiftDates(val);
    },
    /** 钢丝圈代码失焦：触发前端规格校验（实时提示"钢丝圈规格有误"） */
    handleSteelRingCodeBlur() {
      if (!this.form.steelRingCode) return;
      // 调用后端校验接口（validateInsertOrder 会校验施工是否存在）
      // 此处仅做轻量级实时校验，最终校验在提交时执行
      validateInsertOrder(this.buildSubmitParams())
        .then((res) => {
          if (res.code !== 200 && res.msg) {
            this.$modal.msgWarning(res.msg);
          }
        })
        .catch(() => {});
    },
    /** 构建提交参数 */
    buildSubmitParams() {
      return { ...this.form };
    },
    /** 确认按钮：触发前端双向关联校验后提交 */
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.submit);
    },
    /** 提交插单 */
    submit() {
      // 前端预校验：6班次中至少一个班次计划量 > 0（夜班、中班、早班至少一个有效）
      const planQtys = [
        Number(this.form.class1PlanQty) || 0,
        Number(this.form.class2PlanQty) || 0,
        Number(this.form.class3PlanQty) || 0,
        Number(this.form.class4PlanQty) || 0,
        Number(this.form.class5PlanQty) || 0,
        Number(this.form.class6PlanQty) || 0,
      ];
      const hasAnyPlanQty = planQtys.some((q) => q > 0);
      if (!hasAnyPlanQty) {
        this.$modal.msgError(
          this.$t("ui.data.column.gsqScheduleResult.qtyCannotBeZero")
        );
        return;
      }

      // 前端预校验：班次与计划量双向关联性校验
      // 规则：有计划量的班次，其顺序必须有值；反之，有顺序的班次，其计划量也必须有值
      for (let i = 1; i <= 6; i++) {
        const planQty = Number(this.form[`class${i}PlanQty`]) || 0;
        const sequence = Number(this.form[`class${i}Sequence`]) || 0;
        if (planQty > 0 && sequence <= 0) {
          this.$modal.msgError(
            this.$t("ui.data.column.gsqScheduleResult.sequenceRequired", { shift: i })
          );
          return;
        }
        if (sequence > 0 && planQty <= 0) {
          this.$modal.msgError(
            this.$t("ui.data.column.gsqScheduleResult.planQtyRequired", { shift: i })
          );
          return;
        }
      }

      // 调用新接口 insertTask（走任务链路径，支持锚点插入、resequence 重排，内置校验）
      this.loading = true;
      const params = this.buildSubmitParams();
      insertTask(params)
        .then((res) => {
          const tip = res.msg || res.message || "";
          if (res.code != null && res.code !== 200) {
            this.$modal.msgError(tip || this.$t("ui.common.message.operateFail"));
            return;
          }
          this.$modal.msgSuccess(
            tip || this.$t("ui.data.column.gsqScheduleResult.insertOrderSuccess")
          );
          this.$emit("refresh");
          this.hide();
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>
