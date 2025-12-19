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
  validateBeforeAdd,
  validateAdd,
  cxScheduleResultEdit,
  getProductEmbryoVersions,
  getCxMachines,
  getBomData,
} from "@/api/cx/cxScheduleResult";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      versionLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        taskType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sapCode: [
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
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        bomDataVersion: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        cxMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        storageLocation: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      mouldMethod: null,
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    machines: function () {
      if (this.mouldMethod) {
        return this.moldingMachines.filter((row) => {
          return row.mouldMethod + "" === this.mouldMethod;
        });
      }
      return [];
    },

    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.cxAutoPlan");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.cxScheduleResult.factoryCode"),
          prop: "factoryCode",
          span: 24,
          clearable: false,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          clearable: false,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.taskType"),
        //   prop: "taskType",
        //   span: 24,
        //   disabled: true,
        //   type: "select",
        //   dictData: this.parentDict.type.TASK_TYPE,
        // },
        {
          label: this.$t("ui.data.column.cxScheduleResult.sapCode"),
          prop: "sapCode",
          span: 24,
          maxlength: "20",
          listeners: {
            change: this.onEmbryoCodeChange,
            blur: this.toUpperCase,
          },
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.specCode"),
          prop: "specCode",
          span: 24,
          maxlength: "20",
          listeners: {
            change: this.onEmbryoCodeChange,
            blur: this.toUpperCase,
          },
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.embryoCode"),
          prop: "embryoCode",
          span: 24,
          listeners: {
            change: this.onEmbryoCodeChange,
            blur: this.toUpperCase,
          },
        },
        {
          label: this.$t("ui.data.column.productConstruction.embryoVersion"),
          prop: "bomDataVersion",
          span: 24,
          disabled: true,
        },
        // {
        //   label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
        //   prop: "storageLocation",
        //   span: 24,
        //   type: "select",
        //   dictData: this.parentDict.type.STORAGE_LOCATION,
        // },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          span: 24,
          type: "select",
          dictData: this.machines,
          labelKey: "moldingMachineCode",
          valueKey: "moldingMachineCode",
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty"),
          prop: "class1PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1AnalysisInput"),
          prop: "class1AnalysisInput",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2AnalysisInput"),
          prop: "class2AnalysisInput",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3PlanQty"),
          prop: "class4PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3AnalysisInput"),
          prop: "class4AnalysisInput",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4PlanQty"),
          prop: "class5PlanQty",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4AnalysisInput"),
          prop: "class5AnalysisInput",
          span: 24,
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.class5PlanQty"),
        //   prop: "class5AnalysisInput",
        //   span: 24,
        //   type: "number",
        // },
        {
          label: this.$t("ui.data.column.stock.remark"),
          prop: "remark",
          span: 24,
        },
      ];
    },
  },
  methods: {
    // api
    validateBeforeAdd(params) {
      return new Promise((resolve, reject) => {
        validateBeforeAdd(params)
          .then((res) => {
            if (res.msg == "0") {
              this.$confirm(
                this.$t("ui.data.column.scheduleResult.isContinueAdd")
              )
                .then(() => {
                  resolve();
                })
                .catch(() => {
                  reject();
                });
            } else {
              resolve();
            }
          })
          .catch((e) => {
            console.error(e);
            reject();
          });
      });
    },
    validateAdd(params) {
      return new Promise((resolve, reject) => {
        validateAdd(params)
          .then((res) => {
            if (this.isEmpty(res.msg)) {
              resolve();
            } else {
              this.$confirm(result.msg)
                .then(() => {
                  resolve();
                })
                .catch(() => {
                  reject();
                });
            }
          })
          .catch((e) => {
            console.error(e);
            reject();
          });
      });
    },

    async save(params) {
      try {
        this.loading = true;
        // await validateBeforeAdd(params);
        await validateAdd(params);
        const res = await cxScheduleResultEdit(params);

        this.loading = false;
        this.$modal.success(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
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
      this.form = {
        factoryCode: "",
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      };
    },
    hide() {
      // this.form = {};
      this.$refs.form.triggerResetForm();
      this.mouldMethod = null;
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        e.target.value = value.toUpperCase();
      }
    },

    onEmbryoCodeChange() {
      this.$set(this.form, "bomDataVersion", "");
      this.$set(this.form, "cxMachineCode", "");
      this.$set(this.form, "mouldMethod", "");
      this.mouldMethod = null;
      if (!this.form.sapCode || !this.form.specCode || !this.form.embryoCode) {
        return;
      }
      this.versionLoading = true;
      getBomData({
        sapCode: this.form.sapCode,
        specCode: this.form.specCode,
        embryoCode: this.form.embryoCode,
      })
        .then((res) => {
          console.log(res);

          this.$set(this.form, "bomDataVersion", res.bomVersion);
          this.$set(this.form, "mouldMethod", res.mouldMethod);
          this.$set(this.form, "cxMachineCode", "");
          this.mouldMethod = res.mouldMethod;
          this.versionLoading = false;
        })
        .catch((e) => {
          this.versionLoading = false;
        });
      // getCxMachines({ embryoCode: val })
      //   .then((res) => {
      //     this.moldingMachines = res.map((row) => {
      //       return {
      //         machineCode: row.machineCode,
      //         machineName: row.machineName,
      //       };
      //     });
      //   })
      //   .catch((e) => {
      //     console.error(e);
      //     this.moldingMachines = [];
      //   });
    },
  },
};
</script>
