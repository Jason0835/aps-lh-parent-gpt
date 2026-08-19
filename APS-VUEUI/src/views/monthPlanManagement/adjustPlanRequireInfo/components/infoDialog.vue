<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="760px"
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
      label-width="140px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { addAdjustPlanRequireInfo, updateAdjustPlanRequireInfo } from "@/api/monthplan/adjustPlanRequireInfo";
import infoForm from "@/views/components/infoForm.vue";
import structureSelect from "@/views/components/structureSelect.vue";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";

export default {
  components: { infoForm, structureSelect, materialCodeSelect },
  inject: ["parentDict"],
  data() {
    const requiredSelect = {
      required: true,
      message: this.$t("common.rule.select"),
      trigger: "change",
    };
    const requiredInput = {
      required: true,
      message: this.$t("common.rule.input"),
      trigger: "blur",
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
        factoryCode: "116",
        monthPlanQty: undefined,
        adjustPlanQty: undefined,
        adjustFinalQty: undefined,
      },
      rules: {
        factoryCode: [requiredSelect],
        locationType: [requiredSelect],
        adjustDate: [requiredSelect],
        area: [requiredInput],
        planAdjustType: [requiredSelect],
      },
    };
  },
  computed: {
    title() {
      return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add");
    },
    filterAdjustReasons() {
      const reasons = this.parentDict.type.biz_adjust_reason || [];
      if (!this.form.planAdjustType) {
        return reasons;
      }
      return reasons.filter((d) => d.value && d.value.startsWith(this.form.planAdjustType));
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          disabled: this.isEdit,
          listeners: {
            change: () => {
              // 切换工厂后重置结构/物料选择
              this.$set(this.form, "structureName", "");
              this.$set(this.form, "materialCode", "");
              this.$set(this.form, "materialDesc", "");
            },
          },
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.locationType"),
          type: "select",
          dictData: this.parentDict.type.biz_stor_type,
        },
        {
          prop: "adjustDate",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustDate"),
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "area",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.area"),
        },
        {
          prop: "planAdjustType",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.planAdjustType"),
          type: "select",
          dictData: this.parentDict.type.biz_plan_adjust_type,
        },
        {
          prop: "adjustReason",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustReason"),
          type: "select",
          dictData: this.filterAdjustReasons,
          filterable: true,
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.structureName"),
          render: (form) => {
            return (
              <structureSelect
                key={form.structureName}
                v-model={form.structureName}
                factoryCode={form.factoryCode || "116"}
                machineType="CX"
                clearable
                onChange={this.handleStructureChange}
              />
            );
          },
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.materialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.materialCode}
                v-model={form.materialCode}
                structureName={form.structureName}
                disabled={!form.structureName}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.materialDesc"),
          disabled: true,
        },
        {
          prop: "monthPlanQty",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.monthPlanQty"),
          type: "number",
          precision: 0,
          listeners: {
            change: (val) => {
              this.$set(this.form, "monthPlanQty", val);
              this.calculateAdjustFinalQty();
            },
            input: (val) => {
              this.$set(this.form, "monthPlanQty", val);
              this.calculateAdjustFinalQty();
            },
          },
        },
        {
          prop: "adjustPlanQty",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustPlanQty"),
          type: "number",
          precision: 0,
          listeners: {
            change: (val) => {
              this.$set(this.form, "adjustPlanQty", val);
              this.calculateAdjustFinalQty();
            },
            input: (val) => {
              this.$set(this.form, "adjustPlanQty", val);
              this.calculateAdjustFinalQty();
            },
          },
        },
        {
          prop: "adjustFinalQty",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustFinalQty"),
          type: "number",
          precision: 0,
          disabled: true,
        },
        {
          prop: "realAdjustQty",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.realAdjustQty"),
          type: "number",
          precision: 0,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 900,
        },
      ];
    },
  },
  watch: {
    "form.monthPlanQty"() {
      this.calculateAdjustFinalQty();
    },
    "form.adjustPlanQty"() {
      this.calculateAdjustFinalQty();
    },
  },
  methods: {
    /** 调整后计划量 = 本月计划产量 + 调整数量（调整数量可为负数：正数追加、负数调减）。 */
    calculateAdjustFinalQty() {
      const monthPlanQty =
        this.form.monthPlanQty !== undefined &&
        this.form.monthPlanQty !== null &&
        this.form.monthPlanQty !== ""
          ? Number(this.form.monthPlanQty)
          : 0;
      const adjustPlanQty =
        this.form.adjustPlanQty !== undefined &&
        this.form.adjustPlanQty !== null &&
        this.form.adjustPlanQty !== ""
          ? Number(this.form.adjustPlanQty)
          : 0;
      this.$set(this.form, "adjustFinalQty", monthPlanQty + adjustPlanQty);
    },
    /** 选择产品结构后清空原物料信息，避免结构与物料不匹配。 */
    handleStructureChange(val, row) {
      this.$set(this.form, "structureName", val && row ? row.structureName : "");
      this.$set(this.form, "materialCode", "");
      this.$set(this.form, "materialDesc", "");
    },
    /** 选择物料编码后反显物料描述。 */
    handleMaterialCodeChange(val, row) {
      this.$set(this.form, "materialCode", val && row ? row.materialCode : "");
      this.$set(this.form, "materialDesc", val && row ? row.materialDesc || "" : "");
    },
    async save(params) {
      this.loading = true;
      try {
        const res = this.isEdit
          ? await updateAdjustPlanRequireInfo(params)
          : await addAdjustPlanRequireInfo(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          monthPlanQty: undefined,
          adjustPlanQty: undefined,
          adjustFinalQty: undefined,
          ...data,
        };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
          monthPlanQty: undefined,
          adjustPlanQty: undefined,
          adjustFinalQty: undefined,
        };
      }
      this.calculateAdjustFinalQty();
    },
    hide() {
      this.form = {
        factoryCode: "116",
        monthPlanQty: undefined,
        adjustPlanQty: undefined,
        adjustFinalQty: undefined,
      };
      this.$refs.form && this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.calculateAdjustFinalQty();
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
