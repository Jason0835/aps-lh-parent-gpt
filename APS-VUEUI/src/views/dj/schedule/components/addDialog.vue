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
import { validateAdd, editScheduleResult, getPaddingDistList, getCurrentShift } from "@/api/dj/djScheduleResult";

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
      // 连续3个班次信息（来自 getCurrentShift API）
      currentShiftData: null,
      shiftList: [],
      // 排产起始班次（首班班次），用于提交后端计算实际排程日期和班次
      startShiftClass: null,
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
      const seqLabel = this.$t("ui.data.column.dj.scheduleResult.sequence");
      const analysisLabel = this.$t("ui.data.column.dj.scheduleResult.analysis");
      const planQtyLabel = this.$t("ui.data.column.dj.scheduleResult.planQty");
      const shiftLabels = this.shiftList.map((s) => s.label || "");
      const colDefs = [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
          disabled: true,
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
      ];

      // 动态生成连续3个班次的字段，每个班次前加标题区隔
      for (let i = 0; i < 3; i++) {
        const label = shiftLabels[i] || "class" + (i + 1);
        const classIdx = i + 1;

        // 标题区隔：x班 mm/dd
        colDefs.push({
          type: "title",
          label: label,
        });

        colDefs.push({
          label: planQtyLabel,
          prop: "class" + classIdx + "PlanQty",
          span: 12,
        });
        colDefs.push({
          label: seqLabel,
          prop: "class" + classIdx + "Sequence",
          span: 12,
        });
        colDefs.push({
          label: analysisLabel,
          prop: "class" + classIdx + "Analysis",
          span: 24,
          maxlength: "100",
        });
      }

      colDefs.push({
        label: this.$t("ui.common.column.remark"),
        prop: "remark",
        span: 24,
        type: "textarea",
      });
      return colDefs;
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
        this.paddingList = res || [];
      });
    },
    loadCurrentShift() {
      this.loading = true;
      getCurrentShift().then((res) => {
        this.loading = false;
        // res 是 AjaxResult 解包后的 data
        if (res) {
          this.currentShiftData = res;
          this.shiftList = res.shifts || [];
          this.startShiftClass = res.currentShiftClass || null;
          if (res.scheduleDate) {
            this.form.scheduleDate = res.scheduleDate;
          }
        }
      }).catch(() => {
        this.loading = false;
      });
    },
    show(data) {
      this.visible = true;
      // 新建时调用 getCurrentShift 获取当前班次
      if (!data) {
        this.loadCurrentShift();
      }
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
      this.shiftList = [];
      this.currentShiftData = null;
      this.startShiftClass = null;
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
        // 传入排产起始班次，供后端计算实际排程日期和班次
        params.scheduleShiftClass = this.startShiftClass;

        const groupLabels = this.shiftList.map((s) => s.label || "class" + s.classIndex);
        // 自定义校验：至少一个班有录入计划量
        const shifts = [
          { qtyProp: "class1PlanQty", seqProp: "class1Sequence", label: groupLabels[0] },
          { qtyProp: "class2PlanQty", seqProp: "class2Sequence", label: groupLabels[1] },
          { qtyProp: "class3PlanQty", seqProp: "class3Sequence", label: groupLabels[2] },
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
