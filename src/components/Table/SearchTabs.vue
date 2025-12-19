<script>
import HeaderSearch from "./HeaderSearch.vue";

export default {
  components: { HeaderSearch },
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
    return {};
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
        <el-tabs type="border-card">{his.$slots.default}</el-tabs>
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
