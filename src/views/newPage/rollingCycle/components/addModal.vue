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
import structureSelect from "@/views/components/structureSelect.vue";
import formingCapacitySelect from "@/views/components/formingCapacitySelect.vue";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm, formingCapacitySelect, structureSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        beginDay: "",
        endDay: "",
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
        structureName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        beginDay: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        endDay: [
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
      daysNum:0
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
                key="formCxMachineCode"
                v-model={form.cxMachineCode}
              />
            );
          },
        },

        {
          prop: "structureName",
          label: this.$t("产品结构"),
          render: (form) => {
            return (
              <structureSelect
                key="formStructureName"
                v-model={form.structureName}
              />
            );
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
            change: this.yearMonthChange,
          },
        },
        {
          prop: "beginDay",
          label: this.$t("开始日期"),
          render: (form) => {
            const days = Array.from({ length: this.daysNum }, (_, i) => i + 1);
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
            const days = Array.from({ length: this.daysNum }, (_, i) => i + 1);
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
      this.$nextTick(()=>{
        let arr = this.form.yearMonth.split("-");
      this.daysNum=this.getDaysInMonth(arr[0],arr[1])
      })
    },
    yearMonthChange() {
      this.$set(this.form,'beginDay','')
      this.$set(this.form,'endDay','')
      let arr = this.form.yearMonth.split("-");
      this.daysNum=this.getDaysInMonth(arr[0],arr[1])
    },
    getDaysInMonth(year, month) {
      // 月份从0开始（0=1月，11=12月）
      // 下个月的第0天就是本月的最后一天
      return new Date(year, month, 0).getDate();
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
