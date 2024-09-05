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
      :defaultValue="defaultValue"
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

import infoForm from "@/views/components/infoForm.vue";

import {
  autoPlan,
  validateAutoPlan,
  lhAutoPlan,
  lhValidateAutoPlan,
  modelChange,
  modelChangeValidate,
  modelAdjustPlan,
} from "@/api/cx/cxScheduleResult";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      defaultValue: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.taskType"),
          prop: "taskType",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.sapCode"),
          prop: "sapCode",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.embryoCode"),
          prop: "embryoCode",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.productConstruction.embryoVersion"),
          prop: "embryoVersion",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
          prop: "storageLocation",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty"),
          prop: "class1PlanQty",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2AnalysisInput"),
          prop: "class2AnalysisInput",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3PlanQty"),
          prop: "class3PlanQty",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3AnalysisInput"),
          prop: "class3AnalysisInput",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4PlanQty"),
          prop: "class4PlanQty",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4AnalysisInput"),
          prop: "class4AnalysisInput",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5PlanQty"),
          prop: "class5AnalysisInput",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.stock.remark"),
          prop: "remark",
          span: 24,
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.cxAutoPlan");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      if (this.editType == "1") {
        this.autoPlan(params);
      } else if (this.editType == "2") {
        this.lhAutoPlan({
          scheduleDate: params.scheduleDate,
        });
      } else {
        throw "editType error";
      }
    },
    async autoPlan(params) {
      try {
        this.loading = true;
        let valid = await validateAutoPlan(params);
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.biz.alter.makeSureRecreateByPublished")
          ).then(async () => {
            let result = await autoPlan(params);
            this.loading = false;
            if (result.code == 200) {
              this.$emit("success");
              this.hide();
            }
          });
        } else if (valid.msg == "1") {
          this.$confirm(this.$t("ui.biz.alter.makeSureRecreate")).then(
            async () => {
              let result = await autoPlan(params);
              this.loading = false;
              if (result.code == 200) {
                this.$emit("success");
                this.hide();
              }
            }
          );
        } else if (valid.msg == "2") {
          let result = await autoPlan(params);
          this.loading = false;
          if (result.code == 200) {
            this.$emit("success");
            this.hide();
          }
        } else if (valid.msg == "3") {
          this.loading = false;
          this.$modal.warning(this.$t("ui.biz.alter.CanNotRecreate"));
        } else {
          this.loading = false;
          this.$modal.warning(result.msg);
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async lhAutoPlan(params) {
      try {
        this.loading = true;
        const valid = await lhValidateAutoPlan(params);
        if (valid.msg == "1") {
          this.$confirm(this.$t("ui.biz.alter.makeSureRecreate")).then(
            async () => {
              let result = await lhAutoPlan(params);
              this.loading = false;
              if (result.code == 200) {
                this.$emit("success");
                this.hide();
              }
            }
          );
        } else if (valid.msg == "2") {
          let result = await lhAutoPlan(params);
          this.loading = false;
          if (result.code == 200) {
            this.$emit("success");
            this.hide();
          }
        } else if (valid.msg == "3") {
          this.$modal.warning(this.$t("ui.biz.alter.CanNotRecreate"));
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async modelChange(params) {
      try {
        this.loading = true;
        const valid = await modelChangeValidate(params);

        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.modelChangeValidate")
          ).then(async () => {
            await modelChange(params);
            this.loading = false;
            this.$emit("success");
            this.hide();
          });
        } else {
          await modelChange(params);
          this.loading = false;
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async modelAdjustPlan(params) {
      try {
        this.loading = true;

        await modelAdjustPlan(params);
        this.loading = false;
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      this.editType = editType;
      // if (data) {
      //   this.isEdit = true;
      //   this.defaultValue = {
      //     ...data,
      //   };
      // }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(() => {
        if (this.editType === "1") {
        }
      });
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
