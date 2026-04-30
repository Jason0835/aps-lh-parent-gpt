<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1200px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    fullscreen
  >
    <info-form
      ref="form"
      :form="form"
      :rules="rules"
      :columns="formColumns"
      label-position="right"
      label-width="auto"
      v-loading="loading"
    >
    </info-form>
    <div v-loading="loading" class="table-container">
      <el-row>
        <el-col :span="24">
          <el-card class="table-box-card" shadow="never">
            <template slot="header">
              <div>{{ $t("ui.data.column.lhApsMoldAdjustPlan.detail") }}</div>
            </template>
            <div>
              <t-form
                ref="valiFormRef"
                :model="valiForm"
                :show-message="true"
                :inline-message="true"
              >
                <page-table
                  tableRef="addMoldAdjustPlanTableRef"
                  height="150px"
                  :columns="columns"
                  :data="data"
                  :toolbar="false"
                  @selection-change="handleSelectionChange"
                >
                  <template slot="header">
                    <el-button :loading="loading" @click="handleAdd">{{
                      this.$t("ui.frame.btn.add")
                    }}</el-button>
                    <el-button @click="handleDelete">{{
                      this.$t("ui.frame.btn.delete")
                    }}</el-button>
                  </template>
                </page-table>
              </t-form>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <template slot="footer">
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.save")
      }}</el-button>
      <el-button @click="hide">{{ this.$t("common.button.close") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { mapState } from "vuex";

import { addSubData, checkIsRequired } from "@/api/lh/lhApsMoldAdjustPlan";

import infoForm from "@/views/components/infoForm.vue";

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
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        { type: "selection", fixed: "left" },
        {
          type: "index",
          label: this.$t("ui.data.column.scheduleResult.no"),
          fixed: "left",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeMoldTime"),
          prop: "changeMoldTime",
          minWidth: 100,
          render: ({ row }) => {
            return (
              <el-date-picker
                class="w100"
                type="datetime"
                v-model={row.changeMoldTime}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeType"),
          prop: "changeType",
          minWidth: 100,
          render: ({ row }) => {
            return (
              <dict-select
                v-model={row.changeType}
                options={this.parentDict.type.MOLD_CHANGE_TYPE}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeSapCode"),
          prop: "beforeSapCode",
          minWidth: 100,
          render: ({ row }) => {
            return <el-input v-model={row.beforeSapCode} />;
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeEmbryoCode"),
          prop: "beforeEmbryoCode",
          minWidth: 100,
          render: ({ row }) => {
            return <el-input v-model={row.beforeEmbryoCode} />;
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.tireRoughStock"),
          prop: "tireRoughStock",
          minWidth: 100,
          render: ({ row }) => {
            return <el-input v-model={row.tireRoughStock} />;
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.useMoldNumber"),
          prop: "useMoldNumber",
          minWidth: 100,
          render: ({ row }) => {
            return <el-input v-model={row.useMoldNumber} />;
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.leftRightMold"),
          prop: "leftRightMold",
          minWidth: 100,
          render: ({ row }) => {
            return <el-input v-model={row.leftRightMold} />;
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterSapCode"),
          prop: "afterSapCode",
          minWidth: 100,
          required: true,
          render: ({ row, $index }) => {
            return (
              <el-form-item
                prop={"afterSapCode" + $index}
                rules={{
                  required: true,
                  message: "第 " + ($index + 1) + " 行 后SAP号为空",
                  trigger: "blur",
                }}
              >
                <el-input
                  v-model={row.afterSapCode}
                  onInput={(val) => {
                    this.valiForm["afterSapCode" + $index] = val;
                  }}
                />
              </el-form-item>
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterEmbryoCode"),
          prop: "afterEmbryoCode",
          minWidth: 100,
          required: true,
          // render: ({ row }) => {
          //   return <el-input v-model={row.afterEmbryoCode} />;
          // },
          render: ({ row, $index }) => {
            return (
              <el-form-item
                prop={"afterEmbryoCode" + $index}
                rules={{
                  required: true,
                  message: "第 " + ($index + 1) + " 行 后规格胎胚为空",
                  trigger: "blur",
                }}
              >
                <el-input
                  v-model={row.afterEmbryoCode}
                  onInput={(val) => {
                    this.valiForm["afterEmbryoCode" + $index] = val;
                  }}
                />
              </el-form-item>
            );
          },
        },

        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          minWidth: 100,
          render: ({ row }) => {
            return <el-input v-model={row.remark} />;
          },
        },
      ],
      data: [
        {
          changeMoldTime: "",
          changeType: "",
          beforeSapCode: "",
          beforeEmbryoCode: "",
          tireRoughStock: "",
          useMoldNumber: "",
          leftRightMold: "",
          afterSapCode: "",
          afterEmbryoCode: "",
          remark: "",
        },
      ],
      selection: [],
      valiForm: {
        afterSapCode0: "",
        afterSapCode1: "",
        afterEmbryoCode0: "",
        afterEmbryoCode1: "",
      },
    };
  },
  computed: {
    ...mapState({
      curingMachines: (state) => state.curing.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.lhApsMoldAdjustPlan.modelName");
    },
    formColumns() {
      return [
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.lhMachineCode"),
          prop: "lhMachineCode",
          span: 12,
          type: "select",
          // dictData: this.curingMachines,
          // valueKey: "machineCode",
          // labelKey: "machineName",
          // listeners: {
          //   change: this.curingMachineChange,
          // },
          render: (form) => {
            return (
              <el-select
                class="w100"
                v-model={form.lhMachineCode}
                onChange={this.curingMachineChange}
              >
                {this.curingMachines.map((row) => {
                  return (
                    <el-option
                      key={row.machineCode}
                      value={row.machineCode}
                      label={row.machineCode}
                    ></el-option>
                  );
                })}
              </el-select>
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.planDate"),
          prop: "planDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          span: 12,
          clearable: false,
        },
      ];
    },
  },
  methods: {
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    // api

    // checkIsRequired(params) {
    //   return new Promise((resole, reject) => {
    //     checkIsRequired(params)
    //       .then(() => {
    //         resolve();
    //       })
    //       .catch((e) => {
    //         reject(e);
    //       });
    //   });
    // },

    async save(params) {
      try {
        this.loading = true;
        await checkIsRequired(params);
        const data = await addSubData(params);
        this.$modal.msgSuccess(data.msg);
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
    handleAdd() {
      if (this.data.length < 2) {
        this.data.push({
          changeMoldTime: "",
          changeType: "",
          beforeSapCode: "",
          beforeEmbryoCode: "",
          tireRoughStock: "",
          useMoldNumber: "",
          leftRightMold: "",
          afterSapCode: "",
          afterEmbryoCode: "",
          remark: "",
        });
      } else {
        this.$modal.msgError(
          this.$t("ui.data.message.lhApsMoldAdjustPlan.atMostTwoRecord")
        );
      }
    },
    handleDelete() {
      if (this.data.length == 0) {
        this.$modal.msgError(
          this.$t("ui.data.message.lhApsMoldAdjustPlan.atLeastOneRecord")
        );
        return;
      } else if (this.selection.length == 0) {
        this.$modal.msgError(this.$t("请至少选择一条记录"));
        return;
      }
      console.log(this.data, this.selection);
      this.data = this.data.filter((el) => {
        return !this.selection.includes(el);
      });
    },

    curingMachineChange(val, row) {
      this.form.lhMachineName = row.machineName;
    },

    async handleConfirm() {
      let formData = await this.$refs.form.triggerConfirm();
      // let tableValid = await this.$refs.valiFormRef.validate().catch(()=>{});
      this.$refs.valiFormRef.validate((valid) => {
        if (formData && valid) {
          this.save({
            ...formData,
            apsMoldAdjustPlanList: this.data,
          });
        }
        // console.log(formData, valid);
      });
      // this.$refs.form.triggerConfirm((formData) => {
      //   this.$refs.valiFormRef.validate((valid) => {
      //     if (valid) {
      //       this.save({
      //         ...formData,
      //         apsMoldAdjustPlanList: this.data,
      //       });
      //     }
      //   });
      // });
    },
  },
};
</script>
<style scoped>
::v-deep .table-box-card .el-card__header {
  padding: 10px 20px;
  /* color: #515a6e; */
  font-weight: bold;
}
</style>
