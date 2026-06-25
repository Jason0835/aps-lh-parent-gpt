<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
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
      label-width="120px"
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
import infoForm from "@/views/components/infoForm.vue";
import { insertOrder, validateInsertOrder, listScheduleShiftDates } from "@/api/tq/tqNewScheduleResult";
import { listEnabledMachines } from "@/api/tq/machine";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
      tqMachines: [],
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        beadCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      dateList: [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.btn.tqNewScheduleResult.insertOrder");
    },
    columns() {
      const columns = [
        {
          type: "title",
          label: this.$t("ui.data.column.tqNewScheduleResult.baseInfo"),
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
          listeners: {
            change: this.handleScheduleDateChange,
          },
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.machineCode"),
          prop: "machineCode",
          type: "select",
          dictData: this.tqMachines,
          filterable: true,
          clearable: true,
        },
        {
          label: this.$t("ui.data.column.tqNewScheduleResult.beadCode"),
          prop: "beadCode",
        },
      ];

      // 动态生成6个班次表单区域
      for (let i = 1; i <= 6; i++) {
        columns.push(
          { type: "title", label: this.shiftBannerTitle(i) },
          {
            label: this.$t("ui.data.column.tqNewScheduleResult.planQty"),
            prop: `class${i}PlanQty`,
            span: 8,
            type: "number",
          },
          {
            label: this.$t("ui.data.column.tqNewScheduleResult.sequence"),
            prop: `class${i}Sequence`,
            span: 8,
            type: "number",
          },
          {
            label: this.$t("ui.data.column.tqNewScheduleResult.analysis"),
            prop: `class${i}Analysis`,
            span: 8,
          }
        );
      }

      columns.push({
        label: this.$t("ui.common.column.remark"),
        prop: "remark",
        type: "textarea",
        span: 24,
      });

      return columns;
    },
  },
  methods: {
    /** 班次名称映射 */
    shiftPeriodName(shiftType) {
      const map = {
        night: this.$t("ui.data.column.scheduleResult.nightShift"),
        morning: this.$t("ui.data.column.scheduleResult.morningShift"),
        afternoon: this.$t("ui.data.column.scheduleResult.middleShift"),
      };
      return map[shiftType] || "";
    },
    /** 班次标题：班次名称 + 日期 */
    shiftBannerTitle(classIndex) {
      const item = this.dateList[classIndex - 1];
      if (!item) return "";
      const shiftName = this.shiftPeriodName(item.shiftType);
      return shiftName + " " + (item.shiftDate || "");
    },
    /** 获取班次日期列表 */
    async fetchScheduleShiftDates(scheduleDate) {
      const empty = [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ];
      if (!scheduleDate) {
        this.dateList = empty;
        return;
      }
      try {
        const res = await listScheduleShiftDates({ scheduleDateQuery: scheduleDate });
        if (Array.isArray(res) && res.length) {
          this.dateList = res;
        } else {
          this.dateList = empty;
        }
      } catch (error) {
        console.error(error);
        this.dateList = empty;
      }
    },
    /** 加载胎圈机台列表 */
    async loadMachines() {
      try {
        const res = await listEnabledMachines();
        this.tqMachines = (res || []).map((r) => ({
          label: r.machineName,
          value: r.machineCode,
        }));
      } catch (error) {
        console.error(error);
        this.tqMachines = [];
      }
    },
    /** 排程日期变更 */
    handleScheduleDateChange() {
      this.fetchScheduleShiftDates(this.form.scheduleDate);
    },
    /** 提交插单 */
    async save(params) {
      try {
        this.loading = true;
        // 先校验
        const validateRes = await validateInsertOrder(params);
        if (validateRes.code === 200) {
          // 校验通过，执行插单
          const res = await insertOrder(params);
          this.$modal.msgSuccess(res.msg);
          this.$emit("success");
          this.hide();
        } else {
          this.$modal.msgError(validateRes.msg || "校验失败");
        }
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    /** 打开弹窗 */
    async show(data) {
      this.visible = true;
      const nowDate = new Date();
      const offsetDate = new Date(nowDate);
      offsetDate.setDate(nowDate.getDate() + 2);
      const defaultScheduleDate = offsetDate.toISOString().slice(0, 10);

      const form = {
        scheduleDate: defaultScheduleDate,
      };

      if (data) {
        // 从列表行数据中回填
        const keys = ["scheduleDate", "machineCode", "beadCode"];
        for (let i = 1; i <= 6; i++) {
          keys.push(`class${i}PlanQty`, `class${i}Sequence`, `class${i}Analysis`);
        }
        keys.forEach((k) => {
          if (data[k] !== undefined && data[k] !== null) {
            form[k] = data[k];
          }
        });
      }

      this.form = form;
      await this.loadMachines();
      await this.fetchScheduleShiftDates(this.form.scheduleDate);
    },
    /** 关闭弹窗 */
    hide() {
      this.form = {};
      this.dateList = [
        { shift: 1, shiftType: "afternoon", shiftDate: "" },
        { shift: 2, shiftType: "night", shiftDate: "" },
        { shift: 3, shiftType: "morning", shiftDate: "" },
        { shift: 4, shiftType: "afternoon", shiftDate: "" },
        { shift: 5, shiftType: "night", shiftDate: "" },
        { shift: 6, shiftType: "morning", shiftDate: "" },
      ];
      this.$refs.form.triggerResetForm();
      this.visible = false;
    },
    /** 确认按钮 */
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
