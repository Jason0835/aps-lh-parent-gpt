
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
          @click="handleAdd('month')"
             v-hasPermi="['maindata:rawWarningRecord:executeNewMaterialWarning']"
          >{{ $t("ui.data.rawWarningRecord.executeNewMaterialWarning") }}</el-button
        >
        <el-button
          @click="handleAdd('week')"
             v-hasPermi="['maindata:rawWarningRecord:executeUsageWarning']"
          >{{ $t("ui.data.rawWarningRecord.executeUsageWarning") }}</el-button
        >
        <!-- <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:ProductMoldingLimit:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
      </template>
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
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listRawWarningRecord,
} from "@/api/maindata/rawWarningRecord";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "RawWarningRecord",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["warn_type", "biz_yes_no", "biz_factory_name",'warn_level'],
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
          prop: "materialCode",
          label: this.$t("ui.data.column.rawMaterial.materialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.rawMaterial.materialName"),
          width: 300,
        },
        {
          prop: "relatedMonth",
          label: this.$t("ui.data.colume.month"),
          width: 120,
        },

        {
          prop: "relatedWeek",
          label: this.$t("ui.data.column.rawMaterial.relatedWeek"),
          width: 180,
        },
        {
          prop: "warningContent",
          label: this.$t("ui.data.column.rawMaterial.warningContent"),
          width: 300,
        },
        // {
        //   prop: "warningData",
        //   label: this.$t("预警数据"),
        // },
        {
          prop: "warningLevel",
          label: this.$t("ui.data.column.rawMaterial.warningLevel"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.warn_level, value);
          },
        },
        {
          prop: "warningTitle",
          label: this.$t("ui.data.column.rawMaterial.warningTitle"),
          width: 280,
        },
        {
          prop: "warningType",
          label: this.$t("ui.data.column.rawMaterial.warningType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.warn_type, value);
          },
          width: 160,

        },

        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 180,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 180,
        },

      ];

      return columns;
    },
    searchColumns() {
      return [
      {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.rawMaterial.warningType"),
          prop: "warningType",
          type: "select",
          dictData: this.dict.type.warn_type,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.rawMaterial.materialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.rawMaterial.materialName"),
        },


      ];
    },
  },
  methods: {
    handleAdd(type) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(type);
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
        const data = await listRawWarningRecord(this.formatParams());
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
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
