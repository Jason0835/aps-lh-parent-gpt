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
      label-width="150px"
      v-loading="loading"
    >
    </info-form>
    <page-table
      tableRef="formulaMachineInfoMachineTable"
      rowKey="rowKey"
      :columns="tableColumns"
      :data="tableData"
      @selection-change="handleSelectionChange"
    >
      <template slot="header">
        <el-button
          :disabled="!this.form.mixArea || !this.form.glue"
          @click="handleAddColumn"
          >{{$t("common.button.add")}}</el-button
        >
        <el-button :disabled="selection.length === 0" @click="handleDelete"
          >{{$t("common.button.delete")}}</el-button
        >
      </template>
    </page-table>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import { saveFormulaMachine } from "@/api/setting/formulaMachine";
import { selectMesPmtRecipeMachine } from "@/api/setting/MesPmtRecipe";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        mixArea: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        glue: [
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
    };
  },
  computed: {
    title: function () {
      return this.$t("setting.type.modelName");
    },
    columns() {
      return [
        {
          label: this.$t("setting.formulaMachine.mixArea"),
          prop: "mixArea",
          maxlength: "10",
          required: true,
          type: "select", //MIX_AREA
          dictData: this.parentDict.type.MIX_AREA,
          disabled: this.isEdit,
          listeners: {
            change: this.getMachines,
          },
        },
        {
          label: this.$t("setting.formulaMachine.glue"),
          prop: "glue",
          maxlength: "50",
          required: true,
          disabled: this.isEdit,
          listeners: {
            change: this.getMachines,
          },
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
        },
      ];
    },
    tableColumns() {
      return [
        {
          type: "selection",
        },
        {
          type: "index",
        },
        {
          label: this.$t("setting.formulaMachine.machineCode"),
          prop: "machineCode",
          render: ({ row, $index }) => {
            return (
              <el-select v-model={row.machineCode}>
                {this.machines.map((item) => {
                  return (
                    <el-option
                      key={`machine-${$index}-${item.recipeEquipCode}`}
                      value={item.recipeEquipCode}
                      label={item.machineName}
                    ></el-option>
                  );
                })}
              </el-select>
            );
          },
        },
        {
          label: this.$t("setting.formulaMachine.machineOrder"),
          prop: "machineOrder",
          render: ({ row }) => {
            return <el-input v-model={row.machineOrder} />;
          },
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
        const data = await saveFormulaMachine(params);
        this.$modal.msgSuccess("操作成功");
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async getMachines() {
      if (!this.form.mixArea || !this.form.glue) {
        if (this.machines.length) {
          this.machines = [];
        }
        if (this.tableData.length) {
          this.tableData = [];
        }
        return;
      }

      try {
        const res = await selectMesPmtRecipeMachine({
          mixArea: this.form.mixArea,
          recipeMaterialName: this.form.glue,
        });

        this.machines = res;
      } catch (error) {}
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          id: data.id,
          mixArea: data.mixArea,
          glue: data.glue,
          remark: data.remark,
        };
        const codes = data.machineCode.split(",");
        const names = data.machineName.split(",");
        const orders = data.machineOrder.split(",");

        this.tableData = codes.map((code, index) => {
          return {
            rowKey: `${code}-${index}`,
            machineCode: code,
            machineOrder: orders[index],
          };
        });
        this.getMachines();
      } else {
        this.form = {};
      }
    },
    hide() {
      this.machines = [];
      this.tableData = [];
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      if (this.tableData.length === 0) {

        this.$modal.msgError(this.$t("setting.formulaMachine.mustAddOneRecord"))
        return;
      }

    const codeSet = new Set();
    const orderSet = new Set();

    this.tableData.forEach(row => {
      codeSet.add(row.machineCode)
      orderSet.add(row.machineOrder)
    })

    if(codeSet.size !==  this.tableData.length) {
      this.$modal.msgError(this.$t("setting.formulaMachine.sameMachine"))
      return;
    }
    if(orderSet.size !==  this.tableData.length) {
      this.$modal.msgError(this.$t("setting.formulaMachine.sameOrder"))
      return;
    }




      this.$refs.form.triggerConfirm((params) => {
        this.save({
          ...params,
          machineOrderList: this.tableData.map((row) => {
            return {
              machineCode: row.machineCode,
              machineOrder: row.machineOrder,
            };
          }),
        });
      });
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleAddColumn() {
      if (!this.form.glue || !this.form.mixArea) {
        this.$modal.msgError(
          this.$t("ui.message.filterMachine.mixAreaAndGlueNotNull")
        );
        return;
      }
      var row = {
        rowKey: Date.now(),
        machineName: "",
        machineCode: "",
        machineOrder: "",
      };
      this.tableData.push(row);
    },
    handleDelete() {
      let rowKeys = this.selection.map((row) => row.rowKey);
      this.tableData = this.tableData.filter((row) => {
        return !rowKeys.includes(row.rowKey);
      });
    },
  },
};
</script>
