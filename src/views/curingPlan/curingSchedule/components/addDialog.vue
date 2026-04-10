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
import { debounce } from "@/utils";
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
      return this.$t("添加硫化排程结果信息");
    },
    columns() {
      return [
        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          clearable: false,
          listeners: {
            change: this.handleScheduleDateChange,
          },
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
        // {
        //   label: this.$t("ui.data.column.scheduleResult.specCode"),
        //   prop: "specCode",
        //   listeners: {
        //     blur: this.handleSpecCodeChange,
        //     clear: this.handleSpecCodeClear,
        //   },
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.sapCode"),
        //   prop: "productCode",
        //   type: "select",
        //   dictData: this.sapCodeList,
        //   disabled: this.sapCodeList.length == 0,
        //   labelKey: "productCode",
        //   valueKey: "productCode",
        //   listeners: {
        //     change: this.handleProductCodeChange,
        //   },
        // },
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
        {
          label: this.$t("一班计划"),
          prop: "class1PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("一班备注"),
          prop: "class1Analysis",
          span: 24,
        },

         {
          label: this.$t("二班计划"),
          prop: "class2PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("二班备注"),
          prop: "class2Analysis",
          span: 24,
        },
         {
          label: this.$t("三班计划"),
          prop: "class3PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("三班备注"),
          prop: "class3Analysis",
          span: 24,
        },
          {
            label: this.$t("四班计划"),
            prop: "class4PlanQty",
            span: 24,
            type: "number",
          },
          {
            label: this.$t("四班备注"),
            prop: "class4Analysis",
            span: 24,
          },
          {
            label: this.$t("五班计划"),
            prop: "class5PlanQty",
            span: 24,
            type: "number",
          },
          {
            label: this.$t("五班备注"),
            prop: "class5Analysis",
            span: 24,
          },
            {
              label: this.$t("六班计划"),
              prop: "class6PlanQty",
              span: 24,
              type: "number",
            },
            {
              label: this.$t("六班备注"),
              prop: "class6Analysis",
              span: 24,
            },
            {
              label: this.$t("七班计划"),
              prop: "class7PlanQty",
              span: 24,
              type: "number",
            },
            {
              label: this.$t("七班备注"),
              prop: "class7Analysis",
              span: 24,
            },
            {
          label: this.$t("八班计划"),
          prop: "class8PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("八班备注"),
          prop: "class8Analysis",
          span: 24,
        },
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
      let nowDate = new Date();
      let tomorrow = new Date(nowDate);
      tomorrow.setDate(nowDate.getDate() + 1);
      this.form = {
        factoryCode: "116",
        scheduleDate: tomorrow.toISOString().slice(0, 10), // 保留yyyy-MM-dd
      };
      this.handleScheduleDateChange();
      // if (data) {
      //   this.isEdit = true;
      //   this.form = {
      //     ...data,
      //   };
      // }
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
      if (!this.form.factoryCode ) {
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

        !this.form.scheduleDate
      ) {
        return;
      }

      this.curingMachines = [];
      this.$set(this.form, 'lhMachineCode', "");
      try {
        const res = await getScheduleMachineInfo({
          factoryCode: this.form.factoryCode,
          // productCode: this.form.productCode,
          scheduleTime: this.form.scheduleDate,
          // specCode: this.form.specCode,
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
