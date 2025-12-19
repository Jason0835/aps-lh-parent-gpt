<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="500px"
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
      label-width="150px"
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

import { finalized } from "@/api/factory/console";
import { getVersionList as requireProductionPlanVersionList } from "@/api/demand/requireProductionPlan";
import { listProductionVersionList } from "@/api/monthplan/mouldingDayResult";

import infoForm from "@/views/components/infoForm.vue";

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
        priority: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        valueMin: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        valueMax: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      planVersionList: [],
      productionVersionList: [],
    };
  },
  computed: {
    title: function () {
      return this.$t("定稿");
    },
    columns() {
      return [
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.console.yearMonth"),
          disabled: true,
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.console.factoryCode"),
          disabled: true,
        },
        {
          prop: "monthPlanVersion",
          label: this.$t("ui.data.column.console.monthPlanVersion"),
          render: (form) => {
            return (
              <el-select
                class="w100"
                v-model={form.monthPlanVersion}
                onChange={this.handleChange}
              >
                {this.planVersionList.map((value) => {
                  return <el-option value={value} key={value} label={value} />;
                })}
              </el-select>
            );
          },
        },
        {
          prop: "productionVersion",
          label: this.$t("ui.data.column.console.productionVersion"),
          render: (form) => {
            return (
              <el-select class="w100" v-model={form.productionVersion}>
                {this.productionVersionList.map((value) => {
                  return <el-option value={value} key={value} label={value} />;
                })}
              </el-select>
            );
          },
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        params.yearMonth = undefined;
        const res = await finalized(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async requireProductionPlanVersionList() {
      try {
        const res = await requireProductionPlanVersionList({
          year: this.form.year,
          month: this.form.month,
          factoryCode: this.form.factoryCode,
        });
        console.log(res);
        this.planVersionList = res;
      } catch (error) {}
    },
    async listProductionVersionList() {
      try {
        const res = await listProductionVersionList({
          year: this.form.year,
          month: this.form.month,
          factoryCode: this.form.factoryCode,
          monthPlanVersion: this.form.monthPlanVersion,
        });
        console.log(res);
        this.productionVersionList = res;
      } catch (error) {}
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          yearMonth: `${data.year}-${data.month}`,
        };
        this.requireProductionPlanVersionList();
        this.listProductionVersionList();
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
    handleChange(val) {
      this.form.productionVersion = "";
      this.listProductionVersionList();
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        this.save(params);
      });
    },
  },
};
</script>
