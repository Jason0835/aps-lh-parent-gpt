<script>
import Big from "big.js";

export default {
  name: "TableTree",
  props: {
    //是否显示分组合计
    sum: {
      type: Boolean,
      default: false,
    },
    //分组自定义合计方法
    sumMethod: {
      type: Function,
      default: undefined,
    },
  },
  data() {
    return {
      _data: [],
    };
  },
  watch: {
    "$attrs.data": {
      immediate: true,
      deep: false,
      handler(data) {
        if (!data?.length || !this.sum) {
          return;
        }
        this._data = [...data];
        let _sumKey = 0;
        const getSumKey = () => {
          return `${rowKey}_${++_sumKey}`;
        };
        const rowKey = this.$attrs["row-key"];
        if (!rowKey) {
          console.error("请设置 row-key 属性");
          return;
        }
        this._data.forEach((item) => {
          if (!item.children?.length) {
            return;
          }
          const children = item.children.filter((item) => item.type !== "sum");
          item.children = children;
          const columns = [];
          this.$slots.default.forEach((slot) => {
            if (slot?.componentOptions?.tag !== "t-table-column") {
              return;
            }
            const propsData = slot.componentOptions.propsData;
            if (propsData.type === "selection" || propsData.type === "index") {
              return;
            }
            columns.push({ ...propsData });
          });
          if (columns.length === 0) {
            return;
          }

          const sumRow = this.sumMethod
            ? this.sumMethod(item)
            : ((children) => {
                const rs = {
                  type: "sum",
                  [rowKey]: getSumKey(),
                };
                children.forEach((item) => {
                  columns.forEach((column, index) => {
                    if (index === 0) return;
                    //判断 item[column.prop] 是否是 number
                    if (typeof item[column.prop] !== "number") {
                      return;
                    }
                    rs[column.prop] = Big(rs[column.prop] || 0)
                      .plus(Big(item[column.prop] || 0))
                      .toNumber();
                  });
                });
                return rs;
              })(children);
          children.push(sumRow);
        });
      },
    },
  },
  render: function (h) {
    const treeProps = this.$attrs["tree-props"];
    const spanMethod = this.$attrs["span-method"];
    return (
      <t-table
        ref="table"
        props={{
          ...this.$attrs,
          "tree-props": treeProps
            ? treeProps
            : { children: "children", hasChildren: "hasChildren" },
          "span-method": spanMethod ? spanMethod : this.spanMethod,
        }}
        onSelect={this.select}
        on-select-all={this.selectAll}
        empty-text={this.$t("common.emptyDataDescription")}
        sum-text={this.$t("common.sum")}
      >
        {this.$slots.default}
      </t-table>
    );
  },
  methods: {
    getSelection() {
      const data = this.$refs.table.selection;

      return data.filter((item) => {
        return !item.children;
      });
    },

    select(selection, row) {
      if (!row.children) {
        return;
      }
      const flag = selection.includes(row);
      const table = this.$refs.table;
      row.children.forEach((item) => {
        if (item.type === "sum") return;
        table.toggleRowSelection(item, flag);
      });
    },
    selectAll(selection, allSelect) {
      const table = this.$refs.table;
      const a = [...selection];
      a.forEach((item) => {
        if (!item.children) {
          table.toggleRowSelection(item, allSelect);
          return;
        }
        item.children.forEach((child) => {
          if (child.type === "sum") return;
          table.toggleRowSelection(child, allSelect);
        });
      });
    },
    spanMethod({ row, column, rowIndex, columnIndex }) {
      if (row?.children) {
        return {
          rowspan: 1,
          colspan: 1,
        };
      }
      if (row.type == "sum") {
        if (columnIndex === 0) {
          return {
            rowspan: 0,
            colspan: 0,
          };
        } else if (columnIndex === 1) {
          return {
            rowspan: 1,
            colspan: 2,
          };
        } else {
          return {
            rowspan: 1,
            colspan: 1,
          };
        }
      }
      return {
        rowspan: 1,
        colspan: 1,
      };
    },
  },
};
</script>
