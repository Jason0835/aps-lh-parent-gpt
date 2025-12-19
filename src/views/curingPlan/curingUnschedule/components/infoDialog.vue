<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="120px"
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

import infoForm from "@/views/components/infoForm.vue";
import {
  validateAdd,
  editScheduleResult,
  insertOrder,
  selectListMdmProductConstruction,
  getScheduleMachineInfo,
} from "@/api/lh/scheduleResult";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      sapCodeList: [],
      curingMachines: [],
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        specCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        productCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    // ...mapState({
    //   curingMachines: (state) => state.curing.machines,
    // }),
    title: function () {
      return this.$t("ui.data.column.scheduleResult.insertOrder");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
          listeners: {
            change: this.handleScheduleDateChange,
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.specCode"),
          prop: "specCode",
          listeners: {
            blur: this.handleSpecCodeChange,
            clear: this.handleSpecCodeClear,
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.sapCode"),
          prop: "productCode",
          type: "select",
          dictData: this.sapCodeList,
          labelKey: "productCode",
          valueKey: "productCode",
          listeners: {
            change: this.handleProductCodeChange,
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          type: "select",
          dictData: this.curingMachines,
          labelKey: "showLabel",
          valueKey: "machineCode",
          filterable: true,
          disabled: this.curingMachines.length === 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMold"),
          prop: "leftRightMold",
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.embryoCode"),
        //   prop: "embryoCode",
        //   maxlength: "30",
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty.lh"),
          prop: "class1PlanQty",
          type: "number",
          min: 0,
          max: 999999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1Analysis.lh"),
          prop: "class1Analysis",
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty.lh"),
          prop: "class2PlanQty",
          type: "number",
          min: 0,
          max: 999999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2Analysis.lh"),
          prop: "class2Analysis",
          maxlength: "66",
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class4PlanQty.lh"),
        //   prop: "class4PlanQty",
        //   type: "number",
        //   min: 0,
        //   max: 999999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class4Analysis.lh"),
        //   prop: "class4Analysis",
        //   maxlength: "66",
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class5PlanQty.lh"),
        //   prop: "class5PlanQty",
        //   type: "number",
        //   min: 0,
        //   max: 999999999,
        //   precision: 0,
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class5Analysis.lh"),
        //   prop: "class5Analysis",
        //   maxlength: "66",
        // },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;

        const res = await insertOrder(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      //设置默认值为明天

       if (data) {
        this.isEdit = true;
        this.form = {
          factoryCode: data.factoryCode,
          scheduleDate: data.scheduleDate,
          specCode: data.specCode,
          productCode: data.productCode,
          // lhMachineCode: data.productCode,
        };
        this.handleProductCodeChange()
      } else {
        let nowDate = new Date();
        let tomorrow = new Date(nowDate);
        tomorrow.setDate(nowDate.getDate() + 1);
        this.form = {
          factoryCode: "",
          scheduleDate: tomorrow.toISOString().slice(0, 10), // 保留yyyy-MM-dd
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

    handleScheduleDateChange() {
      this.curingMachines = [];
      this.$set(this.form, 'lhMachineCode', "");
      this.handleProductCodeChange();
    },
    async handleSpecCodeChange() {
      if (!this.form.factoryCode || !this.form.specCode) {
        return;
      }
      this.sapCodeList = [];
      this.form.productCode = "";
      this.curingMachines = [];
      this.$set(this.form, 'lhMachineCode', "");

      try {
        const res = await selectListMdmProductConstruction({
          factoryCode: this.form.factoryCode,
          specCode: this.form.specCode,
        });
        this.sapCodeList = res;
      } catch (error) {
        this.sapCodeList = [];
      }
    },
    handleSpecCodeClear() {
      this.sapCodeList = [];
      this.form.productCode = "";
      this.curingMachines = [];
      this.$set(this.form, 'lhMachineCode', "");
    },
    async handleProductCodeChange() {
      if (
        !this.form.factoryCode ||
        !this.form.productCode ||
        !this.form.scheduleDate ||
        !this.form.specCode
      ) {
        return;
      }

      this.curingMachines = [];
      this.$set(this.form, 'lhMachineCode', "");
      try {
        const res = await getScheduleMachineInfo({
          factoryCode: this.form.factoryCode,
          productCode: this.form.productCode,
          scheduleTime: this.form.scheduleDate,
          specCode: this.form.specCode,
        });
        console.log(res);
        this.curingMachines = res.map((item) => {
          item.showLabel = `${item.machineCode}(剩余产能为${item.remainCapacity})`;
          return item;
        });
      } catch (error) {
        console.error(error);
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
