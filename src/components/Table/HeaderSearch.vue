<script>
import { deepClone } from "@/utils";
import BatchSearch from "@/components/Table/BatchSearch";

export default {
  components: { BatchSearch },
  props: {
    columns: Array,
    batchSearchColumns: {
      type: Array,
      default: () => null,
    },
    defaultValue: {
      type: Object,
      default: () => {
        return {};
      },
    },
  },
  data() {
    return {
      form: {},
    };
  },

  computed: {
    isRow: function () {
      return this.columns.length % 6 == 0 || this.columns.length % 8 == 0;
    },
    length: function () {
      return this.columns.length;
    },
  },
  watch: {
    defaultValue: {
      handler: function (val) {
        this.form = deepClone(this.defaultValue);
      },
      deep: true,
    },
  },

  mounted() {
    this.form = deepClone(this.defaultValue);
  },
  destroyed() {
    this.form = deepClone(this.defaultValue);
  },

  methods: {
    //util
    getValues() {
      return this.form;
    },
    //events
    handleSearch() {
      const data = deepClone(this.form);
      if (this.batchSearchColumns) {
        data.batchSearch = this.$refs.batchSearch.getFormData();
      }
      this.$emit("search", data);
    },
    handleReset() {
      this.form = deepClone(this.defaultValue);
      if (this.batchSearchColumns) {
        this.$refs.batchSearch.resetForm();
      }
      this.$emit("reset");
      this.$emit("search", deepClone(this.defaultValue));
    },

    //
    renderInput(item) {
      return (
        <el-input
          v-model={this.form[item.prop]}
          clearable
          placeholder={item.placeholder || this.$t("common.rule.input")}
          disabled={item.disabled}
          onKeydown={() => {
            //
            console.log(1);
          }}
        />
      );
    },
    renderSelect(item) {
      if (item.dictData) {
        let label = item.labelKey? item.labelKey : "label"
        let value = item.valueKey? item.valueKey : "value"
        return (
          <el-select
            style="width:100%;"
            v-model={this.form[item.prop]}
            clearable={item.clearable == false ? false : true}
            disabled={item.disabled}
            placeholder={this.$t("common.rule.select")}
            filterable={item.filterable}
          >
            {item.dictData.map((row) => {
              return (
                <el-option
                  key={row[value]}
                  value={row[value]}
                  label={row[label]}
                ></el-option>
              );
            })}
          </el-select>
        );
      }

      return (
        <t-select
          style="width:100%;"
          v-model={this.form[item.prop]}
          clearable={item.clearable == false ? false : true}
          disabled={item.disabled}
        ></t-select>
      );
    },
    renderDate(item) {
      return (
        <el-date-picker
          style="width:100%"
          type={item.dateType || "date"}
          v-model={this.form[item.prop]}
          clearable={item.clearable == false ? false : true}
          format={item.valueFormat || "yyyy-MM-dd HH:mm:ss"}
          value-format={item.valueFormat || "yyyy-MM-dd HH:mm:ss"}
          placeholder={this.$t("common.rule.select")}
          start-placeholder={this.$t("common.startTime")}
          end-placeholder={this.$t("common.endTime")}
          popper-class={item.popperClass || ""}
          disabled={item.disabled}
          default-time={item.defaultTime}
        ></el-date-picker>
      );
    },
    renderCheckbox(item) {
      return (
        <div style="color:#606266; ">
          <el-checkbox
            v-model={this.form[item.prop]}
            clearable
            disabled={item.disabled}
          ></el-checkbox>
          &nbsp;{item.content || ""}
        </div>
      );
    },
    renderButton(item) {
      return (
        <div>
          <el-button>{item.label}</el-button>
        </div>
      );
    },

    renderSearchItem(item) {
      switch (item.type) {
        case "date":
          return this.renderDate(item);
        case "select":
          return this.renderSelect(item);
        case "checkbox":
          return this.renderCheckbox(item);
        case "button":
          return this.renderButton(item);
        default:
          return this.renderInput(item);
      }
    },
    renderSearchButton() {
      // xs={24}
      //         sm={24}
      //         md={24}
      //         lg={length % 6 == 0 ? 24 : 4}
      //         lx={length % 8 == 0 ? 24 : 3}

      // if (this.isRow) {
      // }
      return (
        <div class="search-div">
          <el-button
            type="primary"
            class="search-button"
            icon="el-icon-search"
            onClick={this.handleSearch}
          >
            {this.$t("common.button.query")}
          </el-button>
          <el-button icon="el-icon-refresh" class="search-button" onClick={this.handleReset}>
            {this.$t("common.button.reset")}
          </el-button>
        </div>
      );
    },
    renderBranchSearch() {
      return (
        <BatchSearch
          ref="batchSearch"
          batchSearchColumns={this.batchSearchColumns}
        />
      );
    },
  },

  render() {
    return (
      <el-form labelPosition="right" labelWidth="auto" inline={true}>
        {this.batchSearchColumns ? this.renderBranchSearch() : ""}

          {this.columns.map((item) => {
            return (
                <el-form-item label={item.label} prop={item.prop} >
                  <div style="width:200px">
                  {item.render
                    ? item.render(this.form, item)
                    : this.renderSearchItem(item)}
                  </div>
                </el-form-item>
            );
          })}
          {this.renderSearchButton()}
        <el-input style="display:none" />
      </el-form>
    );
  },
};
</script>
<style lang="scss" scoped>
.center {
  text-align: center;
}
.height {
  min-height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.search-div{
  display: inline-flex;
  min-width: 200px;
  text-align: center;
  min-height: 30px;
  align-items: center;
  justify-content: center;
}
.search-button {
  padding: 5px 12px;
}
</style>
