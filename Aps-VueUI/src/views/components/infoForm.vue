<script>
import { deepClone } from "@/utils";
export default {
  // model: {
  //   prop: "form",
  //   event: "updateForm"
  // },
  props: {
    columns: Array,
    form: {
      type: Object,
      default: () => {
        return {};
      },
    },
  },
  data() {
    return {
      // form: {},
      defaultResponsive: {
        3: {
          xs: 24,
          sm: 12,
          md: 6,
          lg: 3,
          xl: 3,
        },
        4: {
          xs: 24,
          sm: 12,
          md: 8,
          lg: 4,
          xl: 4,
        },
        6: {
          xs: 24,
          sm: 12,
          md: 12,
          lg: 6,
          xl: 6,
        },
        8: {
          xs: 24,
          sm: 12,
          md: 12,
          lg: 8,
          xl: 8,
        },
        12: {
          xs: 24,
          sm: 12,
          md: 12,
          lg: 12,
          xl: 12,
        },
        24: {
          xs: 24,
          sm: 24,
          md: 24,
          lg: 24,
          xl: 24,
        },
      },
    };
  },
  // watch: {
  //   defaultValue: {
  //     handler: function (val) {
  //       this.form = deepClone(this.defaultValue);
  //     },
  //     deep: true,
  //   },
  // },

  mounted() {
    // this.form = deepClone(this.defaultValue);
  },
  destroyed() {
    // this.form = deepClone(this.defaultValue);
  },

  methods: {
    //util
    getValues() {
      return this.form;
    },
    triggerResetForm() {
      this.resetForm("infoForm");
    },
    //events
    async triggerConfirm(callback) {
      if (callback && typeof callback == "function") {
        return this.$refs.infoForm.validate((valid) => {
          if (valid) {
            typeof callback == "function" && callback({ ...this.form });
          }
        });
      } else {
        let valid = await this.$refs.infoForm.validate().catch(() => {});
        if (valid) {
          return { ...this.form };
        }
        return false;
      }
    },
    handleSearch() {
      const data = deepClone(this.form);
      if (this.batchSearchColumns) {
        data.batchSearch = this.$refs.batchSearch.getFormData();
      }
      this.$emit("search", data);
    },
    handleReset() {
      this.form = deepClone(this.defaultValue);
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
          on={{ ...item.listeners }}
          props={{
            ...item.attrs,
          }}
          rows={item.attrs?.rows}
          maxlength={item.maxlength}
          onKeydown={() => {
            //
            console.log(1);
          }}
        />
      );
    },
    renderTextarea() {
        return (
        <el-input
          v-model={this.form[item.prop]}
          clearable={typeof item.clearable == 'boolean' ? item.clearable  : true}
          type="textarea"
          rows={item.rows}
          placeholder={item.placeholder || this.$t("common.rule.input")}
          disabled={item.disabled}
          on={{ ...item.listeners }}
          props={{
            ...item.attrs,
          }}
          maxlength={item.maxlength}

        />
      );
    },
    renderInputNumber(item) {
      return (
        <el-input-number
          class="w100 input-number-text-left"
          v-model={this.form[item.prop]}
          disabled={item.disabled}
          min={item.min}
          max={item.max}
          precision={item.precision}
          on={{ ...item.listeners }}
          props={{
            ...item.attrs,
          }}

        />
      );
    },
    renderSelect(item) {
      if (item.dictData) {
        return (
          <el-select
            style="width:100%;"
            v-model={this.form[item.prop]}
            clearable
            disabled={item.disabled}
          >
            {item.dictData.map((item) => {
              return (
                <el-option
                  key={item.value}
                  value={item.value}
                  label={item.label}
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
          clearable
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
          format={item.valueFormat || "yyyy-MM-dd"}
          value-format={item.valueFormat || "yyyy-MM-dd"}
          start-placeholder={this.$t("common.startTime")}
          end-placeholder={this.$t("common.endTime")}
          popper-class={item.popperClass || ""}
          disabled={item.disabled}
        ></el-date-picker>
      );
    },
    renderTime(item) {
      return (
        <el-time-picker
          style="width:100%"
          v-model={this.form[item.prop]}
          clearable={item.clearable == false ? false : true}
          format={item.valueFormat || "HH:mm:ss"}
          value-format={item.valueFormat || "HH:mm:ss"}
          start-placeholder={this.$t("common.startTime")}
          end-placeholder={this.$t("common.endTime")}
          popper-class={item.popperClass || ""}
          disabled={item.disabled}
        ></el-time-picker>
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

    renderFormItem(item) {
      switch (item.type) {
        case "date":
          return this.renderDate(item);
        case "time":
          return this.renderTime(item);
        case "select":
          return this.renderSelect(item);
        case "checkbox":
          return this.renderCheckbox(item);
        case "button":
          return this.renderButton(item);
        case "number":
          return this.renderInputNumber(item);
        default:
          return this.renderInput(item);
      }
    },
  },
  render() {
    return (
      <el-form
        class="form-item-height"
        label-position="right"
        labelWidth={"auto"}
        on={{ ...this.$listeners }}
        props={{
          ...this.$attrs,
          model: this.form,
        }}
        ref="infoForm"
        v-loading={this.loading}
      >
        <el-row>
          {this.columns.map((item) => {
            if (item.hidden === true) {
              return "";
            }

            if (!item.prop) {
              if (item.render) {
                return (
                  <el-col span={item.span || 24}>
                    {item.render(this.form, item)}
                  </el-col>
                );
              } else if (item.span) {
                return <el-col span={item.span} />;
              }
            } else if (item.span && !item.layout) {
              return (
                <el-col
                  span={item.span}
                  xs={
                    this.defaultResponsive[item.span]
                      ? this.defaultResponsive[item.span].xs
                      : item.span
                  }
                  sm={
                    this.defaultResponsive[item.span]
                      ? this.defaultResponsive[item.span].sm
                      : item.span
                  }
                  md={
                    this.defaultResponsive[item.span]
                      ? this.defaultResponsive[item.span].md
                      : item.span
                  }
                  lg={
                    this.defaultResponsive[item.span]
                      ? this.defaultResponsive[item.span].lg
                      : item.span
                  }
                  xl={
                    this.defaultResponsive[item.span]
                      ? this.defaultResponsive[item.span].xl
                      : item.span
                  }
                >
                  <el-form-item
                    label={item.label}
                    prop={item.prop}
                    labelWidth={item.labelWidth}
                    required={item.required}
                  >
                    {item.render
                      ? item.render(this.form, item)
                      : this.renderFormItem(item)}
                  </el-form-item>
                </el-col>
              );
            } else if (item.span || item.layout) {
              return (
                <el-col
                  span={item.span}
                  xs={item.layout.xs}
                  sm={item.layout.sm}
                  md={item.layout.md}
                  lg={item.layout.lg}
                  xl={item.layout.xl}
                >
                  <el-form-item
                    label={item.label}
                    prop={item.prop}
                    labelWidth={item.labelWidth}
                  >
                    {item.render
                      ? item.render(this.form, item)
                      : this.renderFormItem(item)}
                  </el-form-item>
                </el-col>
              );
            } else {
              return (
                <el-col span={24}>
                  <el-form-item
                    label={item.label}
                    prop={item.prop}
                    labelWidth={item.labelWidth}
                  >
                    {item.render
                      ? item.render(this.form, item)
                      : this.renderFormItem(item)}
                  </el-form-item>
                </el-col>
              );
            }
          })}
        </el-row>
      </el-form>
    );
  },
};
</script>
<style scoped>
::v-deep .input-number-text-left .el-input__inner {
  text-align: left;
}
.el-form-item {
  margin-right: 10px;
}
</style>
