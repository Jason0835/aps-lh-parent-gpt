<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import {
  editLhMouldChangePlan,
  getMachineList,
} from "@/api/lh/lhMouldChangePlan";

import infoForm from "@/views/components/infoForm.vue";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";

export default {
  components: { infoForm, materialCodeSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      machineLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      machineOptions: [],
      machinePageSize: 100,
      machinePageNum: 1,
      machineHasMore: true,
      machineQuery: "",
      machineScrollWrap: null,
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        lhResultBatchNo: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        orderNo: [
          {
            required: false,
            trigger: "change",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        mouldCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  beforeDestroy() {
    this.unbindMachineScroll();
  },
  computed: {
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "lhResultBatchNo",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhResultBatchNo"),
          disabled: this.isEdit,
          maxlength: 64,
        },
        {
          prop: "orderNo",
          label: this.$t("ui.data.column.lhMouldChangePlan.orderNo"),
          disabled: true,
          placeholder: "系统默认生成",
          maxlength: 64,
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.lhMouldChangePlan.planDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "scheduleDate",
          label: this.$t("ui.data.column.lhMouldChangePlan.scheduleDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "planOrder",
          label: this.$t("ui.data.column.lhMouldChangePlan.planOrder"),
          type: "number",
          min: 0,
          precision: 0,
        },
        {
          prop: "classIndex",
          label: this.$t("ui.data.column.lhMouldChangePlan.classIndex"),
          type: "select",
          dictData: this.parentDict.type.CLASS_NUM,
          filterable: true,
        },
        {
          prop: "leftRightMould",
          label: this.$t("ui.data.column.lhMouldChangePlan.leftRightMould"),
          maxlength: 32,
        },
        {
          prop: "lhMachineCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineCode"),
          type: "select",
          dictData: this.machineOptions,
          props: {
            label: "machineCode",
            value: "machineCode",
          },
          filterable: true,
          remote: true,
          remoteMethod: this.remoteMachineMethod,
          loading: this.machineLoading,
          onFocus: this.handleMachineFocus,
          onVisibleChange: this.handleMachineDropdownVisibleChange,
          popperClass: "lh-mould-change-machine-select-dropdown",
          listeners: {
            change: this.handleMachineChange,
          },
        },
        {
          prop: "beforeMaterialCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.beforeMaterialCode}
                v-model={form.beforeMaterialCode}
                onChange={this.handleBeforeMaterialChange}
              />
            );
          },
        },
        {
          prop: "beforeMaterialDesc",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialDesc"),
          disabled: true,
          maxlength: 128,
        },
        {
          prop: "changeMouldType",
          label: this.$t("ui.data.column.lhMouldChangePlan.changeMouldType"),
          type: "select",
          dictData: this.parentDict.type.CHANGE_MOULD_TYPE,
          filterable: true,
        },
        {
          prop: "afterMaterialCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.afterMaterialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.afterMaterialCode}
                v-model={form.afterMaterialCode}
                onChange={this.handleAfterMaterialChange}
              />
            );
          },
        },
        {
          prop: "afterMaterialDesc",
          label: this.$t("ui.data.column.lhMouldChangePlan.afterMaterialDesc"),
          disabled: true,
          maxlength: 128,
        },
        {
          prop: "changeTime",
          label: this.$t("ui.data.column.lhMouldChangePlan.changeTime"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldCode"),
          maxlength: 64,
        },
        {
          prop: "isRelease",
          label: this.$t("ui.data.column.lhMouldChangePlan.isRelease"),
          type: "select",
          dictData: this.parentDict.type.IS_RELEASE,
          filterable: true,
          disabled: true,
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldStatus"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
          filterable: true,
          disabled: true,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 500,
        },
      ];
    },
  },
  methods: {
    async queryMachineList(append = false) {
      this.machineLoading = true;
      try {
        const res = await getMachineList({
          machineCode: this.machineQuery,
          pageNum: this.machinePageNum,
          pageSize: this.machinePageSize,
        });
        const currentList = res.data || res || [];
        if (append) {
          const optionMap = new Map();
          [...this.machineOptions, ...currentList].forEach((item) => {
            optionMap.set(item.machineCode, item);
          });
          this.machineOptions = Array.from(optionMap.values());
        } else {
          this.machineOptions = currentList;
        }
        this.machineHasMore = currentList.length >= this.machinePageSize;
      } catch (error) {
        console.log(error);
      } finally {
        this.machineLoading = false;
      }
    },
    async remoteMachineMethod(query) {
      this.machineQuery = (query || "").trim();
      this.machinePageNum = 1;
      this.machineHasMore = true;
      await this.queryMachineList(false);
    },
    async loadMoreMachineList() {
      if (this.machineLoading || !this.machineHasMore) {
        return;
      }
      this.machinePageNum += 1;
      await this.queryMachineList(true);
    },
    handleMachineScroll(event) {
      if (this.machineLoading || !this.machineHasMore) {
        return;
      }
      const wrap = event.target;
      const reachedBottom =
        wrap.scrollTop + wrap.clientHeight >= wrap.scrollHeight - 20;
      if (reachedBottom) {
        this.loadMoreMachineList();
      }
    },
    bindMachineScroll() {
      this.$nextTick(() => {
        const wrap = document.querySelector(
          ".lh-mould-change-machine-select-dropdown .el-select-dropdown__wrap"
        );
        if (!wrap) {
          return;
        }
        this.unbindMachineScroll();
        this.machineScrollWrap = wrap;
        this.machineScrollWrap.addEventListener(
          "scroll",
          this.handleMachineScroll,
          { passive: true }
        );
      });
    },
    unbindMachineScroll() {
      if (!this.machineScrollWrap) {
        return;
      }
      this.machineScrollWrap.removeEventListener("scroll", this.handleMachineScroll);
      this.machineScrollWrap = null;
    },
    handleMachineDropdownVisibleChange(visible) {
      if (visible) {
        this.bindMachineScroll();
        return;
      }
      this.unbindMachineScroll();
    },
    handleMachineFocus() {
      this.remoteMachineMethod(this.machineQuery || "");
    },
    handleMachineChange(val) {
      if (val) {
        const item = this.machineOptions.find((i) => i.machineCode === val);
        if (item) {
          this.$set(this.form, "lhMachineName", item.machineName || val);
        }
      } else {
        this.$set(this.form, "lhMachineName", "");
      }
    },
    handleBeforeMaterialChange(val, row) {
      if (val) {
        this.$set(this.form, "beforeMaterialDesc", (row && row.materialDesc) || "");
      } else {
        this.$set(this.form, "beforeMaterialDesc", "");
      }
    },
    handleAfterMaterialChange(val, row) {
      if (val) {
        this.$set(this.form, "afterMaterialDesc", (row && row.materialDesc) || "");
      } else {
        this.$set(this.form, "afterMaterialDesc", "");
      }
    },

    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editLhMouldChangePlan(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    // utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
        if (data.isRelease === "1") {
          this.$set(this.form, "isRelease", "4");
          this.$set(this.form, "mouldStatus", "0");
        }
        if (data.lhMachineCode) {
          this.machineOptions = [
            {
              machineCode: data.lhMachineCode,
              machineName: data.lhMachineName || data.lhMachineCode,
            },
          ];
        }
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
          isRelease: "0",
          mouldStatus: "0",
        };
        this.machineOptions = [];
      }
      this.machinePageNum = 1;
      this.machineHasMore = true;
      this.machineQuery = "";
    },
    hide() {
      this.unbindMachineScroll();
      this.form = {};
      this.machineOptions = [];
      this.machinePageNum = 1;
      this.machineHasMore = true;
      this.machineQuery = "";
      this.$refs.form && this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      if (!this.form.scheduleDate) {
        this.$modal.msgError(
          this.$t("ui.data.alert.lhMouldChangePlan.scheduleDateRequired")
        );
        return;
      }
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
