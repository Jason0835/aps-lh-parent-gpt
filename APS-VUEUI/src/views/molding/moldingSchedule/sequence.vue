<template>
  <basic-container>
    <page-table
      tableRef="MoldingScheduleSequenceTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="pagedData"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @reset="handleReset"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      :showSummary="false"
      :selectArea="false"
      :row-style="rowStyle"
      :cell-style="cellStyle"
    >
      <template slot="header">
        <!-- <el-button
          v-hasPermi="['cx:cxScheduleResult:add']"
          type="warning"
          @click="handleAdd"
          >{{ $t("common.button.add") }}</el-button
        > -->
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
} from "@/api/cx/cxScheduleResult";
import {
  listCxScheduleDetailByQuery,
  updateCxScheduleDetailPlanQty,
} from "@/api/cx/cxScheduleDetail";
import { listStructureName } from "@/api/mdm/mdmStructureName";
import { getScheduleDate } from "@/api/lh/scheduleResult";
import editDialog from "./components/editDialog.vue";

const SHIFT_COUNT = 8;
const EDIT_FIELDS = ["PlanQty", "StockHours", "Sequence"];

export default {
  name: "MoldingScheduleSequence",
  components: {
    editDialog,
  },
  dicts: ["IS_RELEASE", "biz_factory_name", "MACHINE_TYPE", "trial_status"],
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
      newStructureList: [],
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
    pagedData() {
      const start = (this.page.current - 1) * this.page.pageSize;
      const end = start + this.page.pageSize;
      return this.data.slice(start, end);
    },
    columns() {
      const fixedColumns = [
        {
          label: this.$t("ui.data.column.unscheduleResult.scheduleDate"),
          prop: "scheduleDate",
          align: "center",
          minWidth: 130,
        },
        { label: this.$t("成型机台"), prop: "cxMachineCode", align: "center" },
        { label: this.$t("硫化机台"), prop: "lhMachineCode", align: "center", minWidth: 180, },
        { label: this.$t("物料编码"), prop: "materialCode", minWidth: 130, align: "center" },
        { label: this.$t("物料描述"), prop: "materialDesc", minWidth: 300 },
        { label: this.$t("胎胚描述"), prop: "mainMaterialDesc", minWidth: 320 },
        {
          label: "车次号",
          prop: "tripNo",
          minWidth: 100,
          align: "center",
          formatter: (row, column, value) => {
            if (!value) return "";
            return  "车"+ value;
          },
        },
        {
          label: "整车条数",
          prop: "tripCapacity",
          minWidth: 140,
          align: "center",
        },
        { label: this.$t("工单号"), prop: "orderNo", align: "center", minWidth: 180 },
        { label: this.$t("成型批次号"), prop: "cxBatchNo", align: "center", minWidth: 150 },
      ];

      const shiftColumns = Array.from({ length: SHIFT_COUNT }, (_, idx) => {
        const shift = idx + 1;
        return {
          label: `${this.$t(this.getShiftLabel(shift))} ${this.dateList[idx].shiftDate}`,
          children: [
            this.createEditableColumn(`class${shift}PlanQty`, this.$t("ui.data.column.scheduleResult.plan"), "number"),
            this.createEditableColumn(`class${shift}StockHours`, "库存可供硫化时长", "number"),
            this.createEditableColumn(`class${shift}Sequence`, "顺位", "number"),
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
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData: this.newStructureList,
          filterable: true,
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
    getRouteQueryParams() {
      const { queryParams, ...restQuery } = this.$route.query || {};
      if (queryParams) {
        try {
          const parsed = JSON.parse(decodeURIComponent(queryParams));
          if (parsed && typeof parsed === "object") {
            return parsed;
          }
        } catch (error) {
          console.error(error);
        }
      }
      return restQuery;
    },
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
            const rawVal = row[prop];
            const numVal = Number(rawVal);
            let displayValue = undefined;
            if (!isNaN(numVal) && numVal > 0) {
              displayValue = numVal;
            }
            const isStockHours = prop.endsWith("StockHours");
            if (isStockHours) {
              const planQtyProp = prop.replace("StockHours", "PlanQty");
              const planQty = Number(row[planQtyProp]);
              if (!isNaN(planQty) && planQty > 0) {
                displayValue = numVal;
              }
            }
            return (
              <el-input-number
                value={displayValue}
                min={0}
                controls={false}
                style="width: 100%;"
                onInput={(value) => this.handleFieldChange(row, prop, value)}
              />
            );
          }
          if (type === "select") {
            return (
              <el-select
                value={row[prop]}
                clearable
                filterable
                onInput={(value) => this.handleFieldChange(row, prop, value)}
              >
                {(this.dict?.type?.trial_status || []).map((item) => (
                  <el-option
                    key={item.value ?? item.dictValue}
                    label={item.label ?? item.dictLabel}
                    value={item.value ?? item.dictValue}
                  />
                ))}
              </el-select>
            );
          }
          return (
            <el-input
              value={row[prop]}
              style="width: 100%;"
              clearable
              onInput={(value) => this.handleFieldChange(row, prop, value)}
            />
          );
        },
      };
    },
    getShiftFromProp(prop) {
      const matched = /^class(\d+)(PlanQty|StockHours|Sequence)$/.exec(prop);
      return matched ? Number(matched[1]) : null;
    },
    canEditField(row, prop) {
      return !!this.getShiftFromProp(prop);
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
      this.page.current = current;
      this.page.pageSize = pageSize;
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
      const payload = changedRows.map((row) => {
        const item = {
          detailId: row.id || row.detailId || 0,
        };
        for (let i = 1; i <= SHIFT_COUNT; i += 1) {
          item[`class${i}PlanQty`] = Number(row[`class${i}PlanQty`] || 0);
          item[`class${i}StockHours`] = row[`class${i}StockHours`] ?? "";
          item[`class${i}Sequence`] = Number(row[`class${i}Sequence`] || 0);
        }
        return item;
      });
      const res = await updateCxScheduleDetailPlanQty(payload);
      this.$modal.msgSuccess(res.msg || this.$t("操作成功"));
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
    formatParams(hasPage = false) {
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
        const res = await listCxScheduleDetailByQuery(this.formatParams(false));
        const rows = Array.isArray(res)
          ? res
          : res?.rows || res?.data?.rows || res?.data || [];
        this.data = Array.isArray(rows) ? rows : [];
        this.page.total = this.data.length;
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
    async getNewList() {
      try {
        const res = await listStructureName({});
        const rows = res?.rows || [];
        this.newStructureList = rows.map((item) => ({
          label: item.structureName,
          value: item.structureName,
        }));
      } catch (error) {
        console.error(error);
      }
    },
  },
  created() {
    const date = moment().add(2, "days").format("YYYY-MM-DD");
    const routeQuery = this.getRouteQueryParams();
    this.query = Object.keys(routeQuery || {}).length > 0
      ? { ...routeQuery }
      : { scheduleDate: date };
    this.search = { ...this.query };
    if (!this.query.scheduleDate) {
      this.query.scheduleDate = date;
      this.search.scheduleDate = date;
    }
    this.$store.dispatch("molding/getMachineList");
  },
  mounted() {
    this.getNewList();
    this.getList();
  },
};
</script>
