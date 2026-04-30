<script>
import HeaderSearch from "./HeaderSearch.vue";
import PageTable from "./PageTable";

export default {
  components: { PageTable, HeaderSearch },
  props: {
    data: Array,
    columns: Array,
    active: String | Number,
    searchColumns: {
      type: Array,
      default: () => [],
    },
    export: {
      type: Boolean,
      default: false,
    },
    tabs: {
      default: Array,
      default: () => [],
    },
  },
  data() {
    return {
      test: 1,
    };
  },

  methods: {
    renderHeader() {
      if (this.searchColumns.length) {
        return (
          <div class="page-table-header">
            <el-button>{this.$t("common.button.query")}</el-button>
            {this.export ? (
              <el-button>{this.$t("common.button.export")}</el-button>
            ) : (
              ""
            )}
            {this.$slots.header
              ? [<el-divider direction="vertical" />, ...this.$slots.header]
              : ""}
          </div>
        );
      }
      return this.$slots.header ? (
        <div class="page-table-header"> this.$slots.header</div>
      ) : (
        ""
      );
    },
  },

  render() {
    return (
      <div class="page-table">
        {this.renderHeader()}
        {this.searchColumns.length ? (
          <HeaderSearch columns={this.searchColumns} />
        ) : (
          ""
        )}
        <el-tabs type="border-card">
          {this.tabs.map((tab) => {
            return (
              <el-tab-pane label={tab.label} name={tab.value} key={tab.value}>
                <PageTable
                  columns={tab.columns}
                  data={this.data}
                  bulkEdit={tab.bulkEdit}
                  filter={tab.filter}
                />
              </el-tab-pane>
            );
          })}
        </el-tabs>
      </div>
    );
  },
};
</script>
<style lang="scss" scoped>
.page-table {
  &-header {
    padding: 5px 0 10px;
    margin-bottom: 0px;
  }
}
</style>
