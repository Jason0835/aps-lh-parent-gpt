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
    <page-table
      :columns="columns"
      :data="tableData"
      :toolbar="false"
    ></page-table>
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

import { saveCxPersionTrainSettingList } from "@/api/cx/cxPersionTrainSetting.js";

export default {
  components: {},
  inject: ["parentDict"],
  props: {},
  data() {
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
            trigger: "blur",
          },
        ],
        productTypeCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        moldingMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        moldingMachineClassId: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        // machineName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
        // quata: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
      tableData: [],
    };
  },
  computed: {
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        { type: "index" },
        {
          label: this.$t("成型法"),
          prop: "mouldMethod",
          render: ({ row }) => {
            return (
              <dict-select
                disabled={row.id ? true : false}
                options={this.parentDict.type.molding_method}
                v-model={row.mouldMethod}
              />
            );
          },
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          disabled: this.isEdit,
        },
        {
          label: this.$t("早班"),
          prop: "quotaClass1",
          render: ({ row }) => {
            return <el-input v-model={row.quotaClass1} />;
          },
        },
        {
          label: this.$t("晚班"),
          prop: "quotaClass2",
          span: 24,
          render: ({ row }) => {
            return <el-input v-model={row.quotaClass2} />;
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

        const res = await saveCxPersionTrainSettingList(params);
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
    show(data, scheduleDate) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.tableData = data;
        if (data.length === 1) {
          this.tableData.push({ scheduleDate: scheduleDate });
        }
      } else {
        this.tableData = [
          { scheduleDate: scheduleDate },
          { scheduleDate: scheduleDate },
        ];
      }
    },
    hide() {
      this.tableData = [];
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    checkMachineCode(rule, value, callback) {
      return new Promise((resolve, reject) => {
        checkMachineCodeUnique({
          id: this.form.id,
          machineCode: this.form.machineCode,
        })
          .then((res) => {
            if (res === 0) {
              resolve();
            } else {
              reject(new Error(this.$t("ui.data.column.cx.machine.message")));
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error("验证失败，请稍后再试"));
          });
      });
    },
    checkMachineName() {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          checkMachineCodeUnique({
            id: this.form.id,
            machineName: this.form.machineName,
          })
            .then((res) => {
              if (res === 0) {
                resolve();
              } else {
                reject(
                  new Error(this.$t("ui.data.column.cx.machineName.message"))
                );
              }
            })
            .catch((error) => {
              console.error(error);
              reject(new Error("验证失败，请稍后再试"));
            });
        }, 201);
      });
    },

    handleConfirm() {
      let str = [];
      let set = new Set();
      let count = 0;
      this.tableData.forEach((row, index) => {
        if (row.mouldMethod) {
          set.add(row.mouldMethod);
          count += 1;
          let arr = [];
          if (!row.quotaClass1) {
            arr.push(this.$t("早班"));
          }
          if (!row.quotaClass2) {
            arr.push(this.$t("晚班"));
          }
          if (arr.length) {
            str.push(`行序号${index}：请输入${arr.join(",")}`);
          }
        }
      });
      if (set.size !== count) {
        this.$modal.msgError("存在相同的成型法");
        return;
      }

      if (str.length) {
        this.$modal.msgErrorHtml(str.join("<br/>"));
        return;
      }

      this.save(this.tableData);
    },
  },
};
</script>
