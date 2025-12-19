
<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
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
            v-hasPermi="['maindata:mdmModelInfo:mesCapture']"
          plain
          @click="capture"
          >{{ $t("ui.data.column.moldLedger.mes") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['maindata:mdmModelInfo:edit']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['maindata:mdmModelInfo:remove']"
            :disabled="selection.length == 0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['maindata:mdmModelInfo:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['maindata:mdmModelInfo:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/maindata/mdmModelInfo/importTemplate"
      uploadUrl="/maindata/mdmModelInfo/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listMdmModelInfo,removeMdmModelInfo,mesCapture
} from "@/api/maindata/mdmModelInfo";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "moldingRestrictions",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["logistics_status", "biz_mould_Type", "biz_factory_name",'biz_available_status','logistics_status'],
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
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.moldLedger.mouldCode"),
        },
        {
          prop: "mouldType",
          label: this.$t("ui.data.column.modelinfo.mouldType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_mould_Type, value);
          },
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.docVulcanizationMachStatus.status"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_available_status, value);
          },
        },
        {
          prop: "logisticsStatus",
          label: this.$t("ui.data.column.moldLedger.logisticsStatus"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.logistics_status, value);
          },
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.reportClassAccuracy.materialCode"),
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.moldLedger.pattern"),
        },
        {
          prop: "shellStandard",
          label: this.$t("ui.data.column.moldLedger.shellStandard"),
        },
        {
          prop: "remark",
          label: this.$t("ui.remark"),
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width:200,
        },

        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          fixed: "right",
          width: 160,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["maindata:mdmModelInfo:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["maindata:mdmModelInfo:remove"]}
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select", //GLUE_TYPE
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.moldLedger.mouldCode"),
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.reportClassAccuracy.materialCode"),
        },
        {
          prop: "logisticsStatus",
          label: this.$t("ui.data.column.moldLedger.logisticsStatus"),
          type: "select", //GLUE_TYPE
          dictData: this.dict.type.logistics_status,
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
        },
        {
          prop: "mouldType",
          label: this.$t("ui.data.column.modelinfo.mouldType"),
          type: "select",
          dictData: this.dict.type.biz_mould_Type,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.moldLedger.pattern"),
          // type: "select",
        },
        {
          prop: "shellStandard",
          label: this.$t("ui.data.column.moldLedger.shellStandard"),
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.docVulcanizationMachStatus.status"),
          type: "select",
          dictData: this.dict.type.biz_available_status,
        },

      ];
    },
  },
  methods: {
    handleExport() {
      downloadLink(
        "/maindata/mdmModelInfo/export",
        this.formatParams(false)
      );
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    async capture() {
      try {
        let res = await mesCapture();
        this.$modal.msgSuccess(res.msg);
        this.$set(this.page, "current", 1);
        this.getList();
      } catch (err) {
        console.log(err)
      }
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

    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeMdmModelInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteAll() {
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids =  ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeMdmModelInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
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
    // api
    async getList() {
      try {
        this.loading = true;

        const data = await listMdmModelInfo(this.formatParams());
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
    let defaultParams = {
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
