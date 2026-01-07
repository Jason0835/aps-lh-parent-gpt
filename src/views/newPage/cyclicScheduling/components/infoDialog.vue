<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
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
import { mapState } from "vuex";

import {
  saveSupplyOrderPool,
  queryRelationByMaterialCode,
  checkSupplyOrderPool,
} from "@/api/monthplan/supplyOrderPool";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm, materialCodeSelect },
  inject: ["parentDict"],
  data() {
    const validatePositiveInteger = (rule, value, callback) => {
      if (value === "" || value === null || value === undefined) {
        if (rule.required) {
          return callback(new Error(this.$t("common.rule.noData")));
        }
        return callback();
      }
      const strValue = String(value).trim();

      // 检查是否只包含数字
      if (!/^\d+$/.test(strValue)) {
        return callback(new Error(this.$t("common.rule.noPoint")));
      }

      // 转换为数字
      const numValue = Number(strValue);
      if (numValue > 99999999) {
        return callback(new Error(this.$t("common.rule.inoutMax")));
      }

      if (!Number.isInteger(numValue)) {
        return callback(new Error(this.$t("common.rule.peleaseInteger")));
      }

      callback();
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        orderType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        locationType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        qty: [
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: false }, value, callback);
            },
            trigger: ["change"],
          },
        ],
      },
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          format: "yyyy-MM",
        },
        {
          prop: "orderType",
          label: this.$t("ui.data.defectiveStock.orderType"),
          type: "select",
          dictData: this.parentDict.type.supply_order_type,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
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
          prop: "locationType",
          label: this.$t("ui.data.column.finishStock.wai"),
          type: "select",
          dictData: this.parentDict.type.biz_stor_type,
        },
        {
          prop: "qty",
          label: this.$t("ui.data.defectiveStock.qty"),
          type: "number",
          min: 0,
          max: 99999999,
        },

        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          disabled: true,
        },
        {
          prop: "productCategory",
          label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.product_category,
        },
        {
          prop: "brand",
          label: this.$t("common.brand"),
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.biz_brand_type,
        },
        {
          prop: "averageSaleQty",
          label: this.$t("ui.data.defectiveStock.averageSaleQty"),
          disabled: true,
          type: "number",
        },
        {
          prop: "deliveryFrequency",
          label: this.$t("ui.data.defectiveStock.deliveryFrequency"),
          disabled: true,
          type: "number",
        },
        {
          prop: "structureFrequency",
          label: this.$t("ui.data.defectiveStock.structureFrequency"),
          disabled: true,
          type: "number",
        },
        {
          prop: "saleAreaName",
          label: this.$t("ui.data.defectiveStock.saleArea"),
          disabled: true,
        },
        {
          prop: "threeOverdueStockQty",
          label: this.$t("ui.data.defectiveStock.threeOverdueStockQty"),
          disabled: true,
          type: "number",
        },
        {
          prop: "sixOverdueStockQty",
          label: this.$t("ui.data.defectiveStock.sixOverdueStockQty"),
          disabled: true,
          type: "number",
        },
        // {
        //   prop: "nightOverdueStockQty",
        //   label: this.$t("ui.data.defectiveStock.nightOverdueStockQty"),
        //   disabled: true,
        //   type: "number",
        // },
        {
          prop: "twelveOverdueStockQty",
          label: this.$t("ui.data.defectiveStock.twelveOverdueStockQty"),
          disabled: true,
          type: "number",
        },
        {
          prop: "stockLimit",
          label: this.$t("ui.data.defectiveStock.stockLimit"),
          disabled: true,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          maxlength: 200,
        },
      ];
    },
  },
  methods: {
    async blurMaterialCode() {
      let res = await queryRelationByMaterialCode({
        materialCode: this.form.materialCode,
      });
      let defultdata = JSON.parse(JSON.stringify(this.form));
      this.form = {
        ...res.data,
        ...defultdata,
      };
      console.log(this.form);
    },
    // api
    async save(params) {
      let arr = params.yearMonth.split("-");
      params.year = arr[0];
      params.month = arr[1];
      let that = this;
      try {
        this.loading = true;

        let res = await checkSupplyOrderPool(params);
        this.confirmSave(params);
      } catch (error) {
        console.log(error);
        this.noCheckConfirmSave(error, params);

        this.loading = false;
      }
    },
    async confirmSave(params) {
      try {
        const res = await saveSupplyOrderPool(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (e) {
        return;
      }
    },
    async noCheckConfirmSave(error, params) {
      console.log("错误提示");

      this.$confirm(error, {
        type: "warning",
      })
        .then(() => {
          this.confirmSave(params)
        })
        .catch((action) => {
          this.hide();
          // 用户点击"取消"

        });
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          factoryCode: "116",
          locationType: "2",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    handleMaterialCodeChange(val, row) {
      if (val) {
        this.$set(this.form, "materialDesc", row.materialDesc);
        this.$set(this.form, "productTypeCode", row.productTypeCode);
        this.$set(this.form, "materialbrandDesc", row.brand);
        this.blurMaterialCode();
      } else {
        this.$set(this.form, "materialDesc", "");
        this.$set(this.form, "productTypeCode", "");
        this.$set(this.form, "materialbrandDesc", "");
      }
    },
  },
};
</script>
