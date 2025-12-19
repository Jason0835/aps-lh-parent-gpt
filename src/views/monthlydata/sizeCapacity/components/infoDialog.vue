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
      <el-button type="primary" :loading="loading" @click="handleConfirm"
        >{{ this.$t("common.button.confirm") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import infoForm from "@/views/components/infoForm.vue";

import { getVersionList } from "@/api/demand/requireProductionPlan";
import {
  getInfo,
  getDemandInfo,
  saveSizeCapacity,
} from "@/api/monthsetting/sizeCapacity";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      id: null,
      form: {},
      versionList: [],
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
        monthPlanVersion: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        proSize: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        mouldMethod: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        dayCapacity: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        machineNumber: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        // nextProSize: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.select"),
        //     trigger: "blur",
        //   },
        // ],
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
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          disabled: this.isEdit,
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          listeners: {
            change: this.handleChangeYearMonthFactoryChange,
          },
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.colume.factory"),
          disabled: this.isEdit,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          listeners: {
            change: this.handleChangeYearMonthFactoryChange,
          },
        },
        {
          prop: "monthPlanVersion",
          label: this.$t("ui.data.column.sizeCapacity.monthPlanVersion"),
          disabled: this.isEdit || this.versionList.length == 0,
          type: "select",
          dictData: this.versionList,
          listeners: {
            change: this.handleVersionChange,
          },
          // render: () => {
          //   return (
          //     <el-select
          //       class="w100"
          //       v-model={this.form.monthPlanVersion}
          //       disabled={this.isEdit || this.versionList.length == 0}
          //     >
          //       {this.versionList.map((ver) => {
          //         return <el-option key={ver} value={ver} label={ver} />;
          //       })}
          //     </el-select>
          //   );
          // },
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.sizeCapacity.proSize"),
          disabled: this.isEdit,
          type: "number",
          min: 0,
          max: 999999,
          precision: 0,
          attrs: {
            controls: false,
          },
          listeners: {
            change: this.handleVersionChange,
          },
        },

        {
          prop: "mouldMethod",
          label: this.$t("ui.data.column.sizeCapacity.mouldMethod"),
          disabled: this.isEdit,
          type: "select",
          dictData: this.parentDict.type.molding_method,
        },
        {
          prop: "demandQty",
          label: this.$t("ui.data.column.sizeCapacity.demandQty"),
          disabled: true,
        },
        {
          prop: "netDemandQty",
          label: this.$t("ui.data.column.sizeCapacity.netDemandQty"),
          disabled: true,
        },
        {
          prop: "stockUpDemandQty",
          label: this.$t("ui.data.column.sizeCapacity.stockUpDemandQty"),
          disabled: true,
        },
        {
          prop: "dayCapacity",
          label: this.$t("ui.data.column.sizeCapacity.dayCapacity"),
        },
        {
          prop: "machineNumber",
          label: this.$t("ui.data.column.sizeCapacity.machineNumber"),
        },
        {
          prop: "nextProSize",
          label: this.$t("ui.data.column.sizeCapacity.nextProSize"),
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          type: "textarea",
          maxlength: 300,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveSizeCapacity(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async getInfo() {
      try {
        this.loading = true;
        const res = await getInfo({ id: this.id });
        this.form = {
          yearMonth: `${res.year}-${res.month}`,
          ...res,
        };
        this.loading = false;
      } catch (error) {
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.id = data.id;
        this.getInfo();
      }
    },
    hide() {
      this.form = {};
      this.versionList = [];
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    async handleChangeYearMonthFactoryChange() {
      if (!this.form.yearMonth || !this.form.factoryCode) {
        return;
      }

      this.versionList = [];
      this.$set(this.form, "monthPlanVersion", "");

      let arr = this.form.yearMonth.split("-");

      try {
        const res = await getVersionList({
          year: arr[0],
          month: arr[1],
          factoryCode: this.form.factoryCode,
        });
        this.versionList = res.map((val) => {
          return {
            label: val,
            value: val,
          };
        });

        console.log(res);
      } catch (error) {
        console.error(error);
      }
    },
    async handleVersionChange() {
      try {
        if (!this.form.monthPlanVersion || !this.form.proSize) {
          return;
        }
        this.$set(this.form, "demandQty", "");
        this.$set(this.form, "netDemandQty", "");
        this.$set(this.form, "stockUpDemandQty", "");

        let arr = this.form.yearMonth.split("-");
        const res = await getDemandInfo({
          year: arr[0],
          month: arr[1],
          factoryCode: this.form.factoryCode,
          monthPlanVersion: this.form.monthPlanVersion,
          proSize: this.form.proSize,
        });
        if (res) {
          this.$set(this.form, "demandQty", res.demandQty);
          this.$set(this.form, "netDemandQty", res.netDemandQty);
          this.$set(this.form, "stockUpDemandQty", res.stockUpDemandQty);
        }
      } catch (error) {
        console.error(error);
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        // 解析年月
        let array = params.yearMonth.split("-");
        params.year = array[0];
        params.month = array[1];

        try {
          this.loading = true;
          this.save(params);
        } catch (error) {
          console.error(error);
          this.$modal.msgError(error.message);
          this.loading = false;
        }
      });
    },
  },
};
</script>
