
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
          @click="handleAdd"
            v-hasPermi="['maindata:rawWeekUsage:generateByMonth']"
          >{{ $t("ui.data.column.rawMaterial.monthGen") }}</el-button
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
  listRawWeekUsage,
} from "@/api/maindata/rawWeekUsage";
import {
  getMdmProductVersion,
} from "@/api/maindata/rawMaterialRequirePlan​";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "RawWeekUsage",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["LINE_TYPE", "biz_yes_no", "biz_factory_name",'warn_level'],
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
      selectList:[]
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
          prop: "version",
          label: this.$t("plan.planProduction.planVersion"),
        },
        {
          prop: "deviationQty",
          label: this.$t("ui.data.column.rawMaterial.deviationQty"),
        },
        {
          prop: "deviationRate",
          label: this.$t("ui.data.column.rawMaterial.deviationRate"),
        },
        {
          prop: "hasWarning",
          label: this.$t("ui.data.column.rawMaterial.hasWarning"),

          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
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
          prop: "year",
          label: this.$t("ui.data.colume.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
        },
        {
          prop: "week",
          label: this.$t("ui.data.column.rawMaterial.relatedWeek"),
        },
        {
          prop: "warningLevel",
          label: this.$t("ui.data.column.rawMaterial.warningLevel"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.warn_level, value);
          },
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 240,
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
          prop: "version",
          label: this.$t("plan.planProduction.planVersion"),
          type: "select",
          dictData: this.selectList,
          clearable: false,
          filterable: true,
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
        const data = await listRawWeekUsage(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getVersion(isGet = false) {
      try {
        this.loading = true;
        const params = {
          ...this.search,
        ...this.query,
        };
        const res = await getMdmProductVersion({
          factoryCode: params.factoryCode,
        });
        let list = [];
        for (let i = 0; i < res.length; i++) {
          list.push({
            label: res[i].version,
            value: res[i].version,
          });
        }
        this.selectList = list;
        if (res.length > 0) {
          this.$set(this.query, "version", res[0].version);
          this.$set(this.search, "version", res[0].version);
        } else {
          this.$set(this.query, "version", "");
          this.$set(this.search, "version", "");
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
        if (isGet) {
          this.getList();
        }
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
    // this.getList();
    this.getVersion(true);
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
