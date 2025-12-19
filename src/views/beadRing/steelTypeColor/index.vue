
<template>
  <basic-container>
    <page-table
      tableRef="steelTypeColorMainTable"
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
          v-hasPermi="['gsq:gsqSteelTypeColor:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['gsq:gsqSteelTypeColor:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['gsq:gsqSteelTypeColor:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['gsq:gsqSteelTypeColor:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['gsq:gsqSteelTypeColor:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/gsq/gsqSteelTypeColor/importTemplate"
      uploadUrl="/gsq/gsqSteelTypeColor/importData"
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
  listSteelTypeColor,
  editSteelTypeColor,
  removeSteelTypeColor,
} from "@/api/gsq/gsqSteelTypeColor";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "SteelTypeColor",
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
          label: this.$t("ui.data.column.scheduleResult.steelType"),
          prop: "steelType",
        },
        {
          label: this.$t("ui.bigRollColor.column.colorCode"),
          prop: "colorCode",
        },
        {
          label: this.$t("ui.bigRollColor.column.colorType"),
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
          prop: "steelType",
          halign: "center",
          label: this.$t("ui.data.column.scheduleResult.steelType"),
          // sortable: "custom",
        },
        {
          prop: "colorCode",
          halign: "center",
          label: this.$t("ui.bigRollColor.column.colorCode"),
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
          prop: "colorType",
          halign: "center",
          label: this.$t("ui.bigRollColor.column.colorType"),
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
                  v-hasPermi={["gsq:gsqSteelTypeColor:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["gsq:gsqSteelTypeColor:remove"]}
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
          const res = await editSteelTypeColor({
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
        removeSteelTypeColor({ ids })
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
      downloadLink("/gsq/gsqSteelTypeColor/export", this.formatParams(false));
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
        const data = await listSteelTypeColor(this.formatParams());
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
