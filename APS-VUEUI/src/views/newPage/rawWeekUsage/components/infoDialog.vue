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
  monthRawWeekUsage,
} from "@/api/maindata/rawWeekUsage";
import {
  getMdmProductVersion,
} from "@/api/maindata/rawMaterialRequirePlan​";

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
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        sapCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        lineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        jobType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
      selectList:[]
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("ui.data.column.rawMaterial.monthGen")
        : this.$t("ui.data.column.rawMaterial.monthGen");
    },
    columns() {
      return [
      {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          listeners: {
            change: this.getVersion,
          },
        },

        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
          listeners: {
            change: this.getVersion,
          },
        },
        {
          prop: "version",
          label: this.$t("plan.planProduction.planVersion"),
          type: "select",
          dictData: this.selectList,
          clearable: false,
          filterable: true,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let arr=params.yearMonth.split("-");
        params.year=arr[0];
        params.month=arr[1];

        const res = await monthRawWeekUsage(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      const now = new Date();
      // now.setMonth(now.getMonth() + 1);
      // const year = now.getFullYear();
      // const month = String(now.getMonth() + 1).padStart(2, "0"); // 月份从0开始，需要+1
      const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
      const year = nextMonth.getFullYear();
      const month = nextMonth.getMonth() + 1; // 月份从0开始，需要+1
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          yearMonth: `${year}-${month}`,
        };
      } else {
        this.form = {
          factoryCode: "116",
          yearMonth: `${year}-${month}`,
        };
      }
      this.getVersion()
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
    async getVersion() {
      try {
        this.loading = true;
        const params = {
          ...this.form,
        };
        let arr = params.yearMonth.split("-");
        const res = await getMdmProductVersion({
          factoryCode: params.factoryCode,
          year: arr[0],
          month: arr[1],
        });
        let list = [];
        for (let i = 0; i < res.length; i++) {
          list.push({
            label: res[i].version,
            value: res[i].version,
          });
        }
        this.selectList = list;
        if (res.length > 0) {
          this.$set(this.form, "version", res[0].version);
        } else {
          this.$set(this.form, "version", "");
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;

      }
    },
  },
};
</script>
