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
import { saveMonCycleSchStruConf } from "@/api/monthplan/mdmMonCycleSchStruConf";

import infoForm from "@/views/components/infoForm.vue";
import DictData from "@/components/DictData";
import { min } from "lodash";

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
        工厂: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        turnoverMonth: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        minVulcanizingMachine: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        最低硫化机台数: [
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
          disabled: true,
        },
        {
          prop: "year",
          label: this.$t("ui.data.colume.year"),
          disabled: true,
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
          disabled: true,
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          disabled: true,
        },
        {
          prop: "turnoverMonth",
          label: this.$t("ui.data.column.curingPlan.turnoverMonth"),
          type: "number",
          min: 0,
        },
        {
          prop: "minVulcanizingMachine",
          label: this.$t("ui.data.column.curingPlan.minVulcanizingMachine"),
          type: "number",
          min: 0,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveMonCycleSchStruConf(params);
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
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          factoryCode: "",
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
