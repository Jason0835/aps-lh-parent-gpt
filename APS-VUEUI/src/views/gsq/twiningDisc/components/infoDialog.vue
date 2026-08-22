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
      <!-- 主表表单（单表维护，规格关系/机台关系独立页面管理） -->
      <info-form
        class="form-item-height"
        ref="form"
        :form="form"
        :rules="rules"
        :columns="mainColumns"
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
  saveTwiningDisc,
  getTwiningDiscInfo,
} from "@/api/gsq/twiningDisc";

export default {
  dicts: ["sys_normal_disable", "biz_factory_name", "lh_precision_data_source"],
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        twiningDiscCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        twiningDiscName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        proSize: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sortType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
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
        this.$t("ui.data.column.gsq.twiningDisc.modalName")
      );
    },
    mainColumns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.factoryCode"),
          prop: "factoryCode",
          span: 12,
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.twiningDiscCode"),
          prop: "twiningDiscCode",
          span: 12,
          required: true,
          type: "input",
          maxlength: 50,
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.twiningDiscName"),
          prop: "twiningDiscName",
          span: 12,
          required: true,
          type: "input",
          maxlength: 100,
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.proSize"),
          prop: "proSize",
          span: 12,
          required: true,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.sortType"),
          prop: "sortType",
          span: 12,
          required: true,
          type: "input",
          maxlength: 50,
          placeholder: "3-4-5-4-3",
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.qty"),
          prop: "qty",
          span: 12,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.status"),
          prop: "status",
          span: 12,
          type: "select",
          dictData: this.dict.type.sys_normal_disable,
          filterable: true,
        },
        {
          // 数据来源（字典lh_precision_data_source）：系统维护字段，0-MES同步，1-手工维护，只读展示
          label: this.$t("ui.data.column.gsq.twiningDisc.dataSource"),
          prop: "dataSource",
          span: 12,
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
     * 保存缠绕盘主表数据（单表保存）
     * @param {Object} params 表单数据
     */
    async save(params) {
      try {
        this.loading = true;
        const res = await saveTwiningDisc(params);
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
      if (data && data.id) {
        this.isEdit = true;
        try {
          this.loading = true;
          // 获取详细信息（单表）
          const res = await getTwiningDiscInfo(data.id);
          const detail = res.data || res;
          this.form = {
            ...detail,
          };
        } catch (error) {
          console.error(error);
          this.form = { ...data };
        } finally {
          this.loading = false;
        }
      } else {
        this.isEdit = false;
        this.form = {
          // 工厂默认越南工厂116，用户可切换
          factoryCode: "116",
          status: "0",
          // 数据来源默认1-手工维护（字典lh_precision_data_source）
          dataSource: "1",
          subList: [],
        };
      }
    },
    hide() {
      // 先关闭弹窗再重置表单，避免resetFields在弹窗可见时记录错误初始值
      this.visible = false;
      this.isEdit = false;
      this.form = {};
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
