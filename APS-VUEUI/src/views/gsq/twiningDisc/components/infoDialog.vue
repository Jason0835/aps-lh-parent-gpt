<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="900px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div v-loading="loading">
      <!-- 主表表单 -->
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

      <!-- 子表明细：钢丝圈列表 -->
      <div class="sub-section">
        <div class="sub-header">
          <span class="sub-title">{{ $t("ui.data.column.gsq.twiningDisc.subTitle") }}</span>
          <el-button
            type="primary"
            plain
            size="mini"
            icon="el-icon-plus"
            @click="addSubRow"
          >{{ $t("ui.frame.btn.add") }}</el-button>
        </div>
        <el-table
          :data="form.subList"
          border
          size="mini"
          max-height="300"
          style="width: 100%"
        >
          <el-table-column
            type="index"
            :label="$t('ui.data.column.gsq.twiningDisc.subIndex')"
            width="50"
            align="center"
          />
          <el-table-column
            :label="$t('ui.data.column.gsq.twiningDisc.steelRingCode')"
            min-width="160"
            align="center"
          >
            <template slot-scope="scope">
              <el-input
                v-model="scope.row.steelRingCode"
                :placeholder="$t('ui.data.column.gsq.twiningDisc.steelRingCode')"
                size="mini"
              />
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('ui.data.column.gsq.twiningDisc.steelRingName')"
            min-width="160"
            align="center"
          >
            <template slot-scope="scope">
              <el-input
                v-model="scope.row.steelRingName"
                :placeholder="$t('ui.data.column.gsq.twiningDisc.steelRingName')"
                size="mini"
                maxlength="30"
                @input="handleSteelRingNameInput(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('ui.common.column.remark')"
            min-width="160"
            align="center"
          >
            <template slot-scope="scope">
              <el-input
                v-model="scope.row.remark"
                :placeholder="$t('ui.common.column.remark')"
                size="mini"
              />
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('ui.data.btn.option')"
            width="80"
            align="center"
            fixed="right"
          >
            <template slot-scope="scope">
              <el-button
                type="danger"
                plain
                size="mini"
                @click="deleteSubRow(scope.$index)"
              >{{ $t("ui.frame.btn.delete") }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
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
  dicts: ["sys_normal_disable"],
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
        subList: [],
      },
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
          label: this.$t("ui.data.column.gsq.twiningDisc.status"),
          prop: "status",
          span: 12,
          type: "select",
          dictData: this.dict.type.sys_normal_disable,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.proSize"),
          prop: "proSize",
          span: 12,
          required: true,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.gsq.twiningDisc.qty"),
          prop: "qty",
          span: 12,
          type: "number",
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
     * 处理钢丝圈名称输入，限制最多30个汉字，超过则截断
     * @param {Object} row 子表行数据
     */
    handleSteelRingNameInput(row) {
      if (row.steelRingName && row.steelRingName.length > 30) {
        row.steelRingName = row.steelRingName.substring(0, 30);
      }
    },
    /**
     * 新增子表行
     */
    addSubRow() {
      if (!this.form.subList) {
        this.$set(this.form, "subList", []);
      }
      this.form.subList.push({
        steelRingCode: "",
        steelRingName: "",
        remark: "",
      });
    },
    /**
     * 删除子表行
     * @param {Number} index 行索引
     */
    deleteSubRow(index) {
      this.form.subList.splice(index, 1);
    },
    /**
     * 保存主子表数据
     * @param {Object} params 表单数据（含 subList）
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
          // 获取详细信息（含子表明细及反显）
          const res = await getTwiningDiscInfo(data.id);
          const detail = res.data || res;
          this.form = {
            ...detail,
            subList: detail.subList || [],
          };
        } catch (error) {
          console.error(error);
          this.form = { ...data, subList: [] };
        } finally {
          this.loading = false;
        }
      } else {
        this.isEdit = false;
        this.form = {
          status: "0",
          subList: [],
        };
      }
    },
    hide() {
      this.form = { subList: [] };
      if (this.$refs.form) {
        this.$refs.form.triggerResetForm();
      }
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>

<style lang="scss" scoped>
.sub-section {
  margin-top: 16px;

  .sub-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .sub-title {
      font-size: 14px;
      font-weight: bold;
    }
  }
}
</style>
