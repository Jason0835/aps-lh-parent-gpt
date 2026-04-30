
<template>
  <basic-container>
    <page-table
      tableRef="CuringMesLhScheduleResultMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
      :cell-style="cellStyle"
    >
      <template slot="header">
        <!-- <el-button
          type="primary"
          v-hasPermi="['cxlh:mesLhScheduleResult:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['cxlh:mesLhScheduleResult:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['cxlh:mesLhScheduleResult:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['cxlh:mesLhScheduleResult:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/cxlh/mesLhScheduleResult/importTemplate"
      uploadUrl="/cxlh/mesLhScheduleResult/importData"
      @uploadSuccess="getList"
    /> -->
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listMesLhScheduleResult } from "@/api/cxlh/mesLhScheduleResult";
//components
// import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MesLhScheduleResult",
  components: {
    // tltUpload,
    // infoDialog,
  },
  dicts: ["biz_factory_name", "IS_RELEASE"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    ...mapState({
      curingMachines: (state) => state.curing.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          minWidth: 180,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.factoryCode"),
          prop: "factoryCode",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          label: this.$t("ui.data.column.mesLhScheduleResult.issuedStatus"),
          prop: "issuedStatus",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
        },
        // {
        //   label: "id",
        //   prop: "id",
        //   minWidth: 100,
        //   sortable: "custom",
        //   visible: false,
        // },
        {
          label: this.$t("ui.data.column.scheduleResult.machine"),
          prop: "lhMachineCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.leftRightMold"),
          prop: "leftRightMold",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.remark"),
          prop: "orderNo",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.sapCode"),
          prop: "productCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoCode"),
          prop: "embryoCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.specDesc"),
          prop: "specDesc",
          minWidth: 140,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhTime"),
          prop: "lhTime",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dailyPlanQty"),
          prop: "dailyPlanQty",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mesLhScheduleResult.mpMoldQty"),
          prop: "mpMoldQty",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mesLhScheduleResult.moldInfo"),
          prop: "moldInfo",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mesLhScheduleResult.mouldMethod"),
          prop: "mouldMethod",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mesLhScheduleResult.bomVersion"),
          prop: "bomVersion",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mesLhScheduleResult.productionStatus"),
          prop: "productionStatus",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class11") +
            this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class1PlanQty",
          minWidth: 100,
          // sortable: "custom",
          // render: ({ row }) => {
          //   return (
          //     <TPopover
          //       title={this.$t("ui.data.column.scheduleResult.plan")}
          //       v-model={row.class1PlanQty}
          //       showClose={false}
          //       min={0}
          //       onConfirm={(val) => {
          //         this.handleChangeQty({
          //           ...row,
          //           class1PlanQty: val,
          //         });
          //       }}
          //     />
          //   );
          // },
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class11") +
            this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class1FinishQty",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class11") +
            this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class1Analysis",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class22") +
            this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class2PlanQty",
          minWidth: 100,
          // sortable: "custom",
          // render: ({ row }) => {
          //   return (
          //     <TPopover
          //       title={this.$t("ui.data.column.scheduleResult.plan")}
          //       v-model={row.class2PlanQty}
          //       showClose={false}
          //       min={0}
          //       onConfirm={(val) => {
          //         this.handleChangeQty({
          //           ...row,
          //           class2PlanQty: val,
          //         });
          //       }}
          //     />
          //   );
          // },
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class22") +
            this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class2FinishQty",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class22") +
            this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class2Analysis",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class33") +
            this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class3PlanQty",
          minWidth: 100,
          // sortable: "custom",
          // render: ({ row }) => {
          //   return (
          //     <TPopover
          //       title={this.$t("ui.data.column.scheduleResult.plan")}
          //       v-model={row.class3PlanQty}
          //       showClose={false}
          //       min={0}
          //       onConfirm={(val) => {
          //         this.handleChangeQty({
          //           ...row,
          //           class3PlanQty: val,
          //         });
          //       }}
          //     />
          //   );
          // },
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class33") +
            this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class3FinishQty",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label:
            this.$t("ui.data.column.scheduleResult.class33") +
            this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class3Analysis",
          minWidth: 100,
          // sortable: "custom",
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          type: "select",
          dictData: this.curingMachines,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          render: (form) => {
            return (
              <dict-select
                v-model={form.isRelease}
                options={this.dict.type.IS_RELEASE}
              />
            );
          },
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        this.loading = true;
        removeQuota({ ids })
          .then((data) => {
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          })
          .catch((error) => {
            console.log(error);
            this.loading = false;
          });
      });
    },

    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handelSuccess() {
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink(
        "/cxlh/mesLhScheduleResult/export",
        this.formatParams(false)
      );
    },

    // utils
    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "afterMachineId") {
        if (row.beforeMachineId != row.afterMachineId) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass1Plan") {
        if (row.beforeClass1Plan !== row.afterClass1Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass2Plan") {
        if (row.beforeClass2Plan !== row.afterClass2Plan) {
          return { background: "#FF7B7B" };
        }
      }
      if (column.property === "afterClass3Plan") {
        if (row.beforeClass3Plan !== row.afterClass3Plan) {
          return { background: "#FF7B7B" };
        }
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

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listMesLhScheduleResult(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    if (!this.curingMachines.length) {
      this.$store.dispatch("curing/getMachineList");
    }
  },
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
