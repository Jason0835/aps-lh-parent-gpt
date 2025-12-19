
<template>
  <basic-container>
    <page-table
      tableRef="cxSpecColorMainTable"
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
          v-hasPermi="['cx:specColor:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['cx:machine:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          v-hasPermi="['cx:specColor:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button @click="handleExport" v-hasPermi="['cx:specColor:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/quota/importTemplate"
      uploadUrl="/cx/quota/importData"
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
  listSpecColor,
  removeSpecColor,
  editSpecColor,
} from "@/api/cx/specColor";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "SpecColor",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["BIG_ROLL_COLOR", "STATUS"],
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
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "specDesc",
          align: "left",
          halign: "center",
          label: this.$t("ui.data.column.specColor.specDesc"),
          // sortable: "custom",
        },
        {
          prop: "colorType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specColor.colorType"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.BIG_ROLL_COLOR, value);
          },
        },
        {
          prop: "colorCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.specColor.colorCode"),
          // sortable: "custom",
          render: ({ row }) => {
            return (
              <div>
                <span
                  style={`margin-right:5px;width:12px;height:12px;display: inline-block;background: ${row.colorCode}`}
                ></span>
                <span>{row.colorCode}</span>
              </div>
            );
          },
        },
        {
          prop: "status",
          widthUnit: "%",
          width: 10,
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.status"),
          // sortable: "custom",
          render: ({ row }) => {
            return (
              <el-switch
                value={row.status}
                active-value="0"
                inactive-value="1"
                onChange={(value) => this.handleChangeStatus(value, row)}
              />
            );
          },

          // formatter: (row, column, value, index) => {
          //   if (value == 1) {
          //     return (
          //       '<i class="fa fa-toggle-off text-info fa-2x" onclick="enable(\'' +
          //       row.id +
          //       "')\"></i> "
          //     );
          //   } else {
          //     return (
          //       '<i class="fa fa-toggle-on text-info fa-2x" onclick="disable(\'' +
          //       row.id +
          //       "')\"></i> "
          //     );
          //   }
          // },
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          // sortable: "custom",
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
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
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.specColor.specDesc"),
          prop: "specDesc",
        },
        {
          label: this.$t("ui.data.column.specColor.colorCode"),
          prop: "colorCode",
        },
        {
          label: this.$t("ui.data.column.specColor.colorType"),
          prop: "colorType",
          type: "select", //BIG_ROLL_COLOR
          dictData: this.dict.type.BIG_ROLL_COLOR,
        },
        {
          label: this.$t("ui.data.column.status"),
          prop: "status",
          type: "select", // "STATUS",
          dictData: this.dict.type.STATUS,
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
        removeSpecColor({ ids })
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
      downloadLink("/cx/quota/export", this.formatParams(false));
    },

    handleChangeStatus(status, row) {
      this.$confirm(this.$t("ui.common.layer.boxMsg.context"), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editSpecColor({
            ...row,
            status,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
      });
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
        const data = await listSpecColor(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {},
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
