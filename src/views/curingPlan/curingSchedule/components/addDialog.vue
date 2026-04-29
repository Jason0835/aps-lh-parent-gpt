<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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
    };
  },
  computed: {
    // ...mapState({
    //   curingMachines: (state) => state.curing.machines,
    // }),
    title: function () {
      return this.$t("ui.frame.btn.add") + this.$t("ui.data.column.lh.scheduleResult.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          clearable: false,
          listeners: {
            change: this.handleScheduleDateChange,
          },
          disabled:true
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
          listeners: {
            change: this.handleScheduleDateChange,
          },
           disabled:true
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.specCode"),
        //   prop: "specCode",
        //   listeners: {
        //     blur: this.handleSpecCodeChange,
        //     clear: this.handleSpecCodeClear,
        //   },
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.sapCode"),
        //   prop: "productCode",
        //   type: "select",
        //   dictData: this.sapCodeList,
        //   disabled: this.sapCodeList.length == 0,
        //   labelKey: "productCode",
        //   valueKey: "productCode",
        //   listeners: {
        //     change: this.handleProductCodeChange,
        //   },
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          type: "select",
          dictData: this.curingMachines,
          filterable: true,
          disabled: this.curingMachines.length === 0,
           disabled:true
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.scheduleResult.materialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.materialCode}
                v-model={form.materialCode}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.materialDesc"),
          prop: "materialDesc",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty"),
          prop: "class1PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1Analysis"),
          prop: "class1Analysis",
          span: 24,
        },
        // 左右模
        // {
        //   label: this.$t("ui.data.column.scheduleResult.leftRightMold"),
        //   prop: "leftRightMold",
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2Analysis"),
          prop: "class2Analysis",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3PlanQty"),
          prop: "class3PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3Analysis"),
          prop: "class3Analysis",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4PlanQty"),
          prop: "class4PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4Analysis"),
          prop: "class4Analysis",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5PlanQty"),
          prop: "class5PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5Analysis"),
          prop: "class5Analysis",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class6PlanQty"),
          prop: "class6PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class6Analysis"),
          prop: "class6Analysis",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class7PlanQty"),
          prop: "class7PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class7Analysis"),
          prop: "class7Analysis",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class8PlanQty"),
          prop: "class8PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class8Analysis"),
          prop: "class8Analysis",
          span: 24,
        },
      ];
    },
  },

  methods: {
    handleMaterialCodeChange(val, row) {
      if (val) {
        this.$set(this.form, "materialDesc", row.materialDesc);
      } else {
        this.$set(this.form, "materialDesc", "");
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
      }
      this.form = form;
      await this.loadCuringMachinesDropdown();
      if (!data?.lhMachineCode) {
        this.$set(this.form, "lhMachineCode", "");
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleScheduleDateChange() {
      this.$set(this.form, "lhMachineCode", "");
      this.loadCuringMachinesDropdown();
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
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
