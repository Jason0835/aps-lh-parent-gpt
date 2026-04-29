<template>
  <basic-container>
    <page-table
      tableRef="MoldingScheduleSequenceTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @reset="handleReset"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      :showSummary="false"
      :selectArea="false"
      :row-style="rowStyle"
      :cell-style="cellStyle"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['cx:cxScheduleResult:add']"
          type="warning"
          @click="handleAdd"
          >{{ $t("common.button.add") }}</el-button
        >
        <el-button v-if="hasDirtyData" type="primary" @click="handleSubmit"
          >{{ $t("ui.frame.btn.submit") }}</el-button
        >
      </template>
    </page-table>
    <editDialog ref="editRef" @success="getList" />
  </basic-container>
</template>

<script>
import moment from "moment";
import { mapState } from "vuex";
import {
  addMoldingScheduleSequence,
  listCxScheduleResult,
  submitMoldingScheduleSequence,
} from "@/api/cx/cxScheduleResult";
import { getScheduleDate } from "@/api/lh/scheduleResult";
import editDialog from "./components/editDialog.vue";

const SHIFT_COUNT = 8;
const EDIT_FIELDS = ["PlanQty", "FinishQty", "Analysis", "RecipeType"];

export default {
  name: "MoldingScheduleSequence",
  components: {
    editDialog,
  },
  dicts: ["IS_RELEASE", "biz_factory_name", "MACHINE_TYPE"],
  data() {
    return {
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      dateList: Array.from({ length: SHIFT_COUNT }, (_, i) => ({
        shift: i + 1,
        shiftDate: "",
      })),
      originEditMap: {},
      dirtyRowIds: [],
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    hasDirtyData() {
      return this.dirtyRowIds.length > 0;
    },
    columns() {
      const fixedColumns = [
        {
          label: this.$t("ui.data.column.unscheduleResult.scheduleDate"),
          prop: "scheduleDate",
          align: "center",
          minWidth: 100,
        },
        { label: this.$t("工单号"), prop: "orderNo", align: "center", minWidth: 100 },
        { label: this.$t("成型批次号"), prop: "cxBatchNo", align: "center", minWidth: 100 },
        { label: this.$t("成型机台"), prop: "cxMachineCode", align: "center" },
        { label: this.$t("硫化机台"), prop: "lhMachineCode", align: "center" },
        { label: this.$t("物料编码"), prop: "materialCode", minWidth: 100, align: "center" },
        { label: this.$t("物料描述"), prop: "materialDesc", minWidth: 350 },
        { label: this.$t("胎胚描述"), prop: "mainMaterialDesc", minWidth: 350 },
      ];

      const shiftColumns = Array.from({ length: SHIFT_COUNT }, (_, idx) => {
        const shift = idx + 1;
        return {
          label: `${this.$t(this.getShiftLabel(shift))} ${this.dateList[idx].shiftDate}`,
          children: [
            this.createEditableColumn(`class${shift}PlanQty`, this.$t("ui.data.column.scheduleResult.plan"), "number"),
            this.createEditableColumn(`class${shift}FinishQty`, this.$t("实际"), "number"),
            this.createEditableColumn(`class${shift}Analysis`, this.$t("ui.data.column.scheduleResult.analysis"), "text"),
            this.createEditableColumn(`class${shift}RecipeType`, this.$t("示方类型"), "text"),
          ],
        };
      });

      return [...fixedColumns, ...shiftColumns];
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("物料编码"),
          prop: "materialCode",
        },
        {
          label: this.$t("物料描述"),
          prop: "materialDesc",
        },
        {
          label: this.$t("胎胚描述"),
          prop: "mainMaterialDesc",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          type: "select",
          dictData: this.moldingMachines,
          labelKey: "cxMachineCode",
          valueKey: "cxMachineCode",
          filterable: true,
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.editRef) {
        this.$refs.editRef.show(
          {
            scheduleDate: this.query.scheduleDate,
          },
          "insert",
          {
            submitHandler: addMoldingScheduleSequence,
          }
        );
      }
    },
    getShiftLabel(shift) {
      // 表头班次顺序：早班、中班、晚班、早班、中班、晚班、早班、中班
      const labels = ["早班", "中班", "晚班", "早班", "中班", "晚班", "早班", "中班"];
      return labels[shift - 1] || "";
    },
    createEditableColumn(prop, label, type) {
      return {
        prop,
        label,
        align: "center",
        minWidth: 120,
        render: ({ row }) => {
          if (!this.canEditField(row, prop)) {
            return <span>{row[prop]}</span>;
          }
          if (type === "number") {
            return (
              <el-input-number
                value={Number(row[prop] || 0)}
                min={0}
                controls={false}
                onInput={(value) => this.handleFieldChange(row, prop, value)}
              />
            );
          }
          return (
            <el-input
              value={row[prop]}
              clearable
              onInput={(value) => this.handleFieldChange(row, prop, value)}
            />
          );
        },
      };
    },
    getShiftFromProp(prop) {
      const matched = /^class(\d+)(PlanQty|FinishQty|Analysis|RecipeType)$/.exec(prop);
      return matched ? Number(matched[1]) : null;
    },
    canEditField(row, prop) {
      const shift = this.getShiftFromProp(prop);
      if (!shift) return false;
      const finishProp = `class${shift}FinishQty`;
      const rowId = row.id || row.mainId || row.scheduleMainId;
      const snapshot = rowId ? this.originEditMap[rowId] : null;
      const finishQty = snapshot && Object.prototype.hasOwnProperty.call(snapshot, finishProp)
        ? snapshot[finishProp]
        : row[finishProp];
      return Number(finishQty || 0) === 0;
    },
    buildEditableSnapshot(row) {
      const snapshot = {};
      for (let i = 1; i <= SHIFT_COUNT; i += 1) {
        EDIT_FIELDS.forEach((field) => {
          const prop = `class${i}${field}`;
          snapshot[prop] = row[prop] === undefined || row[prop] === null ? "" : row[prop];
        });
      }
      return snapshot;
    },
    refreshDirtyState(row) {
      const rowId = row.id || row.mainId || row.scheduleMainId;
      if (!rowId || !this.originEditMap[rowId]) return;
      const current = this.buildEditableSnapshot(row);
      const changed = JSON.stringify(current) !== JSON.stringify(this.originEditMap[rowId]);
      const exists = this.dirtyRowIds.includes(rowId);
      if (changed && !exists) this.dirtyRowIds.push(rowId);
      if (!changed && exists) this.dirtyRowIds = this.dirtyRowIds.filter((id) => id !== rowId);
    },
    handleFieldChange(row, prop, value) {
      this.$set(row, prop, value);
      this.refreshDirtyState(row);
    },
    handleSearch(data) {
      const filteredData = {};
      Object.keys(data).forEach((key) => {
        const value = data[key];
        if (value !== null && value !== undefined && value !== "") {
          filteredData[key] = value;
        }
      });
      this.query = filteredData;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handleReset() {
      const date = moment().add(1, "days").format("YYYY-MM-DD");
      this.query = { scheduleDate: date };
      this.search = { scheduleDate: date };
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handleSortChange({ prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order === "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
    },
    async handleSubmit() {
      if (!this.hasDirtyData) return;
      const changedRows = this.data.filter((row) => this.dirtyRowIds.includes(row.id || row.mainId || row.scheduleMainId));
      await submitMoldingScheduleSequence({
        scheduleDate: this.query.scheduleDate,
        rows: changedRows,
      });
      this.$modal.msgSuccess(this.$t("操作成功"));
      this.getList();
    },
    rowStyle({ row }) {
      if (row.markCloseOutTip == "0") return { "background-color": "#FFFFBF" };
      if (row.dataSource == "1") return { "background-color": "#BFE0F7" };
      return {};
    },
    cellStyle({ row, column }) {
      if (column.property === "cxMachineCode" && row.changeCxMachine == 1) {
        return { background: "#ef6776" };
      }
      return {};
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }
      return params;
    },
    async getDate() {
      try {
        const res = await getScheduleDate({
          scheduleDate: this.query.scheduleDate,
        });
        if (res && res.length > 0) {
          this.dateList = res;
        }
      } catch (error) {
        console.error(error);
      }
    },
    async getList() {
      try {
        this.loading = true;
        const res = await listCxScheduleResult(this.formatParams());
        this.data = res.rows || [];
        this.page.total = res.total || 0;
        this.originEditMap = {};
        this.data.forEach((row) => {
          const rowId = row.id || row.mainId || row.scheduleMainId;
          if (rowId) {
            this.originEditMap[rowId] = this.buildEditableSnapshot(row);
          }
        });
        this.dirtyRowIds = [];
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
        this.getDate();
      }
    },
  },
  created() {
    const date = moment().add(2, "days").format("YYYY-MM-DD");
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;
    this.$store.dispatch("molding/getMachineList");
  },
  mounted() {
    this.getList();
  },
};
</script>
