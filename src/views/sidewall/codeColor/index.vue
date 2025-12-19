
<template>
  <basic-container>
    <page-table
      tableRef="sidewallCodeColorMainTable"
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
          v-hasPermi="['tc:sidewallCodeColor:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['tc:sidewallCodeColor:edit']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          plain
          v-hasPermi="['tc:sidewallCodeColor:remove']"
          :disabled="selection.length === 0"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['tc:sidewallCodeColor:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['tc:sidewallCodeColor:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/tc/sidewallCodeColor/importTemplate"
      uploadUrl="/tc/sidewallCodeColor/importData"
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
  listCodeColor,
  editCodeColor,
  removeCodeColor,
} from "@/api/tc/codeColor";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "SidewallCodeColor",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["match_type", "BIG_ROLL_COLOR"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.data.column.sidewallCodeColor.sidewallCode"),
          prop: "sidewallCode",
        },
        {
          label: this.$t("ui.data.column.sidewallCodeColor.matchType"),
          prop: "matchType",
          render: (form) => {
            return (
              <dict-select
                v-model={form.matchType}
                options={this.dict.type.match_type}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.sidewallCodeColor.colorType"),
          prop: "colorType",
          render: (form) => {
            return (
              <dict-select
                v-model={form.colorType}
                options={this.dict.type.BIG_ROLL_COLOR}
              />
            );
          },
        },
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
          prop: "sidewallCode",
          halign: "center",
          label: this.$t("ui.data.column.sidewallCodeColor.sidewallCode"),
          // sortable: "custom",
        },
        {
          prop: "matchType",
          halign: "center",
          label: this.$t("ui.data.column.sidewallCodeColor.matchType"),
          // sortable: "custom",
          render: ({ row }) => {
            return (
              <dict-tag
                options={this.dict.type.match_type}
                value={row.matchType}
              />
            );
          },
        },
        {
          prop: "colorType",
          halign: "center",
          label: this.$t("ui.data.column.sidewallCodeColor.colorType"),
          // sortable: "custom",
          render: ({ row }) => {
            return (
              <dict-tag
                options={this.dict.type.BIG_ROLL_COLOR}
                value={row.colorType}
              />
            );
          },
        },
        {
          prop: "colorCode",
          halign: "center",
          label: this.$t("ui.data.column.sidewallCodeColor.colorCode"),
          // sortable: "custom",
          render: ({ row }) => {
            return (
              <div>
                <span
                  style={{
                    marginRight: "5px",
                    width: "12px",
                    height: "12px",
                    display: "inline-block",
                    background: row.colorCode,
                  }}
                ></span>
                <span>{row.colorCode}</span>
              </div>
            );
          },
        },
        {
          prop: "status",
          halign: "center",
          label: this.$t("ui.data.column.sidewallCodeColor.status"),
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
          minWidth: 180,
          width: 180,
          fixed: "right",
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
    handleChangeStatus(status, row) {
      console.log(status);
      let title =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(title, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editCodeColor({
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
        removeCodeColor({ ids })
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
      downloadLink("/tc/sidewallCodeColor/export", this.formatParams(false));
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
        const data = await listCodeColor(this.formatParams());
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
