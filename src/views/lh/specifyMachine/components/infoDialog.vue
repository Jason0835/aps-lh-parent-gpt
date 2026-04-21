<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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
  editLhSpecifyMachine,
  getLhMachineList,
} from "@/api/lh/lhSpecifyMachine";

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
        specCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        jobType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
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
          prop: "specCode",
          label: this.$t("ui.data.column.lhSpecifyMachine.specCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.specCode}
                v-model={form.specCode}
                onChange={this.handleSpecCodeChange}
              />
            );
          },
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.lhSpecifyMachine.materialDesc"),
          disabled: true,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.lhSpecifyMachine.machineCode"),
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
          popperClass: "lh-specify-machine-select-dropdown",
        },
        {
          prop: "jobType",
          label: this.$t("ui.data.column.lhSpecifyMachine.jobType"),
          type: "select",
          dictData: this.parentDict.type.JOB_TYPE,
          filterable: true,
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          type: "textarea",
          rows: 3,
          maxlength: 300,
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;

        const res = await editLhSpecifyMachine(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async queryMachineList(append = false) {
      this.machineLoading = true;
      try {
        const res = await getLhMachineList({
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
          ".lh-specify-machine-select-dropdown .el-select-dropdown__wrap"
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
    handleSpecCodeChange(value, row) {
      this.$set(this.form, "specCode", value);
      this.$set(this.form, "materialDesc", (row && row.materialDesc) || "");
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
        if (data.machineCode) {
          this.machineOptions = [
            {
              machineCode: data.machineCode,
              machineName: data.machineName || data.machineCode,
            },
          ];
        }
      } else {
        this.form = {
          factoryCode: "116",
          materialDesc: "",
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
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
