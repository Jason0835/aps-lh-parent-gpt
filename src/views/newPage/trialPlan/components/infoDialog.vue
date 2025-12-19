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
  saveMpTrialPlan
} from "@/api/monthplan/mpTrialPlan";

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
        specifications: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        pattern: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        trialStatus: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        trialQty: [
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
          dateType:'month',
          valueFormat: "yyyy-MM",
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.pattern"),

        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.trialPlan.planDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "trialStatus",
          label: this.$t("ui.data.column.trialPlan.trialStatus"),
          type: "select",
          dictData: this.parentDict.type.biz_construction_stage,
        },
        {
          prop: "trialQty",
          label: this.$t("common.num"),
          type: "number",
        },

        {
          prop: "urgencyType",
          label: this.$t("ui.data.column.trialPlan.urgencyType"),
          type: "select",
          dictData: this.parentDict.type.biz_urgency_type,
        },
        {
          prop: "trialType",
          label: this.$t("ui.data.column.trialPlan.trialType"),
          type: "select",
          dictData: this.parentDict.type.biz_trial_type,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
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
        params.year = arr[0];
        params.month = arr[1];
        const res = await saveMpTrialPlan(params);
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
      if (data) {
        this.isEdit = true;
        data.yearMonth = data.year + "-" + (data.month < 10 ? "0" + data.month : data.month);
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          factoryCode: "116",
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
  },
};
</script>
