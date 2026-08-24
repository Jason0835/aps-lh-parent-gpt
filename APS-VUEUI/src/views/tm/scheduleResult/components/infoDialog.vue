<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1000px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="160px"
      v-loading="loading"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import {changeQty, insertTask} from "@/api/tm/scheduleResult";
import {resolveErrorMessage} from "@/utils/errorMessage";

const CHANGE_QTY_EDITABLE_FIELDS = [
  "class1PlanQty",
  "class1Analysis",
  "class2PlanQty",
  "class2Analysis",
  "class3PlanQty",
  "class3Analysis",
  "class4PlanQty",
  "class4Analysis",
  "class5PlanQty",
  "class5Analysis",
  "class6PlanQty",
  "class6Analysis",
];

const INSERT_TASK_BASE_FIELDS = [
  "factoryCode",
  "scheduleDate",
  "machineCode",
  "treadCode",
  "remark",
];

const INSERT_SHIFT_COUNT = 6;

export default {
  components: { infoForm },
  inject: ["parentDict"],
  props: {
    machineOptions: {
      type: Array,
      default: () => [],
    },
    shiftDateList: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      originalForm: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        treadCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("ui.data.column.scheduleResult.changePlan")
          : this.$t("ui.data.column.scheduleResult.insertOrder")) +
        this.$t("ui.data.column.tm.scheduleResult.modelName")
      );
    },
    columns() {
      if (!this.isEdit) {
        const baseColumns = [
          {
            prop: "factoryCode",
            label: this.$t("ui.data.column.tm.scheduleResult.factoryCode"),
            type: "select",
            span: 12,
            dictData: this.parentDict.type.biz_factory_name,
            filterable: true,
            listeners: {
              change: this.handleFactoryChange,
            },
          },
          {
            prop: "scheduleDate",
            label: this.$t("ui.data.column.tm.scheduleResult.scheduleDate"),
            type: "date",
            span: 12,
            valueFormat: "yyyy-MM-dd",
            listeners: {
              change: this.handleScheduleDateChange,
            },
          },
          {
            prop: "machineCode",
            label: this.$t("ui.data.column.tm.scheduleResult.machineCode"),
            span: 12,
            type: "select",
            dictData: this.machineOptions,
            props: {
              label: "machineCode",
              value: "machineCode",
            },
            filterable: true,
          },
          {
            prop: "treadCode",
            label: this.$t("ui.data.column.tm.scheduleResult.treadCode"),
            span: 12,
            maxlength: 50,
          },
          {
            prop: "remark",
            label: this.$t("ui.data.column.tm.scheduleResult.remark"),
            span: 24,
            type: "textarea",
            rows: 3,
            maxlength: 500,
          },
        ];
        const shiftColumns = Array.from({length: INSERT_SHIFT_COUNT}, (item, index) => {
          const shiftOrder = index + 1;
          const fieldPrefix = `class${shiftOrder}`;
          const shiftDisabled = this.isShiftStarted(shiftOrder);
          return [
            {
              label: this.getShiftTitle(shiftOrder),
              span: 24,
              type: "title",
            },
            {
              prop: `${fieldPrefix}PlanQty`,
              label: this.$t(`ui.data.column.tm.scheduleResult.${fieldPrefix}PlanQty`),
              span: 8,
              type: "number",
              min: 0,
              disabled: shiftDisabled,
            },
            {
              prop: `${fieldPrefix}Sequence`,
              label: this.$t(`ui.data.column.tm.scheduleResult.${fieldPrefix}Sequence`),
              span: 8,
              type: "number",
              min: 1,
              precision: 0,
              disabled: shiftDisabled,
            },
            {
              prop: `${fieldPrefix}Analysis`,
              label: this.$t(`ui.data.column.tm.scheduleResult.${fieldPrefix}Analysis`),
              span: 8,
              maxlength: 200,
              disabled: shiftDisabled,
            },
          ];
        }).reduce((columns, shiftColumn) => columns.concat(shiftColumn), []);
        return baseColumns.slice(0, 4).concat(shiftColumns, baseColumns.slice(4));
      }
      const columns = [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.scheduleResult.factoryCode"),
          type: "select",
          span: 12,
          required: true,
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "batchNo",
          label: this.$t("ui.data.column.tm.scheduleResult.batchNo"),
          span: 12,
          maxlength: 50,
          disabled: this.isEdit,
        },
        {
          prop: "orderNo",
          label: this.$t("ui.data.column.tm.scheduleResult.orderNo"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "scheduleDate",
          label: this.$t("ui.data.column.tm.scheduleResult.scheduleDate"),
          type: "date",
          span: 12,
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tm.scheduleResult.machineCode"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "treadCode",
          label: this.$t("ui.data.column.tm.scheduleResult.treadCode"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "glueCode",
          label: this.$t("ui.data.column.tm.scheduleResult.glueCode"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "wholeGlueCode",
          label: this.$t("ui.data.column.tm.scheduleResult.wholeGlueCode"),
          span: 12,
          maxlength: 100,
        },
        {
          prop: "glueSeq",
          label: this.$t("ui.data.column.tm.scheduleResult.glueSeq"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "mouthPlateCode",
          label: this.$t("ui.data.column.tm.scheduleResult.mouthPlateCode"),
          span: 12,
          maxlength: 50,
        },
        {
          prop: "releaseStatus",
          label: this.$t("ui.data.column.tm.scheduleResult.releaseStatus"),
          span: 12,
          type: "select",
          dictData: this.parentDict.type.IS_RELEASE,
          filterable: true,
        },
        {
          prop: "dataSource",
          label: this.$t("ui.data.column.tm.scheduleResult.dataSource"),
          span: 12,
          type: "select",
          dictData: this.parentDict.type.tm_data_source,
          filterable: true,
        },
        {
          prop: "tailFlag",
          label: this.$t("ui.data.column.tm.scheduleResult.tailFlag"),
          span: 12,
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
          filterable: true,
        },
        // 1班：顺序、计划量、完成量、原因分析
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class1Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class1Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class1Sequence"),
          span: 8,
          type: "number",
        },
        {
          prop: "class1StartTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class1StartTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class1EndTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class1EndTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class1PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class1PlanQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class1FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class1FinishQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class1Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class1Analysis"),
          span: 8,
          maxlength: 200,
        },
        // 2班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class2Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class2Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class2Sequence"),
          span: 8,
          type: "number",
        },
        {
          prop: "class2StartTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class2StartTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class2EndTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class2EndTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class2PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class2PlanQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class2FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class2FinishQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class2Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class2Analysis"),
          span: 8,
          maxlength: 200,
        },
        // 3班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class3Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class3Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class3Sequence"),
          span: 8,
          type: "number",
        },
        {
          prop: "class3StartTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class3StartTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class3EndTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class3EndTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class3PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class3PlanQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class3FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class3FinishQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class3Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class3Analysis"),
          span: 8,
          maxlength: 200,
        },
        // 4班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class4Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class4Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class4Sequence"),
          span: 8,
          type: "number",
        },
        {
          prop: "class4StartTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class4StartTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class4EndTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class4EndTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class4PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class4PlanQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class4FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class4FinishQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class4Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class4Analysis"),
          span: 8,
          maxlength: 200,
        },
        // 5班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class5Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class5Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class5Sequence"),
          span: 8,
          type: "number",
        },
        {
          prop: "class5StartTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class5StartTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class5EndTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class5EndTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class5PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class5PlanQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class5FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class5FinishQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class5Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class5Analysis"),
          span: 8,
          maxlength: 200,
        },
        // 6班
        {
          label: this.$t("ui.data.column.tm.scheduleResult.class6Sequence"),
          span: 24,
          type: "group",
        },
        {
          prop: "class6Sequence",
          label: this.$t("ui.data.column.tm.scheduleResult.class6Sequence"),
          span: 8,
          type: "number",
        },
        {
          prop: "class6StartTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class6StartTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class6EndTime",
          label: this.$t("ui.data.column.tm.scheduleResult.class6EndTime"),
          span: 8,
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "class6PlanQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class6PlanQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class6FinishQty",
          label: this.$t("ui.data.column.tm.scheduleResult.class6FinishQty"),
          span: 8,
          type: "number",
        },
        {
          prop: "class6Analysis",
          label: this.$t("ui.data.column.tm.scheduleResult.class6Analysis"),
          span: 8,
          maxlength: 200,
        },
      ];
      return columns.map((column) => ({
        ...column,
        disabled:
          this.isEdit && !CHANGE_QTY_EDITABLE_FIELDS.includes(column.prop)
            ? true
            : column.disabled,
      }));
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const changedShiftOrders = this.isEdit ? this.getChangedShiftOrders(params) : [];
        if (this.isEdit && changedShiftOrders.length !== 1) {
          this.$modal.alertWarning(this.$t("ui.data.alert.tm.schedule.changeQty.singleShiftOnly"));
          this.loading = false;
          return;
        }
        const requestParams = this.isEdit
          ? this.buildChangeQtyRequest(params, changedShiftOrders[0])
          : INSERT_TASK_BASE_FIELDS
          .concat(Array.from({length: INSERT_SHIFT_COUNT}, (item, index) => {
            const fieldPrefix = `class${index + 1}`;
            return [`${fieldPrefix}PlanQty`, `${fieldPrefix}Sequence`, `${fieldPrefix}Analysis`];
          }).reduce((fields, shiftFields) => fields.concat(shiftFields), []))
          .reduce((result, fieldName) => {
            result[fieldName] = params[fieldName];
            return result;
          }, {});
        const res = this.isEdit
          ? await changeQty(requestParams)
          : await insertTask(requestParams);
        this.$emit("success", res);
        this.hide();
        this.loading = false;
      } catch (error) {
        this.$modal.alertError(resolveErrorMessage(
          error,
          this.$t("ui.data.column.tm.scheduleResult.operationFailed")
        ));
        this.loading = false;
      }
    },

    /**
     * 比较编辑前后的计划量和原因分析，确定唯一调量班次。
     *
     * @param {Object} params 编辑后的表单参数
     * @returns {Array<Number>} 发生变化的班次序号
     */
    getChangedShiftOrders(params) {
      return Array.from({length: INSERT_SHIFT_COUNT}, (item, index) => index + 1)
        .filter((shiftOrder) => {
          const fieldPrefix = `class${shiftOrder}`;
          const oldPlanQty = Number(this.originalForm[`${fieldPrefix}PlanQty`] || 0);
          const newPlanQty = Number(params[`${fieldPrefix}PlanQty`] || 0);
          const oldAnalysis = this.originalForm[`${fieldPrefix}Analysis`] || "";
          const newAnalysis = params[`${fieldPrefix}Analysis`] || "";
          return oldPlanQty !== newPlanQty || oldAnalysis !== newAnalysis;
        });
    },

    /**
     * 构造只包含调量目标班次字段的请求，避免提交顺序、完成量和时间等只读字段。
     *
     * @param {Object} params 编辑后的表单参数
     * @param {Number} shiftOrder 调量目标班次
     * @returns {Object} TM 调量请求
     */
    buildChangeQtyRequest(params, shiftOrder) {
      const fieldPrefix = `class${shiftOrder}`;
      return {
        id: params.id,
        shiftOrder,
        [`${fieldPrefix}PlanQty`]: params[`${fieldPrefix}PlanQty`],
        [`${fieldPrefix}Analysis`]: params[`${fieldPrefix}Analysis`],
      };
    },

    show(data) {
      this.visible = true;
      if (data && data.id) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
        this.originalForm = {
          ...data,
        };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: (data && data.factoryCode) || "116",
          scheduleDate: data && data.scheduleDate,
        };
        this.originalForm = {};
      }
    },
    /**
     * 获取与列表页一致的班次分组标题。
     *
     * @param {Number} shiftOrder 班次顺序
     * @returns {String} 班次名称和日期
     */
    getShiftTitle(shiftOrder) {
      const shiftDate = this.shiftDateList[shiftOrder - 1] || {};
      const shiftNameMap = {
        night: this.$t("ui.data.column.scheduleResult.nightShift"),
        morning: this.$t("ui.data.column.scheduleResult.morningShift"),
        afternoon: this.$t("ui.data.column.scheduleResult.middleShift"),
      };
      const shiftName = shiftNameMap[shiftDate.shiftType]
        || this.$t(`ui.data.column.scheduleResult.class${shiftOrder}Shift`);
      return `${shiftName} ${shiftDate.shiftDate || ""}`.trim();
    },
    /**
     * 判断班次开始时间是否已经到达。
     *
     * @param {Number} shiftOrder 班次顺序
     * @returns {Boolean} true 表示班次已开始，表单字段应置灰
     */
    isShiftStarted(shiftOrder) {
      const shiftDate = this.shiftDateList[shiftOrder - 1] || {};
      const startTime = shiftDate.shiftStartTime;
      if (!startTime) {
        return false;
      }
      const normalizedStartTime = typeof startTime === "string"
        ? startTime.replace(" ", "T")
        : startTime;
      const timestamp = new Date(normalizedStartTime).getTime();
      return Number.isFinite(timestamp) && timestamp <= Date.now();
    },
    /**
     * 清除已开始班次的输入值，避免日期切换后误提交旧班次数据。
     *
     * @returns {void}
     */
    clearStartedShiftValues() {
      for (let shiftOrder = 1; shiftOrder <= INSERT_SHIFT_COUNT; shiftOrder += 1) {
        if (!this.isShiftStarted(shiftOrder)) {
          continue;
        }
        ["PlanQty", "Sequence", "Analysis"].forEach((suffix) => {
          this.$set(this.form, `class${shiftOrder}${suffix}`, undefined);
        });
      }
    },
    /**
     * 工厂变化时清空旧机台和班次输入，并通知父页面刷新关联数据。
     *
     * @param {String} factoryCode 新工厂编码
     * @returns {void}
     */
    handleFactoryChange(factoryCode) {
      this.$set(this.form, "machineCode", undefined);
      this.$emit("scope-change", {
        factoryCode,
        scheduleDate: this.form.scheduleDate,
      });
    },
    /**
     * 排程日期变化时清空旧班次输入，并通知父页面刷新六班日期。
     *
     * @param {String} scheduleDate 新排程日期
     * @returns {void}
     */
    handleScheduleDateChange(scheduleDate) {
      for (let shiftOrder = 1; shiftOrder <= INSERT_SHIFT_COUNT; shiftOrder += 1) {
        ["PlanQty", "Sequence", "Analysis"].forEach((suffix) => {
          this.$set(this.form, `class${shiftOrder}${suffix}`, undefined);
        });
      }
      this.$emit("scope-change", {
        factoryCode: this.form.factoryCode,
        scheduleDate,
      });
    },
    hide() {
      this.form = {};
      this.originalForm = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      if (!this.isEdit && !this.validateInsertShiftFields()) {
        return;
      }
      this.$refs.form.triggerConfirm(this.save);
    },
    validateInsertShiftFields() {
      this.clearStartedShiftValues();
      let hasPlanQty = false;
      for (let shiftOrder = 1; shiftOrder <= INSERT_SHIFT_COUNT; shiftOrder += 1) {
        const planQty = this.form[`class${shiftOrder}PlanQty`];
        const sequence = this.form[`class${shiftOrder}Sequence`];
        const analysis = this.form[`class${shiftOrder}Analysis`];
        const hasPlanValue = planQty !== null && planQty !== undefined && planQty !== "";
        const hasSequence = sequence !== null && sequence !== undefined && sequence !== "";
        const hasAnalysis = typeof analysis === "string" && analysis.trim().length > 0;
        if (!hasPlanValue) {
          if (hasSequence || hasAnalysis) {
            this.$modal.alertWarning(this.$t("ui.tm.schedule.insert.shiftPairRequired"));
            return false;
          }
          continue;
        }
        if (Number(planQty) <= 0 || !hasSequence || Number(sequence) < 1 || !Number.isInteger(Number(sequence))) {
          this.$modal.alertWarning(this.$t("ui.tm.schedule.insert.shiftPairRequired"));
          return false;
        }
        hasPlanQty = true;
      }
      if (!hasPlanQty) {
        this.$modal.alertWarning(this.$t("ui.tm.schedule.insert.planQtyRequired"));
        return false;
      }
      return true;
    },
  },
};
</script>
