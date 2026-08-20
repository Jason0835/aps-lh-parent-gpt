
<template>
  <basic-container>
    <page-table
      tableRef="insideLinerFixedPointMachineMainTable"
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
          v-hasPermi="['nc:specifyMachine:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['nc:specifyMachine:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          v-hasPermi="['nc:specifyMachine:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['nc:specifyMachine:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload-form
      ref="tltUploadForm"
      :title="$t('ui.nc.specifyMachine.column.modalName')"
      downloadUrl="/nc/specifyMachine/importTemplate"
      uploadUrl="/nc/specifyMachine/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      :rules="importRules"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listSpecifyMachine,
  removeSpecifyMachine,
  removeAllSpecifyMachine,
} from "@/api/nc/specifyMachine";
import { getConfigKey } from "@/api/system/config";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "ncFixedPointMachine",
  components: {
    TltUploadForm,
    infoDialog,
  },
  dicts: ["LINE_TYPE", "JOB_TYPE", "biz_factory_name"],
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
        factoryCode: '',
      },
      query: {
        factoryCode: '',
      },
      importDefaultValue: {
        updateSupport: false,
      },
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("ui.checkbox.updateExistingData")}
                v-model={form.updateSupport}
              >
                {this.$t("ui.checkbox.updateExistingData")}
              </el-checkbox>
            );
          },
        },
      ],
      importRules: {},
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.insideLiner.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "liningCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.nc.specifyMachine.column.liningCode"),
          // sortable: "custom",
        },
        {
          prop: "machineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.specifyMachine.column.machineName"),
          // sortable: "custom",
        },
        {
          prop: "lineType",
          align: "center",
          halign: "center",
          label: this.$t("ui.specifyMachine.column.lineType"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.LINE_TYPE, value);
          },
        },
        {
          prop: "jobType",
          align: "center",
          halign: "center",
          label: this.$t("ui.specifyMachine.column.jobType"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.JOB_TYPE, value);
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
          prop: "updateTime",
          align: "center",
          halign: "center",
          label: this.$t("common.updateTime"),
          minWidth: 160,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["nc:specifyMachine:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["nc:specifyMachine:remove"]}
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
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.nc.specifyMachine.column.liningCode"),
          prop: "liningCode",
        },
        {
          label: this.$t("ui.specifyMachine.column.machineName"),
          prop: "machineCode",
          type: "select",
          dictData: this.machines,
          props: {
            value: "machineCode",
            label: "machineName",
          },
        },
        {
          label: this.$t("ui.data.column.specifyMachine.lineType"),
          prop: "lineType",
          type: "select",
          dictData: this.dict.type.LINE_TYPE, // "LINE_TYPE",
        },
        {
          label: this.$t("ui.data.column.specifyMachine.jobType"),
          prop: "jobType",
          type: "select",
          dictData: this.dict.type.JOB_TYPE, // "JOB_TYPE",
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
        removeSpecifyMachine({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
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
      downloadLink("/nc/specifyMachine/export", this.formatParams(false));
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
        const data = await listSpecifyMachine(this.formatParams());
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
    getConfigKey("sys.factory.code").then(response => {
      this.search.factoryCode = response.msg;
      this.query.factoryCode = response.msg;
      // 按当前工厂编码加载内衬机台下拉数据（机台信息存于 vuex state.insideLiner.machines），避免带出其他厂的机台
      this.$store.dispatch("insideLiner/getMachineList", { factoryCode: this.query.factoryCode });
      this.getList();
    }).catch(() => {
      this.getList();
    });
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
