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
  saveMonCycleSchStruConf,
  getStructList,
  addSave
} from "@/api/monthplan/mdmMonCycleSchStruConf";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    const validatePositiveInteger = (rule, value, callback) => {
      if (value === "" || value === null || value === undefined) {
        if (rule.required) {
          return callback(new Error(this.$t("common.rule.noData")));
        }
        return callback();
      }
      const strValue = String(value).trim();

      // 检查是否只包含数字
      if (!/^\d+$/.test(strValue)) {
        return callback(new Error(this.$t("common.rule.noPoint")));
      }

      // 转换为数字
      const numValue = Number(strValue);
      if (numValue > 999999) {
        return callback(new Error(this.$t("common.rule.inoutMax")));
      }

      if (!Number.isInteger(numValue)) {
        return callback(new Error(this.$t("common.rule.peleaseInteger")));
      }

      callback();
    };
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
        turnoverMonth: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        minVulcanizingMachine: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        structureName: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
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
          disabled: this.isEdit,
          listeners: {
            change: this.yearMonthChange,
          },
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          disabled: this.isEdit,
          listeners: {
            change: this.yearMonthChange,
          },
        },
        // {
        //   prop: "year",
        //   label: this.$t("ui.data.colume.year"),
        //   disabled: true,
        // },
        // {
        //   prop: "month",
        //   label: this.$t("ui.data.colume.month"),
        //   disabled: true,
        // },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          disabled: this.isEdit,
          type: "select",
          dictData: this.selectList,
          filterable: true,
        },
        {
          prop: "turnoverMonth",
          label: this.$t("ui.data.column.curingPlan.turnoverMonth"),
          type: "number",
          min: 0,
          max: 999999,
        },
        {
          prop: "minVulcanizingMachine",
          label: this.$t("ui.data.column.curingPlan.minVulcanizingMachine"),
          type: "number",
          min: 0,
          max: 999999,
        },
      ];
    },
  },
  methods: {
    async yearMonthChange() {
      try {
        const [year, month] = this.form.yearMonth.split("-");

        const res = await getStructList({
          factoryCode: this.form.factoryCode,
          year:year,
          month: month,
        });
        let list=[]
        for (let i = 0; i < res.length; i++) {
          list.push({
            label: res[i].structureName,
            value: res[i].structureName,
          });

        }
        this.selectList=list

        console.log(res)
      } catch (error) {
        console.log(error);
      }
    },
    // api
    async save(params) {
      try {
        this.loading = true;
        const [year, month] = params.yearMonth.split("-");
        params.year = year;
        params.month = month;
        let res
        if(this.isEdit){
          res = await saveMonCycleSchStruConf(params);
        }else{
          res = await addSave(params);
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
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          yearMonth: `${data.year}-${
            data.month < 10 ? "0" + data.month : data.month
          }`,
        };
      } else {
        const now = new Date();
        const year = now.getFullYear(); // 2024
        const month = now.getMonth() + 1; // 注意：月份从0开始，需要+1
        this.form = {
          factoryCode: "116",
          yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
        };
        this.yearMonthChange()
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
