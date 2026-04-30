
<template>
  <basic-container>
    <page-table
      tableRef="treadGlueOrderMainTable"
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
          v-hasPermi="['tm:glueOrder:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['tm:glueOrder:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          v-hasPermi="['tm:glueOrder:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button @click="handleExport" v-hasPermi="['tm:glueOrder:export']">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/tm/glueOrder/importTemplate"
      uploadUrl="/tm/glueOrder/importData"
      @uploadSuccess="getList"
    />
    <infoDialog
      ref="infoRef"
      :glueGroupList="glueGroupList"
      @success="getList"
    />
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listGlueGroupOrder } from "@/api/tm/glueGroupOrder";
import { listGlueOrder, removeGlueOrder } from "@/api/tm/glueOrder";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "TreadGlueOrder",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.glueOrder.column.glueGroup"),
          prop: "glueGroupId",
          render: (form) => {
            return (
              <el-select class="w100" v-model={form.glueGroupId} clearable>
                {this.glueGroupList.map((el) => {
                  return <el-option value={el.value} label={el.label} />;
                })}
              </el-select>
            );
          },
        },
        {
          label: this.$t("ui.glueOrder.column.glueCode"),
          prop: "glueCode",
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
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
      glueGroupList: [],
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "glueGroupCode",
          // sortable: "custom",
          label: this.$t("ui.glueGroup.column.glueGroupCode"),
          halign: "center",
          align: "center",
        },
        {
          prop: "glueGroupName",
          // sortable: "custom",
          label: this.$t("ui.glueGroup.column.glueGroupName"),
          halign: "center",
          align: "center",
        },
        {
          prop: "glueCode",
          // sortable: "custom",
          label: this.$t("ui.glueOrder.column.glueCode"),
          halign: "center",
          align: "center",
        },
        {
          prop: "orderNum",
          // sortable: "custom",
          label: this.$t("ui.glueOrder.column.orderNum"),
          halign: "center",
          align: "center",
        },
        {
          prop: "glueGroupOrderNum",
          // sortable: "custom",
          label: this.$t("ui.glueOrder.column.groupGlue.orderNum"),
          halign: "center",
          align: "center",
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
        removeGlueOrder({ ids })
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
      downloadLink("/tm/glueOrder/export", this.formatParams(false));
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
        const data = await listGlueOrder(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getGlueGroup() {
      try {
        const data = await listGlueGroupOrder({});
        this.glueGroupList = data.rows.map((el) => {
          return {
            value: el.id,
            label: el.glueGroupName,
          };
        });
      } catch (error) {
        console.error(error);
      } finally {
      }
    },
  },
  created() {},
  activated() {
    this.getGlueGroup();
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
