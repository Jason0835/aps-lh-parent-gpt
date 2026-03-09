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
  materialRawWarningRecord,
  usageRawWarningRecord,
} from "@/api/maindata/rawWarningRecord";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      isType: "month",
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
        week: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isType=='week'
        ? this.$t("ui.data.rawWarningRecord.executeUsageWarning")
        : this.$t("ui.data.rawWarningRecord.executeNewMaterialWarning");
    },
    columns() {
      let columnsDate = [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },

        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
      ];
      if(this.isType=='week'){
        columnsDate.push(
          {
            prop: "week",
            label: this.$t("ui.data.column.rawMaterial.relatedWeek"),
            type: "number",
            min: 1,
            max: 4,
          },

        );
      }
      return columnsDate;
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        let res;
        if (this.isType == "month") {
          res = await materialRawWarningRecord(params);
        } else {
          res = await usageRawWarningRecord(params);
        }
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
    const year = now.getFullYear(); // 2024
    const month = now.getMonth() + 1; // 注意：月份从0开始，需要+1
      this.form = {
        factoryCode: "116",
        yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
      };
      this.isType = data;
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
  },
};
</script>
