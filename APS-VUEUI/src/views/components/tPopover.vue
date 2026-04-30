<script>
import { render } from "nprogress";
export default {
  model: {
    prop: "value",
    event: "input",
  },
  props: {
    type: {
      type: String,
      default: "number",
    },
    title: {
      type: String,
      default: "",
    },
    width: Number,
    value: {
      type: Number | String,
      default: 0,
    },
    clearable: { type: Boolean, default: true },
    min: {
      type: Number,
      default: -9999999,
    },
    max: {
      type: Number,
      default: 9999999,
    },
    showClose: {
      type: Boolean | Function,
      default: true,
    },
    showConfirm: {
      type: Boolean | Function,
      default: true,
    },
    enterConfirm: {
      type: Boolean | Function,
      default: true,
    },
  },
  data() {
    return {
      tempValue: 0,
    };
  },
  methods: {
    handleEnterConfirm(event) {
      if (event.keyCode !== 13 && this.enterConfirm) return;
      this.handleConfirmClick();
    },
    handleShowClick() {
      this.tempValue = this.value;
    },
    handleConfirmClick() {
      this.$emit("confirm", this.tempValue);
      this.handleHideClick();
    },
    handleHideClick() {
      this.$refs.hidePopoverBtnRef.$el.click();
    },
    renderInput() {
      return (
        <el-input
          style="width:100%;"
          clearable={this.clearable == false ? false : true}
          v-model={this.tempValue}
          vOn:keyup_native={this.handleEnterConfirm}
          disabled={this.disabled}
        />
      );
    },
    renderInputNumber() {
      return (
        <el-input-number
          style="width:100%;"
          controls={false}
          min={this.min}
          max={this.max}
          v-model={this.tempValue}
          vOn:keyup_native={this.handleEnterConfirm}
          disabled={this.disabled}
        />
      );
    },
    renderDatePicker() {
      return (
        <el-date-picker
          style="width:100%"
          type={this.dateType || "date"}
          v-model={this.tempValue}
          clearable={this.clearable == false ? false : true}
          format={this.valueFormat || "yyyy-MM-dd"}
          value-format={this.valueFormat || "yyyy-MM-dd"}
          start-placeholder={this.$t("common.startTime")}
          end-placeholder={this.$t("common.endTime")}
          popper-class={this.popperClass || ""}
          disabled={this.disabled}
        ></el-date-picker>
      );
    },
    renderItem() {
      switch (this.type) {
        case "text":
          return this.renderInput();
          break;
        case "number":
          return this.renderInputNumber();
          break;
        case "date":
          return this.renderDatePicker();
          break;
        default:
          break;
      }
    },
  },
  computed: {
    popWidth() {
      if (this.width) {
        return this.width;
      } else {
        //根据默认值235，和是否显示确认，关闭按钮计算
        let tempWidth =
          235 - (this.showClose ? 0 : 45) - (this.showConfirm ? 0 : 45);
        return tempWidth;
      }
    },
    inpWidth() {
      //输入框可用空间计算
      let tempWidth =
        this.popWidth -
        25 -
        (this.showClose ? 55 : 0) -
        (this.showConfirm ? 55 : 0);
      return tempWidth;
    },
  },
  render() {
    return (
      <div>
        <el-popover
          placement="bottom"
          title={this.title}
          width={this.popWidth}
          trigger="click"
        >
          <div
            style={{
              display: "inline-block",
              margin: 0,
              width: this.inpWidth + "px",
            }}
          >
            {
              this.$slots.default ? this.$slots.default : this.renderItem()
              // <el-input-number
              //   style="width:100%;"
              //   controls={false}
              //   min={this.min}
              //   max={this.max}
              //   clearable
              //   v-model={this.tempValue}
              //   vOn:keyup_native={this.handleEnterConfirm}
              // />
            }
          </div>
          <div style="display:inline-block;margin-left:10px">
            {this.showConfirm && (
              <el-button
                type="primary"
                size="mini"
                icon="el-icon-check"
                loading={this.loading}
                onClick={this.handleConfirmClick}
              ></el-button>
            )}
            {this.showClose && (
              <el-button
                size="mini"
                icon="el-icon-close"
                onClick={this.handleHideClick}
              ></el-button>
            )}
          </div>
          {this.$slots.reference ? (
            this.$slots.reference
          ) : (
            <el-link
              type="primary"
              slot="reference"
              onClick={this.handleShowClick}
            >
              {this.value || '-'}
            </el-link>
          )}
        </el-popover>
        <el-button style="display: none" ref="hidePopoverBtnRef"></el-button>
      </div>
    );
  },
};
</script>

<style>
</style>
