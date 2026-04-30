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
import moment from "moment";
import { mapState } from "vuex";

import {
  // validateAdd,
  editGlueScheduleResult,
} from "@/api/schedule/glueScheduleResult";
import { getRecipeMachineList } from "@/api/setting/formulaMachine";

import infoForm from "@/views/components/infoForm.vue";
import recipeSelect from "./recipeSelect.vue";

export default {
  components: { infoForm, recipeSelect },
  props: {
    scheduleMixAreaPermission: Array,
  },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      machines: [],
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.mix.machines,
    }),
    title: function () {
      return this.$t("schedule.glueScheduleResult.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.glueScheduleResult.mixArea"),
          prop: "mixArea",
          span: 24,
          type: "select",
          dictData: this.scheduleMixAreaPermission,
          labelKey: "dictLabel",
          valueKey: "dictValue",
          listeners: {
            change: this.handleChangeGetMachines,
          },
        },
        {
          label: this.$t("药品名称"),
          prop: "glue",
          span: 24,
          type: "input",
          listeners: {
            change: this.handleChangeGetMachines,
          },
        },
        {
          label: this.$t("称重工位"),
          prop: "machineCode",
          span: 24,
          type: "select",
          dictData: this.machines,
          labelKey: "machineName",
          valueKey: "machineCode",
          // render: (form) => {
          //   return (
          //     <el-select v-model={form.machineCode}>
          //       {this.machines.map((row) => {
          //         return (
          //           <el-option
          //             value={row.machineCode}
          //             label={row.machineName}
          //           />
          //         );
          //       })}
          //     </el-select>
          //   );
          // },
        },
        {
          label: this.$t("schedule.glueScheduleResult.recipeTypeName"),
          prop: "recipeType",
          span: 24,
          render: (form) => {
            return (
              <recipeSelect
                v-model={form.recipeType}
                productStage={this.parentDict.type.PRODUCT_STAGE}
                disabled={!(this.form.glue && this.form.machineCode)}
                params={{
                  recipeMaterialName: this.form.glue,
                  machineName: this.form.machineName,
                }}
                onChange={this.recipeChange}
              />
            );
          },
        },
        {
          label: this.$t("schedule.glueScheduleResult.recipeVersionId"),
          prop: "recipeVersionId",
          span: 24,
          disabled: true,
        },
        {
          label: this.$t("schedule.glueScheduleResult.recipeStage"),
          prop: "recipeStage",
          span: 24,
          disabled: true,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.glueScheduleResult.midPlanQty"),
          prop: "midPlanQty",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.glueScheduleResult.midProduceOrder"),
          prop: "midProduceOrder",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.glueScheduleResult.midRemark"),
          prop: "midRemark",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightPlanQty"),
          prop: "nightPlanQty",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightProduceOrder"),
          prop: "nightProduceOrder",
          span: 24,
          maxlength: "100",
        },

        {
          label: this.$t("schedule.glueScheduleResult.nightRemark"),
          prop: "nightRemark",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("中班计划量"),
          prop: "nightPlanQty",
          span: 24,
        },
        {
          label: this.$t("中班生产顺序"),
          prop: "nightHandAnalysis",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("中班备注"),
          prop: "nightHandAnalysis",
          span: 24,
          maxlength: "100",
        },
        // {
        //   label: this.$t("schedule.glueScheduleResult.dayPlanQty"),
        //   prop: "dayPlanQty",
        //   span: 24,
        //   maxlength: "100",
        // },

        // {
        //   label: this.$t("schedule.glueScheduleResult.dayProduceOrder"),
        //   prop: "dayProduceOrder",
        //   span: 24,
        //   maxlength: "100",
        // },
        // {
        //   label: this.$t("schedule.glueScheduleResult.dayRemark"),
        //   prop: "dayRemark",
        //   span: 24,
        //   maxlength: "100",
        // },
        // {
        //   label: this.$t("ui.common.column.remark"),
        //   prop: "class1PlanQty",
        //   span: 24,
        //   type: "textarea",
        // },
      ];
    },
    rules() {
      return {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        // mixArea: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "change",
        //   },
        // ],
        glue: [
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
        recipeType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        midProduceOrder: [
          {
            required: !isNaN(this.form.midPlanQty) && this.form.midPlanQty > 0,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        nightProduceOrder: [
          {
            required:
              !isNaN(this.form.nightPlanQty) && this.form.nightPlanQty > 0,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        // dayProduceOrder: [
        //   {
        //     required: !isNaN(this.form.dayPlanQty) && this.form.dayPlanQty > 0,
        //     message: this.$t("common.rule.input"),
        //     trigger: "change",
        //   },
        // ],
      };
    },
  },
  watch: {
    "form.machineCode": function (val) {
      let item = this.machines.find((row) => row.machineCode === val);
      if (item) {
        this.form.machineName = item.machineName;
      }
    },
  },

  methods: {
    // api
    async save(params) {
      try {
        // this.loading = true;
        // let valid = await validateAdd(params);
        // if (valid.msg == "0") {
        //   this.$confirm(
        //     this.$t("ui.data.column.scheduleResult.isContinueAdd")
        //   ).then(async () => {
        //     let result = await save(params);
        //     this.loading = false;
        //     if (result.code == 200) {
        //       this.$emit("success");
        //       this.hide();
        //     }
        //   });
        // } else {
        //   let result = await save(params);
        //   this.loading = false;
        //   if (result.code == 200) {
        //     this.$emit("success");
        //     this.hide();
        //   }
        //   this.loading = false;
        // }
        this.loading = true;
        let result = await editGlueScheduleResult(params);
        this.loading = false;
        if (result.code == 200) {
          this.$modal.msgSuccess(result.msg);
          this.$emit("success");
          this.hide();
        }
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    async getRecipeMachineList(params) {
      try {
        const res = await getRecipeMachineList(params);
        this.machines = res;
      } catch (error) {}
    },

    //utils
    show(data, editType) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
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
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        this.form.treadCode = value.toUpperCase();
      }
    },
    resetRecipeType() {
      this.form = {
        ...this.form,
        recipeTypeName: "",
        recipeType: "",
        recipeVersionId: "",
        recipeStage: "",
      };
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.save(params);
      });
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
    async handleChangeGetMachines() {
      if (this.form.mixArea && this.form.glue) {
        this.getRecipeMachineList({
          mixArea: this.form.mixArea,
          glue: this.form.glue,
        });
      }
    },
    recipeChange(value, row) {
      this.form.recipeTypeName = row.recipeTypeName;
      this.form.recipeVersionId = row.recipeVersionId;
      this.form.recipeStage = row.recipeStage;
    },
  },
};
</script>
