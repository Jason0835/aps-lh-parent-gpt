
<template>
  <basic-container>
    <page-table
      tableRef="lhMachineMsgMainTable"
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
    >
      <template slot="header">
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['lh:lhSpecifyMachine:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <el-button
          type="warning"
          >{{ $t("编辑") }}</el-button
        >
         <el-button
          v-hasPermi="['lh:lhScheduleResult:remove']"
          type="danger"
          >{{ $t("删除") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhSpecifyMachine:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['lh:lhSpecifyMachine:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/lh/lhSpecifyMachine/importTemplate"
      uploadUrl="/lh/lhSpecifyMachine/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listLhSpecifyMachine,
  removeLhSpecifyMachine,
} from "@/api/lh/lhSpecifyMachine";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "CuringPointingMachine",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["LINE_TYPE", "JOB_TYPE"],
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
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
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
        {
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("硫化机台")
        },
        {
          prop: "specCode",
          align: "center",
          halign: "center",
          label: this.$t("结构"),
        },
        {
          prop: "lineType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specifyMachine.lineType"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.LINE_TYPE, value);
          },
        },
         {
          prop: "machineCode1",
          align: "center",
          halign: "center",
          label: this.$t("模壳"),
        },
        {
          prop: "jobType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specifyMachine.jobType"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.JOB_TYPE, value);
          },
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.data.column.remark"),
          minWidth: 100,
        },
        // {
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.btn.option"),
        //   render: ({ row }) => {
        //     return (
        //       <div>
        //         <el-button
        //           v-hasPermi={["lh:lhSpecifyMachine:edit"]}
        //           class="minus"
        //           type="success"
        //           onClick={() => this.handleEdit(row)}
        //         >
        //           {this.$t("ui.frame.btn.update")}
        //         </el-button>
        //         <el-button
        //           v-hasPermi={["lh:lhSpecifyMachine:remove"]}
        //           class="minus"
        //           type="danger"
        //           onClick={() => this.handleDelete(row)}
        //         >
        //           {this.$t("ui.frame.btn.delete")}
        //         </el-button>
        //       </div>
        //     );
        //   },
        // },
      ];

      return columns;
    },
    searchColumns() {
      return [
        // {
        //   label: this.$t("ui.data.column.specifyMachine.sapCode"),
        //   prop: "sapCode",
        // },
        {
          label: this.$t("硫化机台"),
          prop: "machineCode",
          type: "select",
          dictData: this.curingMachines,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
        {
          label: this.$t("结构"),
          prop: "specCode",
        },
        {
          label: this.$t("线路类型"),
          prop: "specCode",
        },
        {
          label: this.$t("作业类型"),
          prop: "specCode",
        },
        {
          label: this.$t("模壳"),
          prop: "specCode",
        }
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
        removeLhSpecifyMachine({ ids })
          .then((data) => {
            this.loading = false;
            this.$modal.msgSuccess(data.msg);
            // this.$set(this.page, "current", 1);
            this.getList();
          })
          .catch(() => {
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
      downloadLink("/lh/lhSpecifyMachine/export", this.formatParams(false));
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
        const data = await listLhSpecifyMachine(this.formatParams());
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
