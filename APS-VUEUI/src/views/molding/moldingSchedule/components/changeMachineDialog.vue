<template>
  <el-dialog
    :title="$t('ui.data.column.scheduleResult.changeMachine')"
    :visible="visible"
    width="92%"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div v-loading="loading">
      <div class="toolbar-row mb10">
        <el-form :inline="true" label-position="right" class="query-form">
          <el-form-item :label="$t('ui.data.column.unscheduleResult.scheduleDate')">
            <el-date-picker
              v-model="query.scheduleDate"
              type="date"
              value-format="yyyy-MM-dd"
              class="w180"
            />
          </el-form-item>
          <el-form-item :label="$t('ui.data.column.scheduleResult.oldMachine')">
            <el-select
              class="w180"
              v-model="query.cxMachineCode"
              filterable
              clearable
            >
              <el-option
                v-for="item in moldingMachines"
                :key="item.cxMachineCode || item.moldingMachineCode"
                :label="item.cxMachineCode || item.moldingMachineCode"
                :value="item.cxMachineCode || item.moldingMachineCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">{{
              $t("ui.frame.btn.search")
            }}</el-button>
          </el-form-item>
        </el-form>
        <el-form
          v-if="selection.length"
          :inline="true"
          label-position="right"
          class="new-machine-form"
        >
          <el-form-item
            :label="$t('ui.data.column.scheduleResult.newMachine')"
            class="new-machine-item"
          >
            <el-select
              class="w180"
              v-model="form.newCxMachineCode"
              filterable
              clearable
            >
              <el-option
                v-for="item in moldingMachines"
                :key="`new-${item.cxMachineCode || item.moldingMachineCode}`"
                :label="item.cxMachineCode || item.moldingMachineCode"
                :value="item.cxMachineCode || item.moldingMachineCode"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <page-table
        tableRef="MoldingChangeMachineTable"
        :calcHeight="false"
        :columns="columns"
        :data="data"
        :page="page"
        :showSummary="false"
        :selectArea="false"
        @pageChange="handlePageChange"
        @selection-change="handleSelectionChange"
      />
    </div>

    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!form.newCxMachineCode"
        @click="handleConfirm"
      >{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { mapState } from "vuex";
import { changeMachine, listCxScheduleResult } from "@/api/cx/cxScheduleResult";
import { getScheduleDate } from "@/api/lh/scheduleResult";

export default {
  data() {
    return {
      visible: false,
      loading: false,
      data: [],
      selection: [],
      query: {
        scheduleDate: "",
        cxMachineCode: "",
      },
      form: {
        newCxMachineCode: "",
      },
      dateList: Array.from({ length: 8 }, (_, i) => ({
        shift: i + 1,
        shiftDate: "",
      })),
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.unscheduleResult.scheduleDate"),
          prop: "scheduleDate",
          align: "center",
          minWidth: 110,
        },
        { label: this.$t("工单号"), prop: "orderNo", align: "center", minWidth: 120 },
        { label: this.$t("成型批次号"), prop: "cxBatchNo", align: "center", minWidth: 120 },
        { label: this.$t("成型机台"), prop: "cxMachineCode", align: "center" },
        { label: this.$t("硫化机台"), prop: "lhMachineCode", align: "center" },
        { label: this.$t("物料编码"), prop: "materialCode", align: "center", minWidth: 120 },
        { label: this.$t("物料描述"), prop: "materialDesc", minWidth: 220 },
        {
          label: `${this.$t("一班")} ${this.dateList[0].shiftDate}`,
          children: [
            { prop: "class1PlanQty", label: this.$t("计划"), align: "center" },
            { prop: "class1FinishQty", label: this.$t("实际"), align: "center" },
            { prop: "class1Analysis", label: this.$t("原因分析"), align: "center", minWidth: 140 },
            { prop: "class1RecipeType", label: this.$t("示方类型"), align: "center", minWidth: 100 },
          ],
        },
        {
          label: `${this.$t("二班")} ${this.dateList[1].shiftDate}`,
          children: [
            { prop: "class2PlanQty", label: this.$t("计划"), align: "center" },
            { prop: "class2FinishQty", label: this.$t("实际"), align: "center" },
            { prop: "class2Analysis", label: this.$t("原因分析"), align: "center", minWidth: 140 },
            { prop: "class2RecipeType", label: this.$t("示方类型"), align: "center", minWidth: 100 },
          ],
        },
        {
          label: `${this.$t("三班")} ${this.dateList[2].shiftDate}`,
          children: [
            { prop: "class3PlanQty", label: this.$t("计划"), align: "center" },
            { prop: "class3FinishQty", label: this.$t("实际"), align: "center" },
            { prop: "class3Analysis", label: this.$t("原因分析"), align: "center", minWidth: 140 },
            { prop: "class3RecipeType", label: this.$t("示方类型"), align: "center", minWidth: 100 },
          ],
        },
        {
          label: `${this.$t("四班")} ${this.dateList[3].shiftDate}`,
          children: [
            { prop: "class4PlanQty", label: this.$t("计划"), align: "center" },
            { prop: "class4FinishQty", label: this.$t("实际"), align: "center" },
            { prop: "class4Analysis", label: this.$t("原因分析"), align: "center", minWidth: 140 },
            { prop: "class4RecipeType", label: this.$t("示方类型"), align: "center", minWidth: 100 },
          ],
        },
        {
          label: `${this.$t("五班")} ${this.dateList[4].shiftDate}`,
          children: [
            { prop: "class5PlanQty", label: this.$t("计划"), align: "center" },
            { prop: "class5FinishQty", label: this.$t("实际"), align: "center" },
            { prop: "class5Analysis", label: this.$t("原因分析"), align: "center", minWidth: 140 },
            { prop: "class5RecipeType", label: this.$t("示方类型"), align: "center", minWidth: 100 },
          ],
        },
        {
          label: `${this.$t("六班")} ${this.dateList[5].shiftDate}`,
          children: [
            { prop: "class6PlanQty", label: this.$t("计划"), align: "center" },
            { prop: "class6FinishQty", label: this.$t("实际"), align: "center" },
            { prop: "class6Analysis", label: this.$t("原因分析"), align: "center", minWidth: 140 },
            { prop: "class6RecipeType", label: this.$t("示方类型"), align: "center", minWidth: 100 },
          ],
        },
        {
          label: `${this.$t("七班")} ${this.dateList[6].shiftDate}`,
          children: [
            { prop: "class7PlanQty", label: this.$t("计划"), align: "center" },
            { prop: "class7FinishQty", label: this.$t("实际"), align: "center" },
            { prop: "class7Analysis", label: this.$t("原因分析"), align: "center", minWidth: 140 },
            { prop: "class7RecipeType", label: this.$t("示方类型"), align: "center", minWidth: 100 },
          ],
        },
        {
          label: `${this.$t("八班")} ${this.dateList[7].shiftDate}`,
          children: [
            { prop: "class8PlanQty", label: this.$t("计划"), align: "center" },
            { prop: "class8FinishQty", label: this.$t("实际"), align: "center" },
            { prop: "class8Analysis", label: this.$t("原因分析"), align: "center", minWidth: 140 },
            { prop: "class8RecipeType", label: this.$t("示方类型"), align: "center", minWidth: 100 },
          ],
        },
      ];
    },
  },
  methods: {
    async show(payload = {}) {
      this.visible = true;
      this.selection = [];
      this.form.newCxMachineCode = "";
      this.query.scheduleDate = payload.scheduleDate || "";
      this.query.cxMachineCode = payload.cxMachineCode || "";
      this.page.current = 1;
      this.data = [];
      this.page.total = 0;
      // 如果传入了机台编码，自动查询并全选
      if (this.query.cxMachineCode) {
        await this.getList();
        if (this.data.length > 0) {
          this.$nextTick(() => {
            const tableRef = this.$refs.MoldingChangeMachineTable?.getTableRef?.();
            if (tableRef) {
              this.data.forEach((row) => tableRef.toggleRowSelection(row, true));
            } else {
              // 回退：直接设置 selection 数组
              this.selection = [...this.data];
            }
          });
        }
      }
    },
    hide() {
      this.visible = false;
      this.loading = false;
      this.selection = [];
      this.data = [];
    },
    handleSelectionChange(rows) {
      this.selection = rows;
      if (!rows.length) {
        this.form.newCxMachineCode = "";
      }
    },
    handlePageChange(current, pageSize) {
      this.page.current = current;
      this.page.pageSize = pageSize;
      this.getList();
    },
    handleSearch() {
      if (!this.query.cxMachineCode) {
        this.$modal.msgWarning(this.$t("ui.data.msg.scheduleResult.selectOldMachine"));
        return;
      }
      this.page.current = 1;
      this.getList();
    },
    formatParams() {
      return {
        scheduleDate: this.query.scheduleDate,
        cxMachineCode: this.query.cxMachineCode,
        pageNum: this.page.current,
        pageSize: this.page.pageSize,
      };
    },
    async getDate() {
      if (!this.query.scheduleDate) return;
      const res = await getScheduleDate({ scheduleDate: this.query.scheduleDate });
      if (res && res.length > 0) {
        this.dateList = res;
      }
    },
    async getList() {
      try {
        this.loading = true;
        const res = await listCxScheduleResult(this.formatParams());
        this.data = res.rows || [];
        this.page.total = res.total || 0;
        this.selection = [];
        this.form.newCxMachineCode = "";
        await this.getDate();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async handleConfirm() {
      if (!this.form.newCxMachineCode) {
        this.$modal.msgWarning(this.$t("common.rule.select"));
        return;
      }
      if (!this.query.cxMachineCode) {
        this.$modal.msgWarning(this.$t("ui.data.msg.scheduleResult.selectOldMachine"));
        return;
      }
      if (this.form.newCxMachineCode === this.query.cxMachineCode) {
        this.$modal.msgWarning(this.$t("ui.data.msg.scheduleResult.newMachineSameOld"));
        return;
      }
      if (!this.selection.length) {
        this.$modal.msgWarning(this.$t("ui.data.msg.scheduleResult.selectRows"));
        return;
      }
      try {
        this.loading = true;
        const params = {
          ids: this.selection.map((row) => row.id),
          newMachineCode: this.form.newCxMachineCode,
          newMachineName: this.getMachineName(this.form.newCxMachineCode),
        };
        // 第一次调用：校验产能（confirmed=false）
        const res = await changeMachine({ ...params, confirmed: false });
        // 产能不足时需要用户确认后再次调用
        if (res.needConfirm) {
          await this.$confirm(res.msg, this.$t("common.prompt"), {
            confirmButtonText: this.$t("common.button.confirm"),
            cancelButtonText: this.$t("common.button.cancel"),
            type: "warning",
          });
          // 用户确认后，第二次调用：正式执行（confirmed=true）
          await changeMachine({ ...params, confirmed: true });
        }
        this.$modal.msgSuccess(this.$t("common.msg.ajax.operation.success"));
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    getMachineName(machineCode) {
      const machine = this.moldingMachines.find((item) => {
        const code = item.cxMachineCode || item.moldingMachineCode;
        return code === machineCode;
      });
      return (machine && (machine.machineName || machine.moldingMachineName)) || machineCode;
    },
  },
};
</script>

<style scoped lang="scss">
.mb10 {
  margin-bottom: 10px;
}
.w180 {
  width: 180px;
}
.toolbar-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.query-form {
  flex: 1;
}
.new-machine-form {
  margin-left: auto;
}
.new-machine-item ::v-deep .el-form-item__label {
  color: #f56c6c;
  font-weight: 600;
}
</style>
