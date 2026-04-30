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
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import {
  saveMonthPlanAdjustNotice,
  getStockInfo,
} from "@/api/factory/monthPlanAdjustNotice";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
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
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        locationType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        productCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        needQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        channel: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          listeners: {
            change: this.getStockInfo,
          },
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          listeners: {
            change: this.getStockInfo,
          },
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.locationType"),
          type: "select",
          dictData: this.parentDict.type.biz_stor_type,
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productCode"),
          listeners: {
            blur: this.getStockInfo,
          },
        },
        {
          prop: "needQty",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.planQty"),
          type: "number",
          min: -99999999,
          max: 99999999,
          precision: 0,
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.channel"),
          type: "select",
          dictData: this.parentDict.type.biz_channel_type,
        },
        {
          label: "剩余库存量",
          prop: "stockQty",
          disabled: true,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveMonthPlanAdjustNotice(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    getStockInfo() {
      if(this.form.factoryCode && this.form.yearMonth && this.form.productCode) {
        const arr = this.form.yearMonth.split("-");

        getStockInfo({
          factoryCode: this.form.factoryCode,
          year: arr[0],
          month: arr[1],
          productCode: this.form.productCode,
        })
        .then((res) => {
          // console.log(res);
          this.$set(this.form, "stockQty", res.stockQty || 0);
        })
        .catch((error) => {
          console.log(error);
        });
      } else {
        this.form.stockQty = 0;
        return;
      }      
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          yearMonth: `${data.year}-${data.month}`,
          ...data,
        };
        this.getStockInfo();
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        let array = params.yearMonth.split("-");
        params.year = array[0];
        params.month = array[1];

        this.save(params);
      });
    },
  },
};
</script>
