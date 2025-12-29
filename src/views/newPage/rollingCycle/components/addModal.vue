<template>
  <el-dialog
    title="新增结构"
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
  listInternalStructure,
  getAdjustDetailList,
  listOutsideStructure,
  confirmAdjust,
  addAdjust,
} from "@/api/monthplan/adjustStructure";

import formingCapacitySelect from "@/views/components/formingCapacitySelect.vue";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm, formingCapacitySelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        beginDay:'',
        endDay:''
      },
      rules: {
        cxMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        factoryCode: [
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
          prop: "cxMachineCode",
          label: this.$t("机台"),
          render: (form) => {
            return (
              <formingCapacitySelect
                factoryCode={form.cxMachineCode}
                key={form.cxMachineCode}
                v-model={form.cxMachineCode}
              />
            );
          },
        },

        {
          prop: "structureName",
          label: this.$t("产品结构"),
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          prop: "beginDay",
          label: this.$t("开始日期"),
          render: (form) => {

            const days = Array.from({ length: 31 }, (_, i) => i + 1);
            return (
              <el-select v-model={form.beginDay}>
                {days.map((item) => (
                  <el-option key={item} label={item} value={item}></el-option>
                ))}
              </el-select>
            );
          },
        },
        {
          prop: "endDay",
          label: this.$t("结束日期"),
          render: (form) => {
            const days = Array.from({ length: 31 }, (_, i) => i + 1);
            return (
              <el-select v-model={form.endDay}>
                {days.map((item) => (
                  <el-option key={item} label={item} value={item}></el-option>
                ))}
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
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.year = arr[0];
          params.month = arr[1];
          params.yearMonth = "";
        }
        const res = await addAdjust(params);
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
