<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div v-loading="loading">
      <info-form
        class="form-item-height"
        ref="form"
        :form="form"
        :rules="rules"
        :columns="columns"
        label-position="right"
        label-width="120px"
      >
      </info-form>
      <page-table
        ref="tableRef"
        tableRef="mixRubberScheduleChangeMachineRecipeTable"
        :select-on-indeterminate="false"
        :columns="tableColumns"
        :data="tableData"
        @select="handleSelectChange"
        @select-all="handleSelectAll"
      >
      </page-table>
    </div>
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

import infoForm from "@/views/components/infoForm.vue";

import { changeMachine } from "@/api/schedule/glueScheduleResult.js";
import { getFormulaMachineList } from "@/api/setting/formulaMachine";
import { selectMesPmtRecipeByParams } from "@/api/setting/MesPmtRecipe";

import PageTable from "@/components/Table/PageTable.vue";

export default {
  components: { infoForm, PageTable },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        machineId: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      machines: [],
      tableData: [],
      selection: [],
      currentRow: null,
    };
  },
  computed: {
    title: function () {
      return this.$t("schedule.glueScheduleResult.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("setting.machine.machineName"),
          prop: "machineCode",
          span: 12,
          type: "select",
          dictData: this.machines,
          valueKey: "machineCode",
          labelKey: "machineName",
          listeners: {
            change: this.handleMachineChange
          }
        },
        {
          label: this.$t("schedule.glueScheduleResult.midProduceOrder"),
          prop: "midProduceOrder",
          span: 12,
        },
        {
          label: this.$t("schedule.glueScheduleResult.nightProduceOrder"),
          prop: "nightProduceOrder",
          span: 12,
        },
        {
          label: this.$t("setting.MesPmtRecipe.machineName"),
          prop: "machineName",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeMaterialName"),
          prop: "glue",
          span: 12,
          disabled: true,
        },
        {
          label:
            this.$t("schedule.glueScheduleResult.mid") +
            this.$t("schedule.glueScheduleResult.plan"),
          prop: "midPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label:
            this.$t("schedule.glueScheduleResult.night") +
            this.$t("schedule.glueScheduleResult.plan"),
          prop: "nightPlanQty",
          span: 12,
          disabled: true,
        },
      ];
    },
    tableColumns() {
      return [
        {
          type: "selection",
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeId"),
          prop: "recipeId",
        },
        {
          label: this.$t("setting.MesPmtRecipe.machineName"),
          prop: "machineName",
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeMaterialCode"),
          prop: "recipeMaterialCode",
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeMaterialName"),
          prop: "recipeMaterialName",
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeTypeName"),
          prop: "recipeTypeName",
        },
        {
          label: this.$t("setting.MesPmtRecipe.productStage"),
          prop: "productStage",
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.PRODUCT_STAGE,
              value
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

        const result = await changeMachine(params);
        if (result.code == 200) {
          this.$modal.msgSuccess("操作成功");
          this.$emit("success");
          this.hide();
        }

        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async getFormulaMachineList() {
      try {
        const res = await getFormulaMachineList({
          mixArea: this.form.mixArea,
          glue: this.form.glue,
        });
        this.machines = res;
      } catch (error) {}
    },
    async selectMesPmtRecipeByParams() {
      try {
        const res = await selectMesPmtRecipeByParams({
          recipeMaterialName: this.form.glue,
          machineName: this.form.machineName,
        });
        this.tableData = res.rows;

        this.$nextTick(() => {
          const selects = this.tableData.filter((row) => {
            return (
              row.recipeType === this.form.recipeType &&
              row.recipeTypeName === this.form.recipeTypeName &&
              row.recipeVersionId === this.form.recipeVersionId &&
              row.productStage === this.form.recipeStage
            );
          });

          if (selects.length) {
            selects.forEach((row) => {
              this.$refs.tableRef.getTableRef().toggleRowSelection(row, true);
              this.currentRow = row;
              this.onCheck(row);
            });
          }
        });
      } catch (error) {
        this.tableData = [];
      }
    },

    //utils
    async show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
        await this.getFormulaMachineList();
        await this.selectMesPmtRecipeByParams();
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.resetForm();
      this.isEdit = false;
      this.visible = false;
    },
    onCheck(row) {
      if (row) {
        this.form.recipeType = row.recipeType;
        this.form.recipeTypeName = row.recipeTypeName;
        this.form.recipeVersionId = row.recipeVersionId;
        this.form.recipeStage = row.productStage;
      } else {
        this.form.recipeType = "";
        this.form.recipeTypeName = "";
        this.form.recipeVersionId = "";
        this.form.recipeStage = "";
      }
    },
    handleMachineChange(val) {
      console.log(val)
      let current = this.machines.find((machine) => machine.machineCode === val)
      if(current) {
        this.form.machineName = current.machineName;
        this.selectMesPmtRecipeByParams();

      }

    },
    handleSelectChange(selection, row) {
      console.log(selection, row);
      if (selection.length) {
        this.$refs.tableRef.getTableRef().clearSelection();
        this.$refs.tableRef.getTableRef().toggleRowSelection(row, true);
        this.currentRow = row;
        this.onCheck(row);
      } else {
        this.currentRow = null;
        this.onCheck(null);
      }
    },
    handleSelectAll(selection) {
      this.currentRow = null;
    },

    handleConfirm() {
      if (this.currentRow == null) {
        this.$modal.msgError(this.$t("ui.message.mustChooseOneRecipe"));
        return;
      }
      this.$refs.form.triggerConfirm((params) => {
        this.save({
          ...params,
        });
      });
    },
  },
};
</script>
