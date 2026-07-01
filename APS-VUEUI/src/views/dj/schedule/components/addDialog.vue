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

import infoForm from "@/views/components/infoForm.vue";

import { listMachine } from "@/api/dj/machine";
import { validateAdd, editScheduleResult, getPaddingDistList } from "@/api/dj/djScheduleResult";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      machines: [],
      paddingList: [],
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
        paddingCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineCode: [
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
    title: function () {
      return this.$t("ui.data.column.djScheduleResult.modalName");
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
          label: this.$t("ui.data.column.dj.scheduleResult.paddingCode"),
          prop: "paddingCode",
          span: 24,
          type: "select",
          dictData: this.paddingList,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.machineCode"),
          prop: "machineCode",
          span: 24,
          type: "select",
          dictData: this.machines,
          props: {
            label: "machineName",
            value: "id",
          },
        },
        // ============ 夜班计划量 ============
        {
          label: this.$t("ui.data.column.scheduleResult.nightPlanQty"),
          prop: "class2PlanQty",
          span: 12,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class2Sequence",
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightHandAnalysis"),
          prop: "class2Analysis",
          span: 24,
          maxlength: "100",
        },
        // ============ 早班计划量 ============
        {
          label: this.$t("ui.data.column.scheduleResult.dayPlanQty"),
          prop: "class3PlanQty",
          span: 12,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class3Sequence",
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayHandAnalysis"),
          prop: "class3Analysis",
          span: 24,
          maxlength: "100",
        },
        // ============ 中班计划量 ============
        {
          label: this.$t("ui.data.column.scheduleResult.midPlanQty"),
          prop: "class1PlanQty",
          span: 12,
        },
        {
          label: this.$t("ui.data.column.dj.scheduleResult.sequence"),
          prop: "class1Sequence",
          span: 12,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.midHandAnalysis"),
          prop: "class1Analysis",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
        },
      ];
    },
  },
  methods: {
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
    loadMachines() {
      listMachine().then((res) => {
        this.machines = res.rows || [];
      });
    },
    loadPaddingList() {
      getPaddingDistList().then((res) => {
        this.paddingList = res.data || [];
      });
    },
    show(data, editType) {
      this.visible = true;
      this.loadMachines();
      this.loadPaddingList();
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
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        this.form.liningCode = value.toUpperCase();
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        // 自定义校验：至少一个班有录入计划量
        const shifts = [
          { qtyProp: "class2PlanQty", seqProp: "class2Sequence", label: this.$t("ui.data.column.scheduleResult.nightPlanQty") },
          { qtyProp: "class3PlanQty", seqProp: "class3Sequence", label: this.$t("ui.data.column.scheduleResult.dayPlanQty") },
          { qtyProp: "class1PlanQty", seqProp: "class1Sequence", label: this.$t("ui.data.column.scheduleResult.midPlanQty") },
        ];

        const hasQty = shifts.filter((s) => params[s.qtyProp] != null && params[s.qtyProp] !== "");
        if (hasQty.length === 0) {
          this.$modal.msgWarning(this.$t("ui.dj.schedule.validate.atLeastOneShiftQty"));
          return;
        }

        for (const s of hasQty) {
          if (!params[s.seqProp] || params[s.seqProp] === "") {
            this.$modal.msgWarning(this.$t("ui.dj.schedule.validate.seqRequired", { shift: s.label }));
            return;
          }
        }

        this.save(params);
      });
    },
  },
};
</script>
