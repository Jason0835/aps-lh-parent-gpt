<script>
import { deepClone } from "@/utils";

import SelectDialog from "./SelectDialog.vue";

export default {
  name: "SelectTableDialog",
  components: { SelectDialog },
  props: {
    title: String,
    columns: Array,
    data: Array,
    labelProp: {
      type: String,
      default: "id",
    },
    dataKey: String,
    value: String | Number,
    label: String,
    valueProp: {
      type: String,
      default: "id",
    },
    disabled: Boolean,
    multiple: {
      type: Boolean,
      default: false,
    },
    dialogWidth: {
      type: String | Number,
      default: "800px",
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      currentRow: null,
      showValue: "",
      page: {
        current: 1,
        pageSize: 10,
        total: 0,
      },
      listData: [],
      selection: [],
    };
  },
  watch: {
    value: function () {
      this.setLabel();
    },
    data: {
      handler(val, oldVal) {
        if (!this.showValue || this.showValue == this.value) {
          this.setLabel();
        }
        this.page.total = this.data.length;
        this.initListData();
      },
      deep: true,
    },
    label: function (val) {
      if (val && this.value == this.showValue) {
        this.showValue = val;
        // this.setLabel();
      }
    },
  },
  methods: {
    async setLabel() {
      if (!this.value) {
        this.showValue = "";
        return;
      }
      if (Array.isArray(this.data) && this.data.length) {
        let arr = [];
        if (this.value.includes(",")) {
          arr = this.value.split(",");
        } else {
          arr = [this.value];
        }
        let labelArr = [...arr];

        for (let i = 0, n = 0; n < labelArr.length; i++) {
          const item = this.data[i];
          if(!item) break;
          let value = item[this.valueProp];
          let index = labelArr.indexOf(value);
          if (index > -1) {
            labelArr[index] = item[this.labelProp];
            n++;
          }
        }

        this.showValue = labelArr.join(",");
        return;

        // this.data.forEach((item) => {});

        // let obj = this.findLabel(this.data);
        // if (obj) {
        //   this.showValue = obj[this.labelProp];
        //   return;
        // }
      }
      this.showValue = this.value;
    },
    // findLabel(options) {
    //   for (let i = 0; i < options.length; i++) {
    //     if (options[i][this.valueProp] == this.value) {
    //       const row = options[i];
    //       return row;
    //     } else {
    //       if (options[i].children) {
    //         const row = this.findLabel(options[i].children, row);
    //         if (row) {
    //           return row;
    //         }
    //       }
    //     }
    //   }
    //   return null;
    // },

    handleConfirm(done) {
      if (this.multiple) {
        console.log(this.selection);
        if (this.selection.length) {
          const ids = this.selection
            .map((item) => item[this.valueProp])
            .join(",");
          console.log(ids);
          this.$emit("change", ids, deepClone(this.selection));
        } else {
          this.$modal.msgError(this.$t("common.rule.select"));
          return;
        }
      } else if (this.currentRow) {
        // this.showValue = this.currentRow[this.labelProp] + "";
        this.$emit("change", this.currentRow[this.valueProp], {
          ...this.currentRow,
        });
      }

      this.selection = [];
      //确认完之后清空选中的内容，防止下次打开不选中直接确定带出上一次选中的内容。
      this.currentRow = null;

      done();
    },
    handleClear() {
      // if (this.$refs.tableRef) {
      //   const tableRef = this.$refs.tableRef.getTableRef();
      //   tableRef && tableRef.setCurrentRow();
      //   this.showValue = "";
      //   this.$emit("change", undefined);
      // }
      if (this.$refs.tableRef) {
        this.$refs.tableRef.setCurrentRow();
      }
      this.showValue = "";
      this.$emit("change", undefined);
    },
    handleShow() {
      this.$emit("show");
    },
    handleCancel() {
      this.$emit("cancel");
    },
    handleCurrentChange(val) {
      this.currentRow = val;
    },
    handleDbClick(row) {
      this.$emit("change", row[this.valueProp], {
        ...row,
      });
      this.$refs.dialogRef.hide();
      // this.currentRow = row;
      // this.handleConfirm(() => {
      //   this.$refs.dialogRef.hide();
      // });
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handlePageCurrentChange(val) {
      this.page.current = val;
      let startIndex = (this.page.current - 1) * this.page.pageSize;
      let endIndex = this.page.current * this.page.pageSize;
      this.listData = this.data.slice(startIndex, endIndex);
    },
    handleSizeChange(val) {
      this.page.pageSize = val;
      let startIndex = (this.page.current - 1) * this.page.pageSize;
      let endIndex = this.page.current * this.page.pageSize;
      this.listData = this.data.slice(startIndex, endIndex);
    },
    initListData() {
      this.listData = this.data.slice(0, this.page.pageSize);
    },
  },
  mounted() {
    this.setLabel();
    this.page.total = this.data.length;
    this.initListData();
  },
  render() {
    return (
      <select-dialog
        ref="dialogRef"
        title={this.title}
        value={this.showValue}
        onConfirm={this.handleConfirm}
        onClear={this.handleClear}
        disabled={this.disabled}
        onShow={this.handleShow}
        onCancel={this.handleCancel}
        dialogWidth={this.dialogWidth}
        loading={this.loading}
      >
      <div class="table-container" v-loading={this.loading}>
        <div class="page-table flex">
          <div class="page-table-search">
          {this.$slots.header ? this.$slots.header : null}
          </div>
          <t-table
            class="page-table-content"
            height="100%"
            ref="tableRef"

            on={{
              ...this.$listeners,
              "row-dblclick": this.handleDbClick,
              "current-change": this.handleCurrentChange,
              "selection-change": this.handleSelectionChange,
            }}
            props={{
              data: this.listData,
              ...this.$attrs,
              border: true,
              "default-expand-all": true,
              "highlight-current-row": true,
            }}
            empty-text={this.$t("common.emptyDataDescription")}
            sum-text={this.$t("common.sum")}
          >
            {this.multiple ? (
              <t-table-column key="selection" type="selection" width="80px"></t-table-column>
            ) : null}
            {this.columns.map((item) => {
              const { type, prop, render,label, minWidth,width,...props } = item;
              let tempMinWidth = this.computeWidth(label,minWidth);
              let tempWidth =width ? this.computeWidth(label,width) :width
              return (
                <t-table-column
                  type={type}
                  props={{
                    key: prop,
                    prop: prop,
                    label: label,
                    ...props,
                    width: tempWidth,
                    minWidth: tempMinWidth,
                  }}
                  scopedSlots={{
                    default: render ? render : undefined,
                  }}
                ></t-table-column>
              );
            })}
          </t-table>
            <div ref="pageRef" class="page-table-page">
              <el-pagination
                style="text-align:right;margin-top:5px"
                background
                layout="total, sizes, prev, pager, next, jumper"
                currentPage={this.page.current}
                pageSize={this.page.pageSize}
                total={this.page.total}
                on-current-change={this.handlePageCurrentChange}
                on-size-change={this.handleSizeChange}
              ></el-pagination>
            </div>
          </div>
        </div>
      </select-dialog>
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
.table-container{
  height: 450px
}
// ::v-deep .el-table__body-wrapper {
//   .cell {
//     white-space: pre;
//   }
// }
::v-deep .white-space-pre{
  .cell {
    white-space: pre;
  }
}
</style>
<style>
.el-pagination .el-icon-circle-check,
.el-pagination .el-icon-circle-close {
  display: none;
}
</style>
