<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
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
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import {
  insertOrder,
  validateInsertOrder,
  selectListMdmProductConstruction,
  getScheduleMachineInfo,
  getScheduleDate,
  getSkuRelatedData,
} from "@/api/lh/scheduleResult";
export default {
  components: { infoForm, materialCodeSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      sapCodeList: [],
      curingMachines: [],
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        specCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        productCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
      },
      dateList: [
        { shift: 1, shiftDate: "" },
        { shift: 2, shiftDate: "" },
        { shift: 3, shiftDate: "" },
        { shift: 4, shiftDate: "" },
        { shift: 5, shiftDate: "" },
        { shift: 6, shiftDate: "" },
        { shift: 7, shiftDate: "" },
        { shift: 8, shiftDate: "" },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.scheduleResult.insertOrder");
    },
    columns() {
      const columns = [
        {
          type: "title",
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
        },
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          clearable: false,
          listeners: {
            change: this.handleScheduleDateChange,
          },
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.lhScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
          listeners: {
            change: this.handleScheduleDateChange,
          },
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.lhScheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          type: "select",
          dictData: this.curingMachines,
          filterable: true,
          disabled: true,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.lhScheduleResult.materialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.materialCode}
                v-model={form.materialCode}
                scheduleDate={form.scheduleDate}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhScheduleResult.materialDesc"),
          prop: "materialDesc",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.totalSurplusQty"),
          prop: "mouldSurplusQty",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.lhScheduleResult.embryoStock"),
          prop: "embryoStock",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhShiftQty"),
          prop: "machineShiftCapacity",
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.lhScheduleResult.constructionStage"),
          prop: "trialStatus",
          type: "select",
          dictData: this.parentDict.type.lh_trial_status,
          disabled: true,
          span: 12,
        },
        {
          label: this.$t("ui.data.column.lhScheduleResult.leftRightMould"),
          prop: "leftRightMould",
          disabled: true,
          span: 12,
        },
      ];

      for (let i = 1; i <= 8; i += 1) {
        columns.push(
          { type: "title", label: this.shiftBannerTitle(i) },
          {
            label: this.$t("ui.data.column.scheduleResult.plan"),
            prop: `class${i}PlanQty`,
            span: 12,
            type: "number",
          },
          {
            label: this.$t("ui.data.column.scheduleResult.analysis"),
            prop: `class${i}Analysis`,
            span: 12,
          }
        );
      }
      return columns;
    },
  },

  methods: {
    /** 与列表 curingSchedule：class1～8 对应 早/中/夜 轮换 */
    shiftPeriodShortKey(classIndex) {
      const KEYS = [, "morningShift", "middleShift", "nightShift", "morningShift", "middleShift", "nightShift", "morningShift", "middleShift"];
      return KEYS[classIndex];
    },
    shiftPeriodNameOnly(classIndex) {
      return this.$t(`ui.data.column.scheduleResult.${this.shiftPeriodShortKey(classIndex)}`);
    },
    shiftBannerTitle(classIndex) {
      const i = classIndex - 1;
      const dateStr = this.dateList[i]?.shiftDate ?? "";
      const label = this.shiftPeriodNameOnly(classIndex);
      return dateStr ? `${label} ${dateStr}` : label;
    },
    async fetchScheduleShiftDates(scheduleDate) {
      const empty = [
        { shift: 1, shiftDate: "" },
        { shift: 2, shiftDate: "" },
        { shift: 3, shiftDate: "" },
        { shift: 4, shiftDate: "" },
        { shift: 5, shiftDate: "" },
        { shift: 6, shiftDate: "" },
        { shift: 7, shiftDate: "" },
        { shift: 8, shiftDate: "" },
      ];
      if (!scheduleDate) {
        this.dateList = empty;
        return;
      }
      try {
        const res = await getScheduleDate({ scheduleDate });
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
    handleMaterialCodeChange(val, row) {
      if (val) {
        this.$set(this.form, "materialDesc", row.materialDesc);
        this.$set(this.form, "mouldSurplusQty", null);
        this.$set(this.form, "embryoStock", null);
        this.$set(this.form, "machineShiftCapacity", null);
        this.loadSkuRelatedData(val);
      } else {
        this.$set(this.form, "materialDesc", "");
        this.$set(this.form, "mouldSurplusQty", null);
        this.$set(this.form, "embryoStock", null);
        this.$set(this.form, "machineShiftCapacity", null);
        this.$set(this.form, "trialStatus", null);
        this.$set(this.form, "leftRightMould", null);
      }
    },
    async loadSkuRelatedData(materialCode) {
      if (!materialCode || !this.form.factoryCode || !this.form.scheduleDate) {
        return;
      }
      try {
        const params = {
          factoryCode: this.form.factoryCode,
          materialCode: materialCode,
          scheduleDate: this.form.scheduleDate,
          lhMachineCode: this.form.lhMachineCode,
          leftRightMold: this.form.leftRightMould,
        };
        const res = await getSkuRelatedData(params);
        if (res) {
          if (res.mouldSurplusQty != null) {
            this.$set(this.form, "mouldSurplusQty", res.mouldSurplusQty);
          }
          if (res.embryoStock != null) {
            this.$set(this.form, "embryoStock", res.embryoStock);
          }
          if (res.machineShiftCapacity != null) {
            this.$set(this.form, "machineShiftCapacity", res.machineShiftCapacity);
          }
          this.$set(this.form, "trialStatus", res.trialStatus || null);
          this.$set(this.form, "leftRightMould", res.leftRightMould || null);
          if (res.embryoCode != null && res.embryoCode !== '') {
            this.$set(this.form, "embryoCode", res.embryoCode);
          }
          if (res.mainMaterialDesc != null && res.mainMaterialDesc !== '') {
            this.$set(this.form, "mainMaterialDesc", res.mainMaterialDesc);
          }
          if (res.monthPlanVersion != null && res.monthPlanVersion !== '') {
            this.$set(this.form, "monthPlanVersion", res.monthPlanVersion);
          }
          if (res.productionVersion != null && res.productionVersion !== '') {
            this.$set(this.form, "productionVersion", res.productionVersion);
          }
          if (res.specCode != null && res.specCode !== '') {
            this.$set(this.form, "specCode", res.specCode);
          }
          if (res.structureName != null && res.structureName !== '') {
            this.$set(this.form, "structureName", res.structureName);
          }
          if (res.mouldCode != null && res.mouldCode !== '') {
            this.$set(this.form, "mouldCode", res.mouldCode);
          }
          if (res.errorMessages && res.errorMessages.length > 0) {
            this.$modal.msgError(res.errorMessages.join('\n'));
          }
          if (res.warningMessages && res.warningMessages.length > 0) {
            this.$modal.msgWarning(res.warningMessages.join('\n'));
          }
        }
      } catch (error) {
        console.error("获取SKU关联数据失败:", error);
      }
    },
    decodeRemark(remark) {
      if (!remark) return remark;
      return remark
        .replace(/__PERCENT__/g, "%")
        .replace(/__AMP__/g, "&")
        .replace(/__LT__/g, "<")
        .replace(/__GT__/g, ">")
        .replace(/__QUOT__/g, '"')
        .replace(/__APOS__/g, "'");
    },
    encodeRemark(remark) {
      if (!remark) return remark;
      return remark
        .replace(/%/g, '__PERCENT__')
        .replace(/&/g, '__AMP__')
        .replace(/</g, '__LT__')
        .replace(/>/g, '__GT__')
        .replace(/"/g, '__QUOT__')
        .replace(/'/g, '__APOS__');
    },
    async save(params) {
      try {
        this.loading = true;
        const saveParams = {
          ...params,
          class1Analysis: this.encodeRemark(params.class1Analysis),
          class2Analysis: this.encodeRemark(params.class2Analysis),
          class3Analysis: this.encodeRemark(params.class3Analysis),
          class4Analysis: this.encodeRemark(params.class4Analysis),
          class5Analysis: this.encodeRemark(params.class5Analysis),
          class6Analysis: this.encodeRemark(params.class6Analysis),
          class7Analysis: this.encodeRemark(params.class7Analysis),
          class8Analysis: this.encodeRemark(params.class8Analysis),
        };
        const validateRes = await validateInsertOrder(saveParams);
        if (validateRes.valid) {
          if (validateRes.machineShiftCapacity != null) {
            this.$set(this.form, "machineShiftCapacity", validateRes.machineShiftCapacity);
          }
          if (validateRes.mouldSurplusQty != null) {
            this.$set(this.form, "mouldSurplusQty", validateRes.mouldSurplusQty);
          }
          if (validateRes.embryoStock != null) {
            this.$set(this.form, "embryoStock", validateRes.embryoStock);
          }
          if (validateRes.leftRightMould != null) {
            this.$set(this.form, "leftRightMould", validateRes.leftRightMould);
          }
          if (validateRes.warningMessages && validateRes.warningMessages.length > 0) {
            const warningMsg = validateRes.warningMessages.join('\n');
            const confirmResult = await this.$confirm(warningMsg, this.$t('ui.data.column.lhScheduleResult.insertOrder.validateFail'), {
              confirmButtonText: this.$t('common.button.confirm'),
              cancelButtonText: this.$t('common.button.cancel'),
              type: 'warning',
            }).catch(() => false);
            if (!confirmResult) {
              return;
            }
          }
          const res = await insertOrder(saveParams);
          this.$modal.msgSuccess(res.msg);
          this.$emit("success");
          this.hide();
        } else {
          const errorMsg = validateRes.errorMessages ? validateRes.errorMessages.join('\n') : '校验失败';
          this.$modal.msgError(errorMsg);
        }
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    //utils
    async show(data) {
      this.visible = true;
      const nowDate = new Date();
      const tomorrow = new Date(nowDate);
      tomorrow.setDate(nowDate.getDate() + 1);
      const form = {
        factoryCode: "116",
        scheduleDate: tomorrow.toISOString().slice(0, 10),
      };
      if (data) {
        const keys = [
          "factoryCode",
          "scheduleDate",
          "lhMachineCode",
          "materialCode",
          "materialDesc",
          "mouldSurplusQty",
          "embryoStock",
          "machineShiftCapacity",
          "trialStatus",
          "leftRightMould",
          "embryoCode",
          "mainMaterialDesc",
          "monthPlanVersion",
          "productionVersion",
          "specCode",
          "structureName",
          "mouldCode",
        ];
        for (let i = 1; i <= 8; i++) {
          keys.push(`class${i}PlanQty`, `class${i}Analysis`);
        }
        keys.forEach((k) => {
          if (data[k] === undefined || data[k] === null) {
            return;
          }
          let v = data[k];
          if (/^class\d+Analysis$/.test(k)) {
            v = this.decodeRemark(String(v));
          }
          form[k] = v;
        });
        // 列表行数据中示方类型字段名为 changedTrialStatus，映射到表单的 trialStatus
        if (data.changedTrialStatus && !data.trialStatus) {
          form.trialStatus = data.changedTrialStatus;
        }
        if (data.trialStatus || data.changedTrialStatus) {
          form.originalTrialStatus = data.trialStatus || data.changedTrialStatus;
        }
        if (data.singleMouldShiftQty != null) {
          form.machineShiftCapacity = data.singleMouldShiftQty;
        }
      }
      this.form = form;
      await this.loadCuringMachinesDropdown();
      await this.fetchScheduleShiftDates(this.form.scheduleDate);
      if (!data?.lhMachineCode) {
        this.$set(this.form, "lhMachineCode", "");
      }
      // 有物料编码时主动加载SKU关联数据，确保示方类型等信息从后端获取最新值
      if (this.form.materialCode) {
        await this.loadSkuRelatedData(this.form.materialCode);
      }
    },
    hide() {
      this.form = {};
      this.dateList = [
        { shift: 1, shiftDate: "" },
        { shift: 2, shiftDate: "" },
        { shift: 3, shiftDate: "" },
        { shift: 4, shiftDate: "" },
        { shift: 5, shiftDate: "" },
        { shift: 6, shiftDate: "" },
        { shift: 7, shiftDate: "" },
        { shift: 8, shiftDate: "" },
      ];
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleScheduleDateChange() {
      this.$set(this.form, "lhMachineCode", "");
      this.loadCuringMachinesDropdown();
      this.fetchScheduleShiftDates(this.form.scheduleDate);
    },
    async handleSpecCodeChange() {
      if (!this.form.factoryCode ) {
        return;
      }
      this.sapCodeList = [];
      this.form.productCode = "";
      this.curingMachines = [];
      this.$set(this.form, 'lhMachineCode', "");

      try {
        const res = await selectListMdmProductConstruction({
          factoryCode: this.form.factoryCode,
          specCode: this.form.specCode,
        });
        this.sapCodeList = res;
      } catch (error) {
        this.sapCodeList = [];
      }
    },
    handleSpecCodeClear() {
      this.sapCodeList = [];
      this.form.productCode = "";
      this.curingMachines = [];
      this.$set(this.form, 'lhMachineCode', "");
    },
    async loadCuringMachinesDropdown() {
      if (!this.form.factoryCode || !this.form.scheduleDate) {
        this.curingMachines = [];
        return;
      }
      try {
        const res = await getScheduleMachineInfo({
          factoryCode: this.form.factoryCode,
          scheduleDate: this.form.scheduleDate,
        });
        this.curingMachines = (res || []).map((r) => ({
          label: r.machineCode,
          value: r.machineCode,
        }));
      } catch (error) {
        console.error(error);
        this.curingMachines = [];
      }
    },
    handleProductCodeChange() {
      this.handleScheduleDateChange();
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
