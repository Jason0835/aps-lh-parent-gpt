

<template>
  <basic-container>
    <page-table
      tableRef="masterdataConstructionInfoMainTable"
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
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:mdmConstructionInfo:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['cx:machine:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          plain
          v-hasPermi="['monthplan:mdmConstructionInfo:remove']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['monthplan:mdmBomInfo:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmBomInfo:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mdmConstructionInfo/importTemplate"
      uploadUrl="/monthplan/mdmConstructionInfo/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listMdmConstructionInfo } from "@/api/monthplan/mdmConstructionInfo";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "ConstructionInfo",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: ["molding_method"],
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
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.constructionInfo.proSize"),
        },


        {
          prop: "constructionCode",
          label: this.$t("ui.data.column.constructionInfo.constructionCode"),
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.constructionInfo.productDescription"),
          width: 120,
        },
        {
          prop: "headWidth",
          label: this.$t("ui.data.column.constructionInfo.headWidth"),
        },
        {
          prop: "bucklePlageDiameter",
          label: this.$t("ui.data.column.constructionInfo.bucklePlateSize"),
        },
        {
          prop: "tireFabricCode1",
          label: this.$t("ui.data.column.constructionInfo.carcassClothCode1"),
        },
        {
          prop: "tireFabricCode2",
          label: this.$t("ui.data.column.constructionInfo.carcassClothCode2"),
        },
        {
          prop: "tireFabricCode3",
          label: this.$t("ui.data.column.constructionInfo.carcassClothCode3"),
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.constructionInfo.specifications"),
        },
        {
          prop: "insideCode",
          label: this.$t("ui.data.column.constructionInfo.liningCode"),
        },
        {
          prop: "sidewallCode",
          label: this.$t("ui.data.column.constructionInfo.sidewallCode"),
        },
        {
          prop: "supportCode",
          label: this.$t("ui.data.column.constructionInfo.beadFillerCode"),
        },
        {
          prop: "articleCrownCode",
          label: this.$t("ui.data.column.constructionInfo.crownBandCode"),
        },
        {
          prop: "treadCode",
          label: this.$t("ui.data.column.constructionInfo.treadCode"),
          width: 150,
        },
        {
          prop: "fitDrumPerimeter",
          label: this.$t(
            "ui.data.column.constructionInfo.beltdrumCirumference"
          ),
        },
        {
          prop: "chuckDiameter",
          label: this.$t("ui.data.column.constructionInfo.chuckDiameter"),
        },
        {
          prop: "stretchWidth",
          label: this.$t("ui.data.column.constructionInfo.extensionWidth"),
        },
        {
          prop: "qualitativeWidth",
          label: this.$t("ui.data.column.constructionInfo.shapingWidth"),
        },
        {
          prop: "embryoCircle",
          label: this.$t("ui.data.column.constructionInfo.fetalCircumference"),
        },
        {
          prop: "mouldClampingPressure",
          label: this.$t(
            "ui.data.column.constructionInfo.mouldClampingPressure"
          ),
        },
        {
          prop: "belt1", //TODO
          label: this.$t("ui.data.column.constructionInfo.belt1"),
        },
        {
          prop: "belt2", //TODO
          label: this.$t("ui.data.column.constructionInfo.belt2"),
        },
        {
          prop: "belt3", //TODO
          label: this.$t("ui.data.column.constructionInfo.belt3"),
        },
        {
          prop: "tireRingCode",
          label: this.$t("ui.data.column.constructionInfo.bead"),
        },
        {
          prop: "xjCuringTime", //TODO
          label: this.$t("ui.data.column.constructionInfo.xjCuringTime"),
        },
        {
          prop: "djCuringTime", //TODO
          label: this.$t("ui.data.column.constructionInfo.djCuringTime"),
        },
        {
          prop: "mouldMethod",
          label: this.$t("ui.data.column.constructionInfo.mouldMethod"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.molding_method,
              row.mouldMethod
            );
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "constructionCode",
          label: this.$t("ui.data.column.constructionInfo.constructionCode"),
        },
        {
          label: this.$t("ui.data.column.constructionInfo.mouldMethod"),
          prop: "mouldMethod",
          type: "select",
          dictData: this.dict.type.molding_methods,
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
        removeShiftLimit({ ids })
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
      downloadLink(
        "/monthplan/mdmConstructionInfo/export",
        this.formatParams(false)
      );
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
        const data = await listMdmConstructionInfo(this.formatParams());
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

  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
