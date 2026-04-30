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

import { adjustFactoryMonthPlan } from "@/api/factory/monthPlanAdjust.js";
import { matchMouldConfiguration } from "@/api/maindata/relation.js";
export default {
  components: { infoForm },
  props: {
    startAdjustDate: String,
  },
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
            message: this.$t("common.rule.select"),
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
        productCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        startDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        adjustNumber: [
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
    startAdjustDateTime: function () {
      return new Date(this.startAdjustDate +  ' 00:00:00').getTime();
    },
    title: function () {
      return this.isEdit ? "调整" : "插入新规格";
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("分厂"),
          disabled: this.isEdit,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("年月"),
          disabled: this.isEdit,
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "productCode",
          label: this.$t("SAP代码"),
          disabled: this.isEdit,
          listeners: {
            blur: this.handelProductChange,
          },
        },
        {
          prop: "mouldNo",
          label: this.$t("模具"),
          disabled: true,
        },

        {
          prop: "specCode",
          label: this.$t("规格代号"),
          disabled: true,
        },
        {
          label: "库位",
          prop: "locationType",
          disabled: this.isEdit,
          type: "select",
          dictData: this.parentDict.type.biz_stor_type,
        },
        {
          label: "渠道",
          prop: "channel",
          disabled: this.isEdit,
          type: "select",
          dictData: this.parentDict.type.biz_channel_type,
        },
        {
          prop: "startDate",
          label: this.$t("开始日期"),
          render: (form) => {
            return (
              <el-date-picker
                class="w100"
                v-model={form.startDate}
                picker-options={{
                  disabledDate: (val) => {
                    if (!!this.startAdjustDate) {
                      return val.getTime() < this.startAdjustDateTime;
                    }
                    return true;
                  },
                }}
              />
            );
          },
        },
        {
          prop: "adjustNumber",
          label: this.$t("调整量"),
          type: "number",
          precision: 0,
          min: -99999,
          max: 99999,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        // this.$modal.msgSuccess(res.msg);
        this.$emit("success", params, (status) => {
          if (status === "success") {
            this.hide();
          }
          this.loading = false;
        });
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async getMouldNo(val) {
      try {
        const res = await matchMouldConfiguration({
          factoryCode: "",
          productCode: val,
          month: moment().format("MM"),
          year: moment().format("yyyy"),
        });
        console.log(res);
        this.form.brand = res.brand;
        if (res.mouldConfigurationList && res.mouldConfigurationList[0]) {
          let mould = res.mouldConfigurationList[0];
          this.form.mouldNo = mould.mouldNo;
          if (mould.specCodeList && mould.specCodeList[0]) {
            let specCode = mould.specCodeList[0];
            this.form.specCode = specCode;
          }
        }
      } catch (error) {
        console.log(error);
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          startDate: this.startAdjustDate,
          yearMonth: `${data.year}-${data.month}`,
        };
      } else {
        this.form = {
          startDate: this.startAdjustDate,
        };
      }
    },
    hide() {
      this.form = {};
      // this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    handelProductChange(e) {
      let value = e.target.value;

      this.getMouldNo(value);
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
