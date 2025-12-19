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

import infoForm from "@/views/components/infoForm.vue";

import { validateAdd, editScheduleResult } from "@/api/tc/tcScheduleResult";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        sidewallCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineId: [
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
    ...mapState({
      machines: (state) => state.sidewall.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.tcScheduleResult.modalName");
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
          label: this.$t("ui.data.column.tcScheduleResult.sidewallCode"),
          prop: "sidewallCode",
          span: 24,
          maxlength: "20",
          listeners: {
            blur: this.toUpperCase,
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          prop: "machineId",
          span: 24,
          type: "select",
          dictData: this.machines,
          labelKey: "machineName",
          valueKey: "id",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightPlanQty"),
          prop: "dayPlanQty",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightHandAnalysis"),
          prop: "dayHandAnalysis",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayPlanQty"),
          prop: "nightPlanQty",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayHandAnalysis"),
          prop: "nightHandAnalysis",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("中班计划量"),
          prop: "nightPlanQty",
          span: 24,
        },
        {
          label: this.$t("中班手动输入原因分析"),
          prop: "nightHandAnalysis",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "class1PlanQty",
          span: 24,
          type: "textarea",
        },
      ];
    },
  },
  methods: {
    // api
    // api
    validateAdd(params) {
      return new Promise(async (resolve, reject) => {
        try {
          let valid = await validateAdd(params);
          if (valid.msg == "0") {
            this.$confirm(
              this.$t("ui.data.column.scheduleResult.isContinueAdd")
            )
              .then(async () => {
                resolve();
              })
              .catch((error) => {
                reject(error);
              });
          } else {
            resolve();
          }
        } catch (error) {
          reject(error);
        }
      });
    },

    async save(params) {
      try {
        this.loading = true;
        await this.validateAdd(params);
        let result = await editScheduleResult(params);
        this.loading = false;
        if (result.code == 200) {
          this.$modal.msgSuccess(result.msg);
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
        };
      }
    },
    hide() {
      this.form = {};
      // this.$refs.form.triggerResetForm();
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
  },
};
</script>
