<script>
import { debounce } from "@/utils";

import HeaderSearch from "./HeaderSearch.vue";

export default {
  components: { HeaderSearch },
  props: {
    data: Array,
    columns: Array,
    search: Object,
    searchColumns: {
      type: Array,
      default: () => [],
    },
    batchSearchColumns: {
      type: Array,
      default: () => null,
    },
    export: {
      type: Boolean,
      default: false,
    },
    page: Object,
    calcHeight: Number | Boolean,
    toolbar: {
      type: Boolean,
      default: true,
    },
    bulkEdit: {
      type: Boolean,
      default: false,
    },
    filter: {
      type: Boolean,
      default: false,
    },
    headerSum: {
      type: Boolean,
      default: false,
    },
    tableRef: {
      type: String,
      default: "tableRef",
    },
    searchVisible: {
      type: Boolean,
      default: true,
    },
    isHeight: {
      type: Boolean,
      default: false,
    },
    height: {
      type: String | Number,
    },
    columnsSort: {
      type: Boolean,
      default: true,
    },
    //是否手动重置
    isReset:{
      type: Boolean,
      default: false,
    }
  },
  data() {
    return {
      bulkEditForm: {},
      filterForm: {},
      headerSumForm: {},
      _resizeHandler: null,
      computedHeight: null,
    };
  },
  watch: {
    searchVisible: function (val, oldVal) {
      this.$nextTick(() => {
        this.resize();
      });
    },
  },

  created() {
    this._resizeHandler = debounce(() => {
      this.$nextTick(() => {
        this.resize();
      });
    }, 100);
    if (this.calcHeight) {
      window.addEventListener("resize", this._resizeHandler, false);
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.resize();
    });
  },
  beforeDestroy() {
    if (this.calcHeight) {
      window.removeEventListener("resize", this._resizeHandler, false);
    }
  },

  methods: {
    // events
    onSearch(data) {
      // console.log(data);
      // const data = this.$refs.searchRef.getValues();

      this.$emit("search", JSON.parse(JSON.stringify(data)));
    },
    onReset() {
      this.$emit("reset");
    },
    onCurrentChange(val) {
      this.$emit("pageChange", val, this.page.pageSize);
    },
    onSizeChange(val) {
      this.$emit("pageChange", 1, val);
    },
    handleSearchVisibleChange(val) {
      this.searchVisible = val;
    },
    handleRefresh() {
      this.$emit("refresh");
    },

    //utils
    getTableRef() {
      return this.$refs[this.tableRef];
    },
    /**获取除去表格之外元素的高度 */
    getRowHeight() {
      let rowHeight = 144;

      if (this.$refs.searchRef && this.searchVisible) {
        rowHeight += this.$refs.searchRef.$el.clientHeight;
      }

      if (this.$refs.headerRef) {
        rowHeight += this.$refs.headerRef.clientHeight;
      }
      if (this.$refs.pageRef) {
        rowHeight += this.$refs.pageRef.clientHeight;
      }
      return rowHeight;
    },
    resize() {
      console.log('resize');
      if (!this.calcHeight) {
        return;
      }

      let rowHeight = 144;

      if (this.$refs.searchRef && this.searchVisible) {
        rowHeight += this.$refs.searchRef.$el.clientHeight;
      }

      if (this.$refs.headerRef) {
        rowHeight += this.$refs.headerRef.clientHeight;
      }
      if (this.$refs.pageRef) {
        rowHeight += this.$refs.pageRef.clientHeight;
      }
      if (typeof this.calcHeight == "number") {
        rowHeight += this.calcHeight;
      }
      let tempHeight = window.innerHeight - rowHeight;
      this.computedHeight = `${tempHeight}px`;
      // this.computedHeight = `calc(100vh - ${rowHeight}px)`;

      this.$nextTick(() => {
        this.$refs[this.tableRef].doLayout();
        // this.$forceUpdate();
      });
    },

    // render
    renderHeader() {
      return (
        <div class="page-table-header">
          <div class="left">
            {this.$slots.header ? this.$slots.header : null}
          </div>
          <div class="right">
            <div>{this.$slots.headerRight ? this.$slots.headerRight : ""}</div>

            <right-toolbar
              class="toolbar"
              v-show={this.toolbar}
              tableRef={this.tableRef}
              search={this.searchColumns.length != 0}
              showSearch={this.searchVisible}
              columnsSort={this.columnsSort}
              on={{
                "update:showSearch": this.handleSearchVisibleChange,
                queryTable: this.handleRefresh,
              }}
            ></right-toolbar>
          </div>
        </div>
      );
    },

    renderFilter(filter, prop) {
      switch (filter) {
        case "number":
          return ({ filter }) => {
            return (
              <el-input-number
                class="w100"
                controls={false}
                v-model={filter[prop]}
              />
            );
          };
        default:
          return ({ filter }) => {
            return <el-input v-model={filter[prop]} clearable />;
          };
      }
    },
    renderBulkEdit(bulkEdit, prop) {
      switch (bulkEdit) {
        default:
          return ({ bulkEdit }) => {
            return <el-input v-model={bulkEdit[prop]} />;
          };
      }
    },
    renderColumns(columns) {
      return columns.map((column) => {
        const {
          type,
          render,
          bulkEdit,
          filter,
          sum,
          label,
          prop,
          showOverflowTooltip,
          minWidth,
          width,
          fitWidth,
          fixed,
          align,
          selectable,
          reserveSelection,
          children,
          ...other
        } = column;
        if (children && children.length != 0) {
          return (
            <t-table-column
              minWidth={minWidth}
              width={width}
              fitWidth={fitWidth}
              prop={prop}
              label={label}
              props={{
                "header-align": "center",
                visible: true,
                ...other,
                showOverflowTooltip:
                  showOverflowTooltip !== undefined || !!render
                    ? !!showOverflowTooltip
                    : true,
                align: align || (type == "number" ? "right" : undefined),
                fixed: fixed || false,
                type: type,
                label: label,
                prop: prop,
                width: width,
                minWidth: minWidth,
              }}
            >
              {this.renderColumns(children)}
            </t-table-column>
          );
        }
        if (type == "selection") {
          return (
            <t-table-column
              type="selection"
              prop="table-selection"
              width={width || 60}
              fixed={fixed}
              minWidth={minWidth}
              fitWidth={fitWidth}
              align="center"
              selectable={selectable}
              reserve-selection={reserveSelection}
            ></t-table-column>
          );
        }
        if (type == "index") {
          return (
            <t-table-column
              label="#"
              type="index"
              prop="index"
              width="80px"
              fixed={fixed}
              align="center"
              minWidth={minWidth}
              scopedSlots={{
                default: ({ $index }) => {
                  return $index + 1;
                },
              }}
            ></t-table-column>
          );
        }
        let tempMinWidth = this.computeWidth(label, minWidth);
        let tempWidth = width ? this.computeWidth(label, width) : width;
        return (
          <t-table-column
            minWidth={tempMinWidth}
            width={tempWidth}
            fitWidth={fitWidth}
            prop={prop}
            label={label}
            props={{
              "header-align": "center",
              visible: true,
              ...other,
              showOverflowTooltip:
                showOverflowTooltip !== undefined || !!render
                  ? !!showOverflowTooltip
                  : true,
              align: align || (type == "number" ? "right" : undefined),
              fixed: fixed || false,
              type: type,
              label: label,
              prop: prop,
              width: tempWidth,
              minWidth: tempMinWidth,
            }}
            scopedSlots={{
              default: render ? render : undefined,
              bulkEdit:
                typeof bulkEdit == "function"
                  ? bulkEdit
                  : typeof bulkEdit == "string"
                  ? this.renderBulkEdit(bulkEdit, prop)
                  : undefined,
              filter:
                typeof filter == "function"
                  ? filter
                  : typeof filter == "string"
                  ? this.renderFilter(filter, prop)
                  : undefined,
              headerSum:
                typeof sum == "function"
                  ? sum
                  : typeof sum == "string"
                  ? () => {
                      return sum;
                    }
                  : undefined,
            }}
            labelClassName={`${column.required ? "is-required" : ""} ${
              bulkEdit ? "is-edit" : ""
            }`}
          ></t-table-column>
        );
      });
    },
  },

  render() {
    return (
      <div class={this.calcHeight ? "page-table flex" : "page-table"}>
        {this.searchColumns.length || this.batchSearchColumns ? (
          <HeaderSearch
            ref="searchRef"
            defaultValue={this.search}
            class="page-table-search"
            v-show={this.searchVisible}
            columns={this.searchColumns}
            batchSearchColumns={this.batchSearchColumns}
            onSearch={this.onSearch}
            onReset={this.onReset}
            isReset={this.isReset}
          />
        ) : (
          ""
        )}
        {this.renderHeader()}
        <t-table
          class="page-table-content"
          ref={this.tableRef}
          border
          on={{ ...this.$listeners }}
          props={{
            "numeric-property-suffix": [
              "Num",
              "Amt",
              "Money",
              "Amount",
              "Qty",
              "Stock",
            ],
            "row-Key": "id",
            data: this.data,
            // showSummary: true,
            // selectArea: true,
            ...this.$attrs,
            border: true,
          }}
          height={
            this.calcHeight ? "100%" : this.height ? this.height : undefined
          }
          empty-text={this.$t("common.emptyDataDescription")}
          sum-text={this.$t("common.sum")}
          bulk-edit={this.bulkEdit ? this.bulkEditForm : undefined}
          filter={this.filter ? this.filterForm : undefined}
          header-sum={this.headerSum ? this.headerSumForm : undefined}
          columns={this.columns}
          selectArea={false}
          hiddenContextMenu
        >
          {this.renderColumns(this.columns)}
        </t-table>
        {this.$slots.footer ? this.$slots.footer : null}
        {this.page ? (
          <div ref="pageRef" class="page-table-page">
            <el-pagination
              style="text-align:right;margin-top:5px"
              background
              layout="total, sizes, prev, pager, next, jumper"
              currentPage={this.page.current}
              pageSize={this.page.pageSize}
              total={this.page.total}
              pageSizes={this.page.pageSizes || undefined}
              on={{
                "current-change": this.onCurrentChange,
                "size-change": this.onSizeChange,
              }}
            ></el-pagination>
          </div>
        ) : (
          ""
        )}
      </div>
    );
  },
};
</script>
<style lang="scss" scoped>

.page-table {

  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 0 0px 0;
    padding-bottom: 5px;
    flex-wrap: wrap;
    width: 100%;
    .left {
      flex: 1 1 auto;
    }
    .right {
      flex: 1 1 auto;
      display: flex;
      justify-content: flex-end;
      align-items: center;
      .toolbar {
        flex: 0 0 auto;
      }
      // flex: 0 0 auto;
    }

    // border-bottom: 1px solid #dcdfe6;
    .el-button {
      margin-bottom: 5px;
    }
  }
  &-search {
    width: 100%;
    padding-bottom: 10px;
    box-sizing: border-box;
  }
  &-page {
    width: 100%;
  }
}
.flex {
  display: flex;
  flex-direction: column;
  height: 100%;
}
::v-deep .warning-row {
    background: #FFCCCC;
  }
  ::v-deep .light-yellow {
    background: #FFFFE0;
  }
  ::v-deep .deep-yellow {
    background: #FFCC00;
  }
::v-deep .el-table__header-wrapper {
  .cell.is-required::before {
    content: "*";
    color: #ff4949;
    margin-right: 4px;
  }
  .cell.is-edit {
    color: #409eff;
  }
}
//取消合并连续空格
// ::v-deep .el-table__body-wrapper {
//   .cell {
//     white-space: pre;
//   }
// }
::v-deep .white-space-pre {
  .cell {
    white-space: pre;
  }
}
</style>
