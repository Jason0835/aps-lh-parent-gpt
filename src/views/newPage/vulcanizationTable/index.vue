
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
      <!-- <template slot="header">
        <el-button
          type="primary"
          plain
          >{{ $t("MES抓取") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:ProductMoldingLimit:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['monthplan:ProductMoldingLimit:edit']"
          :disabled="selection.length !== 1"
          @click="handleDelete(selection[0])"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:ProductMoldingLimit:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:ProductMoldingLimit:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template> -->
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/mdm/productMoldingLimit/importTemplate"
      uploadUrl="/mdm/productMoldingLimit/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState ,mapGetters} from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listVulcanizationTable,
} from "@/api/monthplan/vulcanizationTable";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "VulcanizationTable",
  components: {
    tltUpload,
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
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    ...mapGetters('globalList', ['structureList']),
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },

        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          width:180,
        },

        {
          prop: "lhMachine",
          label: this.$t("ui.data.vulcanizationTable.lhMachine"),
          width:120,
        },
        {
          prop: "cxMachine",
          label: this.$t("ui.data.vulcanizationTable.cxMachine"),
          width:120,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.colume.wms.unused.productCode"),
          width:120,
        },

        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width:320,
        },
        {
          prop: "mesMaterialCode",
          label: this.$t("ui.data.column.skuEmbryoRelation.materialCode"),
          width:120,
        },

        {
          prop: "mouldQty",
          label: this.$t("ui.data.vulcanizationTable.mouldQty"),
          width:120,
        },
        {
          prop: "netDemandQty",
          label: this.$t("ui.data.vulcanizationTable.netDemandQty"),
          width:120,
        },
        {
          prop: "onboardDate",
          label: this.$t("ui.data.column.monthplan.boardingDate"),
          width:120,
        },
        {
          prop: "unqualifiedQty",
          label: this.$t("ui.data.vulcanizationTable.unqualifiedQty"),
          width:120,
        },
        {
          prop: "productionQty",
          label: this.$t("ui.data.vulcanizationTable.productionQty"),
          width:120,
        },
        {
          prop: "lhMargin",
          label: this.$t("ui.data.vulcanizationTable.lhMargin"),
          width:120,
        },
        {
          prop: "expectedCloseDay",
          label: this.$t("ui.data.vulcanizationTable.expectedCloseDay"),
          width:120,
        },
        {
          prop: "expectedCloseDate",
          label: this.$t("ui.data.vulcanizationTable.expectedCloseDate"),
          width:120,
        },
        {
          prop: "planCloseDate",
          label: this.$t("ui.data.vulcanizationTable.planCloseDate"),
          width:120,
        },
        {
          prop: "diffDay",
          label: this.$t("ui.data.vulcanizationTable.diffDay"),
          width:120,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData:this.structureList,
          filterable: true
        },
        {
          prop: "cxMachine",
          label: this.$t("ui.data.vulcanizationTable.cxMachine"),
        },
        {
          prop: "lhMachine",
          label: this.$t("ui.data.vulcanizationTable.lhMachine"),
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.colume.wms.unused.productCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
        {
          prop: "mesMaterialCode",
          label: this.$t("ui.data.column.skuEmbryoRelation.materialCode"),
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
        // removeProductMoldingLimit({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
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
      downloadLink("/mdm/productMoldingLimit/export", this.formatParams(false));
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

        const data = await listVulcanizationTable(this.formatParams());
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
