<template>
  <basic-container>
    <page-table
      tableRef="areaListMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button
          @click="handleExport"
          >{{ $t("common.button.export") }}</el-button
        >
      </template>
    </page-table>

    <InfoDialog ref="infoRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>

  import {downloadLink} from "@/utils/request.js";

  import InfoDialog from "./components/infoDialog.vue";
  import {listLanguage} from "@/api/bd/i18nChange";
  // v-hasPermi="['system:language:download']"

export default {
  name: "/system/language",
  components: { InfoDialog },
  data() {
    return {
      columns: [
        { type: "index", fixed: "left" },
        {
          label: this.$t("common.option"),
          prop: "option",
          width: "80px",
          fixed: "left",
          render: ({ row }) => {
            return (
              <div>
                <text-button
                  onClick={() => {
                    this.handleEdit(row);
                  }}
                  v-hasPermi={["bd:area:edit"]}
                >
                  {this.$t("common.button.edit")}
                </text-button>
              </div>
            );
          },
        },
        {
          label: this.$t("common.code"),
          prop: "changeKey",
          sortable: "custom",
        },
        {
          label: this.$t("common.chinese"),
          prop: "changeValueI18n_zh_CN",
        },
        {
          label: this.$t("common.english"),
          prop: "changeValueI18n_en_US",
        },
        {
          label: this.$t("common.vietnamese"),
          prop: "changeValueI18n_vi_VN",
        },
        {
          label: this.$t("common.module"),
          prop: "modeNameI18n"

        }
      ],
      searchColumns: [
        {
          label: this.$t("common.code"),
          prop: "changeKey",
        },
      {
          label: this.$t("common.name"),
          prop: "changeValue",
        },
      {
          label: this.$t("common.module"),
          prop: "modeName",
        },
      ],
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 10,
        total: 0,
      },
      sort: {},
      query: {},
    };
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
        removeArea({ ids }).then((data) => {
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

    //util
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderBy: prop,
          isAsc: order == "ascending",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },

    formatParams() {
      const params = {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        params: {
          ...this.sort,
        },
        areaType: "02",
      };

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listLanguage(this.formatParams());

        this.data = data.rows;

        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    handleExport() {
      // const params = this.formatParams();
      // params.pageNum = undefined;
      // params.pageSize = undefined;
      downloadLink("/bd/i18nChange/download");
    },
  },
  activated() {
    this.getList();
  },
};
</script>
