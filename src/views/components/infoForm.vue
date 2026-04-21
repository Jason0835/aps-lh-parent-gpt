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
    rules: Object,
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

  created() {},
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
        let valid = await this.$refs.infoForm.validate().catch((error) => {
          console.error(error);
        });
        if (valid) {
          return { ...this.form };
        }
        return false;
      }
    },
    //
    renderInput(item) {
      return (
        <el-input
          v-model={this.form[item.prop]}
          clearable={typeof item.clearable === "boolean" ? item.clearable : true}
          placeholder={item.placeholder || this.$t("common.rule.input")}
          disabled={item.disabled}
          on={{
            ...item.listeners,
            input: (value) => {
              this.form[item.prop] = value.trim();
            },
          }}
          props={{
            ...item.attrs,
          }}
          rows={item.attrs?.rows}
          maxlength={item.maxlength}
        />
      );
    },
    renderTextarea(item) {
      return (
        <el-input
          v-model={this.form[item.prop]}
          clearable={typeof item.clearable === "boolean" ? item.clearable : true}
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
          class={"w100 input-number-text-left"}
          v-model={this.form[item.prop]}
          disabled={item.disabled}
          min={item.min}
          max={item.max}
          precision={item.precision}
          on={{ ...item.listeners }}
          controlsPosition={"right"}
          props={{
            ...item.attrs,
          }}
        />
      );
    },
    renderSelect(item) {
      let labelKey = item.props?.label ? item.props.label : "label";
      let valueKey = item.props?.value ? item.props.value : "value";
      // 字典数据场景
      if (item.dictData) {
        return (
          <el-select
            style="width:100%;"
            v-model={this.form[item.prop]}
            placeholder={item.placeholder || this.$t("common.rule.select")}
            clearable={typeof item.clearable === "boolean" ? item.clearable : true}
            filterable={item.filterable}
            disabled={item.disabled}
            remote={item.remote}
            remote-method={item.remoteMethod}
            popper-class={item.popperClass}
            loading={item.loading}
            on={{
              ...item.listeners,
              ...(item.onFocus && { focus: item.onFocus }),
              ...(item.onVisibleChange && { "visible-change": item.onVisibleChange }),
            }}
          >
            {item.dictData.map((row) => {
              let value = row[valueKey];
              if (item.valueType) {
                if (item.valueType === "string") {
                  value += "";
                }
              }
              return (
                <el-option
                  key={row[valueKey]}
                  value={value}
                  label={row[labelKey]}
                ></el-option>
              );
            })}
          </el-select>
        );
      }
      // 远程搜索场景 (options + remoteMethod)
      if (item.options) {
        return (
          <el-select
            style="width:100%;"
            v-model={this.form[item.prop]}
            placeholder={item.placeholder || this.$t("common.rule.select")}
            clearable={typeof item.clearable === "boolean" ? item.clearable : true}
            filterable={item.filterable}
            disabled={item.disabled}
            remote={item.remote}
            remote-method={item.remoteMethod}
            popper-class={item.popperClass}
            loading={item.loading}
            on={{
              ...item.listeners,
              ...(item.onFocus && { focus: item.onFocus }),
              ...(item.onVisibleChange && { "visible-change": item.onVisibleChange }),
            }}
          >
            {item.options.map((row) => {
              let value = row[valueKey];
              return (
                <el-option
                  key={value}
                  value={value}
                  label={row[labelKey]}
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
          on={{ ...item.listeners }}
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
          format={item.format || item.valueFormat || "yyyy-MM-dd"}
          value-format={item.valueFormat || "yyyy-MM-dd"}
          start-placeholder={this.$t("common.startTime")}
          end-placeholder={this.$t("common.endTime")}
          placeholder={this.$t("common.rule.select")}
          popper-class={item.popperClass || ""}
          disabled={item.disabled}
          picker-options={item.pickerOptions}
          on={{ ...item.listeners }}
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
    renderSwitch(item) {
      let activeValue = this.isEmpty(item.activeValue) ? false : item.activeValue;
      let inactiveValue = this.isEmpty(item.inactiveValue)
        ? true
        : item.inactiveValue;
      return (
        <el-switch
          v-model={this.form[item.prop]}
          disabled={item.disabled}

        />
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
        case "textarea":
          return this.renderTextarea(item);
        case "switch":
          return this.renderSwitch(item);
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
          rules: this.rules,
        }}
        ref="infoForm"
        v-loading={this.loading}
      >
        <el-row type="flex" style="flex-wrap: wrap">
          {this.columns.map((item) => {
            if (item.hidden === true) {
              return "";
            }
            if (item.type === "title") {
              return (
                <el-col span={24}>
                  <h4 class="form-header h4">{item.label}</h4>
                </el-col>
              );
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
