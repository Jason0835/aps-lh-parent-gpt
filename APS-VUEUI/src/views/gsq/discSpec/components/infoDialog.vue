<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="700px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div v-loading="loading">
      <info-form
        class="form-item-height"
        ref="form"
        :form="form"
        :rules="rules"
        :columns="formColumns"
        label-position="right"
        label-width="120px"
      >
      </info-form>
    </div>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import {
  saveDiscSpec,
  getDiscSpecInfo,
  listSteelRingOptions,
} from "@/api/gsq/discSpec";
import { listTwiningDisc } from "@/api/gsq/twiningDisc";

export default {
  dicts: ["sys_normal_disable", "biz_factory_name", "lh_precision_data_source"],
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      // 缠绕盘下拉选项（启用状态的缠绕盘主表数据）
      discOptions: [],
      // 钢丝圈下拉选项（施工信息表去重数据）
      steelRingOptions: [],
      // 表单默认值：工厂116/状态0(启用)/数据来源1(手工维护，字典lh_precision_data_source)
      // 必须在data()中声明所有字段，避免Vue2响应式属性丢失
      form: {
        factoryCode: "116",
        status: "0",
        dataSource: "1",
      },
      rules: {
        twiningDiscCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        steelRingCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.gsq.discSpec.modalName")
      );
    },
    formColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.twiningDiscCode"),
          prop: "twiningDiscCode",
          span: 24,
          required: true,
          type: "select",
          filterable: true,
          clearable: true,
          placeholder: this.$t("ui.data.column.gsq.discMachine.discPlaceholder"),
          options: this.discOptions.map((item) => ({
            label:
              item.twiningDiscCode +
              (item.twiningDiscName ? " - " + item.twiningDiscName : ""),
            value: item.twiningDiscCode,
          })),
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.steelRingCode"),
          prop: "steelRingCode",
          span: 24,
          required: true,
          type: "select",
          filterable: true,
          clearable: true,
          placeholder: this.$t("ui.data.column.gsq.discSpec.steelRingPlaceholder"),
          options: this.steelRingOptions.map((item) => ({
            label:
              item.BEAD_CODE +
              (item.BEAD_NAME ? " - " + item.BEAD_NAME : ""),
            value: item.BEAD_CODE,
          })),
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.steelRingName"),
          prop: "steelRingName",
          span: 24,
          type: "input",
          // 钢丝圈名称为空时由后端按编号从施工信息表反显，选完编号自动带出可修改
          placeholder: this.$t("ui.data.column.gsq.discSpec.steelRingNamePlaceholder"),
        },
        {
          label: this.$t("ui.data.column.gsq.discSpec.status"),
          prop: "status",
          span: 24,
          type: "select",
          dictData: this.dict.type.sys_normal_disable,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gsq.discSpec.factoryCode"),
          prop: "factoryCode",
          span: 24,
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          // 数据来源（字典lh_precision_data_source）：系统维护字段，MES同步/手工，只读展示
          label: this.$t("ui.data.column.gsq.discSpec.dataSource"),
          prop: "dataSource",
          span: 24,
          type: "select",
          dictData: this.dict.type.lh_precision_data_source,
          filterable: true,
          disabled: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "900",
        },
      ];
    },
  },
  methods: {
    /**
     * 加载缠绕盘下拉选项（启用状态的缠绕盘主表数据）
     */
    async loadDiscOptions() {
      if (this.discOptions.length > 0) {
        return;
      }
      try {
        const res = await listTwiningDisc({
          status: "0",
          pageNum: 1,
          pageSize: 9999,
        });
        this.discOptions = res.rows || [];
      } catch (error) {
        console.error(error);
      }
    },
    /**
     * 加载钢丝圈下拉选项（施工信息表去重数据）
     */
    async loadSteelRingOptions() {
      if (this.steelRingOptions.length > 0) {
        return;
      }
      try {
        const res = await listSteelRingOptions();
        this.steelRingOptions = res.data || res || [];
      } catch (error) {
        console.error(error);
      }
    },
    /**
     * 保存规格关系
     * @param {Object} params 表单数据
     */
    async save(params) {
      try {
        this.loading = true;
        const res = await saveDiscSpec(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    /**
     * 打开弹窗
     * @param {Object} data 编辑时传入行数据，新增时不传
     */
    async show(data) {
      this.visible = true;
      // 并行加载缠绕盘与钢丝圈下拉选项
      this.loadDiscOptions();
      this.loadSteelRingOptions();
      if (data && data.id) {
        this.isEdit = true;
        try {
          this.loading = true;
          const res = await getDiscSpecInfo(data.id);
          this.form = { ...(res.data || res) };
        } catch (error) {
          console.error(error);
          this.form = { ...data };
        } finally {
          this.loading = false;
        }
      } else {
        this.isEdit = false;
        // 新增：重置为默认值
        this.form = {
          factoryCode: "116",
          status: "0",
          dataSource: "1",
        };
      }
    },
    hide() {
      // 先关闭弹窗再重置表单，避免resetFields在弹窗可见时记录错误初始值（与cxStock模式一致）
      this.visible = false;
      this.isEdit = false;
      this.form = {
        factoryCode: "116",
        status: "0",
        dataSource: "1",
      };
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.triggerResetForm();
        }
      });
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
