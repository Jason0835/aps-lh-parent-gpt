
<template>
  <basic-container>
    <page-table
      tableRef="beadRingTwiningDiscMainTable"
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
        <el-button
          type="primary"
          plain
          v-hasPermi="['gsq:twiningDisc:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['gsq:twiningDisc:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="danger"
          plain
          v-hasPermi="['gsq:twiningDisc:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['gsq:twiningDisc:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['gsq:twiningDisc:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/gsq/twiningDisc/importTemplate"
      uploadUrl="/gsq/twiningDisc/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listTwiningDisc,
  editTwiningDisc,
  removeTwiningDisc,
} from "@/api/gsq/twiningDisc";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "BeadRingTwiningDisc",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["BIG_ROLL_COLOR"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.twiningDisc.column.serialNumber"),
          prop: "serialNumber",
        },
        {
          label: this.$t("ui.twiningDisc.column.name"),
          prop: "name",
        },
        // {
        //   label: this.$t("ui.data.column.sidewallCodeColor.colorType"),
        //   prop: "colorType",
        //   render: (form) => {
        //     return (
        //       <dict-select
        //         v-model={form.colorType}
        //         options={this.dict.type.BIG_ROLL_COLOR}
        //       />
        //     );
        //   },
        // },
      ],
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
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "serialNumber",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.serialNumber"),
          // sortable: "custom",
        },
        {
          prop: "name",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.name"),
          // sortable: "custom",
        },
        {
          prop: "spec",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.spec"),
          // sortable: "custom",
        },
        {
          prop: "orderWay",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.orderWay"),
          // sortable: "custom",
        },
        {
          prop: "purpose",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.purpose"),
          // sortable: "custom",
        },
        {
          prop: "twiningNum",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.twiningNum"),
          // sortable: "custom",
        },
        {
          prop: "inTime",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.inTime"),
          // sortable: "custom",
        },
        {
          prop: "scrapTime",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.scrapTime"),
          // sortable: "custom",
        },
        {
          prop: "scrapReason",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.scrapReason"),
          // sortable: "custom",
        },
        {
          prop: "machineName",
          halign: "center",
          label: this.$t("ui.twiningDisc.column.machine"),
          // sortable: "custom",
        },
        {
          prop: "remark",
          halign: "center",
          minWidth: 100,
          label: this.$t("ui.common.column.remark"),
          // sortable: "custom",
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 180,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["gsq:twiningDisc:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["gsq:twiningDisc:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete([row])}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
        },
      ];

      return columns;
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
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        this.loading = true;
        removeTwiningDisc({ ids })
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
      downloadLink("/gsq/twiningDisc/export", this.formatParams(false));
    },

    // utils
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
        const data = await listTwiningDisc(this.formatParams());
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
    this.$store.dispatch("beadRing/getMachineList");
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
